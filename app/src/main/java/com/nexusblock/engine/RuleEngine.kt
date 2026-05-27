package com.nexusblock.engine

import android.util.Log
import com.nexusblock.data.model.BlockedDomain
import java.util.regex.PatternSyntaxException

/**
 * Full AdGuard/Adblock-style rule engine supporting:
 * - ||example.org^ (hostname with subdomains)
 * - |example.org (prefix match)
 * - example.org| (suffix match)
 * - /regex/ (regular expression)
 * - @@||example.org^ (exception rules)
 * - 127.0.0.1 example.org (hosts syntax)
 * - example.org (plain domain)
 */
class RuleEngine {

    companion object {
        private const val TAG = "Argus/Rules"

        /**
         * Known Prime Video ad-serving CloudFront distribution IDs.
         * Sourced from network traffic analysis of Prime Video SSAI system.
         * These distributions serve ad creatives (video segments) injected
         * into HLS/DASH manifests by AWS MediaTailor.
         */
        private val BLOCKED_CLOUDFRONT_DISTRIBUTIONS = setOf(
            // Confirmed Prime Video ad creative distributions
            "d3gqasl9vmjfd8",
            "d1v5w5eed7sjkx",
            "d2lkq7nlcrdi7q",
            "d3p8zr0ffa9t17",
            // Amazon Ads / VAST creative delivery
            "d2x35nyx282v2b",
            "d1g0l5r0b4out4",
            "d3aqoihi2n8ty8",
            "d31qbv1cthcecs",
            "d25xi2x97liuc3",
            "d3a4ild1hld3bz",
            "d1xfq2sbl6n3hb",
            "d2z1hgrnpjv0ho",
            "d3ic7bsnpehvh5",
            "d32fzssy5lk346",
            // MediaTailor SSAI ad segment delivery
            "d3swu0o76264hs",
            "d2cnnru4029bq2",
            "d3qxef0t3hvyim",
            "d1ws21kp0j6ega",
            "d23caukrnoefrp",
            // IMDb TV / Freevee ad delivery (shared infra with Prime)
            "d27xxe7juh1us6",
            "d1fds7brc3ho6y",
            "d3tvtfb6mhrqvr",
            "d38b8me95wjkbc",
            // Amazon advertising SDK creatives
            "d2v02itv0y9u9t",
            "d3lhz43t5kkr4j"
        )

        // Max domains to load locally. Beyond this, rely on filtered upstream DNS.
        // 200K domains ≈ 30-40MB heap, safe for 256MB Android TV devices.
        private const val MAX_DOMAIN_RULES = 200_000
    }

    // Exact domain matches for O(1) lookup
    private val exactBlocks = HashSet<String>()
    private val exactAllows = HashSet<String>()

    // Bloom Filter for fast blocklist skipping
    private val bloomFilter = BloomFilter()

    // Trie for subdomain wildcard matching (||...^)
    private val blockTrie = DomainTrie()
    private val allowTrie = DomainTrie()

    // Prefix/suffix patterns
    private val prefixBlocks = mutableListOf<String>()
    private val prefixAllows = mutableListOf<String>()
    private val suffixBlocks = mutableListOf<String>()
    private val suffixAllows = mutableListOf<String>()

    // Regex patterns
    private val blockRegexes = mutableListOf<Regex>()
    private val allowRegexes = mutableListOf<Regex>()

    // IP blocks and CIDR ranges
    private val exactIpBlocks = HashSet<String>()
    private val cidrBlocks = mutableListOf<CidrMatcher>()

    fun loadRules(domains: List<BlockedDomain>) {
        clearAll()
        addRules(domains)
        Log.i(TAG, "Loaded ${exactBlocks.size} domains, ${exactIpBlocks.size} IPs, ${cidrBlocks.size} CIDRs. Patterns: ${blockRegexes.size} regex, ${prefixBlocks.size} prefix")
    }

