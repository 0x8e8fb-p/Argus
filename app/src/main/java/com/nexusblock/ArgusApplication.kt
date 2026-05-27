package com.nexusblock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.nexusblock.data.worker.VpnWatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@HiltAndroidApp
class ArgusApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepo: com.nexusblock.data.repository.SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // BouncyCastle provider removed — no proxy/MitM layer needs it anymore.
        createNotificationChannels()
        initializeWorkManager()
        VpnWatchdogWorker.enqueue(this)

        // Ensure default streaming-app bypass rules are applied on first launch
        settingsRepo.initDefaultBypassIfEmpty()

        // Blocklist updates are manual-only via Settings to avoid
        // network hit and SQLite churn on every cold start.
        // Built-in emergency rules are loaded synchronously on first VPN start.
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    private fun initializeWorkManager() {
        try {
            WorkManager.getInstance(this)
        } catch (e: IllegalStateException) {
            WorkManager.initialize(this, workManagerConfiguration)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    Constants.CHANNEL_VPN,
                    getString(R.string.vpn_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.vpn_notification_text)
                    setShowBadge(false)
                },
                NotificationChannel(
                    Constants.CHANNEL_WATCHDOG,
                    getString(R.string.watchdog_notification_title),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.watchdog_notification_text)
                    setShowBadge(false)
                }
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(channels)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
