package com.nexusblock.engine

import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.VpnMode
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

enum class ActiveVpnMode {
    LUNA,
    LOCAL,
    TRANSITIONING,
    DISCONNECTED
}

/**
 * Orchestrates the primary (Luna VPN) and secondary (Local VPN) with
 * automatic failover and recovery.
 *
 * Flow:
 * 1. On start → try Luna VPN (15s timeout)
 * 2. If Luna connects → stay on Luna, monitor connection
 * 3. If Luna fails/drops → start Local VPN immediately, schedule Luna retry
 * 4. When Luna recovers → stop Local VPN, switch back to Luna
 */
@Singleton
class VpnFailoverController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lunaVpnManager: LunaVpnManager,
    private val settingsRepo: SettingsRepository
) {
    companion object {
        private const val TAG = "NexusBlock/Failover"
        private const val LUNA_CONNECT_TIMEOUT_MS = 15_000L
        private const val RETRY_INITIAL_MS = 60_000L
        private const val RETRY_MAX_MS = 300_000L
        private const val RETRY_BACKOFF_FACTOR = 2.0
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeMode = MutableStateFlow(ActiveVpnMode.DISCONNECTED)
    val activeMode: StateFlow<ActiveVpnMode> = _activeMode

    private var retryJob: Job? = null
    private var monitorJob: Job? = null
    private var currentRetryDelayMs = RETRY_INITIAL_MS
    private var isStarted = false

    /**
     * Start the failover controller. Tries Luna first, falls back to Local.
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        val mode = settingsRepo.vpnMode
        Log.i(TAG, "Starting failover controller in mode: $mode")

        when (mode) {
            VpnMode.LUNA_PRIMARY -> startWithLunaPrimary()
            VpnMode.LOCAL_ONLY -> startLocalOnly()
            VpnMode.LUNA_ONLY -> startLunaOnly()
        }
    }

    /**
     * Stop all VPN connections and cancel retries.
     */
    fun stop() {
        isStarted = false
        retryJob?.cancel()
        monitorJob?.cancel()
        retryJob = null
        monitorJob = null

        lunaVpnManager.disconnect()
        stopLocalVpn()

        _activeMode.value = ActiveVpnMode.DISCONNECTED
        Log.i(TAG, "Failover controller stopped")
    }

    private fun startWithLunaPrimary() {
        _activeMode.value = ActiveVpnMode.TRANSITIONING

        scope.launch {
            val lunaConnected = tryConnectLuna()
            if (lunaConnected) {
                _activeMode.value = ActiveVpnMode.LUNA
                startLunaMonitor()
            } else {
                // Luna failed — failover to local
                Log.w(TAG, "Luna VPN failed, falling back to Local VPN")
                startLocalVpn()
                _activeMode.value = ActiveVpnMode.LOCAL
                scheduleLunaRetry()
            }
        }
    }

    private fun startLocalOnly() {
        startLocalVpn()
        _activeMode.value = ActiveVpnMode.LOCAL
    }

    private fun startLunaOnly() {
        _activeMode.value = ActiveVpnMode.TRANSITIONING

        scope.launch {
            val connected = tryConnectLuna()
            if (connected) {
                _activeMode.value = ActiveVpnMode.LUNA
                startLunaMonitor()
            } else {
                _activeMode.value = ActiveVpnMode.DISCONNECTED
                scheduleLunaRetry()
            }
        }
    }

    /**
     * Attempt to connect Luna VPN with timeout.
     * Returns true if connected within timeout.
     */
    private suspend fun tryConnectLuna(): Boolean {
        if (!lunaVpnManager.isSupported) {
            Log.w(TAG, "Luna VPN not supported on this device (API < 30)")
            return false
        }

        val started = lunaVpnManager.connect()
        if (!started) return false

        // Wait for connection confirmation with timeout
        return try {
            withTimeout(LUNA_CONNECT_TIMEOUT_MS) {
                lunaVpnManager.state
                    .first { it == LunaState.CONNECTED || it == LunaState.FAILED }
            } == LunaState.CONNECTED
        } catch (_: TimeoutCancellationException) {
            Log.w(TAG, "Luna VPN connection timed out (${LUNA_CONNECT_TIMEOUT_MS}ms)")
            lunaVpnManager.disconnect()
            false
        }
    }

    /**
     * Monitor Luna connection state. On disconnect, failover to Local.
     */
    private fun startLunaMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            lunaVpnManager.state.collect { state ->
                when (state) {
                    LunaState.DISCONNECTED, LunaState.FAILED -> {
                        if (_activeMode.value == ActiveVpnMode.LUNA && isStarted) {
                            Log.w(TAG, "Luna VPN dropped (state=$state), failing over to Local")
                            onLunaDropped()
                        }
                    }
                    else -> { /* ignore CONNECTING, CONNECTED, UNSUPPORTED */ }
                }
            }
        }
    }

    private fun onLunaDropped() {
        val mode = settingsRepo.vpnMode
        when (mode) {
            VpnMode.LUNA_PRIMARY -> {
                // Failover to local
                startLocalVpn()
                _activeMode.value = ActiveVpnMode.LOCAL
                scheduleLunaRetry()
            }
            VpnMode.LUNA_ONLY -> {
                _activeMode.value = ActiveVpnMode.DISCONNECTED
                scheduleLunaRetry()
            }
            VpnMode.LOCAL_ONLY -> { /* shouldn't happen */ }
        }
    }

    /**
     * Schedule periodic Luna reconnection attempts with exponential backoff.
     */
    private fun scheduleLunaRetry() {
        retryJob?.cancel()
        currentRetryDelayMs = RETRY_INITIAL_MS

        retryJob = scope.launch {
            while (isActive && isStarted) {
                delay(currentRetryDelayMs)
                Log.i(TAG, "Retrying Luna VPN (delay=${currentRetryDelayMs}ms)")

                val connected = tryConnectLuna()
                if (connected) {
                    Log.i(TAG, "Luna VPN recovered!")
                    onLunaRecovered()
                    break
                }

                // Exponential backoff
                currentRetryDelayMs = (currentRetryDelayMs * RETRY_BACKOFF_FACTOR)
                    .toLong()
                    .coerceAtMost(RETRY_MAX_MS)
            }
        }
    }

    private fun onLunaRecovered() {
        retryJob?.cancel()
        retryJob = null
        currentRetryDelayMs = RETRY_INITIAL_MS

        // Stop local VPN if it was running as failover
        if (_activeMode.value == ActiveVpnMode.LOCAL) {
            stopLocalVpn()
        }

        _activeMode.value = ActiveVpnMode.LUNA
        startLunaMonitor()
    }

    private fun startLocalVpn() {
        val intent = Intent(context, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_START_SECONDARY
        }
        context.startForegroundService(intent)
        Log.i(TAG, "Local VPN started as secondary")
    }

    private fun stopLocalVpn() {
        val intent = Intent(context, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_STOP_FOR_PRIMARY
        }
        context.startService(intent)
        Log.i(TAG, "Local VPN stopped for primary")
    }
}
