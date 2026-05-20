package com.nexusblock.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.service.NexusVpnService
import com.nexusblock.service.VpnWatchdogService
import com.nexusblock.ui.components.NexusBackground
import com.nexusblock.ui.components.NexusNavigationRail
import kotlinx.coroutines.launch
import com.nexusblock.ui.screens.*
import com.nexusblock.ui.theme.NexusBlockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NexusBlock/Main"
        private const val REQUEST_VPN = 1001
    }

    private var pendingVpnStart = false

    @Inject
    lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusBlockTheme {
                NexusBlockApp(
                    onRequestVpnPermission = {
                        val intent = VpnService.prepare(this)
                        if (intent != null) {
                            Log.i(TAG, "Launching VPN permission dialog")
                            pendingVpnStart = true
                            startActivityForResult(intent, REQUEST_VPN)
                        } else {
                            Log.i(TAG, "VPN permission already granted, starting service")
                            startVpnService()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume only restores a user-desired active VPN. An explicit Stop should
        // stay stopped when the app is reopened.
        val intent = VpnService.prepare(this)
        if (intent == null && settingsRepo.vpnActive && !NexusVpnService.isRunning) {
            Log.i(TAG, "Auto-starting VPN on resume")
            startVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VPN) {
            pendingVpnStart = false
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "VPN permission granted by user")
                startVpnService()
            } else {
                Log.w(TAG, "VPN permission denied or cancelled by user")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startVpnService() {
        val serviceIntent = Intent(this, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_START
        }
        startForegroundService(serviceIntent)
        VpnWatchdogService.start(this)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NexusBlockApp(
    onRequestVpnPermission: () -> Unit = {}
) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NexusBackground()
        Row(modifier = Modifier.fillMaxSize()) {
            NexusNavigationRail(navController = navController)
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onRequestVpnPermission = onRequestVpnPermission
                    )
                }
                composable(Screen.Blocklists.route) { BlocklistScreen() }
                composable(Screen.CustomRules.route) { CustomRulesScreen() }
                composable(Screen.Firewall.route) { FirewallScreen() }
                composable(Screen.Logs.route) { LogsScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Blocklists : Screen("blocklists", "Blocklists")
    object CustomRules : Screen("custom_rules", "Rules")
    object Firewall : Screen("firewall", "Firewall")
    object Logs : Screen("logs", "Logs")
    object Settings : Screen("settings", "Settings")
}
