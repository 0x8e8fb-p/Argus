package com.nexusblock.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.data.repository.VpnRoutingMode
import com.nexusblock.engine.PrivateDnsManager
import kotlinx.coroutines.launch
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val privateDnsManager: PrivateDnsManager
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val autoStart: StateFlow<Boolean> = settingsRepo.observeAutoStart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dnsProfile: StateFlow<String> = settingsRepo.observeDnsProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "adguard_standard")

    val vpnRoutingMode: StateFlow<VpnRoutingMode> = settingsRepo.observeVpnRoutingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnRoutingMode.FULL_ROUTE)

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    val privateDnsActive: StateFlow<Boolean> = privateDnsManager.isActive
    val privateDnsPermission: StateFlow<Boolean> = privateDnsManager.hasPermission

    init {
        checkBatteryOptimization()
    }

    fun setAutoStart(enabled: Boolean) {
        settingsRepo.autoStart = enabled
    }

    fun setDnsProfile(profileId: String) {
        settingsRepo.dnsProfile = profileId
    }

    fun setVpnRoutingMode(mode: VpnRoutingMode) {
        settingsRepo.vpnRoutingMode = mode
        if (NexusVpnService.isRunning) {
            val intent = Intent(context, NexusVpnService::class.java).apply {
                action = NexusVpnService.ACTION_RESTART
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun setPrivateDnsProvider(hostname: String) {
        privateDnsManager.enable(hostname)
    }

    fun refreshPrivateDnsPermission() {
        privateDnsManager.refreshPermission()
    }

    fun requestBatteryOptimization(): Boolean {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun checkBatteryOptimization() {
        val pm = context.getSystemService(PowerManager::class.java)
        _isBatteryOptimized.value = pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
