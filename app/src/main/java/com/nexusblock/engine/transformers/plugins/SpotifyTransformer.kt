package com.nexusblock.engine.transformers.plugins

import android.util.Log
import com.nexusblock.engine.transformers.TransformerPlugin
import org.json.JSONArray
import org.json.JSONObject

/**
 * Spotify API response transformer.
 *
 * Spotify serves audio ads and banner ads through its spclient.wg.spotify.com
 * API. The Stash/Surge approach intercepts bootstrap/user-customization-service
 * and pendragon endpoints to strip ad data.
 *
 * Target endpoints:
 * - /pendragon/ — ad metadata endpoint; we reject it entirely
 * - /bootstrap/ — contains ad-related flags and user attributes
 * - /user-customization-service/ — personalization data, sometimes contains ad prefs
 * - /artistview/ — may inject promotional banners
 *
 * Fields stripped:
 * - ads[] arrays in user profile
 * - ad-formats configuration
 * - sponsorship banners in artist view
 * - promo/premium upsell interstitials
 */
class SpotifyTransformer : TransformerPlugin {

    companion object {
        private const val TAG = "Argus/SpotifyTransform"

        private val CONTENT_TYPES = setOf(
            "application/json",
            "text/json",
            "application/x-protobuf",
            "application/octet-stream"
        )
    }

    override fun name(): String = "SpotifyTransformer"

    override fun matches(host: String, path: String, contentType: String, method: String): Boolean {
        return (host.contains("spclient.wg.spotify.com") ||
                host.endsWith("-spclient.spotify.com")) &&
                CONTENT_TYPES.any { contentType.contains(it) }
    }

    override fun transform(
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: ByteArray
    ): ByteArray? {
        // For pendragon endpoints, return synthetic empty JSON
        if (path.contains("/pendragon/")) {
            Log.v(TAG, "Rejecting Spotify pendragon request: $path")
            return "{}".toByteArray()
        }

        val jsonString = body.decodeToString()
        if (!jsonString.trimStart().startsWith("{")) return body

        return try {
            val root = JSONObject(jsonString)
            var modified = false

            // Strip top-level ad arrays
            if (root.has("ads")) {
                root.remove("ads")
                modified = true
            }
            if (root.has("ad_formats")) {
                root.remove("ad_formats")
                modified = true
            }
            if (root.has("sponsorships")) {
                root.remove("sponsorships")
                modified = true
            }

            // Strip promotional content from artist view
            modified = removePromotionalFields(root) || modified

            // Strip ad-related attributes from bootstrap user attributes
            modified = stripAdAttributes(root) || modified

            if (modified) {
                val result = root.toString().toByteArray()
                Log.v(TAG, "Spotify response stripped: ${body.size} -> ${result.size} bytes")
                result
            } else {
                body
            }
        } catch (e: Exception) {
            Log.w(TAG, "Spotify JSON transform failed", e)
            body
        }
    }

    private fun removePromotionalFields(root: JSONObject): Boolean {
        var modified = false

        // artistview may contain promotionalVideoRenderer or merch sections
        val keysToRemove = mutableListOf<String>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = root.opt(key)
            if (value is JSONObject) {
                if (value.has("promotionalVideoRenderer") ||
                    value.has("merchCard") ||
                    value.has("concertCard")
                ) {
                    keysToRemove.add(key)
                    modified = true
                }
            }
        }
        for (key in keysToRemove) {
            root.remove(key)
        }
        return modified
    }

    private fun stripAdAttributes(root: JSONObject): Boolean {
        var modified = false
        val attrs = root.optJSONObject("attributes")
        if (attrs != null) {
            val adKeys = listOf(
                "ads_enabled",
                "ad_formattings",
                "ad_type",
                "ad_supported",
                "show_ads",
                "advertiser_name"
            )
            for (key in adKeys) {
                if (attrs.has(key)) {
                    attrs.put(key, false)
                    modified = true
                }
            }
        }
        return modified
    }
}
