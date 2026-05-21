package com.nexusblock.engine.transformers.plugins

import org.junit.Assert.*
import org.junit.Test

class SpotifyTransformerTest {

    private val transformer = SpotifyTransformer()

    @Test
    fun `matches Spotify spclient endpoints`() {
        assertTrue(
            transformer.matches(
                "spclient.wg.spotify.com",
                "/bootstrap",
                "application/json",
                "GET"
            )
        )
        assertTrue(
            transformer.matches(
                "de-spclient.spotify.com",
                "/user-customization-service",
                "application/json",
                "GET"
            )
        )
    }

    @Test
    fun `rejects pendragon endpoint entirely`() {
        val input = """{"some":"data"}""".toByteArray()
        val result = transformer.transform(
            "https://spclient.wg.spotify.com/pendragon/v1/ads",
            "spclient.wg.spotify.com",
            "/pendragon/v1/ads",
            "application/json",
            input
        )
        assertNotNull(result)
        assertEquals("{}", result!!.decodeToString())
    }

    @Test
    fun `strips ads array from bootstrap`() {
        val input = """
        {
          "ads": [{"id":"1"},{"id":"2"}],
          "user": {"name":"test"}
        }
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://spclient.wg.spotify.com/bootstrap",
            "spclient.wg.spotify.com",
            "/bootstrap",
            "application/json",
            input
        )

        assertNotNull(result)
        val json = result!!.decodeToString()
        assertFalse(json.contains(""""ads"""))
        assertTrue(json.contains(""""user"""))
    }

    @Test
    fun `strips ad_formats field`() {
        val input = """
        {"ad_formats":{"enabled":true},"user":{"name":"test"}}
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://spclient.wg.spotify.com/bootstrap",
            "spclient.wg.spotify.com",
            "/bootstrap",
            "application/json",
            input
        )

        assertNotNull(result)
        assertFalse(result!!.decodeToString().contains(""""ad_formats"""))
    }

    @Test
    fun `disables ad attributes`() {
        val input = """
        {"attributes":{"ads_enabled":true,"show_ads":true,"username":"test"}}
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://spclient.wg.spotify.com/user-customization-service",
            "spclient.wg.spotify.com",
            "/user-customization-service",
            "application/json",
            input
        )

        assertNotNull(result)
        val json = result!!.decodeToString()
        assertTrue(json.contains(""""ads_enabled":false"""))
        assertTrue(json.contains(""""show_ads":false"""))
    }
}
