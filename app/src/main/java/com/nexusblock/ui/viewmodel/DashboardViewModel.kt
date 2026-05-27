package com.nexusblock.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.StatsRepository
import com.nexusblock.service.NexusVpnService
import com.nexusblock.service.VpnWatchdogService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Immutable snapshot of the entire Dashboard UI. Consumed as a single
 * [collectAsState] to minimize recomposition — when this changes, the UI
 * updates; when only bandwidth changes, a separate [bandwidthTotal] flow
 * handles it without touching the rest of the screen.
 */
data class DashboardUiState(
    val vpnActive: Boolean = false,
    val blockedCountText: String = "0",
    val dataSavedText: String = "0 KB",
    val domainCountText: String = "0",
    val techniques: com.nexusblock.data.repository.BlockingTechniques =
        com.nexusblock.data.repository.BlockingTechniques()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository,
    private val blocklistRepo: BlocklistRepository,
    private val packetRouter: com.nexusblock.engine.PacketRouter
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // --- Static UI state: combines everything that changes infrequently ---
    // Using SharingStarted.WhileSubscribed(5000) stops upstream collection when
    // the screen is not visible (e.g. user navigated to Settings).
    val uiState: StateFlow<DashboardUiState> = combine(
        settingsRepo.observeVpnActive(),
        observeVpnRunning(),
        statsRepo.observeTotalBlocked(),
        blocklistRepo.observeDomainCount(),
        settingsRepo.observeTechniques()
    ) { _, running, blocked, domains, techs ->
        // Estimate ~35KB saved per blocked request (avg ad creative size)
        val savedKb = blocked * 35L
        DashboardUiState(
            vpnActive = running,
            blockedCountText = blocked.toString(),
            dataSavedText = when {
                savedKb > 1_000_000 -> String.format("%.1f GB", savedKb / 1_000_000.0)
                savedKb > 1000 -> String.format("%.1f MB", savedKb / 1000.0)
                else -> "$savedKb KB"
            },
            domainCountText = domains.toString(),
            techniques = techs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )

    // --- Bandwidth: separate because it updates every 2s while VPN is on ---
    // We only want 1 upstream collection, Sampled (2000ms) so the UI isn't
    // thrashed by rapid packet-router updates.
    val bandwidthTotal: StateFlow<String> = uiState
        .map { it.vpnActive }
        .distinctUntilChanged()
        .flatMapLatest { active ->
            if (active) {
                flow {
                    while (currentCoroutineContext().isActive) {
                        val stats = packetRouter.getRouterStats()
                        emit(formatBytes(stats.bytesTotal))
                        delay(2000)
                    }
                }
            } else {
                flowOf("0 B")
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "0 B"
        )

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
            bytes >= 1000 -> String.format("%.1f KB", bytes / 1000.0)
            else -> "$bytes B"
        }
    }

    private fun observeVpnRunning(): Flow<Boolean> = NexusVpnService.runningState

    fun toggleVpn(onRequestPermission: () -> Unit = {}) {
        val currentlyActive = uiState.value.vpnActive
        if (currentlyActive) {
            stopVpn()
        } else {
            requestStartVpn(onRequestPermission)
        }
    }

    fun requestStartVpn(onRequestPermission: () -> Unit) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            Log.i("DashboardVM", "VPN permission required, requesting from user")
            onRequestPermission()
        } else {
            Log.i("DashboardVM", "VPN permission already granted, starting service")
            startVpnService()
        }
    }

    fun setTechnique(name: String, enabled: Boolean) {
        val current = settingsRepo.techniques
        val updated = when (name) {
            "dns" -> current.copy(dnsFiltering = enabled)
            "ip" -> current.copy(ipBlocking = enabled)
            "stealth" -> current.copy(stealthMode = enabled)
            "firewall" -> current.copy(appFirewall = enabled)
            else -> current
        }
        settingsRepo.techniques = updated
    }

    private fun startVpnService() {
        val serviceIntent = Intent(context, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        VpnWatchdogService.start(context)
    }

    private fun stopVpn() {
        VpnWatchdogService.stop(context)
        val serviceIntent = Intent(context, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }
}
