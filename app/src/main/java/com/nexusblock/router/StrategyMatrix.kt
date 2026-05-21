package com.nexusblock.router

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardcoded strategy matrix mapping major OTT app packages to their
 * optimal defense-layer combinations.
 *
 * Rationale per app is documented in the `remark` field of each profile.
 * Users can override these defaults via the App Strategy Editor UI.
 */
@Singleton
class StrategyMatrix @Inject constructor() {

    companion object {
        /**
         * Default built-in profiles for major streaming platforms.
         * These are loaded on first install and can be user-modified afterward.
         */
        val DEFAULT_PROFILES: List<AppProfile> = listOf(
            // ─── YouTube ───────────────────────────────────────────────
            AppProfile(
                packageName = "com.google.android.youtube.tv",
                appName = "YouTube TV",
                dns = true, sni = true, mitm = true, accessibility = true,
                remark = "MITM on youtubei.googleapis.com strips adPlacements/playerAds. " +
                        "Accessibility handles residual SSAI prerolls."
            ),
            AppProfile(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                dns = true, sni = true, mitm = true, accessibility = true,
                remark = "Same strategy as YouTube TV."
            ),

            // ─── Spotify ───────────────────────────────────────────────
            AppProfile(
                packageName = "com.spotify.tv.android",
                appName = "Spotify TV",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "MITM on spclient.wg.spotify.com strips audio ad metadata."
            ),
            AppProfile(
                packageName = "com.spotify.music",
                appName = "Spotify",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "Same strategy as Spotify TV."
            ),

            // ─── Hotstar / JioHotstar ───────────────────────────────────
            AppProfile(
                packageName = "in.startv.hotstar",
                appName = "Hotstar",
                dns = true, sni = true, mitm = true, accessibility = true,
                remark = "Mixed client-side and SSAI ads. MITM for API manifest stripping; " +
                        "Accessibility as fallback for SSAI segments."
            ),
            AppProfile(
                packageName = "in.startv.hotstar.dplus",
                appName = "Disney+ Hotstar",
                dns = true, sni = true, mitm = true, accessibility = true,
                remark = "Same strategy as Hotstar."
            ),

            // ─── JioCinema ─────────────────────────────────────────────
            AppProfile(
                packageName = "com.jio.media.jiobeats",
                appName = "JioCinema",
                dns = true, sni = true, mitm = false, accessibility = true,
                remark = "Heavy SSAI usage; API rewriting is unreliable. Fallback to Accessibility."
            ),

            // ─── SonyLIV ───────────────────────────────────────────────
            AppProfile(
                packageName = "com.sonyliv",
                appName = "SonyLIV",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "Primarily client-side ads via API. MITM effective."
            ),

            // ─── ZEE5 ───────────────────────────────────────────────────
            AppProfile(
                packageName = "com.graymatrix.did",
                appName = "ZEE5",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "Client-side ads via VAST. MITM strips ad response objects."
            ),

            // ─── MX Player ─────────────────────────────────────────────
            AppProfile(
                packageName = "com.mxtech.videoplayer.ad",
                appName = "MX Player",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "Client-side ads via API."
            ),

            // ─── Netflix ────────────────────────────────────────────────
            AppProfile(
                packageName = "com.netflix.ninja",
                appName = "Netflix",
                dns = true, sni = false, mitm = false, accessibility = true,
                remark = "Pure SSAI. No separate ad domain to MITM. " +
                        "DNS blocks analytics. Accessibility mutes audio spikes."
            ),
            AppProfile(
                packageName = "com.netflix.mediaclient",
                appName = "Netflix Mobile",
                dns = true, sni = false, mitm = false, accessibility = true,
                remark = "Same strategy as Netflix TV."
            ),

            // ─── Amazon Prime Video ─────────────────────────────────────
            AppProfile(
                packageName = "com.amazon.amazonvideo.livingroom",
                appName = "Prime Video",
                dns = true, sni = false, mitm = false, accessibility = true,
                remark = "Pure SSAI with certificate pinning on video endpoints. " +
                        "DNS blocks trackers. Accessibility catches ad loudness."
            ),

            // ─── Apple TV ───────────────────────────────────────────────
            AppProfile(
                packageName = "com.apple.atve.android.appletv",
                appName = "Apple TV",
                dns = true, sni = false, mitm = false, accessibility = true,
                remark = "SSAI. Minimal client-side ads."
            ),

            // ─── Voot ───────────────────────────────────────────────────
            AppProfile(
                packageName = "com.tv.v18.viola",
                appName = "Voot",
                dns = true, sni = true, mitm = true, accessibility = false,
                remark = "Client-side ads via API."
            ),

            // ─── Google TV Launcher ─────────────────────────────────────
            AppProfile(
                packageName = "com.google.android.apps.tv.launcher.phone",
                appName = "Google TV Launcher",
                dns = true, sni = true, mitm = false, accessibility = false,
                remark = "Launcher promotional tiles only. DNS handles tuned."
            ),

            // ─── Generic fallback ───────────────────────────────────────
            AppProfile(
                packageName = "*",
                appName = "All other apps",
                dns = true, sni = true, mitm = false, accessibility = false,
                remark = "Default: DNS + SNI only. No per-app special handling."
            )
        )
    }

    private var userOverrides: MutableMap<String, AppProfile> = mutableMapOf()

    /**
     * Look up the profile for a package, applying user overrides if present.
     */
    fun getProfile(packageName: String): AppProfile {
        return userOverrides[packageName]
            ?: DEFAULT_PROFILES.find { it.packageName == packageName }
            ?: DEFAULT_PROFILES.find { it.packageName == "*" }!!
    }

    fun setOverride(profile: AppProfile) {
        userOverrides[profile.packageName] = profile
    }

    fun removeOverride(packageName: String) {
        userOverrides.remove(packageName)
    }

    fun listProfiles(): List<AppProfile> {
        val overridden = userOverrides.keys
        return DEFAULT_PROFILES.filter { it.packageName !in overridden } +
                userOverrides.values.toList()
    }
}
