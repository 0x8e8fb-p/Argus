package com.nexusblock.engine

/**
 * Matches IPv4 addresses against known AWS CloudFront CIDR ranges.
 * Used to force QUIC→TCP downgrade for ALL CloudFront traffic, not just
 * IPs we've observed via DNS. This gives SNI visibility into connections
 * that were established before the VPN started or during DNS cache hits.
 *
 * Source: https://ip-ranges.amazonaws.com/ip-ranges.json (service=CLOUDFRONT)
 * Last updated: 2026-05-22
 */
object CloudFrontCidr {

    // CloudFront IPv4 CIDR blocks encoded as (network, mask) pairs.
    // Pre-computed at class load for O(n) lookup with n = ~25 ranges.
    private val ranges: Array<LongArray> = arrayOf(
        cidr("3.160.0.0", 14),
        cidr("3.164.0.0", 18),
        cidr("3.168.0.0", 14),
        cidr("3.172.0.0", 18),
        cidr("13.32.0.0", 15),
        cidr("13.35.0.0", 16),
        cidr("13.48.32.0", 21),
        cidr("13.54.63.128", 26),
        cidr("13.59.250.0", 26),
        cidr("13.113.196.64", 26),
        cidr("13.113.203.0", 24),
        cidr("13.124.199.0", 24),
        cidr("13.210.67.128", 26),
        cidr("13.224.0.0", 14),
        cidr("13.228.69.0", 24),
        cidr("13.233.177.0", 24),
        cidr("13.249.0.0", 16),
        cidr("15.158.0.0", 16),
        cidr("18.64.0.0", 14),
        cidr("18.68.0.0", 16),
        cidr("18.154.0.0", 15),
        cidr("18.160.0.0", 15),
        cidr("18.164.0.0", 15),
        cidr("18.172.0.0", 15),
        cidr("18.238.0.0", 15),
        cidr("18.244.0.0", 15),
        cidr("34.195.252.0", 24),
        cidr("34.226.14.0", 24),
        cidr("35.162.63.192", 26),
        cidr("35.167.191.128", 26),
        cidr("36.103.232.0", 25),
        cidr("36.103.232.128", 26),
        cidr("44.227.178.0", 24),
        cidr("44.234.108.128", 25),
        cidr("44.234.90.252", 30),
        cidr("52.15.127.128", 26),
        cidr("52.46.0.0", 18),
        cidr("52.56.127.0", 25),
        cidr("52.57.254.0", 24),
        cidr("52.66.194.128", 26),
        cidr("52.78.247.128", 26),
        cidr("52.82.128.0", 23),
        cidr("52.84.0.0", 15),
        cidr("52.124.128.0", 17),
        cidr("52.199.127.192", 26),
        cidr("52.212.248.0", 26),
        cidr("52.222.128.0", 17),
        cidr("54.182.0.0", 16),
        cidr("54.192.0.0", 16),
        cidr("54.230.0.0", 16),
        cidr("54.233.255.128", 26),
        cidr("54.239.128.0", 18),
        cidr("54.239.192.0", 19),
        cidr("54.240.128.0", 18),
        cidr("56.49.0.0", 17),
        cidr("58.254.138.0", 25),
        cidr("64.252.64.0", 18),
        cidr("64.252.128.0", 18),
        cidr("65.8.0.0", 16),
        cidr("65.9.0.0", 17),
        cidr("65.9.128.0", 18),
        cidr("70.132.0.0", 18),
        cidr("71.152.0.0", 17),
        cidr("99.84.0.0", 16),
        cidr("99.86.0.0", 16),
        cidr("108.138.0.0", 15),
        cidr("108.156.0.0", 14),
        cidr("116.129.226.0", 25),
        cidr("116.129.226.128", 26),
        cidr("118.193.97.64", 26),
        cidr("118.193.97.128", 25),
        cidr("119.147.182.0", 25),
        cidr("119.147.182.128", 26),
        cidr("120.52.22.96", 27),
        cidr("120.52.39.128", 27),
        cidr("120.52.153.192", 26),
        cidr("120.232.236.0", 25),
        cidr("120.232.236.128", 26),
        cidr("120.253.240.192", 26),
        cidr("120.253.241.160", 27),
        cidr("120.253.245.128", 26),
        cidr("120.253.245.192", 27),
        cidr("130.176.0.0", 17),
        cidr("130.176.128.0", 18),
        cidr("130.176.192.0", 19),
        cidr("130.176.224.0", 20),
        cidr("143.204.0.0", 16),
        cidr("144.220.0.0", 16),
        cidr("180.163.57.0", 25),
        cidr("180.163.57.128", 26),
        cidr("204.246.164.0", 22),
        cidr("204.246.168.0", 22),
        cidr("204.246.172.0", 24),
        cidr("204.246.174.0", 23),
        cidr("204.246.176.0", 20),
        cidr("205.251.200.0", 21),
        cidr("205.251.208.0", 20),
        cidr("205.251.249.0", 24),
        cidr("205.251.250.0", 23),
        cidr("205.251.252.0", 23),
        cidr("205.251.254.0", 24),
        cidr("216.137.32.0", 19),
        cidr("223.71.11.0", 27),
        cidr("223.71.71.96", 27),
        cidr("223.71.71.128", 25)
    )

    /**
     * Returns true if [ipStr] is within any known CloudFront CIDR range.
     * Designed for hot-path usage (called per-packet in PacketRouter).
     */
    fun isCloudFrontIp(ipStr: String): Boolean {
        val ip = ipToLong(ipStr) ?: return false
        for (range in ranges) {
            if ((ip and range[1]) == range[0]) return true
        }
        return false
    }

    private fun cidr(baseIp: String, prefixLen: Int): LongArray {
        val ip = ipToLong(baseIp) ?: 0L
        val mask = if (prefixLen == 0) 0L else (0xFFFFFFFFL shl (32 - prefixLen)) and 0xFFFFFFFFL
        return longArrayOf(ip and mask, mask)
    }

    private fun ipToLong(ipStr: String): Long? {
        val parts = ipStr.split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet < 0 || octet > 255) return null
            result = (result shl 8) or octet.toLong()
        }
        return result
    }
}
