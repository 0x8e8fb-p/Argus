package com.nexusblock.data.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.service.NexusVpnService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Isolated watchdog that monitors the main VpnService and auto-restarts it
 * if it has died unexpectedly while the user expects it to be running.
 *
 * Runs every ~15 minutes as a periodic WorkManager task. WorkManager is
 * resilient across Doze/App Standby on Android TV.
 */
@HiltWorker
class VpnWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepo: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "Argus/Watchdog"
        private const val WORK_NAME = "vpn_watchdog"

        /** How often the watchdog wakes up to check VPN health. */
        private const val REPEAT_INTERVAL_MIN = 15L

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequestBuilder<VpnWatchdogWorker>(
                REPEAT_INTERVAL_MIN, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES  // flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // don't overwrite if already scheduled
                request
            )
            Log.i(TAG, "Watchdog scheduled every $REPEAT_INTERVAL_MIN min")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Watchdog cancelled")
        }
    }

    override suspend fun doWork(): Result {
        val desiredActive = settingsRepo.vpnActive
        val actuallyRunning = NexusVpnService.isRunning

        return try {
            if (desiredActive && !actuallyRunning) {
                Log.w(TAG, "VPN expected active but not running — restarting")
                val intent = Intent(applicationContext, NexusVpnService::class.java).apply {
                    action = NexusVpnService.ACTION_RESTART
                }
                applicationContext.startService(intent)
            } else {
                Log.d(TAG, "VPN health OK (desired=$desiredActive, running=$actuallyRunning)")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Watchdog check failed", e)
            Result.retry()
        }
    }
}
