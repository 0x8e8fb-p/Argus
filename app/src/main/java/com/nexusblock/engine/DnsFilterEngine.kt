package com.nexusblock.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.model.CustomRule
import com.nexusblock.data.repository.BuiltInBlockRules
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.data.repository.RawBlocklistLoader
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.StatsRepository
import com.nexusblock.data.db.CustomRuleDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import org.xbill.DNS.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Inet4Address
import java.net.Inet6Address
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsFilterEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blocklistRepo: BlocklistRepository,
    private val statsRepo: StatsRepository,
    private val customRuleDao: CustomRuleDao,
    private val okHttpClient: OkHttpClient,
    private val connectionTracker: ConnectionTracker,
    private val settingsRepo: SettingsRepository
) {
    companion object {
        private const val TAG = "NexusBlock/DNS"
        private const val DNS_PORT = 53
        private const val BUFFER_SIZE = 4096

        // YouTube essential domain whitelist.
        // These are injected as narrow allow-rules when the user
        // has "Allow YouTube recommendations" enabled (default: ON).
        //
        // CRITICAL: This list is intentionally MINIMAL. Previous versions
        // whitelisted far too much (googlevideo.com, googleapis.com, gstatic.com,
        // google.com, s.youtube.com) which ALLOWED ads through. The trimmed list
        // keeps only domains essential for basic playback:
        // - youtube.com / www / m: core site and app
        // - ytimg.com / i.ytimg.com: thumbnails and images
        // - youtubei.googleapis.com: API (unfortunately also serves ads)
        // - accounts.google.com: login
        // - youtube-nocookie.com / youtu.be: embeds and short links
        //
        // REMOVED from whitelist:
        // - googlevideo.com: video CDN serves BOTH ads and content. Cannot be
        //   whitelisted without allowing video ads. DNS blocking alone cannot
        //   distinguish ad videos from content videos on this domain.
        // - s.youtube.com: tracking / ad-signal endpoint
        // - gstatic.com, ggpht.com, googleapis.com: too broad
        // - google.com, play.google.com, clients4.google.com: not needed for playback
        val YOUTUBE_WHITELIST_RULES = listOf(
            BlockedDomain(host = "@@youtube.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@www.youtube.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@m.youtube.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@youtubei.googleapis.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@||i.ytimg.com^", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@||ytimg.com^", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@accounts.google.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@youtube-nocookie.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@www.youtube-nocookie.com", source = "nexusblock_whitelist"),
            BlockedDomain(host = "@@youtu.be", source = "nexusblock_whitelist")
        )
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Core rule engine (replaces old HashSet/Trie approach)
    @Volatile
    private var ruleEngine = RuleEngine()
    private var upstreamManager: DnsUpstreamManager? = null
    private var requestedDnsMode = DnsUpstreamManager.DnsMode.PLAIN
    private var startJob: Job? = null
    private var customRulesJob: Job? = null

    // DNS Cache
    private val dnsCache = object : LinkedHashMap<String, Message>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Message>?): Boolean {
            return size > 1000
        }
    }

    // Batch logging
    private val pendingLogs = mutableListOf<com.nexusblock.data.model.BlockedEvent>()
    private var logJob: Job? = null

    // Stats counter for diagnostics
    private var queriesProcessed = 0L
    private var queriesBlocked = 0L
    private var queriesAllowed = 0L

    private var lastYoutubeSetting: Boolean = true

    init {
        // Auto-reload blocklists when YouTube recommendation setting changes
        scope.launch {
            settingsRepo.observeYoutubeRecommendations().collect { newValue ->
                if (lastYoutubeSetting != newValue) {
                    lastYoutubeSetting = newValue
                    if (_isRunning.value) {
                        Log.i(TAG, "YouTube recommendation setting changed to $newValue, reloading rules")
                        reloadBlocklists()
                    }
                }
            }
        }

        customRulesJob = scope.launch {
            customRuleDao.observeEnabled()
                .drop(1)
                .collect {
                    if (_isRunning.value) {
                        Log.i(TAG, "Custom rules changed, reloading DNS rules")
                        reloadBlocklists()
                    }
                }
        }
    }

    fun start(localAddress: InetAddress = InetAddress.getByName("127.0.0.1"), localPort: Int = DNS_PORT) {
        if (_isRunning.value || startJob?.isActive == true) return
        startJob = scope.launch {
            requestedDnsMode = parseDnsMode(settingsRepo.dnsMode)
            upstreamManager = DnsUpstreamManager(okHttpClient, requestedDnsMode)
            lastYoutubeSetting = settingsRepo.youtubeRecommendationsEnabled
            loadBootstrapRules()
            startLogBatcher()
            _isRunning.value = true
            Log.i(TAG, "DNS engine initialized with upstream mode $requestedDnsMode")
            launch {
                reloadBlocklists()
            }
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
        Log.d(TAG, "DNS cache cleared")
    }

    /**
     * Resolve a DNS query payload. Returns response payload bytes, or null if
     * resolution failed and caller should forward packet transparently.
     */
    suspend fun resolveDns(queryPayload: ByteArray): ByteArray? {
        startJob?.join()

        val query = try { Message(queryPayload) } catch (e: Exception) {
            Log.w(TAG, "Failed to parse DNS query", e)
            return upstreamResolve(queryPayload)
        }

        if (query.header.opcode != Opcode.QUERY) {
            return upstreamResolve(queryPayload)
        }

        val question = query.question ?: return upstreamResolve(queryPayload)
        val host = question.name.toString(true).lowercase()
        val type = question.type
        val cacheKey = "$host|$type"

        queriesProcessed++

        // Block HTTPS/SVCB records for ad domains to prevent QUIC/H3 discovery
        if ((type == 65 || type == 64) && ruleEngine.isBlocked(host)) {
            queriesBlocked++
            queueLog(host, "dns-svcb")
            val response = Message(query.header.id)
            response.header.setFlag(Flags.QR.toInt())
            response.header.setFlag(Flags.RA.toInt())
            response.header.rcode = Rcode.NOERROR
            response.addRecord(question, Section.QUESTION)
            return response.toWire()
        }

        // 1. Check Cache
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

        // 2. Check Rule Engine before any upstream lookup.
        //    Returns sinkhole (0.0.0.0 / ::) instead of NXDOMAIN to prevent
        //    aggressive retry via DoH/DoT/IPv6.
        if (ruleEngine.isBlocked(host)) {
            queriesBlocked++
            val response = buildSinkholeResponse(query, type)
            queueLog(host, "dns")
            return response.toWire()
        }

        val upstreamResponse = upstreamResolve(queryPayload) ?: return null
        val blockedAlias = findBlockedAnswer(upstreamResponse, host)
        if (blockedAlias != null) {
            queriesBlocked++
            queueLog("$host -> $blockedAlias", "dns-cname")
            return buildSinkholeResponse(query, type).toWire()
        }

        cacheAllowedResponse(cacheKey, upstreamResponse)
        queriesAllowed++
        return upstreamResponse
    }

    private suspend fun upstreamResolve(queryPayload: ByteArray): ByteArray? {
        return try {
            val responseBytes = withContext(Dispatchers.IO) {
                upstreamManager?.resolve(queryPayload)
            }
            if (responseBytes != null) {
                responseBytes
            } else {
                systemResolve(queryPayload) ?: fallbackResolve(queryPayload)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstream resolution error", e)
            systemResolve(queryPayload) ?: fallbackResolve(queryPayload)
        }
    }

    private fun cacheAllowedResponse(cacheKey: String, responseBytes: ByteArray) {
        try {
            val response = Message(responseBytes)
            if (response.rcode == Rcode.NOERROR) {
                synchronized(dnsCache) {
                    dnsCache[cacheKey] = response
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun findBlockedAnswer(responseBytes: ByteArray, originalHost: String): String? {
        return try {
            val response = Message(responseBytes)
            for (record in response.getSectionArray(Section.ANSWER)) {
                val owner = record.name.toString(true).lowercase().trimEnd('.')
                if (owner != originalHost && ruleEngine.isBlocked(owner)) return owner

                when (record) {
                    is CNAMERecord -> {
                        val target = record.target.toString(true).lowercase().trimEnd('.')
                        if (ruleEngine.isBlocked(target)) return target
                    }
                    is ARecord -> {
                        val address = record.address.hostAddress
                        if (ruleEngine.isIpBlocked(address)) return address
                    }
                    is AAAARecord -> {
                        val address = record.address.hostAddress
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

    private suspend fun systemResolve(queryPayload: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val query = Message(queryPayload)
            val question = query.question ?: return@withContext null
            val type = question.type
            if (type != Type.A && type != Type.AAAA) return@withContext null

            val host = question.name.toString(true).trimEnd('.')
            val addresses = InetAddress.getAllByName(host)
            val response = Message(query.header.id)
            response.header.setFlag(Flags.QR.toInt())
            response.header.setFlag(Flags.RA.toInt())
            response.addRecord(question, Section.QUESTION)

            val ttl = 60L
            addresses.forEach { address ->
                when {
                    type == Type.A && address is Inet4Address -> {
                        response.addRecord(ARecord(question.name, DClass.IN, ttl, address), Section.ANSWER)
                    }
                    type == Type.AAAA && address is Inet6Address -> {
                        response.addRecord(AAAARecord(question.name, DClass.IN, ttl, address), Section.ANSWER)
                    }
                }
            }

            if (response.getSectionArray(Section.ANSWER).isEmpty()) null else response.toWire()
        } catch (e: Exception) {
            Log.w(TAG, "System DNS fallback failed", e)
            null
        }
    }

    private suspend fun fallbackResolve(queryPayload: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            val servers = getNonVpnDnsServers() + listOf(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("9.9.9.9")
            )

            for (server in servers.distinctBy { it.hostAddress }) {
                try {
                    java.net.DatagramSocket().use { upstream ->
                        upstream.soTimeout = 5000
                        upstream.send(DatagramPacket(queryPayload, queryPayload.size, server, 53))
                        val buf = ByteArray(BUFFER_SIZE)
                        val resp = DatagramPacket(buf, buf.size)
                        upstream.receive(resp)
                        return@withContext resp.data.copyOf(resp.length)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback DNS server ${server.hostAddress} failed", e)
                }
            }
            null
        }
    }

    private fun getNonVpnDnsServers(): List<InetAddress> {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        return cm.allNetworks
            .filter { network ->
                val caps = cm.getNetworkCapabilities(network)
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN).not()
            }
            .flatMap { network ->
                cm.getLinkProperties(network)?.dnsServers.orEmpty()
            }
    }

    suspend fun reloadBlocklists() {
        val domains = blocklistRepo.getEnabledDomains()
        val customRules = customRuleDao.getEnabled()

        // Convert custom rules to BlockedDomain format for the rule engine
        val allRules = domains.toMutableList()

        for (rule in customRules) {
            allRules.add(
                BlockedDomain(
                    host = if (rule.isAllow) "@@${rule.rule}" else rule.rule,
                    source = "custom",
                    isRegex = rule.rule.startsWith("/") && rule.rule.endsWith("/"),
                    regexPattern = if (rule.rule.startsWith("/") && rule.rule.endsWith("/"))
                        rule.rule.substring(1, rule.rule.length - 1) else null
                )
            )
        }

        // Load built-in raw blocklist if database is empty (first launch)
        if (allRules.isEmpty()) {
            Log.i(TAG, "Database empty, loading built-in OISD blocklist from raw resource")
            try {
                val builtin = RawBlocklistLoader.loadBuiltinBlocklist(context)
                allRules.addAll(builtin)
                Log.i(TAG, "Loaded ${builtin.size} domains from built-in OISD blocklist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load built-in blocklist, falling back to emergency rules", e)
                allRules.addAll(getBuiltinRules())
            }
        }

        // Always merge emergency rules (Indian OTT, YouTube ad domains, DoH
        // blockers) even on first launch before remote blocklists are downloaded.
        val emergencyRules = getBuiltinRules()
        allRules.addAll(emergencyRules)
        Log.i(TAG, "Merged ${emergencyRules.size} built-in emergency rules")

        /*
            // Database has downloaded blocklists — still merge built-in emergency rules
            // (Indian OTT, YouTube ad domains, DoH blockers) so they are always active
            // regardless of whether downloaded lists include them.
            val emergencyRules = getBuiltinRules()
            allRules.addAll(emergencyRules)
            Log.i(TAG, "Merged ${emergencyRules.size} built-in emergency rules")
        */

        // Inject hard allow-list for YouTube recommendation / essential domains.
        // These MUST resolve even if a blocklist accidentally includes them.
        // Added BEFORE loadRules so they go in as allow-rules in a single pass.
        // Only applied when the user has "Allow YouTube recommendations" enabled (default: ON).
        if (settingsRepo.youtubeRecommendationsEnabled) {
            allRules.addAll(YOUTUBE_WHITELIST_RULES)
            Log.i(TAG, "YouTube whitelist applied (${YOUTUBE_WHITELIST_RULES.size} rules)")
        } else {
            Log.i(TAG, "YouTube whitelist DISABLED by user setting")
        }

        val newRuleEngine = RuleEngine()
        newRuleEngine.loadRules(allRules)
        ruleEngine = newRuleEngine
        clearCache()
        Log.i(TAG, "Loaded ${domains.size} blocklist domains + ${customRules.size} custom rules + built-ins = ${allRules.size} total")
    }

    private suspend fun loadBootstrapRules() {
        val rules = getBuiltinRules().toMutableList()
        customRuleDao.getEnabled().forEach { rule ->
            rules.add(
                BlockedDomain(
                    host = if (rule.isAllow) "@@${rule.rule}" else rule.rule,
                    source = "custom",
                    isRegex = rule.rule.startsWith("/") && rule.rule.endsWith("/"),
                    regexPattern = if (rule.rule.startsWith("/") && rule.rule.endsWith("/"))
                        rule.rule.substring(1, rule.rule.length - 1) else null
                )
            )
        }
        if (settingsRepo.youtubeRecommendationsEnabled) {
            rules.addAll(YOUTUBE_WHITELIST_RULES)
        }
        val newRuleEngine = RuleEngine()
        newRuleEngine.loadRules(rules)
        ruleEngine = newRuleEngine
        clearCache()
        Log.i(TAG, "Loaded ${rules.size} bootstrap DNS rules")
    }

    private fun getBuiltinRules(): List<BlockedDomain> {
        val rules = BuiltInBlockRules.emergencyRules().toMutableList()
        val domains = listOf(
            // ============================================================
            // GOOGLE / YOUTUBE ADS & TRACKING
            // ============================================================
            "adservice.google.com",
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "youtube.cleverads.vn",
            "s0.2mdn.net",
            "static.doubleclick.net",
            "pubads.g.doubleclick.net",
            "tpc.googlesyndication.com",
            "pagead2.googlesyndication.com",
            "ad.youtube.com",
            "ads.youtube.com",
            "googleadapis.l.google.com",
            "video-ad-stats.googlesyndication.com",
            "pagead-googlehosted.l.google.com",
            "clients.l.google.com",
            "pagead.l.doubleclick.net",
            "pagead.google.com",
            "partnerad.l.doubleclick.net",
            "ad.doubleclick.net",
            "ad.mo.doubleclick.net",
            "adm.doubleclick.net",
            "ad-emea.doubleclick.net",
            "ad-apac.doubleclick.net",
            "m.doubleclick.net",
            "mediavisor.doubleclick.net",
            "gg.google.com",
            "id.google.com",
            "googleads.g.doubleclick.net",
            "www.googleadservices.com",
            "www.googletagmanager.com",
            "www.google-analytics.com",
            "ssl.google-analytics.com",
            "googletagservices.com",
            "fwmrm.net",
            "adm.fwmrm.net",
            "m1.fwmrm.net",
            "googleads4.g.doubleclick.net",
            "googleads5.g.doubleclick.net",
            "googleads6.g.doubleclick.net",
            "fls.doubleclick.net",
            "cm.g.doubleclick.net",
            "ads.g.doubleclick.net",
            "iv.doubleclick.net",
            "ad-g.doubleclick.net",
            // YouTube-specific tracking (NOT whitelisted — blocking these reduces ad targeting)
            "s.youtube.com",
            "r.youtube.com",
            "csp.withgoogle.com",
            "breakage.withgoogle.com",
            // ============================================================
            // INDIAN OTT AD DOMAINS
            // ============================================================
            // Hotstar / Disney+ Hotstar
            "ads.hotstar.com",
            "hb-api.omtrdc.net",
            "smetrics.hotstar.com",
            "tracking.hotstar.com",
            "sa.hotstar.com",
            "in-star.ads.yieldmo.com",
            "ads-internal.hotstar.com",
            "hotstar.ad.g.doubleclick.net",
            "hotstarads.akamaized.net",
            "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "googleads4.g.doubleclick.net",
            // JioCinema
            "ads.jiocinema.com",
            "analytics.jiocinema.com",
            "metrics.jiocinema.com",
            "tracking.jiocinema.com",
            "telemetry.jiocinema.com",
            "jiocinema-ad.akamaized.net",
            "jiobeats.cdn.jio.com",
            "jiocinema.com.pagead",
            // SonyLIV
            "ads.sonyliv.com",
            "tracking.sonyliv.com",
            "analytics.sonyliv.com",
            "telemetry.sonyliv.com",
            "adserver.sonyliv.com",
            "sony.ads.yieldmo.com",
            // ZEE5
            "ads.zee5.com",
            "analytics.zee5.com",
            "tracking.zee5.com",
            "telemetry.zee5.com",
            "adserver.zee5.com",
            "zee5ads.akamaized.net",
            // MX Player
            "ads.mxplay.com",
            "ads-server.mxplay.com",
            "analytics.mxplay.com",
            "tracking.mxplay.com",
            // Voot
            "ads.voot.com",
            "tracking.voot.com",
            "analytics.voot.com",
            "vootads.akamaized.net",
            // Alt Balaji
            "ads.altbalaji.com",
            "tracking.altbalaji.com",
            // Eros Now
            "ads.erosnow.com",
            "tracking.erosnow.com",
            // Generic Indian streaming ad infra
            "ads.akamaized.net",
            "adserver.akamaized.net",
            "tvads.indiatimes.com",
            "ads.indiatimes.com",
            "agi-static.indiatimes.com",
            // ============================================================
            // INDIAN AD NETWORKS
            // ============================================================
            "inmobi.com",
            "ads.inmobi.com",
            "i.l.inmobicdn.net",
            "inmobicdn.net",
            "komli.com",
            "tyroo.com",
            "madstreetden.com",
            "adpushup.com",
            "adgebra.in",
            "dgm.in",
            "network18.com",
            "svg.com",
            "timesinternet.com",
            "adtech.de",
            "adform.net",
            "adition.com",
            "adition.de",
            // ============================================================
            // MOBILE / TV SDK ANALYTICS & TRACKERS
            // ============================================================
            "clevertap.com",
            "sdk.clevertap.com",
            "wzrkt.com",
            "moengage.com",
            "api.moengage.com",
            "branch.io",
            "api2.branch.io",
            "appsflyer.com",
            "impressions.appsflyer.com",
            "events.appsflyer.com",
            "firebaseinstallations.googleapis.com",
            "firebaselogging.googleapis.com",
            "firebase-settings.crashlytics.com",
            "crashlyticsreports-pa.googleapis.com",
            "mtalk.google.com",
            "checkin.gstatic.com",
            // ============================================================
            // DNS-OVER-HTTPS (DoH) PROVIDERS — Force apps to use our filter
            // ============================================================
            "cloudflare-dns.com",
            "dns.cloudflare.com",
            "one.one.one.one",
            "dns.google",
            "dns.google.com",
            "dns.quad9.net",
            "dns9.quad9.net",
            "dns10.quad9.net",
            "doh.opendns.com",
            "dns.nextdns.io",
            "dns.adguard.com",
            "family.adguard-dns.com",
            "unfiltered.adguard-dns.com",
            "dns.switch.ch",
            "dns.twnic.tw",
            "dns.rubyfish.cn",
            "doh.dns.sb",
            "doh-fi.blahdns.com",
            "dns.digitale-gesellschaft.ch",
            "dnsforge.de",
            // ============================================================
            // GENERIC AD / TRACKING NETWORKS
            // ============================================================
            "amazon-adsystem.com",
            "c.amazon-adsystem.com",
            "s.amazon-adsystem.com",
            "aax.amazon-adsystem.com",
            "app-measurement.com",
            "scorecardresearch.com",
            "secure-us.imrworldwide.com",
            "crwdcntrl.net",
            "agkn.com",
            "tapad.com",
            "moatads.com",
            "adsrvr.org",
            "adsystem.com",
            "adnxs.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "vungle.com",
            "unity3d.com",
            "applovin.com",
            "chartboost.com",
            "adcolony.com",
            "ironsrc.com",
            "inner-active.mobi",
            "mopub.com",
            "startapp.com",
            "supersonicads.com",
            "facebook.com/tr",
            "facebook.com/tr/",
            "graph.facebook.com",
            "connect.facebook.net",
            "analytics.facebook.com",
            "pixel.facebook.com",
            "an.facebook.com",
            "atdmt.com",
            "ads.yahoo.com",
            "analytics.yahoo.com",
            "gemini.yahoo.com",
            // ============================================================
            // VIDEO AD SERVERS (used by OTT apps)
            // ============================================================
            "freewheel.tv",
            "freewheel.mb",
            "ads.freewheel.tv",
            "hub.com",
            "ads.hub.com",
            "adsafeprotected.com",
            "adsystem.com"
        )
        domains.forEach { rules.add(BlockedDomain(host = it.trim(), source = "builtin_emergency")) }
        return rules
    }

    fun setUpstreamMode(mode: DnsUpstreamManager.DnsMode) {
        requestedDnsMode = mode
        upstreamManager?.setMode(mode)
    }

    private fun parseDnsMode(mode: String): DnsUpstreamManager.DnsMode {
        return try {
            DnsUpstreamManager.DnsMode.valueOf(mode)
        } catch (_: Exception) {
            DnsUpstreamManager.DnsMode.PLAIN
        }
    }

    fun getStats(): DnsStats = DnsStats(
        processed = queriesProcessed,
        blocked = queriesBlocked,
        allowed = queriesAllowed
    )

    fun isDomainBlocked(host: String): Boolean = ruleEngine.isBlocked(host)

    fun isIpBlocked(ip: String): Boolean = ruleEngine.isIpBlocked(ip)

    /** Build an NXDOMAIN response for a raw DNS query payload. Used by PacketRouter
     * when a BLOCK-mode app's DNS query is intercepted (total internet denial). */
    fun buildNxDomainBytes(queryPayload: ByteArray): ByteArray? {
        return try {
            val query = Message(queryPayload)
            buildNxDomainResponse(query).toWire()
        } catch (_: Exception) { null }
    }

    /** Build a sinkhole response for a raw DNS query payload. Used externally when
     * an ad domain needs to be blocked without triggering DoH/DoT retries. */
    fun buildSinkholeBytes(queryPayload: ByteArray): ByteArray? {
        return try {
            val query = Message(queryPayload)
            val type = query.question?.type ?: Type.A
            buildSinkholeResponse(query, type).toWire()
        } catch (_: Exception) { null }
    }

    /** NXDOMAIN response — reserved for BLOCK-mode firewall (total internet denial).
     * Ad-domain blocking uses [buildSinkholeResponse] instead. */
    private fun buildNxDomainResponse(query: Message): Message {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.NXDOMAIN
        response.addRecord(query.question, Section.QUESTION)
        return response
    }

    /** Sinkhole response — returns 0.0.0.0 (A) or :: (AAAA) so the app accepts it,
     * caches it, and the TCP connect to the sinkhole IP fails instantly with no retries.
     * For non-A/AAAA query types, returns NOERROR with no answer records. */
    private fun buildSinkholeResponse(query: Message, queryType: Int): Message {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        response.header.rcode = Rcode.NOERROR
        response.addRecord(query.question, Section.QUESTION)
        val name = query.question.name
        val ttl = 300L
        when (queryType) {
            Type.A -> {
                val sinkhole = InetAddress.getByName("0.0.0.0")
                response.addRecord(ARecord(name, DClass.IN, ttl, sinkhole), Section.ANSWER)
            }
            Type.AAAA -> {
                val sinkhole = InetAddress.getByName("::")
                response.addRecord(AAAARecord(name, DClass.IN, ttl, sinkhole), Section.ANSWER)
            }
            // Other types (MX, TXT, etc.): NOERROR with empty answer
        }
        return response
    }

    private fun queueLog(host: String, type: String, appPackage: String? = null) {
        synchronized(pendingLogs) {
            pendingLogs.add(com.nexusblock.data.model.BlockedEvent(
                host = host, 
                type = type, 
                appPackage = appPackage
            ))
        }
        // Limit queue size to prevent memory issues
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
                    if (pendingLogs.isEmpty()) {
                        null
                    } else {
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
