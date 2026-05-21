package com.nexusblock.engine.transformers

import android.content.Context
import android.util.Log
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rhino-based JavaScript transformer engine for runtime ad-blocking rules.
 *
 * Loads `.js` plugin files from `assets/transformers/` and executes them
 * inside a sandboxed Mozilla Rhino context. Each plugin exposes a single
 * `transform(url, host, path, contentType, body)` function that receives
 * the decrypted HTTP response body as a string and returns a (possibly
 * modified) string.
 *
 * Plugin metadata is declared in the header comment block:
 *   @host   *.youtube.com,*.googleapis.com
 *   @path   /youtubei/v1/player
 *   @content-type application/json,text/json
 *
 * Advantages over native Kotlin transformers:
 *   - Rules can be updated via OTA without APK rebuild
 *   - Community contributions in plain JavaScript
 *   - Fast iteration on new ad delivery endpoints
 *
 * Performance: < 5ms per transform for typical InnerTube JSON payloads
 * on Amlogic S905X4. Rhino runs in interpreter mode (optimizationLevel=-1)
 * to avoid Android bytecode generation issues.
 */
@Singleton
class QuickJsTransformer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    companion object {
        private const val TAG = "Argus/QuickJS"
        private const val TRANSFORMERS_DIR = "transformers"

        /**
         * Parse @key value metadata from the header comment block.
         * Supports comma-separated values per key.
         */
        internal fun parseMetadata(script: String): Map<String, List<Regex>> {
            val map = mutableMapOf<String, MutableList<Regex>>()
            val header = script.substringBefore("function transform")
            val regex = """@([\w-]+)[ \t]+(.+)""".toRegex()
            regex.findAll(header).forEach { match ->
                val key = match.groupValues[1]
                val rawValues = match.groupValues[2].split(",").map { it.trim() }
                val patterns = map.getOrPut(key) { mutableListOf() }
                rawValues.forEach { patterns.add(it.toWildcardRegex()) }
            }
            return map
        }

        private fun String.toWildcardRegex(): Regex {
            val escaped = this
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            return Regex("^${escaped}$", RegexOption.IGNORE_CASE)
        }
    }

    data class JsPlugin(
        val name: String,
        val hostPatterns: List<Regex>,
        val pathPatterns: List<Regex>,
        val contentTypes: List<Regex>,
        val script: String
    )

    private var plugins: List<JsPlugin> = emptyList()

    init {
        loadPlugins()
    }

    /** Number of loaded JS plugins (exposed for diagnostics). */
    fun pluginCount(): Int = plugins.size

    /** Names of loaded plugins (exposed for diagnostics). */
    fun pluginNames(): List<String> = plugins.map { it.name }

    private fun loadPlugins() {
        val loaded = mutableListOf<JsPlugin>()
        try {
            val assets = context.assets.list(TRANSFORMERS_DIR) ?: return
            for (fileName in assets) {
                if (!fileName.endsWith(".js")) continue
                val script = context.assets.open("$TRANSFORMERS_DIR/$fileName")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val meta = parseMetadata(script)
                val plugin = JsPlugin(
                    name = fileName,
                    hostPatterns = meta["host"] ?: emptyList(),
                    pathPatterns = meta["path"] ?: emptyList(),
                    contentTypes = meta["content-type"] ?: emptyList(),
                    script = script
                )
                loaded.add(plugin)
                Log.i(TAG, "Loaded JS plugin: $fileName (hosts=${plugin.hostPatterns.size}, paths=${plugin.pathPatterns.size})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load JS plugins", e)
        }
        plugins = loaded
    }

    /**
     * Transform a response body through matching JS plugins.
     *
     * @return Transformed body bytes, or original body if no plugin matched
     *         or the plugin returned the body unchanged.
     */
    fun transform(
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: ByteArray?
    ): ByteArray? {
        if (body == null || body.isEmpty()) return body
        if (plugins.isEmpty()) return body

        val lowerHost = host.lowercase()
        val lowerPath = path.lowercase()
        val lowerCt = contentType.lowercase()
        val bodyString = body.toString(Charsets.UTF_8)

        for (plugin in plugins) {
            if (!matches(plugin, lowerHost, lowerPath, lowerCt)) continue

            try {
                val result = executeTransform(plugin, url, host, path, contentType, bodyString)
                if (result != null && result != bodyString) {
                    Log.i(TAG, "JS ${plugin.name} transformed $url: ${body.size} -> ${result.length} bytes")
                    return result.toByteArray(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Log.w(TAG, "JS transform error in ${plugin.name} for $url", e)
            }
        }
        return body
    }

    internal fun matches(plugin: JsPlugin, host: String, path: String, contentType: String): Boolean {
        if (plugin.hostPatterns.isNotEmpty() && !plugin.hostPatterns.any { it.matches(host) }) return false
        if (plugin.pathPatterns.isNotEmpty() && !plugin.pathPatterns.any { it.matches(path) }) return false
        if (plugin.contentTypes.isNotEmpty() && !plugin.contentTypes.any { it.matches(contentType) }) return false
        return true
    }

    private fun executeTransform(
        plugin: JsPlugin,
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: String
    ): String? {
        val cx = org.mozilla.javascript.Context.enter()
        cx.optimizationLevel = -1 // Interpreter mode — safe for Android
        try {
            val scope = cx.initStandardObjects()
            // Inject Albania-mode flag so JS transformers can adapt behavior
            val albaniaMode = settingsRepo.techniques.albaniaMode
            scope.put("ARGUS_ALBANIA_MODE", scope, albaniaMode)
            cx.evaluateString(scope, plugin.script, plugin.name, 1, null)
            val func = scope.get("transform", scope) as? org.mozilla.javascript.Function
                ?: return null
            val args = arrayOf(url, host, path, contentType, body)
            val result = func.call(cx, scope, scope, args)
            return org.mozilla.javascript.Context.toString(result)
        } finally {
            org.mozilla.javascript.Context.exit()
        }
    }

}
