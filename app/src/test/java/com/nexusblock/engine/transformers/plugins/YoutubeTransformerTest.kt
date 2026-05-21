package com.nexusblock.engine.transformers.plugins

import org.junit.Assert.*
import org.junit.Test

class YoutubeTransformerTest {

    private val transformer = YoutubeTransformer()

    @Test
    fun `matches YouTube API endpoints`() {
        assertTrue(
            transformer.matches(
                "youtubei.googleapis.com",
                "/youtubei/v1/player",
                "application/json",
                "POST"
            )
        )
        assertTrue(
            transformer.matches(
                "www.youtube.com",
                "/youtubei/v1/next",
                "application/json",
                "POST"
            )
        )
    }

    @Test
    fun `does not match non-YouTube hosts`() {
        assertFalse(
            transformer.matches(
                "googlevideo.com",
                "/videoplayback",
                "application/json",
                "GET"
            )
        )
    }

    @Test
    fun `strips adPlacements from player response`() {
        val input = """
        {
          "videoDetails": {"videoId":"abc123"},
          "adPlacements": [{"adRenderer":{"adId":"1"}}],
          "streamingData": {"formats":[]}
        }
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://youtubei.googleapis.com/youtubei/v1/player",
            "youtubei.googleapis.com",
            "/youtubei/v1/player",
            "application/json",
            input
        )

        assertNotNull(result)
        val json = result!!.decodeToString()
        println("DEBUG OUTPUT: $json")
        assertFalse("Should not contain adPlacements but got: $json", json.contains(""""adPlacements"""))
        assertTrue(json.contains(""""videoDetails"""))
        assertTrue(json.contains(""""streamingData"""))
    }

    @Test
    fun `strips playerAds from player response`() {
        val input = """
        {
          "playerAds": [{"adId":"1"}],
          "videoDetails": {"videoId":"xyz"}
        }
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://youtubei.googleapis.com/youtubei/v1/player",
            "youtubei.googleapis.com",
            "/youtubei/v1/player",
            "application/json",
            input
        )

        assertNotNull(result)
        assertFalse(result!!.decodeToString().contains(""""playerAds"""))
    }

    @Test
    fun `returns original body when no ads present`() {
        val input = """
        {"videoDetails":{"videoId":"abc"},"streamingData":{"formats":[]}}
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://youtubei.googleapis.com/youtubei/v1/player",
            "youtubei.googleapis.com",
            "/youtubei/v1/player",
            "application/json",
            input
        )

        assertSame(input, result)
    }

    @Test
    fun `removes adRenderer objects recursively`() {
        val input = """
        {
          "contents": {
            "twoColumnWatchNextResults": {
              "results": {
                "results": {
                  "contents": [
                    {"videoPrimaryInfoRenderer": {"title":"Real Video"}},
                    {"adRenderer": {"adId":"bad"}}
                  ]
                }
              }
            }
          }
        }
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://youtubei.googleapis.com/youtubei/v1/next",
            "youtubei.googleapis.com",
            "/youtubei/v1/next",
            "application/json",
            input
        )

        assertNotNull(result)
        val json = result!!.decodeToString()
        assertTrue(json.contains("videoPrimaryInfoRenderer"))
        assertFalse(json.contains(""""adRenderer"""))
    }

    @Test
    fun `strips engagementPanels containing ads`() {
        val input = """
        {
          "engagementPanels": [
            {"engagementPanelSectionListRenderer":{"panelIdentifier":"ads_companion"}},
            {"engagementPanelSectionListRenderer":{"panelIdentifier":"comment"}}
          ]
        }
        """.trimIndent().toByteArray()

        val result = transformer.transform(
            "https://youtubei.googleapis.com/youtubei/v1/player",
            "youtubei.googleapis.com",
            "/youtubei/v1/player",
            "application/json",
            input
        )

        assertNotNull(result)
        val json = result!!.decodeToString()
        assertFalse(json.contains("ads_companion"))
        assertTrue(json.contains("comment"))
    }
}
