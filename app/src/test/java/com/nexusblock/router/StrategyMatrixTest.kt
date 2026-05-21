package com.nexusblock.router

import org.junit.Assert.*
import org.junit.Test

class StrategyMatrixTest {

    private val matrix = StrategyMatrix()

    @Test
    fun `returns YouTube profile with MITM enabled`() {
        val profile = matrix.getProfile("com.google.android.youtube.tv")
        assertEquals("YouTube TV", profile.appName)
        assertTrue(profile.dns)
        assertTrue(profile.sni)
        assertTrue(profile.mitm)
        assertTrue(profile.accessibility)
    }

    @Test
    fun `returns Netflix profile with MITM disabled and accessibility enabled`() {
        val profile = matrix.getProfile("com.netflix.ninja")
        assertEquals("Netflix", profile.appName)
        assertTrue(profile.dns)
        assertFalse(profile.sni)
        assertFalse(profile.mitm)
        assertTrue(profile.accessibility)
    }

    @Test
    fun `returns Spotify profile with MITM enabled`() {
        val profile = matrix.getProfile("com.spotify.tv.android")
        assertEquals("Spotify TV", profile.appName)
        assertTrue(profile.mitm)
        assertFalse(profile.accessibility)
    }

    @Test
    fun `returns generic fallback for unknown packages`() {
        val profile = matrix.getProfile("com.unknown.app")
        assertEquals("All other apps", profile.appName)
        assertTrue(profile.dns)
        assertTrue(profile.sni)
        assertFalse(profile.mitm)
    }

    @Test
    fun `user override takes precedence over default`() {
        matrix.setOverride(
            AppProfile(
                packageName = "com.google.android.youtube.tv",
                appName = "YouTube TV (custom)",
                mitm = false,
                accessibility = false
            )
        )
        val profile = matrix.getProfile("com.google.android.youtube.tv")
        assertEquals("YouTube TV (custom)", profile.appName)
        assertFalse(profile.mitm)

        matrix.removeOverride("com.google.android.youtube.tv")
        val restored = matrix.getProfile("com.google.android.youtube.tv")
        assertEquals("YouTube TV", restored.appName)
        assertTrue(restored.mitm)
    }

    @Test
    fun `listProfiles returns all defaults plus overrides`() {
        matrix.setOverride(
            AppProfile("com.example.app", "Example", mitm = true)
        )
        val profiles = matrix.listProfiles()
        assertTrue(profiles.any { it.packageName == "com.example.app" })
        assertTrue(profiles.any { it.packageName == "com.netflix.ninja" })
    }
}
