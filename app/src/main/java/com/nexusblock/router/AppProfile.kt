package com.nexusblock.router

/**
 * Per-app defense profile. Determines which blocking layers are active
 * for a given package when it is in the foreground.
 *
 * @property packageName Target app package name
 * @property appName Human-readable display name
 * @property dns Whether to apply DNS filtering for this app
 * @property sni Whether to inspect TLS SNI for this app
 * @property mitm Whether to decrypt HTTPS API responses for this app
 * @property accessibility Whether to enable AccessibilityService ad skip for this app
 * @property remark Internal note on why this profile exists
 */
data class AppProfile(
    val packageName: String,
    val appName: String,
    val dns: Boolean = true,
    val sni: Boolean = true,
    val mitm: Boolean = false,
    val accessibility: Boolean = false,
    val remark: String = ""
)
