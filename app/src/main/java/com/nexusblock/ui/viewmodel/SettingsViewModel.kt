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
import com.nexusblock.data.repository.VpnMode
import com.nexusblock.data.repository.VpnRoutingMode
import com.nexusblock.cert.CertInstallOrchestrator
import com.nexusblock.engine.PrivateDnsManager
import com.nexusblock.engine.VpnFailoverController
import kotlinx.coroutines.launch
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val failoverController: VpnFailoverController,
    private val privateDnsManager: PrivateDnsManager
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val autoStart: StateFlow<Boolean> = settingsRepo.observeAutoStart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dnsProfile: StateFlow<String> = settingsRepo.observeDnsProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "adguard_standard")

    val vpnRoutingMode: StateFlow<VpnRoutingMode> = settingsRepo.observeVpnRoutingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnRoutingMode.FULL_ROUTE_AGGRESSIVE)

    val vpnMode: StateFlow<VpnMode> = settingsRepo.observeVpnMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnMode.LUNA_PRIMARY)

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    private val _lunaCertInstalled = MutableStateFlow(false)
    val lunaCertInstalled: StateFlow<Boolean> = _lunaCertInstalled.asStateFlow()

    val privateDnsActive: StateFlow<Boolean> = privateDnsManager.isActive
    val privateDnsPermission: StateFlow<Boolean> = privateDnsManager.hasPermission

    private val _showCertWizard = MutableStateFlow(false)
    val showCertWizard: StateFlow<Boolean> = _showCertWizard.asStateFlow()

    init {
        checkBatteryOptimization()
        checkLunaCertStatus()
    }

    private fun checkLunaCertStatus() {
        viewModelScope.launch {
            _lunaCertInstalled.value = CertInstallOrchestrator.isLunaCaInstalled(context)
        }
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

    fun setVpnMode(mode: VpnMode) {
        settingsRepo.vpnMode = mode
        when (mode) {
            VpnMode.PRIVATE_DNS -> {
                // Stop VPN-based blocking, enable system Private DNS
                failoverController.stop()
                privateDnsManager.enable()
            }
            else -> {
                // Disable Private DNS if switching away from it
                if (privateDnsManager.isActive.value) {
                    privateDnsManager.disable()
                }
                failoverController.stop()
                failoverController.start()
            }
        }
    }

    fun setPrivateDnsProvider(hostname: String) {
        privateDnsManager.enable(hostname)
    }

    fun refreshPrivateDnsPermission() {
        privateDnsManager.refreshPermission()
    }

    fun installLunaCert() {
        viewModelScope.launch {
            val result = CertInstallOrchestrator.orchestrate(context)
            if (result.alreadyInstalled) {
                _lunaCertInstalled.value = true
                return@launch
            }
            if (CertInstallOrchestrator.launchBestIntent(context, result)) {
                // Launched intent, user may complete install externally.
                // We don't know result until next check.
            } else {
                // No intent worked — show the wizard
                _showCertWizard.value = true
            }
        }
    }

    fun dismissCertWizard() {
        _showCertWizard.value = false
        checkLunaCertStatus()
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
