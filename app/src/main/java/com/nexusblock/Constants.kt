package com.nexusblock

object Constants {
    const val CHANNEL_VPN = "argus_vpn"
    const val CHANNEL_WATCHDOG = "argus_watchdog"
    const val NOTIFICATION_ID_VPN = 1001
    const val NOTIFICATION_ID_WATCHDOG = 1002

    const val VPN_ADDRESS = "10.0.0.2"
    const val IPV6_BLOCK_ROUTE = "2000::"  // Global unicast prefix to null-route
    const val IPV6_BLOCK_PREFIX = 3        // /3 covers all global unicast IPv6
    const val VPN_DNS = "10.0.0.1"
    const val VPN_MTU = 1500

    const val PREFS_NAME = "nexusblock_prefs"
    const val PREF_AUTO_START = "auto_start"
    const val PREF_VPN_ACTIVE = "vpn_active"

    const val BLOCKLIST_FIREBOG = "https://v.firebog.net/hosts/Prigent-Ads.txt"
    const val BLOCKLIST_OISD = "https://big.oisd.nl/"
    const val BLOCKLIST_ADGUARD = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt"
    const val BLOCKLIST_STEVENBLACK = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    const val BLOCKLIST_HAGEZI_PRO = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.txt"
    const val BLOCKLIST_HAGEZI_PRO_PLUS = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.plus.txt"
    const val BLOCKLIST_HAGEZI_DOH = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/doh-vpn-proxy-bypass.txt"
    const val BLOCKLIST_HAGEZI_NATIVE = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/native.winoffice.txt"
    const val BLOCKLIST_HAGEZI_POPUP = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/popupads.txt"
    const val BLOCKLIST_PERFLYST_ANDROID = "https://raw.githubusercontent.com/Perflyst/PiHoleBlocklist/master/android-tracking.txt"
    const val BLOCKLIST_ADAWAY = "https://adaway.org/hosts.txt"
    const val BLOCKLIST_HAGEZI_NATIVE_AMAZON = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/native.amazon.txt"
    const val BLOCKLIST_PERFLYST_FIRETV = "https://raw.githubusercontent.com/Perflyst/PiHoleBlocklist/master/AmazonFireTV.txt"
    const val BLOCKLIST_HAGEZI_TIF = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.txt"
    const val BLOCKLIST_1HOSTS_LITE = "https://raw.githubusercontent.com/badmojr/1Hosts/master/Lite/adblock.txt"

    val DNS_BYPASS_IPV4_ROUTES = listOf(
        "1.1.1.1",
        "1.0.0.1",
        "8.8.8.8",
        "8.8.4.4",
        "9.9.9.9",
        "149.112.112.112",
        "208.67.222.222",
        "208.67.220.220",
        "94.140.14.14",
        "94.140.15.15",
        "185.228.168.9",
        "185.228.169.9",
        "76.76.2.0",
        "76.76.10.0",
        "194.242.2.2",
        "185.222.222.222",
        "45.11.45.11",
        "45.90.28.0",
        "45.90.30.0"
    )

    val DNS_BYPASS_IPV6_ROUTES = listOf(
        "2606:4700:4700::1111",  // Cloudflare
        "2606:4700:4700::1001",
        "2001:4860:4860::8888",  // Google
        "2001:4860:4860::8844",
        "2620:fe::fe",           // Quad9
        "2620:fe::9"
    )

    val DNS_BYPASS_PORTS = setOf(53, 443, 853, 784, 8853)

    const val WORK_TAG_BLOCKLIST = "blocklist_update"
}
