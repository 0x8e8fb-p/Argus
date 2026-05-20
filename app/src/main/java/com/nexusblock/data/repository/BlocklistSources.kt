package com.nexusblock.data.repository

import com.nexusblock.Constants

enum class BlocklistFormat {
    HOSTS,
    ADGUARD
}

data class BlocklistSourceDefinition(
    val id: String,
    val name: String,
    val url: String?,
    val format: BlocklistFormat,
    val defaultEnabled: Boolean = true
)

object BlocklistSources {
    val remote = listOf(
        BlocklistSourceDefinition(
            id = "adguard",
            name = "AdGuard DNS Filter",
            url = Constants.BLOCKLIST_ADGUARD,
            format = BlocklistFormat.ADGUARD
        ),
        BlocklistSourceDefinition(
            id = "oisd",
            name = "OISD Big",
            url = Constants.BLOCKLIST_OISD,
            format = BlocklistFormat.ADGUARD,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "hagezi_pro",
            name = "HaGeZi Multi Pro",
            url = Constants.BLOCKLIST_HAGEZI_PRO,
            format = BlocklistFormat.ADGUARD,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "hagezi_pro_plus",
            name = "HaGeZi Multi Pro++",
            url = Constants.BLOCKLIST_HAGEZI_PRO_PLUS,
            format = BlocklistFormat.ADGUARD,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "hagezi_doh",
            name = "HaGeZi Encrypted DNS",
            url = Constants.BLOCKLIST_HAGEZI_DOH,
            format = BlocklistFormat.ADGUARD
        ),
        BlocklistSourceDefinition(
            id = "hagezi_native",
            name = "HaGeZi Native Trackers",
            url = Constants.BLOCKLIST_HAGEZI_NATIVE,
            format = BlocklistFormat.ADGUARD,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "hagezi_popup",
            name = "HaGeZi Pop-up Ads",
            url = Constants.BLOCKLIST_HAGEZI_POPUP,
            format = BlocklistFormat.ADGUARD,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "stevenblack",
            name = "StevenBlack Hosts",
            url = Constants.BLOCKLIST_STEVENBLACK,
            format = BlocklistFormat.HOSTS,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "firebog",
            name = "Firebog Ads",
            url = Constants.BLOCKLIST_FIREBOG,
            format = BlocklistFormat.HOSTS,
            defaultEnabled = false
        ),
        BlocklistSourceDefinition(
            id = "perflyst_android",
            name = "Perflyst Android Tracking",
            url = Constants.BLOCKLIST_PERFLYST_ANDROID,
            format = BlocklistFormat.HOSTS
        ),
        BlocklistSourceDefinition(
            id = "adaway",
            name = "AdAway Hosts",
            url = Constants.BLOCKLIST_ADAWAY,
            format = BlocklistFormat.HOSTS,
            defaultEnabled = false
        )
    )

    val builtin = BlocklistSourceDefinition(
        id = "builtin_ads",
        name = "Built-in OTT Rules",
        url = null,
        format = BlocklistFormat.ADGUARD
    )

    val all = remote + builtin
}
