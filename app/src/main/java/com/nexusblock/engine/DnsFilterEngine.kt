package com.nexusblock.engine

import android.content.Context
import android.util.Log
import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.db.CustomRuleDao
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.StatsRepository
import com.nexusblock.engine.dns.DnsProfileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import org.xbill.DNS.*
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified DNS filter engine that delegates the heavy ad-blocking work to
 * the upstream filtered DNS resolver (AdGuard DNS, Cloudflare Family, etc.).
 *
 * Local blocking is now limited to:
 * 1. **User custom rules** — always respected.
 * 2. **Indian OTT emergency rules** — lightweight hardcoded list for regional
 *    streaming apps whose ad domains may not be in global DNS blocklists.
 * 3. **DoH/DoT bypass prevention** — blocking known public resolver hostnames
 *    so apps cannot tunnel around our filter.
 *
 * Why this is better:
 * - AdGuard DNS blocks ~3M+ ad/tracker/malware domains AND updates daily.
 * - No more stale local blocklists consuming memory and missing new domains.
 * - No more "Albania Mode" hacks or YouTube whitelist wars.
 * - Battery efficient: single upstream query instead of local trie + bloom
 *   filter lookups on every DNS packet.
 *
 * The upstream resolver choice is managed by [DnsProfileManager] and can be
 * refreshed remotely every ~2 months.
 */
