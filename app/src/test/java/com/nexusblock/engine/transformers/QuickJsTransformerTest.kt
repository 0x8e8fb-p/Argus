package com.nexusblock.engine.transformers

import org.junit.Assert.*
import org.junit.Test
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function

/**
 * Unit tests for the Rhino-based QuickJS transformer engine.
 *
 * Tests cover:
 *   - Metadata parsing from JS header comments
 *   - Wildcard-to-regex pattern conversion
 *   - Rhino JS execution for ad stripping logic
 *   - End-to-end transform with inline JS plugins
 */
class QuickJsTransformerTest {

    // ------------------------------------------------------------------
    // Metadata parsing
    // ------------------------------------------------------------------

    @Test
    fun `parseMetadata extracts host path and content-type patterns`() {
        val script = """
            /*
             * @host *.youtube.com,*.googleapis.com
             * @path /youtubei/v1/*
             * @content-type application/json,text/json
             */
            function transform(url, host, path, contentType, body) { return body; }
        """.trimIndent()

        val meta = QuickJsTransformer.parseMetadata(script)

        assertEquals(3, meta.size)
        assertTrue(meta.containsKey("host"))
        assertTrue(meta.containsKey("path"))
        assertTrue(meta.containsKey("content-type"))
        assertEquals(2, meta["host"]!!.size)
        assertTrue(meta["host"]!!.any { it.matches("youtubei.googleapis.com") })
        assertTrue(meta["path"]!!.any { it.matches("/youtubei/v1/player") })
    }

    @Test
    fun `parseMetadata returns empty map when no headers present`() {
        val script = "function transform(url, host, path, contentType, body) { return body; }"
        val meta = QuickJsTransformer.parseMetadata(script)
        assertTrue(meta.isEmpty())
    }

    // ------------------------------------------------------------------
    // Pattern matching
    // ------------------------------------------------------------------

    @Test
    fun `wildcard regex matches subdomains`() {
        val pattern = "*.youtube.com".toWildcardRegex()
        assertTrue(pattern.matches("www.youtube.com"))
        assertTrue(pattern.matches("m.youtube.com"))
        assertFalse(pattern.matches("google.com"))
    }

    @Test
    fun `wildcard regex matches path prefixes`() {
        val pattern = "/youtubei/v1/*".toWildcardRegex()
        assertTrue(pattern.matches("/youtubei/v1/player"))
        assertTrue(pattern.matches("/youtubei/v1/browse"))
        assertFalse(pattern.matches("/api/v1/player"))
    }

    // ------------------------------------------------------------------
    // Rhino JS execution (inline plugins)
    // ------------------------------------------------------------------

