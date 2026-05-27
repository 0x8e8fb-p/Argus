package com.nexusblock.engine.dns

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages DNS provider profiles with automatic remote refresh.
 *
 * Design intent: DNS provider IPs and DoH/DoT endpoints rarely change, but they
 * DO change over time (e.g., Cloudflare added IPv6 endpoints in 2020, AdGuard
 * changed DoH paths circa 2023). Rather than requiring an app update via the
 * Play Store, we refresh profiles from a lightweight remote source every
 * ~2 months (60 days). The refresh is a background best-effort operation;
 * if it fails, the built-in hardcoded profiles remain functional.
 *
 * Remote source:
 * A GitHub Gist or similar static JSON endpoint that mirrors the structure of
 * [DnsProviderProfile.BUILTINS]. The URL is intentionally simple and over
 * HTTPS so it can be served from any CDN or self-hosted domain if needed.
 */
@Singleton
class DnsProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val settingsRepo: SettingsRepository,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "Argus/DnsProfiles"

        /** Remote JSON endpoint containing updated DNS profiles. */
        private const val REMOTE_PROFILES_URL =
            "https://gist.githubusercontent.com/nexusblock/dns-profiles/head/profiles-v1.json"

        /** How often to attempt a remote refresh (60 days). */
        private val REFRESH_INTERVAL_MS = TimeUnit.DAYS.toMillis(60)

        /** Fallback: if remote fetch takes longer than this, abort. */
        private const val FETCH_TIMEOUT_MS = 15_000L

        private val PREFS_KEY_PROFILES = stringPreferencesKey("dns_profiles_json")
        private val PREFS_KEY_LAST_REFRESH = longPreferencesKey("dns_profiles_last_refresh")
        private val PREFS_KEY_ACTIVE_PROFILE = stringPreferencesKey("dns_active_profile_id")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /** Profiles currently in memory (may have been refreshed from remote). */
    @Volatile
    private var cachedProfiles: List<DnsProviderProfile> = DnsProviderProfile.BUILTINS

    /** Active profile ID currently selected by the user. */
    @Volatile
    private var activeProfileId: String = "adguard_standard"

    init {
        scope.launch {
            loadPersistedProfiles()
            maybeRefreshProfiles()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────

    /** All available profiles (built-in + any custom/updated from remote). */
    fun allProfiles(): List<DnsProviderProfile> = cachedProfiles

    /** The currently selected active profile. */
    fun activeProfile(): DnsProviderProfile =
        cachedProfiles.find { it.id == activeProfileId }
            ?: DnsProviderProfile.BUILTINS.first { it.id == "adguard_standard" }

    /** Set the active profile by ID. Persists immediately. */
    suspend fun setActiveProfile(id: String) {
        if (cachedProfiles.none { it.id == id }) {
            Log.w(TAG, "Unknown profile ID: $id")
            return
        }
        activeProfileId = id
        dataStore.edit { prefs ->
            prefs[PREFS_KEY_ACTIVE_PROFILE] = id
        }
        Log.i(TAG, "Active DNS profile set to: $id")
    }

    /** Force an immediate remote refresh (e.g., user triggered). */
    suspend fun forceRefresh(): Boolean = fetchAndMergeRemote()

    /** Time until the next scheduled auto-refresh (in millis, may be negative). */
    suspend fun millisUntilNextRefresh(): Long {
        val last = dataStore.data.first()[PREFS_KEY_LAST_REFRESH] ?: 0L
        val next = last + REFRESH_INTERVAL_MS
        return next - System.currentTimeMillis()
    }

    // ──────────────────────────────────────────────────────────────
    // Internal logic
    // ──────────────────────────────────────────────────────────────

    private suspend fun loadPersistedProfiles() {
        try {
            val prefs = dataStore.data.first()
            val jsonStr = prefs[PREFS_KEY_PROFILES]
            activeProfileId = prefs[PREFS_KEY_ACTIVE_PROFILE] ?: "adguard_standard"

            if (!jsonStr.isNullOrBlank()) {
                val remoteList = Json.decodeFromString<List<DnsProviderProfile>>(jsonStr)
                // Merge: remote overrides built-in for same ID, adds new ones
                val merged = DnsProviderProfile.BUILTINS.associateBy { it.id }
                    .toMutableMap()
                remoteList.forEach { merged[it.id] = it }
                cachedProfiles = merged.values.toList()
                Log.i(TAG, "Loaded ${cachedProfiles.size} profiles from persistence")
            } else {
                cachedProfiles = DnsProviderProfile.BUILTINS
                Log.i(TAG, "No persisted profiles, using ${cachedProfiles.size} built-ins")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load persisted profiles, falling back to built-ins", e)
            cachedProfiles = DnsProviderProfile.BUILTINS
        }
    }

    private suspend fun maybeRefreshProfiles() {
        val last = dataStore.data.first()[PREFS_KEY_LAST_REFRESH] ?: 0L
        val elapsed = System.currentTimeMillis() - last
        if (elapsed < REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Profile refresh not due yet (${elapsed / 86400000} days elapsed)")
            return
        }
        fetchAndMergeRemote()
    }

    private suspend fun fetchAndMergeRemote(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(REMOTE_PROFILES_URL)
                .header("Accept", "application/json")
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()

            val response = withTimeout(FETCH_TIMEOUT_MS) {
                okHttpClient.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                Log.w(TAG, "Remote profile fetch failed: HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                Log.w(TAG, "Remote profile fetch returned empty body")
                return@withContext false
            }

            val remoteProfiles = json.decodeFromString<List<DnsProviderProfile>>(body)

            // Merge: remote overrides built-in for same ID
            val merged = DnsProviderProfile.BUILTINS.associateBy { it.id }
                .toMutableMap()
            remoteProfiles.forEach { merged[it.id] = it }
            cachedProfiles = merged.values.toList()

            // Persist
            dataStore.edit { prefs ->
                prefs[PREFS_KEY_PROFILES] = json.encodeToString(cachedProfiles)
                prefs[PREFS_KEY_LAST_REFRESH] = System.currentTimeMillis()
            }

            Log.i(TAG, "Profiles refreshed from remote: ${cachedProfiles.size} total")
            true
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Remote profile fetch timed out after ${FETCH_TIMEOUT_MS}ms")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Remote profile fetch failed", e)
            false
        }
    }
}
