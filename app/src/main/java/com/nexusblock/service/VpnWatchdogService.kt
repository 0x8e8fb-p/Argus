package com.nexusblock.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nexusblock.Constants
import com.nexusblock.R
import com.nexusblock.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint(Service::class)
class VpnWatchdogService : Service() {

    companion object {
        private const val TAG = "NexusBlock/Watchdog"
        private const val CHECK_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, VpnWatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VpnWatchdogService::class.java))
        }
    }

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var checkJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Watchdog created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(Constants.NOTIFICATION_ID_WATCHDOG, buildNotification())

        checkJob?.cancel()
        checkJob = scope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkVpnHealth()
            }
        }

        // Schedule a repeating alarm as backup (for Doze mode resilience)
        scheduleAlarm()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Watchdog destroyed")
        checkJob?.cancel()
        scope.cancel()
        cancelAlarm()
        super.onDestroy()
    }

    private fun checkVpnHealth() {
        if (!NexusVpnService.isRunning && settingsRepo.vpnActive) {
            Log.w(TAG, "VPN not running but should be - restarting")
            val intent = Intent(this, NexusVpnService::class.java).apply {
                action = NexusVpnService.ACTION_RESTART
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun scheduleAlarm() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(this, VpnWatchdogService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED &&
                        alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                Log.w(TAG, "Exact alarm check failed", e)
                false
            }
        } else true

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS,
                    pendingIntent
                )
            } else {
                // Fallback: inexact alarm doesn't need SCHEDULE_EXACT_ALARM permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Using inexact alarm fallback (exact permission unavailable)")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling alarm", e)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(this, VpnWatchdogService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.CHANNEL_WATCHDOG)
            .setContentTitle(getString(R.string.watchdog_notification_title))
            .setContentText(getString(R.string.watchdog_notification_text))
            .setSmallIcon(R.drawable.ic_notification) // placeholder
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