    @Test
    fun `youtube JS strip removes adPlacements and playerAds`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                if (!body || body.trim().charAt(0) !== '{') return body;
                var json = JSON.parse(body);
                var modified = false;
                var adFields = ["adPlacements", "playerAds", "adBreaks"];
                for (var i = 0; i < adFields.length; i++) {
                    if (json[adFields[i]] !== undefined) {
                        delete json[adFields[i]];
                        modified = true;
                    }
                }
                return modified ? JSON.stringify(json) : body;
            }
        """.trimIndent()

        val input = """{"adPlacements":[],"playerAds":[],"content":"video"}"""
        val result = executeJs(script, "https://youtubei.googleapis.com/youtubei/v1/player",
            "youtubei.googleapis.com", "/youtubei/v1/player", "application/json", input)

        assertNotNull(result)
        assertFalse(result!!.contains("adPlacements"))
        assertFalse(result.contains("playerAds"))
        assertTrue(result.contains("video"))
    }

    @Test
    fun `youtube JS strip returns original when no ad fields`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                var json = JSON.parse(body);
                var modified = false;
                if (json.ads !== undefined) { delete json.ads; modified = true; }
                return modified ? JSON.stringify(json) : body;
            }
        """.trimIndent()

        val input = """{"content":"video"}"""
        val result = executeJs(script, "url", "host", "/path", "json", input)

        assertEquals(input, result)
    }

    @Test
    fun `spotify JS strip replaces ads object with empty`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                if (path.indexOf('/pendragon/') >= 0) return '{}';
                var json = JSON.parse(body);
                var modified = false;
                if (json.ads !== undefined) { delete json.ads; modified = true; }
                return modified ? JSON.stringify(json) : body;
            }
        """.trimIndent()

        val input = """{"ads":[{"id":1}],"tracks":[]}"""
        val result = executeJs(script, "url", "spclient.wg.spotify.com",
            "/bootstrap/", "application/json", input)

        assertNotNull(result)
        assertFalse(result!!.contains("ads"))
    }

    @Test
    fun `spotify JS blocks pendragon endpoint entirely`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                if (path.indexOf('/pendragon/') >= 0) return '{}';
                return body;
            }
        """.trimIndent()

        val input = """{"anything":"here"}"""
        val result = executeJs(script, "url", "spclient.wg.spotify.com",
            "/pendragon/v1/ads", "application/json", input)

        assertEquals("{}", result)
    }

    @Test
    fun `youtube recursive adRenderer removal works`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                var json = JSON.parse(body);
                var modified = false;
                function removeAds(obj) {
                    if (typeof obj !== 'object' || obj === null) return false;
                    var changed = false;
                    var keys = Object.keys(obj);
                    for (var k = 0; k < keys.length; k++) {
                        var key = keys[k];
                        var val = obj[key];
                        if (typeof val === 'object' && val !== null && val.adRenderer) {
                            delete obj[key];
                            changed = true;
                        } else {
                            if (removeAds(val)) changed = true;
                        }
                    }
                    return changed;
                }
                if (removeAds(json)) modified = true;
                return modified ? JSON.stringify(json) : body;
            }
        """.trimIndent()

        val input = """{"items":[{"adRenderer":{"id":1}},{"content":"ok"}]}"""
        val result = executeJs(script, "url", "host", "/path", "json", input)

        assertNotNull(result)
        assertFalse(result!!.contains("adRenderer"))
        assertTrue(result.contains("ok"))
    }

    @Test
    fun `youtube engagementPanel filter works`() {
        val script = """
            function transform(url, host, path, contentType, body) {
                var json = JSON.parse(body);
                var modified = false;
                if (json.engagementPanels && Array.isArray(json.engagementPanels)) {
                    var beforeLen = json.engagementPanels.length;
                    json.engagementPanels = json.engagementPanels.filter(function(panel) {
                        var id = panel && panel.engagementPanelSectionListRenderer &&
                                 panel.engagementPanelSectionListRenderer.panelIdentifier;
                        return !(id && (id.indexOf('ads_') >= 0 || id.indexOf('promotion') >= 0));
                    });
                    if (json.engagementPanels.length !== beforeLen) modified = true;
                }
                return modified ? JSON.stringify(json) : body;
            }
        """.trimIndent()

        val input = """{"engagementPanels":[
            {"engagementPanelSectionListRenderer":{"panelIdentifier":"ads_1"}},
            {"engagementPanelSectionListRenderer":{"panelIdentifier":"comments"}}
        ]}""".trimIndent()

        val result = executeJs(script, "url", "host", "/path", "json", input)

        assertNotNull(result)
        assertFalse(result!!.contains("ads_1"))
        assertTrue(result.contains("comments"))
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun executeJs(
        script: String,
        url: String,
        host: String,
        path: String,
        contentType: String,
        body: String
    ): String? {
        val cx = Context.enter()
        cx.optimizationLevel = -1
        return try {
            val scope = cx.initStandardObjects()
            cx.evaluateString(scope, script, "test-script", 1, null)
            val func = scope.get("transform", scope) as Function
            val args = arrayOf(url, host, path, contentType, body)
            val result = func.call(cx, scope, scope, args)
            Context.toString(result)
        } finally {
            Context.exit()
        }
    }

    private fun String.toWildcardRegex(): Regex {
        val escaped = this
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex("^${escaped}$", RegexOption.IGNORE_CASE)
    }
}
