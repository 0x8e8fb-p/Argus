package com.nexusblock.engine.transformers

import android.content.Context
import android.util.Log
import com.nexusblock.engine.transformers.plugins.GenericJsonTransformer
import com.nexusblock.engine.transformers.plugins.SpotifyTransformer
import com.nexusblock.engine.transformers.plugins.YoutubeTransformer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central engine for loading and executing per-app response transformers.
 *
 * Each transformer plugin receives the decrypted HTTP response body and can
 * return a modified body with ad content stripped. Transformers are selected
 * by hostname + URL path matching.
 *
 * Plugins:
 * - YoutubeTransformer: strips adPlacements, playerAds, adBreaks from
 *   youtubei.googleapis.com protobuf/JSON responses
 * - SpotifyTransformer: strips ad segments from spclient.wg.spotify.com JSON
 * - GenericJsonTransformer: configurable JSON field stripper via asset config
 *
 * The engine runs transformers in priority order; the first matching transformer
 * wins. Transformers that don't match return the original body unchanged.
 */
@Singleton
class TransformerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quickJsTransformer: QuickJsTransformer
) {
    companion object {
        private const val TAG = "Argus/Transform"
    }

    // Layered transformer chain:
    // 1. Kotlin-native plugins (fast, compiled, type-safe)
    // 2. QuickJS runtime plugins (community-updatable, no APK rebuild)
    // 3. Generic JSON field stripper (asset-driven config)
    private val nativePlugins: List<TransformerPlugin> = listOf(
        YoutubeTransformer(),
        SpotifyTransformer(),
        GenericJsonTransformer(context)
    )

    /**
     * Transform a decrypted HTTP response body through the full chain.
     *
     * @param url Full request URL (e.g. https://youtubei.googleapis.com/youtubei/v1/player)
     * @param host Hostname extracted from the CONNECT target
     * @param requestPath Request path (e.g. /youtubei/v1/player)
     * @param contentType Response Content-Type header
     * @param requestMethod HTTP method (GET, POST, etc.)
     * @param body Raw decrypted response body bytes
     * @return Transformed body bytes, or original body if no plugin matched
     */
    fun transform(
        url: String,
        host: String,
        requestPath: String,
        contentType: String,
        requestMethod: String,
        body: ByteArray?
    ): ByteArray? {
        if (body == null || body.isEmpty()) return body

        val lowerHost = host.lowercase()
        val lowerPath = requestPath.lowercase()
        val lowerContentType = contentType.lowercase()

        // Pass 1: Native Kotlin plugins (fast path)
        for (plugin in nativePlugins) {
            if (plugin.matches(lowerHost, lowerPath, lowerContentType, requestMethod)) {
                try {
                    val transformed = plugin.transform(url, lowerHost, lowerPath, lowerContentType, body)
                    if (transformed !== body) {
                        Log.i(TAG, "Native ${plugin.name()} transformed $url: ${body.size} -> ${transformed?.size ?: 0} bytes")
                        return transformed
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Native transformer ${plugin.name()} failed for $url", e)
                }
            }
        }

        // Pass 2: QuickJS runtime plugins (community rules, sandboxed)
        try {
            val jsResult = quickJsTransformer.transform(url, lowerHost, lowerPath, lowerContentType, body)
            if (jsResult != null && !jsResult.contentEquals(body)) {
                Log.i(TAG, "QuickJS transformed $url: ${body.size} -> ${jsResult.size} bytes")
                return jsResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "QuickJS transformer failed for $url", e)
        }

        return body
    }
}

/**
 * Interface for response transformer plugins.
 */
interface TransformerPlugin {
    /** Plugin display name for logging. */
    fun name(): String

    /**
     * Return true if this plugin should handle responses for the given
     * host, path, content-type, and method.
     */
    fun matches(
        host: String,
        path: String,
        contentType: String,
        method: String
    ): Boolean

    /**
     * Transform the response body. Must return the original body reference if
     * no transformation was applied, to allow identity-check optimization.
     */
    fun transform(
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: ByteArray
    ): ByteArray?
}
