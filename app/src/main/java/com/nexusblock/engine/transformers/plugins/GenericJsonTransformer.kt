package com.nexusblock.engine.transformers.plugins

import android.content.Context
import android.util.Log
import com.nexusblock.engine.transformers.TransformerPlugin
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader

class GenericJsonTransformer(context: Context? = null) : TransformerPlugin {

    companion object {
        private const val TAG = "Argus/GenericTransform"
        private const val RULES_DIR = "transformers"
    }

    data class TransformRule(
        val name: String,
        val hostPatterns: List<Regex>,
        val pathPatterns: List<Regex>,
        val removeFields: Set<String>,
        val removeObjectsWithKeys: Set<String>,
        val setNullFields: Set<String>
    )

    private val rules: List<TransformRule> = loadRules(context)

    override fun name(): String = "GenericJsonTransformer (${rules.size} rules)"

    override fun matches(host: String, path: String, contentType: String, method: String): Boolean {
        if (!contentType.contains("json")) return false
        return rules.any { rule ->
            rule.hostPatterns.any { it.matches(host) } &&
            rule.pathPatterns.any { it.matches(path) }
        }
    }

    override fun transform(
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: ByteArray
    ): ByteArray? {
        val matchingRules = rules.filter { rule ->
            rule.hostPatterns.any { it.matches(host) } &&
            rule.pathPatterns.any { it.matches(path) }
        }
        if (matchingRules.isEmpty()) return body

        val jsonString = body.decodeToString()
        if (!jsonString.trimStart().startsWith("{")) return body

        return try {
            val root = JSONObject(jsonString)
            var modified = false

            for (rule in matchingRules) {
                for (field in rule.removeFields) {
                    if (root.has(field)) {
                        root.remove(field)
                        modified = true
                    }
                }
                for (field in rule.setNullFields) {
                    if (root.has(field)) {
                        root.put(field, JSONObject.NULL)
                        modified = true
                    }
                }
                if (rule.removeObjectsWithKeys.isNotEmpty()) {
                    if (removeObjectsRecursively(root, rule.removeObjectsWithKeys)) {
                        modified = true
                    }
                }
            }

            if (modified) root.toString().toByteArray() else body
        } catch (e: Exception) {
            Log.w(TAG, "Generic transform failed for $host$path", e)
            body
        }
    }

    private fun removeObjectsRecursively(obj: Any?, keysToRemove: Set<String>): Boolean {
        var modified = false
        when (obj) {
            is JSONObject -> {
                val removeList = mutableListOf<String>()
                val it = obj.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    val value = obj.opt(key)
                    if (value is JSONObject && keysToRemove.any { value.has(it) }) {
                        removeList.add(key)
                        modified = true
                    } else {
                        if (removeObjectsRecursively(value, keysToRemove)) modified = true
                    }
                }
                for (k in removeList) obj.remove(k)
            }
            is JSONArray -> {
                var i = 0
                while (i < obj.length()) {
                    val item = obj.opt(i)
                    if (item is JSONObject && keysToRemove.any { item.has(it) }) {
                        obj.remove(i)
                        modified = true
                    } else {
                        if (removeObjectsRecursively(item, keysToRemove)) modified = true
                        i++
                    }
                }
            }
        }
        return modified
    }

    private fun loadRules(context: Context?): List<TransformRule> {
        if (context == null) return emptyList()
        val rules = mutableListOf<TransformRule>()
        try {
            val assetList = context.assets.list(RULES_DIR) ?: return emptyList()
            for (fileName in assetList.filter { it.endsWith(".json") }) {
                try {
                    val text = context.assets.open("$RULES_DIR/$fileName")
                        .bufferedReader().use(BufferedReader::readText)
                    val obj = JSONObject(text)
                    val hostPatterns = obj.optJSONArray("host_patterns")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it).toWildcardRegex() }
                    } ?: emptyList()
                    val pathPatterns = obj.optJSONArray("path_patterns")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it).toWildcardRegex() }
                    } ?: listOf(".*".toRegex())
                    val removeFields = obj.optJSONArray("remove_fields")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    } ?: emptySet()
                    val removeObjectsWithKeys = obj.optJSONArray("remove_objects_with_keys")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    } ?: emptySet()
                    val setNullFields = obj.optJSONArray("set_null_fields")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    } ?: emptySet()

                    rules.add(TransformRule(
                        name = fileName.removeSuffix(".json"),
                        hostPatterns = hostPatterns,
                        pathPatterns = pathPatterns,
                        removeFields = removeFields,
                        removeObjectsWithKeys = removeObjectsWithKeys,
                        setNullFields = setNullFields
                    ))
                    Log.i(TAG, "Loaded rule '$fileName'")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load rule $fileName", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list rules", e)
        }
        return rules
    }

    private fun String.toWildcardRegex(): Regex {
        val escaped = this
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex("^${escaped}$", RegexOption.IGNORE_CASE)
    }
}
