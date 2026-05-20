package com.nexusblock.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusblock.data.model.FirewallMode
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.service.NexusVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FirewallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val pm = context.packageManager

    private val _apps = MutableStateFlow<List<AppFirewallInfo>>(emptyList())
    val apps: StateFlow<List<AppFirewallInfo>> = _apps.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val fwModes = settingsRepo.observeFirewallModes()

            fwModes.collect { modes ->
                val appList = withContext(Dispatchers.Default) {
                    val launchablePackages = getLaunchablePackages()
                    val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter {
                            it.packageName != context.packageName &&
                                (it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                                    it.packageName in launchablePackages)
                        }
                        .sortedBy { pm.getApplicationLabel(it).toString() }

                    installed.map { app ->
                        AppFirewallInfo(
                            packageName = app.packageName,
                            label = pm.getApplicationLabel(app).toString(),
                            mode = modes[app.packageName] ?: FirewallMode.DEFAULT
                        )
                    }
                }
                _apps.value = appList
            }
        }
    }

    fun setMode(packageName: String, mode: FirewallMode) {
        viewModelScope.launch {
            settingsRepo.setFirewallModeNow(packageName, mode)
            _apps.value = _apps.value.map {
                if (it.packageName == packageName) it.copy(mode = mode) else it
            }
            if (NexusVpnService.isRunning) {
                context.startService(Intent(context, NexusVpnService::class.java).apply {
                    action = NexusVpnService.ACTION_RESTART
                })
            }
        }
    }

    private fun getLaunchablePackages(): Set<String> {
        val launchIntents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        )
        return launchIntents
            .flatMap { pm.queryIntentActivities(it, 0) }
            .map { it.activityInfo.packageName }
            .toSet()
    }
}

data class AppFirewallInfo(
    val packageName: String,
    val label: String,
    val mode: FirewallMode
)