@Singleton
class DnsFilterEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statsRepo: StatsRepository,
    private val customRuleDao: CustomRuleDao,
    private val blocklistRepo: BlocklistRepository,
    private val settingsRepo: SettingsRepository,
    private val dnsProfileManager: DnsProfileManager,
    private val okHttpClient: OkHttpClient,
    private val connectionTracker: ConnectionTracker
) {
    companion object {
        private const val TAG = "NexusBlock/DNS"
        private const val DNS_PORT = 53
        private const val BUFFER_SIZE = 4096
        private const val NEGATIVE_CACHE_TTL_MS = 60_000L
        private const val CLOUDFRONT_IP_TTL_MS = 600_000L // 10 min

        /**
         * Upstream resolver domains that must NEVER be blocked, otherwise
         * DoH/DoT bootstrap self-sabotages and all resolution stops.
         */
        private val UPSTREAM_DOMAINS = setOf(
            "cloudflare-dns.com", "dns.cloudflare.com", "one.one.one.one",
            "dns.google", "dns.google.com",
            "dns.quad9.net", "dns9.quad9.net", "dns10.quad9.net",
            "dns11.quad9.net", "dns12.quad9.net",
            "dns.adguard-dns.com", "family.adguard-dns.com",
            "unfiltered.adguard-dns.com",
            "doh.opendns.com", "dns.nextdns.io",
            "doh.cleanbrowsing.org",
            "freedns.controld.com", "p1.freedns.controld.com"
        )

        fun isUpstreamDomain(host: String): Boolean {
            val h = host.trimEnd('.').lowercase()
            return UPSTREAM_DOMAINS.any { h == it || h.endsWith(".$it") }
        }
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var ruleEngine = RuleEngine()
    @Volatile
    private var criticalAllowSet = HashSet<String>()
    @Volatile
    private var criticalSubdomainSet = HashSet<String>()
    private var upstreamManager: DnsUpstreamManager? = null
    private var startJob: Job? = null
    private var customRulesJob: Job? = null

    private val dnsCache = object : LinkedHashMap<String, Message>(4096, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Message>?): Boolean = size > 4000
    }

    // Negative cache: NXDOMAIN/SERVFAIL responses cached for 60s to prevent
    // repeated upstream queries for blocked/failed domains (apps retry 3-5x).
    private val negativeDnsCache = object : LinkedHashMap<String, Long>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 1000
    }

    private val blockedIpCache = object : LinkedHashMap<String, Long>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 500
    }

    // CloudFront IP cache: IPs resolved from *.cloudfront.net are cached here.
    // PacketRouter uses this to force QUIC→TCP downgrade (block UDP/443) so
    // that TLS SNI inspection can catch ad-serving CloudFront distributions.
    private val cloudFrontIpCache = ConcurrentHashMap<String, Long>(256)

    // Content CDN whitelist: CloudFront distribution IDs discovered from
    // CNAME chains of critical-allow DNS responses. Only these distributions
    // are permitted at the SNI level — all others are blocked as suspected
    // ad creative CDNs (Prime Video SSAI defeat).
    private val contentCdnDistributions = java.util.Collections.synchronizedSet(HashSet<String>())

    private val pendingLogs = mutableListOf<com.nexusblock.data.model.BlockedEvent>()
    private var logJob: Job? = null

    private var queriesProcessed = 0L
    private var queriesBlocked = 0L
    private var queriesAllowed = 0L

    init {
        customRulesJob = scope.launch {
            customRuleDao.observeEnabled()
                .drop(1)
                .collect {
                    if (_isRunning.value) {
                        Log.i(TAG, "Custom rules changed, reloading DNS rules")
                        reloadRules()
                    }
                }
        }
    }

    fun start(localAddress: InetAddress = InetAddress.getByName("127.0.0.1"), localPort: Int = DNS_PORT) {
        if (_isRunning.value || startJob?.isActive == true) return
        startJob = scope.launch {
            upstreamManager = DnsUpstreamManager(okHttpClient, dnsProfileManager)
            reloadRules()
            // If no rules loaded from DB (first launch), load emergency rules immediately
            if (ruleEngine.ruleCount() == 0) {
                Log.i(TAG, "No DB rules found, loading built-in emergency rules directly")
                val builtins = com.nexusblock.data.repository.BuiltInBlockRules.emergencyRules()
                val newRuleEngine = RuleEngine()
                newRuleEngine.loadRules(builtins)
                ruleEngine = newRuleEngine
            }
            startLogBatcher()
            _isRunning.value = true
            Log.i(TAG, "DNS engine started with upstream: ${dnsProfileManager.activeProfile().name}")
        }
    }

    fun stop() {
        _isRunning.value = false
        startJob?.cancel()
        startJob = null
        logJob?.cancel()
        clearCache()
        Log.i(TAG, "DNS engine stopped")
    }

    fun clearCache() {
        synchronized(dnsCache) { dnsCache.clear() }
        synchronized(negativeDnsCache) { negativeDnsCache.clear() }
        Log.d(TAG, "DNS cache cleared")
    }

    /**
     * Resolve a DNS query. Delegates to the upstream filtered resolver
     * unless the domain matches a local custom rule or emergency block.
     */
    suspend fun resolveDns(queryPayload: ByteArray): ByteArray? {
        // Bound the wait for engine startup — never block an app's DNS query
        // for more than ~750ms. If the engine is still warming up we just
        // skip local rule evaluation and pass through to upstream.
        if (startJob?.isActive == true) {
            withTimeoutOrNull(750) { startJob?.join() }
        }

        val query = try { Message(queryPayload) } catch (e: Exception) {
            Log.w(TAG, "Failed to parse DNS query", e)
            return upstreamResolve(queryPayload) ?: buildServFailFromBytes(queryPayload)
        }

        if (query.header.opcode != Opcode.QUERY) {
            return upstreamResolve(queryPayload)
        }

        val question = query.question ?: return upstreamResolve(queryPayload)
        val host = question.name.toString(true).lowercase()
        val type = question.type
        val cacheKey = "$host|$type"

        queriesProcessed++

        // Block HTTPS/SVCB records to prevent QUIC/H3 discovery
        if ((type == 65 || type == 64) && !isUpstreamDomain(host)) {
            // Block SVCB for all ad domains
            if (ruleEngine.isBlocked(host)) {
                queriesBlocked++
                queueLog(host, "dns-svcb")
                return buildSinkholeResponse(query, type).toWire()
            }
            // Also block SVCB for ALL streaming CDN domains — prevents HTTP/3
            // negotiation so traffic stays on TCP where SNI inspection works.
            // Critical for Prime Video SSAI defeat.
            if (host.endsWith(".cloudfront.net") ||
                host.endsWith(".aiv-cdn.net") ||
                host.endsWith(".aiv-delivery.net")) {
                queriesBlocked++
                queueLog(host, "dns-svcb-cdn")
                return buildSinkholeResponse(query, type).toWire()
            }
        }

        // 1. Check cache
        synchronized(dnsCache) {
            val cached = dnsCache[cacheKey]
            if (cached != null) {
                val response = Message(query.header.id)
                response.header.setFlag(Flags.QR.toInt())
                response.header.setFlag(Flags.RA.toInt())
                response.addRecord(question, Section.QUESTION)
                for (record in cached.getSectionArray(Section.ANSWER)) {
                    response.addRecord(record, Section.ANSWER)
                }
                queriesAllowed++
                return response.toWire()
            }
        }

        // 1b. Negative cache — instant SERVFAIL for domains that recently
        // failed upstream. Prevents app retry storms (3-5 retries × 200ms+).
        synchronized(negativeDnsCache) {
            val ts = negativeDnsCache[cacheKey]
            if (ts != null && System.currentTimeMillis() - ts < NEGATIVE_CACHE_TTL_MS) {
                return buildServFail(query)
            }
        }

        // 2. Critical allows — bypass ALL blocking (local + upstream) and use
        //    plain unfiltered DNS so video playback isn't broken by AdGuard.
        //    Only EXACT matches and non-blocked subdomains are allowed through.
        val isCritical = criticalAllowSet.contains(host) ||
            criticalSubdomainSet.any { host.endsWith(".$it") }
        if (!isUpstreamDomain(host) && isCritical && !ruleEngine.isBlocked(host)) {
            val plainResponse = plainDnsResolve(queryPayload)
            if (plainResponse != null) {
                Log.d(TAG, "DNS critical-allow bypass: $host")
                cacheAllowedResponse(cacheKey, plainResponse)
                trackCloudFrontIps(plainResponse)
                queriesAllowed++
                return plainResponse
            }
        }

        // 3. Local rule check (custom rules + blocklist domains)
        if (!isUpstreamDomain(host) && ruleEngine.isBlocked(host)) {
            queriesBlocked++
            Log.i(TAG, "DNS sinkhole (local): $host")
            val response = buildSinkholeResponse(query, type)
            queueLog(host, "dns")
            return response.toWire()
        }

        // 4. Everything else → upstream filtered resolver (AdGuard, etc.)
        val upstreamResponse = upstreamResolve(queryPayload)
        if (upstreamResponse == null) {
            // Cache the failure so retries are instant
            synchronized(negativeDnsCache) {
                negativeDnsCache[cacheKey] = System.currentTimeMillis()
            }
            return buildServFail(query)
        }

        // Check CNAME chain for local blocks
        val blockedAlias = findBlockedAnswer(upstreamResponse, host)
        if (blockedAlias != null) {
            queriesBlocked++
            queueLog("$host -> $blockedAlias", "dns-cname")
            return buildSinkholeResponse(query, type).toWire()
        }

        cacheAllowedResponse(cacheKey, upstreamResponse)
        trackCloudFrontIps(upstreamResponse)
        queriesAllowed++
        return upstreamResponse
    }

    /**
     * Build a SERVFAIL response so the caller fails fast instead of hanging.
     * Returning null from resolveDns causes the original DNS packet to be
     * dropped silently — apps then wait the full 5 s socket timeout, making
     * the system feel frozen. SERVFAIL prompts immediate retry / fallback.
     */
    private fun buildServFail(query: Message): ByteArray {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.SERVFAIL
        query.question?.let { response.addRecord(it, Section.QUESTION) }
        return response.toWire()
    }

    private fun buildServFailFromBytes(queryPayload: ByteArray): ByteArray? = try {
        buildServFail(Message(queryPayload))
    } catch (_: Exception) {
        null
    }

    private suspend fun upstreamResolve(queryPayload: ByteArray): ByteArray? {
        return try {
            withContext(Dispatchers.IO) {
                upstreamManager?.resolve(queryPayload)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstream resolution error", e)
            null
        }
    }

    /**
     * Fallback plain-DNS resolver for critical-allowed domains that the
     * configured filtered upstream (AdGuard, etc.) may block.
     *
     * IMPORTANT: We delegate to [DnsUpstreamManager.resolveWithPlainFallback]
     * which uses VPN-protected sockets. A naive `SimpleResolver("8.8.8.8")`
     * would open an unprotected `DatagramSocket`, and because 8.8.8.8 is in
     * our DNS bypass capture routes the packet would loop back into the TUN
     * → recursive resolution → all apps lose internet.
     */
    private suspend fun plainDnsResolve(queryPayload: ByteArray): ByteArray? {
        return try {
            upstreamManager?.resolvePlainFallbackProtected(queryPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Plain DNS resolution error", e)
            null
        }
    }

    private fun cacheAllowedResponse(cacheKey: String, responseBytes: ByteArray) {
        try {
            val response = Message(responseBytes)
            if (response.rcode == Rcode.NOERROR) {
                synchronized(dnsCache) { dnsCache[cacheKey] = response }
            }
        } catch (_: Exception) {}
    }

    /**
     * Extract IPs from DNS responses that contain CNAME chains resolving to
     * *.cloudfront.net. These IPs are cached so that PacketRouter can force
     * QUIC→TCP downgrade, giving us SNI visibility on ad-serving CloudFront
     * distributions.
     *
     * Also populates [contentCdnDistributions] — when a critical-allow domain
     * CNAMEs to a CloudFront distribution, that distribution is whitelisted
     * as a content CDN. PacketRouter's SNI handler uses this to default-deny
     * unknown CloudFront distributions (suspected Prime Video ad creatives).
     */
    private fun trackCloudFrontIps(responseBytes: ByteArray) {
        try {
            val response = Message(responseBytes)
            var hasCloudFrontCname = false
            for (record in response.getSectionArray(Section.ANSWER)) {
                if (record is CNAMERecord) {
                    val target = record.target.toString(true).lowercase().trimEnd('.')
                    if (target.endsWith(".cloudfront.net")) {
                        hasCloudFrontCname = true
                        // Whitelist this distribution as content CDN
                        val distId = target.substringBefore(".cloudfront.net")
                        if (distId.isNotEmpty()) {
                            contentCdnDistributions.add(distId)
                            Log.d(TAG, "Content CDN discovered: $distId.cloudfront.net")
                        }
                    }
                }
            }
            // If ANY record in the chain touches CloudFront, cache all A/AAAA IPs
            // as "force-tcp" targets. This also applies when the queried host IS
            // a *.cloudfront.net directly (the A records themselves).
            val queriedHost = response.question?.name?.toString(true)?.lowercase()?.trimEnd('.') ?: ""
            if (hasCloudFrontCname || queriedHost.endsWith(".cloudfront.net")) {
                val now = System.currentTimeMillis()
                for (record in response.getSectionArray(Section.ANSWER)) {
                    when (record) {
                        is ARecord -> {
                            record.address.hostAddress?.let { ip ->
                                cloudFrontIpCache[ip] = now
                            }
                        }
                        is AAAARecord -> {
                            record.address.hostAddress?.let { ip ->
                                cloudFrontIpCache[ip] = now
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Returns true if the given IP was resolved from a *.cloudfront.net domain.
     * PacketRouter uses this to block UDP/443 (QUIC) to these IPs, forcing
     * the app to fall back to TCP/443 where we can inspect TLS ClientHello SNI
     * and block ad-serving CloudFront distributions.
     */
    fun shouldForceDowngradeQuic(ip: String): Boolean {
        val ts = cloudFrontIpCache[ip] ?: return false
        if (System.currentTimeMillis() - ts > CLOUDFRONT_IP_TTL_MS) {
            cloudFrontIpCache.remove(ip)
            return false
        }
        return true
    }

    /**
     * Returns true if [sni] is a CloudFront distribution that has NOT been
     * observed via CNAME from a critical-allow domain (i.e. it's not a known
     * content CDN). Used by PacketRouter to default-deny unknown CloudFront
     * distributions — these are suspected Prime Video SSAI ad creatives.
     *
     * Returns false (allow) if the whitelist is still empty (learning phase)
     * or if the distribution is known.
     */
    fun isUnknownCloudFrontDistribution(sni: String): Boolean {
        if (!sni.endsWith(".cloudfront.net")) return false
        // During learning phase (no content distributions discovered yet),
        // don't block — would break all CloudFront traffic.
        if (contentCdnDistributions.isEmpty()) return false
        val distId = sni.removeSuffix(".cloudfront.net")
        return !contentCdnDistributions.contains(distId)
    }

    private fun findBlockedAnswer(responseBytes: ByteArray, originalHost: String): String? {
        return try {
            val response = Message(responseBytes)
            for (record in response.getSectionArray(Section.ANSWER)) {
                val owner = record.name.toString(true).lowercase().trimEnd('.')
                if (owner != originalHost && ruleEngine.isBlocked(owner)) {
                    cacheBlockedIpsFromResponse(response)
                    return owner
                }

                when (record) {
                    is CNAMERecord -> {
                        val target = record.target.toString(true).lowercase().trimEnd('.')
                        if (ruleEngine.isBlocked(target)) {
                            cacheBlockedIpsFromResponse(response)
                            return target
                        }
                    }
                    is ARecord -> {
                        val address = record.address.hostAddress ?: continue
                        if (ruleEngine.isIpBlocked(address)) return address
                    }
                    is AAAARecord -> {
                        val address = record.address.hostAddress ?: continue
                        if (ruleEngine.isIpBlocked(address)) return address
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect DNS answer chain", e)
            null
        }
    }

    private fun cacheBlockedIpsFromResponse(response: Message) {
        val now = System.currentTimeMillis()
        for (record in response.getSectionArray(Section.ANSWER)) {
            when (record) {
                is ARecord -> {
                    record.address.hostAddress?.let { ip ->
                        synchronized(blockedIpCache) { blockedIpCache[ip] = now }
                    }
                }
                is AAAARecord -> {
                    record.address.hostAddress?.let { ip ->
                        synchronized(blockedIpCache) { blockedIpCache[ip] = now }
                    }
                }
            }
        }
    }

    suspend fun reloadRules() {
        val allRules = mutableListOf<BlockedDomain>()

        // 1. Custom user rules
        val customRules = customRuleDao.getEnabled()
        allRules.addAll(customRules.map { rule ->
            BlockedDomain(
                host = if (rule.isAllow) "@@${rule.rule}" else rule.rule,
                source = "custom",
                isRegex = rule.rule.startsWith("/") && rule.rule.endsWith("/"),
                regexPattern = if (rule.rule.startsWith("/") && rule.rule.endsWith("/"))
                    rule.rule.substring(1, rule.rule.length - 1) else null
            )
        })

        // 2. Critical allowlist — domains that must NEVER be blocked to keep apps functional.
        // These use plain DNS (bypassing filtered upstream) because AdGuard/Cloudflare
        // may independently block domains we need for video playback.
        // NOTE: googlevideo.com is NOT here — ad subdomains get caught by isAdVideoServer().
        // We only allow the specific content patterns through.
        val criticalAllows = listOf(
            // Amazon Prime Video APIs — blocking these breaks playback
            "api.eu-west-1.aiv-delivery.net",
            "api.us-east-1.aiv-delivery.net",
            "api.us-west-2.aiv-delivery.net",
            "manifest.aiv-delivery.net",
            // Google Play Services core (exact subdomains only — NOT blanket *.googleapis.com)
            "play.googleapis.com",
            "android.googleapis.com",
            "youtubei.googleapis.com",
            "firebaseinstallations.googleapis.com",
            "firebaseremoteconfig.googleapis.com",
            "fcmregistrations.googleapis.com",
            "www.googleapis.com",
            "oauth2.googleapis.com",
            "people-pa.googleapis.com",
            "content-autofill.googleapis.com",
            // Netflix content delivery
            "nflxvideo.net",
            "nflxso.net",
            // YouTube content delivery (not ads) — ad patterns intercepted by rules
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "i.ytimg.com",
            "yt3.ggpht.com",
            // Hotstar/JioHotstar content delivery
            "hotstar.com",
            "www.hotstar.com",
            "api.hotstar.com",
            "www.jiohotstar.com",
            "api.jiohotstar.com",
            // SonyLiv content delivery
            "www.sonyliv.com",
            "api.sonyliv.com",
            // Zee5 content delivery
            "www.zee5.com",
            "api.zee5.com"
        )
        // Subdomain-matching critical allows — these need *.domain matching
        // for content CDN subdomains.
        // IMPORTANT: Do NOT add broad domains like "googleapis.com" here — that
        // would whitelist ad-serving subdomains (imasdk.googleapis.com, etc.)
        // and let YouTube/app ads through unfiltered.
        val criticalSubdomains = listOf(
            "nflxvideo.net",
            "nflxso.net"
        )
        val newCriticalAllowSet = HashSet<String>()
        newCriticalAllowSet.addAll(criticalAllows)
        criticalAllowSet = newCriticalAllowSet
        val newSubdomainSet = HashSet<String>()
        newSubdomainSet.addAll(criticalSubdomains)
        criticalSubdomainSet = newSubdomainSet

        // 3. Downloaded blocklists from database (builtin + remote sources)
        val enabledSources = blocklistRepo.observeSourceStates().first()
            .filter { it.enabled }
        for (source in enabledSources) {
            val domains = blocklistRepo.getDomainsBySource(source.source)
            allRules.addAll(domains)
        }

        Log.i(TAG, "Loaded ${customRules.size} custom rules + ${criticalAllows.size} critical allows + ${allRules.size - customRules.size} blocklist domains from ${enabledSources.size} sources")

        val newRuleEngine = RuleEngine()
        newRuleEngine.loadRules(allRules)
        ruleEngine = newRuleEngine
        clearCache()
    }

    fun getStats(): DnsStats = DnsStats(
        processed = queriesProcessed,
        blocked = queriesBlocked,
        allowed = queriesAllowed
    )

    fun isDomainBlocked(host: String): Boolean = ruleEngine.isBlocked(host)
    fun isIpBlocked(ip: String): Boolean {
        if (ruleEngine.isIpBlocked(ip)) return true
        synchronized(blockedIpCache) {
            val ts = blockedIpCache[ip]
            if (ts != null && System.currentTimeMillis() - ts < 600_000) return true
        }
        return false
    }

    fun buildNxDomainBytes(queryPayload: ByteArray): ByteArray? = try {
        val query = Message(queryPayload)
        buildNxDomainResponse(query).toWire()
    } catch (_: Exception) { null }

    fun buildSinkholeBytes(queryPayload: ByteArray): ByteArray? = try {
        val query = Message(queryPayload)
        val type = query.question?.type ?: Type.A
        buildSinkholeResponse(query, type).toWire()
    } catch (_: Exception) { null }

    fun buildIPv6SinkholeBytes(queryPayload: ByteArray): ByteArray? = try {
        val query = Message(queryPayload)
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.NOERROR
        response.addRecord(query.question, Section.QUESTION)
        val sinkhole = InetAddress.getByName("::")
        response.addRecord(AAAARecord(query.question.name, DClass.IN, 300L, sinkhole), Section.ANSWER)
        response.toWire()
    } catch (_: Exception) { null }

    private fun buildNxDomainResponse(query: Message): Message {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.NXDOMAIN
        response.addRecord(query.question, Section.QUESTION)
        return response
    }

    private fun buildSinkholeResponse(query: Message, queryType: Int): Message {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.NOERROR
        response.addRecord(query.question, Section.QUESTION)
        val ttl = 300L
        when (queryType) {
            Type.A -> {
                val sinkhole = InetAddress.getByName("0.0.0.0")
                response.addRecord(ARecord(query.question.name, DClass.IN, ttl, sinkhole), Section.ANSWER)
            }
            Type.AAAA -> {
                val sinkhole = InetAddress.getByName("::")
                response.addRecord(AAAARecord(query.question.name, DClass.IN, ttl, sinkhole), Section.ANSWER)
            }
        }
        return response
    }

    private fun queueLog(host: String, type: String, appPackage: String? = null) {
        synchronized(pendingLogs) {
            pendingLogs.add(com.nexusblock.data.model.BlockedEvent(
                host = host, type = type, appPackage = appPackage
            ))
        }
        if (pendingLogs.size > 1000) {
            scope.launch(Dispatchers.IO) {
                val batch = synchronized(pendingLogs) {
                    val copy = pendingLogs.toList()
                    pendingLogs.clear()
                    copy
                }
                try { statsRepo.logBlockedBatch(batch) } catch (_: Exception) {}
            }
        }
    }

    private fun startLogBatcher() {
        logJob = scope.launch {
            while (isActive) {
                delay(5000)
                val batch = synchronized(pendingLogs) {
                    if (pendingLogs.isEmpty()) null
                    else {
                        val copy = pendingLogs.toList()
                        pendingLogs.clear()
                        copy
                    }
                }
                if (batch != null) {
                    try { statsRepo.logBlockedBatch(batch) } catch (_: Exception) {}
                }
            }
        }
    }

    data class DnsStats(
        val processed: Long,
        val blocked: Long,
        val allowed: Long
    )
}
