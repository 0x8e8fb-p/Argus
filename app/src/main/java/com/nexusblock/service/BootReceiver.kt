package com.nexusblock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint(BroadcastReceiver::class)
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Argus/Boot"
    }

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot completed received")

            val autoStart: Boolean
            val wasActive: Boolean
            runBlocking {
                val prefs = dataStore.data.first()
                autoStart = prefs[booleanPreferencesKey("auto_start")] ?: true
                wasActive = prefs[booleanPreferencesKey("vpn_active")] ?: false
            }

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
            }
        }
    }
}