    /**
     * Append rules without clearing existing state. Used to load sources
     * incrementally to avoid OOM from accumulating all domains in memory.
     *
     * Enforces a hard cap of [MAX_DOMAIN_RULES] to prevent OOM on low-memory
     * Android TV devices (256MB heap). Since we use a filtered upstream DNS
     * (AdGuard), local rules only need to cover custom rules + emergency
     * blocklist domains that the upstream might miss.
     */
    fun addRules(domains: List<BlockedDomain>) {
        for (domain in domains) {
            // Hard cap: prevent OOM from loading millions of domains
            if (exactBlocks.size >= MAX_DOMAIN_RULES) {
                Log.w(TAG, "Rule cap reached (${MAX_DOMAIN_RULES}), skipping remaining ${domains.size} domains from this source")
                return
            }

            if (!domain.enabled) continue
            val host = domain.host.trim()
            if (host.isEmpty() || host.startsWith("#")) continue

            try {
                when {
                    // CIDR support: 1.2.3.4/24
                    host.contains("/") && host.all { it.isDigit() || it == '.' || it == '/' } -> {
                        addCidrBlock(host)
                    }
                    // Plain IP
                    host.all { it.isDigit() || it == '.' } && host.split(".").size == 4 -> {
                        exactIpBlocks.add(host)
                    }
                    domain.isRegex && domain.regexPattern != null -> {
                        addRegex(domain.regexPattern, isAllow = false)
                    }
                    host.startsWith("@@||") -> {
                        addHostRule(host.substring(4).trimEnd('^'), isAllow = true, isSubdomain = true)
                    }
                    host.startsWith("@@|") -> {
                        addPrefixRule(host.substring(3), isAllow = true)
                    }
                    host.startsWith("@@") -> {
                        addHostRule(host.substring(2), isAllow = true)
                    }
                    host.startsWith("||") -> {
                        addHostRule(host.substring(2).trimEnd('^'), isAllow = false, isSubdomain = true)
                    }
                    host.startsWith("|") -> {
                        addPrefixRule(host.substring(1), isAllow = false)
                    }
                    host.endsWith("|") -> {
                        addSuffixRule(host.dropLast(1), isAllow = false)
                    }
                    host.startsWith("/") && host.endsWith("/") -> {
                        addRegex(host.substring(1, host.length - 1), isAllow = false)
                    }
                    else -> {
                        // Handle hosts format: 127.0.0.1 domain.com
                        val parts = host.split(Regex("\\s+"))
                        val finalHost = if (parts.size >= 2 && (parts[0] == "127.0.0.1" || parts[0] == "0.0.0.0")) {
                            parts[1]
                        } else {
                            host
                        }
                        
                        val normalized = finalHost.lowercase().trimEnd('.')
                        if (normalized.isNotEmpty()) {
                            exactBlocks.add(normalized)
                            blockTrie.insert(normalized)
                            bloomFilter.add(normalized)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse rule: ${domain.host}", e)
            }
        }
    }

    fun isBlocked(host: String): Boolean {
        val normalized = host.lowercase().trimEnd('.')
        if (normalized.isEmpty()) return false

        // 1. Check exception rules first (highest priority)
        // Note: Exceptions bypass the Bloom Filter check
        if (exactAllows.contains(normalized)) return false
        if (allowTrie.contains(normalized)) return false
        for (prefix in prefixAllows) {
            if (normalized.startsWith(prefix)) return false
        }
        for (suffix in suffixAllows) {
            if (normalized.endsWith(suffix)) return false
        }
        for (regex in allowRegexes) {
            if (regex.containsMatchIn(normalized)) return false
        }

        // 2. Dynamic video ad CDN patterns are not necessarily present in
        // the Bloom filter, so they must be evaluated before the fast-fail.
        if (isAdVideoServer(normalized)) return true

        // 3. Subdomain rules must be checked before the Bloom fast-fail.
        // The Bloom filter stores the configured base rule (example.com), while
        // DNS queries often arrive as subdomains (ads.example.com).
        if (blockTrie.contains(normalized)) return true

        // 4. Fast-fail check with Bloom Filter
        // If bloom filter says no, it's definitely not in exactBlocks or blockTrie
        if (!bloomFilter.mightContain(normalized)) {
            // Still need to check prefix, suffix, and regex blocks as they are not in Bloom Filter
            for (prefix in prefixBlocks) {
                if (normalized.startsWith(prefix)) return true
            }
            for (suffix in suffixBlocks) {
                if (normalized.endsWith(suffix)) return true
            }
            for (regex in blockRegexes) {
                if (regex.containsMatchIn(normalized)) return true
            }
            return false
        }

        // 5. Check block rules
        if (exactBlocks.contains(normalized)) return true
        for (prefix in prefixBlocks) {
            if (normalized.startsWith(prefix)) return true
        }
        for (suffix in suffixBlocks) {
            if (normalized.endsWith(suffix)) return true
        }
        for (regex in blockRegexes) {
            if (regex.containsMatchIn(normalized)) return true
        }

        return false
    }

    fun isIpBlocked(ip: String): Boolean {
        if (exactIpBlocks.contains(ip)) return true
        
        try {
            val ipInt = ipToLong(ip)
            for (cidr in cidrBlocks) {
                if (cidr.matches(ipInt)) return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /**
     * Check if a hostname matches known ad-serving CDN patterns.
     * Covers googlevideo.com (YouTube), aiv-cdn.net (Amazon Prime), and
     * akamaized.net (Hotstar/OTT) ad delivery domains.
     *
     * NOTE: YouTube in 2025-2026 serves most ad video from the SAME CDN nodes
     * as content (indistinguishable hostnames). DNS-level blocking therefore
     * focuses on the ad DECISION infrastructure (IMA SDK, doubleclick, etc.)
     * rather than the video stream itself. The patterns below only catch the
     * minority of ad-dedicated CDN nodes that have identifying hostnames.
     */
    fun isAdVideoServer(hostname: String): Boolean {
        val lower = hostname.lowercase()

        // YouTube ad CDN patterns on googlevideo.com
        if (lower.endsWith("googlevideo.com")) {
            return lower.contains("-ad-") ||
                   lower.contains("-ads-") ||
                   lower.contains("-oad-") ||
                   lower.startsWith("redirector.") ||
                   lower.startsWith("redirects.") ||
                   lower.contains("---ad") ||
                   lower.contains("_ad_") ||
                   lower.contains(".ad.") ||
                   lower.startsWith("ad-") ||
                   lower.startsWith("ads.") ||
                   lower.startsWith("pagead.") ||
                   lower.contains("googleads") ||
                   lower.contains("doubleclick") ||
                   lower.contains("-ptracking") ||
                   lower.contains("-initplayback") ||
                   // Ad payload marker patterns in URL-encoded paths
                   lower.contains("ctier=l") ||
                   lower.contains("oad=") ||
                   lower.contains("atr=") ||
                   lower.contains("dclk_video_ads")
        }

        // Amazon Prime Video CDN — block ad-serving patterns on aiv-cdn.net.
        // Content segments also use this CDN so we CANNOT block unconditionally.
        // Instead we match known ad-related subdomains/patterns.
        if (lower.endsWith("aiv-cdn.net")) {
            return lower.contains("videorolls") ||
                   lower.contains("interstitial") ||
                   lower.contains("ad-creative") ||
                   lower.contains("cf.videorolls") ||
                   lower.contains("ad-") ||
                   lower.contains("-ad.") ||
                   lower.contains("creative") ||
                   lower.contains("preroll") ||
                   lower.contains("midroll") ||
                   lower.contains("postroll") ||
                   lower.contains("bumper") ||
                   lower.contains("slate") ||
                   lower.contains("sponsor") ||
                   lower.contains("tracking") ||
                   lower.contains("beacon") ||
                   lower.contains("impression") ||
                   lower.contains("vast") ||
                   lower.contains("vpaid") ||
                   lower.contains("adserver") ||
                   lower.contains("adsystem")
        }

        // Akamai CDN ad-serving subdomains
        if (lower.endsWith("akamaized.net")) {
            return lower.startsWith("ads") ||
                   lower.startsWith("hotstarads") ||
                   lower.startsWith("hesads") ||
                   lower.startsWith("jiocinema-ad") ||
                   lower.startsWith("jiocinema-ads") ||
                   lower.startsWith("jioads") ||
                   lower.startsWith("sonyliv-ads") ||
                   lower.startsWith("zee5ads") ||
                   lower.startsWith("zee5-ads") ||
                   lower.startsWith("vootads") ||
                   lower.startsWith("adserver") ||
                   lower.startsWith("speee-ad") ||
                   lower.startsWith("freewheel") ||
                   lower.startsWith("inmobisdk") ||
                   lower.startsWith("113vod-adaptive")
        }

        // CloudFront ad delivery — Amazon uses rotating CloudFront distributions
        // for Prime Video ad creatives. These are confirmed ad-serving distributions
        // from network analysis of Prime Video SSAI system. Since the QUIC downgrade
        // forces TCP, these will be caught at SNI level too.
        if (lower.endsWith("cloudfront.net")) {
            val dist = lower.substringBefore(".cloudfront.net")
            return dist in BLOCKED_CLOUDFRONT_DISTRIBUTIONS
        }

        // Amazon ad-system CDN subdomains
        if (lower.endsWith("amazon-adsystem.com")) return true

        // Amazon media-amazon ad tracking
        if (lower.endsWith("media-amazon.com") && (lower.contains("ad") || lower.contains("metric"))) return true

        return false
    }

    fun clearAll() {
        exactBlocks.clear()
        exactAllows.clear()
        blockTrie.clear()
        allowTrie.clear()
        prefixBlocks.clear()
        prefixAllows.clear()
        suffixBlocks.clear()
        suffixAllows.clear()
        blockRegexes.clear()
        allowRegexes.clear()
        exactIpBlocks.clear()
        cidrBlocks.clear()
        bloomFilter.clear()
    }

    fun ruleCount(): Int = exactBlocks.size + blockTrie.size() + prefixBlocks.size + suffixBlocks.size + blockRegexes.size + exactIpBlocks.size + cidrBlocks.size

    private fun addHostRule(host: String, isAllow: Boolean, isSubdomain: Boolean = false) {
        val normalized = host.lowercase().trimEnd('.')
        if (normalized.isEmpty()) return

        if (isAllow) {
            exactAllows.add(normalized)
            if (isSubdomain) allowTrie.insert(normalized)
        } else {
            exactBlocks.add(normalized)
            bloomFilter.add(normalized)
            if (isSubdomain) blockTrie.insert(normalized)
        }
    }

    private fun addPrefixRule(prefix: String, isAllow: Boolean) {
        val normalized = prefix.lowercase().trimEnd('.')
        if (normalized.isEmpty()) return
        if (isAllow) prefixAllows.add(normalized) else prefixBlocks.add(normalized)
    }

    private fun addSuffixRule(suffix: String, isAllow: Boolean) {
        val normalized = suffix.lowercase().trimEnd('.')
        if (normalized.isEmpty()) return
        if (isAllow) suffixAllows.add(normalized) else suffixBlocks.add(normalized)
    }

    private fun addRegex(pattern: String, isAllow: Boolean) {
        try {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (isAllow) allowRegexes.add(regex) else blockRegexes.add(regex)
        } catch (e: PatternSyntaxException) {
            Log.w(TAG, "Invalid regex pattern: $pattern")
        }
    }

    private fun addCidrBlock(cidr: String) {
        try {
            val parts = cidr.split("/")
            if (parts.size != 2) return
            val network = ipToLong(parts[0])
            val mask = parts[1].toInt()
            cidrBlocks.add(CidrMatcher(network, mask))
        } catch (e: Exception) {
            Log.w(TAG, "Invalid CIDR: $cidr")
        }
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0
        var result = 0L
        for (i in 0..3) {
            result = result shl 8 or (parts[i].toLong() and 0xFF)
        }
        return result
    }

    private class CidrMatcher(network: Long, maskBits: Int) {
        private val mask: Long = if (maskBits == 0) 0 else (-1L shl (32 - maskBits)) and 0xFFFFFFFFL
        private val base: Long = network and mask

        fun matches(ip: Long): Boolean {
            return (ip and mask) == base
        }
    }

    // Subdomain-matching Trie
    private class DomainTrie {
        private val root = TrieNode()
        private var nodeCount = 0

        fun size(): Int = nodeCount

        fun insert(domain: String) {
            val parts = domain.split(".").reversed()
            var node = root
            for (part in parts) {
                node = node.children.getOrPut(part) { TrieNode() }
            }
            node.isEnd = true
            nodeCount++
        }

        fun contains(domain: String): Boolean {
            val parts = domain.split(".").reversed()
            var node = root
            for (part in parts) {
                if (node.isEnd) return true
                node = node.children[part] ?: return false
            }
            return node.isEnd
        }

        fun clear() {
            root.children.clear()
            nodeCount = 0
        }

        private class TrieNode {
            val children = mutableMapOf<String, TrieNode>()
            var isEnd = false
        }
    }
}
