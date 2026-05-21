package com.nexusblock.router

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground app detector and per-app strategy activator.
 *
 * Polls UsageStatsManager every 5 seconds to determine which app is in the
 * foreground, then looks up the corresponding AppProfile from StrategyMatrix.
 * Other engine layers query this router to decide whether to MITM, enable
 * Accessibility, or apply specific DNS rules.
 */
@Singleton
class StrategyRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val strategyMatrix: StrategyMatrix
) {
    companion object {
        private const val TAG = "Argus/Router"
        private const val POLL_INTERVAL_MS = 5000L

        /**
         * Hostname patterns that the MITM proxy should decrypt.
         * Only these domains are worth the CPU cost of TLS termination.
         * Video CDN domains (*.googlevideo.com, *.akamaized.net) are NOT
         * listed because they serve mixed content and cannot be safely rewritten.
         */
        private val MITM_HOST_PATTERNS = listOf(
            Regex("^youtubei\\.googleapis\\.com$"),
            Regex("^www\\.youtube\\.com$"),
            Regex("^s\\.youtube\\.com$"),
            Regex("^spclient\\.wg\\.spotify\\.com$"),
            Regex("^[a-z0-9]+-spclient\\.spotify\\.com$"),
            Regex("^.*hotstar\\.com$"),
            Regex("^api\\.hotstar\\.com$"),
            Regex("^.*sonyliv\\.com$"),
            Regex("^.*zee5\\.com$"),
            Regex("^.*mxplay\\.com$"),
            Regex("^.*jiocinema\\.com$"),
            Regex("^.*doubleclick\\.net$"),
            Regex("^.*googlesyndication\\.com$"),
            Regex("^.*googleadservices\\.com$")
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    private val _currentProfile = MutableStateFlow<AppProfile?>(null)
    val currentProfile: StateFlow<AppProfile?> = _currentProfile

    private val _activePackage = MutableStateFlow<String?>(null)
    val activePackage: StateFlow<String?> = _activePackage

    @Volatile
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        pollJob = scope.launch {
            while (isActive && isRunning) {
                detectForegroundApp()
                delay(POLL_INTERVAL_MS)
            }
        }
        Log.i(TAG, "StrategyRouter started")
    }

    fun stop() {
        isRunning = false
        pollJob?.cancel()
        pollJob = null
        _currentProfile.value = null
        _activePackage.value = null
        Log.i(TAG, "StrategyRouter stopped")
    }

    fun shouldMitm(host: String): Boolean {
        // If we have an active profile and it explicitly disables MITM, respect it.
        // Otherwise (no profile detected, e.g. UsageStats unavailable) fall through
        // to hostname matching so the transformer pipeline still works.
        val profile = _currentProfile.value
        if (profile != null && !profile.mitm) return false
        return MITM_HOST_PATTERNS.any { it.matches(host.lowercase()) }
    }

    fun shouldUseAccessibility(): Boolean {
        return _currentProfile.value?.accessibility == true
    }

    private fun detectForegroundApp() {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 10000,
                now
            )
            val topPackage = stats
                ?.filter { it.lastTimeUsed > 0 }
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
            if (topPackage != null && topPackage != _activePackage.value) {
                _activePackage.value = topPackage
                val profile = strategyMatrix.getProfile(topPackage)
                _currentProfile.value = profile
                Log.i(TAG, "Foreground: $topPackage profile=${profile.appName} mitm=${profile.mitm}")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "UsageStats permission not granted")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect foreground app", e)
        }
    }
}
