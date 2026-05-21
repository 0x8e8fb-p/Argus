package com.nexusblock.engine.transformers.plugins

import android.util.Log
import com.nexusblock.engine.transformers.TransformerPlugin
import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube InnerTube API response transformer.
 *
 * YouTube serves ads inside the same API responses as content. This transformer
 * intercepts responses from youtubei.googleapis.com and strips ad-specific
 * fields from the JSON payload before the YouTube app sees them.
 *
 * Target endpoints:
 * - /youtubei/v1/player — contains streamingData + adPlacements + playerAds
 * - /youtubei/v1/next — contains adBreaks in watchNextResults
 * - /youtubei/v1/browse — may contain ad slots in contents
 * - /youtubei/v1/reel/reel_watch_sequence — Shorts ad injection
 *
 * Stripped fields (inspired by Stash/Surge YouTube scripts):
 * - adPlacements[]
 * - playerAds[]
 * - adBreaks[]
 * - adSlots[]
 * - engagementPanels with adPanels
 * - billboard [] in browse responses
 * - promotionalItems
 *
 * Note: This works on the JSON form of InnerTube responses. YouTube also uses
 * protobuf in some contexts; protobuf stripping is a future enhancement.
 */
class YoutubeTransformer : TransformerPlugin {

    companion object {
        private const val TAG = "Argus/YTTransform"

        // Response fields that contain ad objects
        private val AD_FIELD_NAMES = setOf(
            "adPlacements",
            "playerAds",
            "adBreaks",
            "adSlots",
            "promotionalItems",
            "engagementPanelTypes" // sometimes contains ad panels
        )

        // Content types we handle
        private val CONTENT_TYPES = setOf(
            "application/json",
            "text/json",
            "application/x-www-form-urlencoded" // YouTube sometimes uses this
        )
    }

    override fun name(): String = "YouTubeTransformer"

    override fun matches(host: String, path: String, contentType: String, method: String): Boolean {
        return host.contains("youtubei.googleapis.com") ||
                host.contains("www.youtube.com") ||
                host.contains("m.youtube.com") &&
                path.contains("/youtubei/v1/") &&
                CONTENT_TYPES.any { contentType.contains(it) }
    }

    override fun transform(
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: ByteArray
    ): ByteArray? {
        val jsonString = body.decodeToString()
        if (!jsonString.trimStart().startsWith("{")) return body

        return try {
            val root = JSONObject(jsonString)
            var modified = false

            // Strip known top-level ad arrays
            for (field in AD_FIELD_NAMES) {
                if (root.has(field)) {
                    root.remove(field)
                    modified = true
                }
            }

            // Deep scan: remove adRenderers in watchNextResults / contents
            modified = removeAdRenderersRecursively(root) || modified

            // Strip ad-related survey / panel items
            modified = stripEngagementPanels(root) || modified

            // Strip billboard ads from browse responses
            modified = stripBillboardAds(root) || modified

            if (modified) {
                val result = root.toString().toByteArray()
                Log.v(TAG, "YouTube response stripped ads: ${body.size} -> ${result.size} bytes")
                result
            } else {
                body
            }
        } catch (e: Exception) {
            e.printStackTrace()
            body
        }
    }

    /**
     * Recursively walk a JSONObject/JSONArray and remove any objects that
     * contain adRenderer or promotionalVideoRenderer keys.
     */
    private fun removeAdRenderersRecursively(obj: Any?): Boolean {
        var modified = false
        when (obj) {
            is JSONObject -> {
                val keysToRemove = mutableListOf<String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = obj.opt(key)
                    if (value is JSONObject) {
                        if (value.has("adRenderer") ||
                            value.has("promotionalVideoRenderer") ||
                            value.has("bannerAdRenderer") ||
                            value.has("imageOverlayAdRenderer")
                        ) {
                            keysToRemove.add(key)
                            modified = true
                            continue
                        }
                    }
                    if (removeAdRenderersRecursively(value)) {
                        modified = true
                    }
                }
                for (key in keysToRemove) {
                    obj.remove(key)
                }
            }
            is JSONArray -> {
                var i = 0
                while (i < obj.length()) {
                    val item = obj.opt(i)
                    if (item is JSONObject && (
                                item.has("adRenderer") ||
                                item.has("promotionalVideoRenderer") ||
                                item.has("bannerAdRenderer") ||
                                item.has("imageOverlayAdRenderer")
                                )
                    ) {
                        obj.remove(i)
                        modified = true
                        // Don't increment i, next item shifts down
                    } else {
                        if (removeAdRenderersRecursively(item)) modified = true
                        i++
                    }
                }
            }
        }
        return modified
    }

    private fun stripEngagementPanels(root: JSONObject): Boolean {
        var modified = false
        if (root.has("engagementPanels")) {
            val panels = root.optJSONArray("engagementPanels")
            if (panels != null) {
                var i = 0
                while (i < panels.length()) {
                    val panel = panels.optJSONObject(i)
                    val id = panel?.optJSONObject("engagementPanelSectionListRenderer")
                        ?.optString("panelIdentifier", "")
                    if (id != null && (
                                id.contains("ads_", ignoreCase = true) ||
                                id.contains("promotion", ignoreCase = true)
                                )) {
                        panels.remove(i)
                        modified = true
                    } else {
                        i++
                    }
                }
            }
        }
        return modified
    }

    private fun stripBillboardAds(root: JSONObject): Boolean {
        var modified = false
        // In browse responses, contents may contain a billboard with an ad
        val contents = root.optJSONObject("contents")
        val browse = contents?.optJSONObject("browseContents")
        val single = browse?.optJSONObject("singleColumnBrowseResultsRenderer")
        val tabs = single?.optJSONArray("tabs")
        if (tabs != null) {
            for (i in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(i)
                val content = tab?.optJSONObject("tabRenderer")?.optJSONObject("content")
                val sectionList = content?.optJSONObject("sectionListRenderer")
                val sections = sectionList?.optJSONArray("contents")
                if (sections != null) {
                    var j = 0
                    while (j < sections.length()) {
                        val section = sections.optJSONObject(j)
                        val billboard = section?.optJSONObject("itemSectionRenderer")
                            ?.optJSONArray("contents")
                            ?.optJSONObject(0)
                            ?.optJSONObject("promotedVideoRenderer")
                        if (billboard != null) {
                            sections.remove(j)
                            modified = true
                        } else {
                            j++
                        }
                    }
                }
            }
        }
        return modified
    }
}
