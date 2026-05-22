package com.nexusblock.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val autoStart: StateFlow<Boolean> = settingsRepo.observeAutoStart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dnsProfile: StateFlow<String> = settingsRepo.observeDnsProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "adguard_standard")

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    init {
        checkBatteryOptimization()
    }

    fun setAutoStart(enabled: Boolean) {
        settingsRepo.autoStart = enabled
    }

    fun setDnsProfile(profileId: String) {
        settingsRepo.dnsProfile = profileId
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
