package com.nexusblock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.nexusblock.data.worker.BlocklistUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class NexusBlockApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        createNotificationChannels()
        initializeWorkManager()
        schedulePeriodicBlocklistUpdate()
    }

    private fun initializeWorkManager() {
        try {
            WorkManager.getInstance(this)
        } catch (e: IllegalStateException) {
            WorkManager.initialize(this, workManagerConfiguration)
        }
    }

    private fun schedulePeriodicBlocklistUpdate() {
        BlocklistUpdateWorker.schedule(this)
        BlocklistUpdateWorker.runNow(this)
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
