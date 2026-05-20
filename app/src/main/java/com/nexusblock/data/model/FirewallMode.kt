package com.nexusblock.data.model

/**
 * Per-app firewall mode. Determines how the VPN treats traffic from a specific app.
 *
 * [DEFAULT] — Normal VPN routing. The app's DNS queries go through the ad-blocker.
 *             All other traffic bypasses the TUN (full-speed), only DNS is filtered.
 *
 * [ALLOW]   — Bypass VPN entirely. The app uses the device's original DNS and
 *             network stack; no blocking, no interception. Equivalent to the legacy
 *             "whitelist". Implemented via [android.net.VpnService.Builder.addDisallowedApplication].
 *
 * [BLOCK]   — No internet access. All DNS queries from the app receive NXDOMAIN.
 *             Non-DNS traffic (direct IP connections) is dropped at the TUN level.
 *             (NOTE: Full IP-level block requires reading TUN packets; DNS-only
 *             block is active immediately.)
 */
enum class FirewallMode {
    DEFAULT,
    ALLOW,
    BLOCK
}

fun FirewallMode.displayName(): String = when (this) {
    FirewallMode.DEFAULT -> "VPN Only"
    FirewallMode.ALLOW -> "Bypass VPN"
    FirewallMode.BLOCK -> "DNS Block"
}

fun FirewallMode.description(): String = when (this) {
    FirewallMode.DEFAULT -> "Ad blocking enabled for this app"
    FirewallMode.ALLOW -> "No ad blocking; direct internet"
    FirewallMode.BLOCK -> "Domain resolution denied"
}
