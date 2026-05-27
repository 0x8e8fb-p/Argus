package com.nexusblock.engine

import android.content.Context
import android.util.Log
import com.nexusblock.data.model.BlockedEvent
import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.db.CustomRuleDao
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.StatsRepository
import com.nexusblock.engine.dns.DnsProfileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import org.xbill.DNS.ARecord
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Opcode
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
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
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "Argus/DNS"
        private const val NEGATIVE_CACHE_TTL_MS = 60_000L
        private const val CLOUDFRONT_IP_TTL_MS = 600_000L // 10 min
        private const val LOG_CHANNEL_CAPACITY = 1024
        private const val LOG_BATCH_MAX = 100
        private const val LOG_BATCH_INTERVAL_MS = 5_000L
        private const val SINKHOLE_TTL = 300L
        private val SINKHOLE_IPV4: InetAddress = InetAddress.getByName("0.0.0.0")
        private val SINKHOLE_IPV6: InetAddress = InetAddress.getByName("::")

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

    // Cache stores raw wire-format bytes so the immutable payload is safe.
    // A new Message is parsed from bytes on every retrieval, preventing
    // mutation of shared objects.
    private val dnsCache = ConcurrentHashMap<String, ByteArray>(4096)

    // Negative cache: NXDOMAIN/SERVFAIL responses cached for 60s to prevent
    // repeated upstream queries for blocked/failed domains (apps retry 3-5x).
    private val negativeDnsCache = ConcurrentHashMap<String, Long>(1024)

    private val blockedIpCache = ConcurrentHashMap<String, Long>(512)

    // CloudFront IP cache: IPs resolved from *.cloudfront.net are cached here.
    // PacketRouter uses this to force QUIC→TCP downgrade (block UDP/443) so
    // that TLS SNI inspection can catch ad-serving CloudFront distributions.
    private val cloudFrontIpCache = ConcurrentHashMap<String, Long>(256)

    // Content CDN whitelist: CloudFront distribution IDs discovered from
    // CNAME chains of critical-allow DNS responses. Only these distributions
    // are permitted at the SNI level — all others are blocked as suspected
    // ad creative CDNs (Prime Video SSAI defeat).
    private val contentCdnDistributions = java.util.Collections.synchronizedSet(HashSet<String>())

    private val logChannel = Channel<BlockedEvent>(capacity = LOG_CHANNEL_CAPACITY)
    private val droppedLogCount = AtomicLong(0L)
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

    fun start(dnsAddress: InetAddress) {
        if (_isRunning.value || startJob?.isActive == true) return
        // Recreate scope in case it was cancelled by a previous stop()
        if (!scope.isActive) {
            // Cannot restart a cancelled scope; safe-guard by not recreating here
            // because scope is a val. Instead we ensure stop() never cancels the
            // top-level scope — it only cancels children.
        }
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
        // Cancel customRulesJob so it doesn't leak across restarts
        customRulesJob?.cancel()
        clearCache()
        Log.i(TAG, "DNS engine stopped")
    }

    fun clearCache() {
        dnsCache.clear()
        negativeDnsCache.clear()
        blockedIpCache.clear()
        cloudFrontIpCache.clear()
        contentCdnDistributions.clear()
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

        // 1. Check cache (raw bytes → new Message each time to avoid mutation)
        val cachedBytes = dnsCache[cacheKey]
        if (cachedBytes != null) {
            val response = Message(cachedBytes)
            response.header.id = query.header.id
            response.header.setFlag(Flags.QR.toInt())
            response.header.setFlag(Flags.RA.toInt())
            queriesAllowed++
            return response.toWire()
        }

        // 1b. Negative cache — instant SERVFAIL for domains that recently
        // failed upstream. Prevents app retry storms (3-5 retries × 200ms+).
        val negTs = negativeDnsCache[cacheKey]
        if (negTs != null && System.currentTimeMillis() - negTs < NEGATIVE_CACHE_TTL_MS) {
            return buildServFail(query)
        }
        // Prune stale negative entries opportunistically to avoid unbounded growth
        pruneNegativeCache()

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
                trackCloudFrontIps(plainResponse, whitelistDistribution = true)
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
            negativeDnsCache[cacheKey] = System.currentTimeMillis()
            return buildServFail(query)
        }

        // Check CNAME chain for local blocks
        val blockedAlias = findBlockedAnswer(upstreamResponse, host)
        if (blockedAlias != null) {
            queriesBlocked++
            queueLog("$host -> $blockedAlias", "dns-cname")
            return buildSinkholeResponse(query, type).toWire()
        }

        // DNS Rebinding Protection: block public domains resolving to private IPs
        if (isDnsRebinding(upstreamResponse)) {
            queriesBlocked++
            queueLog(host, "dns-rebinding")
            Log.w(TAG, "DNS rebinding blocked: $host")
            return buildServFail(query)
        }

        cacheAllowedResponse(cacheKey, upstreamResponse)
        trackCloudFrontIps(upstreamResponse, whitelistDistribution = false)
        queriesAllowed++
        return upstreamResponse
    }

    /**
     * Build a SERVFAIL response so the caller fails fast instead of hanging.
     * Returning null from resolveDns causes the original DNS packet to be
     * dropped silently — apps then wait the full 5 s socket timeout, making
     * the system feel frozen. SERVFAIL prompts immediate retry / fallback.
     */
    /**
     * DNS Rebinding Protection: inspect resolved A/AAAA records.
     * If a public domain resolves to a private IP range, treat it as an
     * attack and return SERVFAIL instead of the private IP.
     *
     * Blocks: 10.x.x.x, 172.16-31.x.x, 192.168.x.x, 127.x.x.x, 169.254.x.x,
     *         fc00::/7, fe80::/10, ::1
     */
    private fun isDnsRebinding(responseBytes: ByteArray): Boolean {
        return try {
            val response = Message(responseBytes)
            for (record in response.getSectionArray(Section.ANSWER)) {
                when (record) {
                    is ARecord -> {
                        val addr = record.address
                        val bytes = addr.address
                        if (bytes.size == 4 && isPrivateIpv4(bytes)) return true
                    }
                    is AAAARecord -> {
                        val addr = record.address
                        val bytes = addr.address
                        if (bytes.size == 16 && isPrivateIpv6(bytes)) return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
    }

    private fun isPrivateIpv4(b: ByteArray): Boolean {
        val a0 = b[0].toInt() and 0xFF
        val a1 = b[1].toInt() and 0xFF
        // 10.0.0.0/8
        if (a0 == 10) return true
        // 172.16.0.0/12
        if (a0 == 172 && a1 in 16..31) return true
        // 192.168.0.0/16
        if (a0 == 192 && a1 == 168) return true
        // 127.0.0.0/8
        if (a0 == 127) return true
        // 169.254.0.0/16 (link-local)
        if (a0 == 169 && a1 == 254) return true
        return false
    }

    private fun isPrivateIpv6(b: ByteArray): Boolean {
        val a0 = b[0].toInt() and 0xFF
        val a1 = b[1].toInt() and 0xFF
        // fc00::/7 (unique local)
        if ((a0 and 0xFE) == 0xFC) return true
        // fe80::/10 (link-local)
        if ((a0 and 0xFC) == 0xFE && (a1 and 0xC0) == 0x80) return true
        // ::1
        if (b.all { it == 0.toByte() }) return true
        return false
    }

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
                dnsCache[cacheKey] = responseBytes.copyOf()
            }
        } catch (_: Exception) {}
    }

    /** Opportunistic TTL eviction so negativeDnsCache doesn't grow forever. */
    private fun pruneNegativeCache() {
        if (negativeDnsCache.size < 512) return
        val now = System.currentTimeMillis()
        val iter = negativeDnsCache.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (now - entry.value > NEGATIVE_CACHE_TTL_MS) {
                iter.remove()
            }
        }
    }

    /**
     * Extract IPs from DNS responses that contain CNAME chains resolving to
     * *.cloudfront.net. These IPs are cached so that PacketRouter can force
     * QUIC→TCP downgrade, giving us SNI visibility on ad-serving CloudFront
     * distributions.
     *
     * When [whitelistDistribution] is true (critical-allow path only), also
     * populates [contentCdnDistributions] — whitelisting the distribution so
     * PacketRouter's SNI handler allows it through. When false (upstream path),
     * IPs are tracked for QUIC downgrade but the distribution is NOT whitelisted
     * — this prevents ad domain CNAME chains from self-whitelisting.
     */
    private fun trackCloudFrontIps(responseBytes: ByteArray, whitelistDistribution: Boolean) {
        try {
            val response = Message(responseBytes)
            var hasCloudFrontCname = false
            for (record in response.getSectionArray(Section.ANSWER)) {
                if (record is CNAMERecord) {
                    val target = record.target.toString(true).lowercase().trimEnd('.')
                    if (target.endsWith(".cloudfront.net")) {
                        hasCloudFrontCname = true
                        // Only whitelist from critical-allow path (content domains)
                        if (whitelistDistribution) {
                            val distId = target.substringBefore(".cloudfront.net")
                            if (distId.isNotEmpty()) {
                                contentCdnDistributions.add(distId)
                                Log.d(TAG, "Content CDN discovered: $distId.cloudfront.net")
                            }
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
                        blockedIpCache[ip] = now
                    }
                }
                is AAAARecord -> {
                    record.address.hostAddress?.let { ip ->
                        blockedIpCache[ip] = now
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

        // 3. Downloaded blocklists from database (builtin + remote sources).
        // Load incrementally per source to avoid OOM from accumulating all
        // domains in a single list (can exceed 256MB heap on Android TV).
        val newRuleEngine = RuleEngine()
        newRuleEngine.loadRules(allRules)  // custom rules first

        val enabledSources = blocklistRepo.observeSourceStates().first()
            .filter { it.enabled }
        var blocklistCount = 0
        for (source in enabledSources) {
            val domains = blocklistRepo.getDomainsBySource(source.source)
            newRuleEngine.addRules(domains)
            blocklistCount += domains.size
        }

        Log.i(TAG, "Loaded ${customRules.size} custom rules + ${criticalAllows.size} critical allows + $blocklistCount blocklist domains from ${enabledSources.size} sources")

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
        val ts = blockedIpCache[ip]
        if (ts != null && System.currentTimeMillis() - ts < 600_000) return true
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
        when (queryType) {
            Type.A -> {
                response.addRecord(ARecord(query.question.name, DClass.IN, SINKHOLE_TTL, SINKHOLE_IPV4), Section.ANSWER)
            }
            Type.AAAA -> {
                response.addRecord(AAAARecord(query.question.name, DClass.IN, SINKHOLE_TTL, SINKHOLE_IPV6), Section.ANSWER)
            }
        }
        return response
    }

    private fun queueLog(host: String, type: String, appPackage: String? = null) {
        val event = BlockedEvent(host = host, type = type, appPackage = appPackage)
        if (logChannel.trySend(event).isFailure) {
            droppedLogCount.incrementAndGet()
        }
    }

    private fun startLogBatcher() {
        if (logJob?.isActive == true) return
        logJob = scope.launch {
            val batch = ArrayList<BlockedEvent>(LOG_BATCH_MAX)
            while (isActive) {
                val firstEvent = withTimeoutOrNull(LOG_BATCH_INTERVAL_MS) { logChannel.receive() }
                if (firstEvent == null) {
                    logDroppedCountIfNeeded()
                    continue
                }

                batch.add(firstEvent)
                while (batch.size < LOG_BATCH_MAX) {
                    val nextEvent = logChannel.tryReceive().getOrNull() ?: break
                    batch.add(nextEvent)
                }

                try { statsRepo.logBlockedBatch(batch.toList()) } catch (_: Exception) {}
                batch.clear()
                logDroppedCountIfNeeded()
            }
        }
    }

    private fun logDroppedCountIfNeeded() {
        val dropped = droppedLogCount.getAndSet(0L)
        if (dropped > 0) {
            Log.w(TAG, "Dropped $dropped DNS blocked log events due to logging backpressure")
        }
    }

    data class DnsStats(
        val processed: Long,
        val blocked: Long,
        val allowed: Long
    )
}
