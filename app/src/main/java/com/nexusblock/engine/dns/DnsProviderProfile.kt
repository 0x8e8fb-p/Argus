package com.nexusblock.engine.dns

import kotlinx.serialization.Serializable

/**
 * Immutable DNS provider profile. Each profile describes one upstream resolver
 * that may support multiple protocols (plain UDP, DoH, DoT, DNSCrypt).
 *
 * Profiles are versioned so [DnsProfileManager] can detect stale data and
 * refresh from remote sources on a ~2-month cadence.
 */
@Serializable
 data class DnsProviderProfile(
    val id: String,
    val name: String,
    val description: String,
    val category: Category,
    val version: Int = 1,
    val updatedAt: Long = 0,
    val plainIpv4: List<String> = emptyList(),
    val plainIpv6: List<String> = emptyList(),
    val dohUrl: String? = null,
    val dotHostname: String? = null,
    val dnscryptStamp: String? = null,
    val supportsAdBlocking: Boolean = false,
    val supportsAdultFilter: Boolean = false,
    val supportsTrackerBlocking: Boolean = false,
    val supportsDnssec: Boolean = false,
    val supportsNoLog: Boolean = false,
    val regionHints: List<String> = emptyList(),
    val sourceUrl: String? = null
) {
    enum class Category {
        ADGUARD,
        CLOUDFLARE,
        CLEANBROWSING,
        NEXTDNS,
        CONTROLD,
        QUAD9,
        GOOGLE,
        OPEN_DNS,
        CUSTOM
    }

    companion object {
        /**
         * Built-in profiles that ship with the APK. These are the fallback
         * if the 2-month remote refresh fails or the device is offline.
         *
         * IPs and URLs are sourced directly from each provider's official
         * documentation as of 2026-05.
         */
        val BUILTINS: List<DnsProviderProfile> = listOf(
            // ── AdGuard ──────────────────────────────────────────────
            DnsProviderProfile(
                id = "adguard_standard",
                name = "AdGuard Standard",
                description = "Blocks ads, trackers, and malicious domains. No logging.",
                category = Category.ADGUARD,
                version = 1,
                plainIpv4 = listOf("94.140.14.14", "94.140.15.15"),
                plainIpv6 = listOf("2a10:50c1::ad1:ff", "2a10:50c1::ad2:ff"),
                dohUrl = "https://dns.adguard-dns.com/dns-query",
                dotHostname = "dns.adguard-dns.com",
                supportsAdBlocking = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global", "europe", "asia"),
                sourceUrl = "https://adguard-dns.io/en/public-dns.html"
            ),
            DnsProviderProfile(
                id = "adguard_family",
                name = "AdGuard Family",
                description = "Blocks ads, trackers, malware, AND adult content. No logging.",
                category = Category.ADGUARD,
                version = 1,
                plainIpv4 = listOf("94.140.14.15", "94.140.15.16"),
                plainIpv6 = listOf("2a10:50c1::ad1:ff", "2a10:50c1::ad2:ff"),
                dohUrl = "https://family.adguard-dns.com/dns-query",
                dotHostname = "family.adguard-dns.com",
                supportsAdBlocking = true,
                supportsAdultFilter = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global", "family-safe"),
                sourceUrl = "https://adguard-dns.io/en/public-dns.html"
            ),
            DnsProviderProfile(
                id = "adguard_nonfiltering",
                name = "AdGuard Non-filtering",
                description = "Fast, encrypted DNS with NO filtering. Pure privacy.",
                category = Category.ADGUARD,
                version = 1,
                plainIpv4 = listOf("94.140.14.140", "94.140.14.141"),
                plainIpv6 = listOf("2a10:50c1::1:ff", "2a10:50c1::2:ff"),
                dohUrl = "https://unfiltered.adguard-dns.com/dns-query",
                dotHostname = "unfiltered.adguard-dns.com",
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global"),
                sourceUrl = "https://adguard-dns.io/en/public-dns.html"
            ),

            // ── Cloudflare ───────────────────────────────────────────
            DnsProviderProfile(
                id = "cloudflare_1111",
                name = "Cloudflare 1.1.1.1",
                description = "Fast, privacy-first resolver. No logging.",
                category = Category.CLOUDFLARE,
                version = 1,
                plainIpv4 = listOf("1.1.1.1", "1.0.0.1"),
                plainIpv6 = listOf("2606:4700:4700::1111", "2606:4700:4700::1001"),
                dohUrl = "https://cloudflare-dns.com/dns-query",
                dotHostname = "cloudflare-dns.com",
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global"),
                sourceUrl = "https://developers.cloudflare.com/1.1.1.1/setup/"
            ),
            DnsProviderProfile(
                id = "cloudflare_security",
                name = "Cloudflare Security",
                description = "1.1.1.2 — blocks malware + phishing only.",
                category = Category.CLOUDFLARE,
                version = 1,
                plainIpv4 = listOf("1.1.1.2", "1.0.0.2"),
                plainIpv6 = listOf("2606:4700:4700::1112", "2606:4700:4700::1002"),
                dohUrl = "https://security.cloudflare-dns.com/dns-query",
                dotHostname = "security.cloudflare-dns.com",
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global"),
                sourceUrl = "https://developers.cloudflare.com/1.1.1.1/setup/"
            ),
            DnsProviderProfile(
                id = "cloudflare_family",
                name = "Cloudflare Family",
                description = "1.1.1.3 — blocks malware, phishing, AND adult content.",
                category = Category.CLOUDFLARE,
                version = 1,
                plainIpv4 = listOf("1.1.1.3", "1.0.0.3"),
                plainIpv6 = listOf("2606:4700:4700::1113", "2606:4700:4700::1003"),
                dohUrl = "https://family.cloudflare-dns.com/dns-query",
                dotHostname = "family.cloudflare-dns.com",
                supportsAdultFilter = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global", "family-safe"),
                sourceUrl = "https://developers.cloudflare.com/1.1.1.1/setup/"
            ),

            // ── CleanBrowsing ────────────────────────────────────────
            DnsProviderProfile(
                id = "cleanbrowsing_adult",
                name = "CleanBrowsing Adult Filter",
                description = "Blocks adult content, malware, and phishing.",
                category = Category.CLEANBROWSING,
                version = 1,
                plainIpv4 = listOf("185.228.168.10", "185.228.169.11"),
                plainIpv6 = listOf("2a0d:2a00:1::1", "2a0d:2a00:2::1"),
                dohUrl = "https://doh.cleanbrowsing.org/doh/adult-filter/",
                dotHostname = "adult-filter-dns.cleanbrowsing.org",
                supportsAdultFilter = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                regionHints = listOf("global", "family-safe"),
                sourceUrl = "https://cleanbrowsing.org/filters/"
            ),
            DnsProviderProfile(
                id = "cleanbrowsing_security",
                name = "CleanBrowsing Security",
                description = "Blocks malware and phishing ONLY.",
                category = Category.CLEANBROWSING,
                version = 1,
                plainIpv4 = listOf("185.228.168.9", "185.228.169.9"),
                plainIpv6 = listOf("2a0d:2a00:1::2", "2a0d:2a00:2::2"),
                dohUrl = "https://doh.cleanbrowsing.org/doh/security-filter/",
                dotHostname = "security-filter-dns.cleanbrowsing.org",
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                regionHints = listOf("global"),
                sourceUrl = "https://cleanbrowsing.org/filters/"
            ),

            // ── NextDNS ──────────────────────────────────────────────
            DnsProviderProfile(
                id = "nextdns",
                name = "NextDNS",
                description = "Highly configurable, privacy-focused with ad/tracker blocking.",
                category = Category.NEXTDNS,
                version = 1,
                plainIpv4 = listOf("45.90.28.0", "45.90.30.0"),
                plainIpv6 = listOf("2a07:a8c0::", "2a07:a8c1::"),
                dohUrl = "https://dns.nextdns.io/",
                dotHostname = "dns.nextdns.io",
                supportsAdBlocking = true,
                supportsAdultFilter = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global"),
                sourceUrl = "https://nextdns.io/"
            ),

            // ── ControlD ─────────────────────────────────────────────
            DnsProviderProfile(
                id = "controld_free",
                name = "ControlD Free",
                description = "Blocks ads, trackers, malware, and social media.",
                category = Category.CONTROLD,
                version = 1,
                plainIpv4 = listOf("76.76.2.0", "76.76.10.0"),
                plainIpv6 = listOf("2606:1a40::", "2606:1a40:1::"),
                dohUrl = "https://freedns.controld.com/p1",
                dotHostname = "p1.freedns.controld.com",
                supportsAdBlocking = true,
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global", "north-america"),
                sourceUrl = "https://controld.com/free-dns"
            ),

            // ── Quad9 ────────────────────────────────────────────────
            DnsProviderProfile(
                id = "quad9",
                name = "Quad9",
                description = "Blocks malicious domains. Strong DNSSEC. No logging.",
                category = Category.QUAD9,
                version = 1,
                plainIpv4 = listOf("9.9.9.9", "149.112.112.112"),
                plainIpv6 = listOf("2620:fe::fe", "2620:fe::9"),
                dohUrl = "https://dns.quad9.net/dns-query",
                dotHostname = "dns.quad9.net",
                supportsTrackerBlocking = true,
                supportsDnssec = true,
                supportsNoLog = true,
                regionHints = listOf("global"),
                sourceUrl = "https://www.quad9.net/"
            ),

            // ── Google ───────────────────────────────────────────────
            DnsProviderProfile(
                id = "google",
                name = "Google DNS",
                description = "Fast, reliable, but NOT privacy-focused (logs queries).",
                category = Category.GOOGLE,
                version = 1,
                plainIpv4 = listOf("8.8.8.8", "8.8.4.4"),
                plainIpv6 = listOf("2001:4860:4860::8888", "2001:4860:4860::8844"),
                dohUrl = "https://dns.google/dns-query",
                dotHostname = "dns.google",
                supportsDnssec = true,
                regionHints = listOf("global"),
                sourceUrl = "https://developers.google.com/speed/public-dns"
            )
        )

        fun byId(id: String): DnsProviderProfile? = BUILTINS.find { it.id == id }
    }
}
