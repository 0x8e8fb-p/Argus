package com.nexusblock.engine

import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.repository.BlocklistParsers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun plainDomainBlocksSubdomains() {
        val engine = RuleEngine()

        engine.loadRules(listOf(BlockedDomain(host = "doubleclick.net", source = "test")))

        assertTrue(engine.isBlocked("doubleclick.net"))
        assertTrue(engine.isBlocked("pubads.g.doubleclick.net"))
    }

    @Test
    fun exactAllowDoesNotAllowSiblingSubdomains() {
        val engine = RuleEngine()

        engine.loadRules(
            listOf(
                BlockedDomain(host = "ads.youtube.com", source = "test"),
                BlockedDomain(host = "@@youtube.com", source = "test")
            )
        )

        assertFalse(engine.isBlocked("youtube.com"))
        assertTrue(engine.isBlocked("ads.youtube.com"))
    }

    @Test
    fun youtubeRecommendationAllowlistDoesNotUnblockAdSubdomains() {
        val engine = RuleEngine()

        engine.loadRules(
            listOf(
                BlockedDomain(host = "ads.youtube.com", source = "test"),
                BlockedDomain(host = "s.youtube.com", source = "test"),
                BlockedDomain(host = "@@youtube.com", source = "test"),
                BlockedDomain(host = "@@www.youtube.com", source = "test"),
                BlockedDomain(host = "@@m.youtube.com", source = "test")
            )
        )

        assertFalse(engine.isBlocked("youtube.com"))
        assertFalse(engine.isBlocked("www.youtube.com"))
        assertTrue(engine.isBlocked("ads.youtube.com"))
        assertTrue(engine.isBlocked("s.youtube.com"))
    }

    @Test
    fun subdomainAllowStillAllowsChildrenWhenExplicitlyRequested() {
        val engine = RuleEngine()

        engine.loadRules(
            listOf(
                BlockedDomain(host = "example.com", source = "test"),
                BlockedDomain(host = "@@||safe.example.com", source = "test")
            )
        )

        assertFalse(engine.isBlocked("safe.example.com"))
        assertFalse(engine.isBlocked("cdn.safe.example.com"))
        assertTrue(engine.isBlocked("ads.example.com"))
    }

    @Test
    fun adguardParserPreservesExceptionRules() {
        val rules = BlocklistParsers.parseAdGuardFilter(
            """
            ||ads.example.com^
            @@||safe.example.com^
            example.com##.ad
            """.trimIndent()
        )

        assertEquals(listOf("ads.example.com", "@@||safe.example.com^"), rules)
    }

    @Test
    fun googlevideoAdPatternBypassesBloomFastFail() {
        val engine = RuleEngine()

        engine.loadRules(emptyList())

        assertTrue(engine.isBlocked("r1---sn-qxa7sn7s-googlevideo-ad-abc.googlevideo.com"))
        assertFalse(engine.isBlocked("r1---sn-qxa7sn7s.googlevideo.com"))
    }

    @Test
    fun hostParserNormalizesSinkholeEntries() {
        val rules = BlocklistParsers.parseHostFile(
            """
            0.0.0.0 ads.example.com # inline comment
            127.0.0.1 tracker.example.net
            ::1 ipv6-sink.example.org
            """.trimIndent()
        )

        assertEquals(
            listOf("ads.example.com", "tracker.example.net", "ipv6-sink.example.org"),
            rules
        )
    }
}
