package com.nexusblock.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.service.NexusVpnService
import com.nexusblock.service.VpnWatchdogService
import com.nexusblock.ui.components.ArgusBackground
import com.nexusblock.ui.components.ArgusNavigationRail
import com.nexusblock.ui.screens.*
import com.nexusblock.ui.theme.ArgusBlockTheme
import com.nexusblock.ui.theme.LocalTvDimensions
import com.nexusblock.ui.theme.dimensionsForWidth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ArgusBlock/Main"
    }

    private var pendingVpnStart = false

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingVpnStart = false
        if (result.resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "VPN permission granted by user")
            startVpnService()
        } else {
            Log.w(TAG, "VPN permission denied or cancelled by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val intent = VpnService.prepare(this)
            if (intent != null) {
                Log.i(TAG, "Auto-requesting VPN permission on launch")
                pendingVpnStart = true
                vpnPermissionLauncher.launch(intent)
            } else {
                Log.i(TAG, "VPN permission already granted, auto-starting service")
                if (!NexusVpnService.isRunning) startVpnService()
            }
        }, 1500)
        setContent {
            ArgusBlockTheme {
                ArgusBlockApp(
                    onRequestVpnPermission = {
                        val intent = VpnService.prepare(this)
                        if (intent != null) {
                            Log.i(TAG, "Launching VPN permission dialog")
                            pendingVpnStart = true
                            vpnPermissionLauncher.launch(intent)
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
        val intent = VpnService.prepare(this)
        if (intent == null && settingsRepo.vpnActive && !NexusVpnService.isRunning) {
            Log.i(TAG, "Auto-starting VPN on resume")
            startVpnService()
        }
    }

    private fun startVpnService() {
        val serviceIntent = Intent(this, NexusVpnService::class.java).apply {
            action = NexusVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        VpnWatchdogService.start(this)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArgusBlockApp(
    onRequestVpnPermission: () -> Unit = {}
) {
    val navController = rememberNavController()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dims = dimensionsForWidth(maxWidth.value.toInt())
        CompositionLocalProvider(LocalTvDimensions provides dims) {
            Box(modifier = Modifier.fillMaxSize()) {
                ArgusBackground()
                Row(modifier = Modifier.fillMaxSize()) {
                    ArgusNavigationRail(navController = navController)
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        // Container-transform style: gentle fade + scale +
                        // slight horizontal travel for a premium TV feel.
                        enterTransition = {
                            fadeIn(tween(380, easing = EaseOutCubic)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(380, easing = EaseOutCubic)
                                ) +
                                slideInHorizontally(tween(380, easing = EaseOutCubic)) { it / 24 }
                        },
                        exitTransition = {
                            fadeOut(tween(240, easing = EaseInOutCubic)) +
                                scaleOut(
                                    targetScale = 1.02f,
                                    animationSpec = tween(240, easing = EaseInOutCubic)
                                )
                        },
                        popEnterTransition = {
                            fadeIn(tween(380, easing = EaseOutCubic)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(380, easing = EaseOutCubic)
                                ) +
                                slideInHorizontally(tween(380, easing = EaseOutCubic)) { -it / 24 }
                        },
                        popExitTransition = {
                            fadeOut(tween(240, easing = EaseInOutCubic)) +
                                scaleOut(
                                    targetScale = 1.02f,
                                    animationSpec = tween(240, easing = EaseInOutCubic)
                                )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = dims.contentPadding,
                                vertical = dims.spacingLarge
                            )
                    ) {
                        composable(Screen.Home.route) {
                            DashboardScreen(
                                onRequestVpnPermission = onRequestVpnPermission
                            )
                        }
                        composable(Screen.Activity.route) { LogsScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Activity : Screen("activity", "Activity")
    object Settings : Screen("settings", "Settings")
}
