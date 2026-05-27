package com.nexusblock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(BroadcastReceiver::class)
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NexusBlock/Boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot completed received")

            // Hilt injection in BroadcastReceiver
            // We can't fully inject in onReceive for receivers, so use a fallback
            val prefs = context.getSharedPreferences("nexusblock_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", true)
            val wasActive = prefs.getBoolean("vpn_active", false)

            if (autoStart && wasActive) {
                Log.i(TAG, "Auto-starting VPN service")
                val serviceIntent = Intent(context, NexusVpnService::class.java).apply {
                    action = NexusVpnService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Also start watchdog
                VpnWatchdogService.start(context)
            }
        }
    }
}
