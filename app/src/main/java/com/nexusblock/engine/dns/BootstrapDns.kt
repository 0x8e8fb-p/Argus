package com.nexusblock.engine.dns

import okhttp3.Dns
import java.net.InetAddress

/**
 * Bootstrap DNS resolver used by OkHttp for upstream DoH/DoT calls.
 *
 * Problem this solves: when the VPN is active, the system DNS is set to
 * 10.0.0.1 (our TUN). OkHttp normally uses [Dns.SYSTEM] to resolve the DoH
 * hostname (e.g. dns.adguard-dns.com) — that resolution lands in our own
 * [DnsFilterEngine], which then triggers DoH for the same hostname — an
 * infinite recursion that times out every DNS query, breaking internet for
 * all apps and saturating the IO thread pool (causing system lag).
 *
 * We short-circuit by mapping known upstream resolver hostnames directly to
 * their hardcoded IPs from [DnsProviderProfile.BUILTINS]. Anything else
 * falls back to [Dns.SYSTEM] (used only for sporadic blocklist downloads
 * that resolve fine once DoH is working).
 */
class BootstrapDns : Dns {

    private val hostMap: Map<String, List<InetAddress>> = buildHostMap()

    override fun lookup(hostname: String): List<InetAddress> {
        val key = hostname.lowercase().trimEnd('.')
        hostMap[key]?.let { return it }
        return Dns.SYSTEM.lookup(hostname)
    }

    private fun buildHostMap(): Map<String, List<InetAddress>> {
        val map = HashMap<String, List<InetAddress>>()
        for (profile in DnsProviderProfile.BUILTINS) {
            val ips = (profile.plainIpv4 + profile.plainIpv6).mapNotNull { ip ->
                try { InetAddress.getByName(ip) } catch (_: Exception) { null }
            }
            if (ips.isEmpty()) continue
            profile.dohUrl?.let { url ->
                hostFromUrl(url)?.let { host -> map[host.lowercase()] = ips }
            }
            profile.dotHostname?.let { host -> map[host.lowercase()] = ips }
        }
        return map
    }

    private fun hostFromUrl(url: String): String? = try {
        java.net.URI(url).host
    } catch (_: Exception) { null }
}
