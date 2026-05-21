package com.nexusblock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Attempts to clean up Argus CA certificate and VPN state before uninstall.
 *
 * Android LIMITATION: Starting with Android 11 (API 30), apps cannot receive
 * their own {@link Intent#ACTION_PACKAGE_REMOVED} broadcast. This receiver
 * only fires when OTHER packages are removed.
 *
 * Therefore we implement a two-tier cleanup strategy:
 *   1. Proactive: Dashboard provides a "Full Reset & Remove CA" button that
 *      stops all services, removes the CA from KeyChain (guided), and clears
 *      all app data before the user uninstalls.
 *   2. Reactive: This receiver fires if another app is uninstalled, and we
 *      use it as a heartbeat to check if Argus is still running properly.
 *
 * For true automatic cleanup on uninstall, rooted users can rely on the
 * Magisk module's cleanup scripts (Sprint 4).
 */
class UninstallCleanupReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Argus/Cleanup"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(TAG, "Package removed: $packageName")
                // If another VPN app was removed, we might want to reassert our VPN
                // or log telemetry. For Argus self-uninstall, this does NOT fire.
            }
            Intent.ACTION_MY_PACKAGE_SUSPENDED -> {
                Log.w(TAG, "Argus package suspended — stopping services")
                NexusVpnService.currentInstance?.let {
                    val intent = android.content.Intent(context, NexusVpnService::class.java)
                    intent.action = NexusVpnService.ACTION_STOP
                    context.startService(intent)
                }
            }
        }
    }
}
