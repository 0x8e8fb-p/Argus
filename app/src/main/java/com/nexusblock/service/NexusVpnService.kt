package com.nexusblock.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nexusblock.Constants
import com.nexusblock.R
import com.nexusblock.data.repository.SettingsRepository
import com.nexusblock.engine.DnsFilterEngine
import com.nexusblock.engine.PacketRouter
import com.nexusblock.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.net.InetAddress
import javax.inject.Inject

@AndroidEntryPoint(VpnService::class)
class NexusVpnService : VpnService() {

    companion object {
        private const val TAG = "NexusBlock/VPN"
        const val ACTION_START = "com.nexusblock.START_VPN"
        const val ACTION_STOP = "com.nexusblock.STOP_VPN"
        const val ACTION_RESTART = "com.nexusblock.RESTART_VPN"

        @Volatile
        var isRunning = false
            private set

        /** Exposed so other components can call [protect] on their sockets. */
        @Volatile
        var currentInstance: NexusVpnService? = null
    }

    @Inject
    lateinit var packetRouter: PacketRouter

    @Inject
    lateinit var dnsEngine: DnsFilterEngine

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var activeUnderlyingNetwork: Network? = null
    private var reconnectJob: Job? = null
    private var explicitStop = false

    override fun onCreate() {
        super.onCreate()
        currentInstance = this
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: $action")

        when (action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_RESTART -> {
                explicitStop = false
                stopVpnInternal(updateDesiredState = false)
                startVpn()
                return START_STICKY
            }
            else -> {
                explicitStop = false
                if (!isRunning) {
                    startVpn()
                }
                return START_STICKY
            }
        }
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN privilege revoked")
        explicitStop = true
        stopVpn()
        settingsRepo.vpnActive = false
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        stopVpnInternal(updateDesiredState = explicitStop)
        serviceScope.cancel()
        currentInstance = null
        super.onDestroy()
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            // Start foreground IMMEDIATELY — Android requires this within 5 seconds
            // of startForegroundService(). Do it before any potentially-blocking
            // or fallible operation (like builder.establish()).
            startForeground(Constants.NOTIFICATION_ID_VPN, buildNotification())

            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .setMtu(Constants.VPN_MTU)
                .addAddress(Constants.VPN_ADDRESS, 24)
                .addDnsServer(Constants.VPN_DNS)

            // Routing mode: DNS capture + DNS bypass routes.
            // All DNS queries enter TUN for filtering. TCP/UDP to known DNS servers
            // on alternate ports (DoH/DoT) also captured to prevent bypass.
            builder.addRoute(Constants.VPN_DNS, 32)

            // IPv6: minimal local address for IPv6 DNS capture.
            // Do NOT block all IPv6 (2000::/3) — that breaks dual-stack apps
            // which wait for IPv6 connection timeouts before IPv4 fallback.
            // Instead, we capture IPv6 DNS by routing known public IPv6 DNS
            // servers through the TUN (they'll be handled like IPv4 DNS).
            try {
                builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                Constants.DNS_BYPASS_IPV6_ROUTES.forEach { ip ->
                    try { builder.addRoute(ip, 128) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "IPv6 not supported on this device")
            }

            applyDnsBypassRoutes(builder)

            // Bypass proxy itself to avoid loops
            builder.addDisallowedApplication(packageName)

            // Apply per-app firewall rules
            applyFirewallRules(builder)

            // Block IPv6 if needed (some ad blockers do this to force IPv4)
            // builder.allowFamily(android.system.OsConstants.AF_INET6)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                settingsRepo.vpnActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                return
            }

            isRunning = true
            settingsRepo.vpnActive = true

            // Start packet router (includes DNS engine)
            val dnsAddr = InetAddress.getByName(Constants.VPN_DNS)
            packetRouter.start(this, vpnInterface!!, dnsAddr)

            // Register network monitoring for auto-reconnect
            registerNetworkCallback()
            VpnWatchdogService.start(this)

            Log.i(TAG, "VPN started successfully with IPv6 null-routing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            settingsRepo.vpnActive = false
            stopSelf()
        }
    }

    private fun applyDnsBypassRoutes(builder: Builder) {
        if (!settingsRepo.techniques.dnsFiltering) return

        Constants.DNS_BYPASS_IPV4_ROUTES.forEach { ip ->
            try {
                builder.addRoute(ip, 32)
                Log.d(TAG, "DNS bypass capture route: $ip/32")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add DNS bypass route $ip", e)
            }
        }
    }

    private fun stopVpn() {
        explicitStop = true
        reconnectJob?.cancel()
        VpnWatchdogService.stop(this)
        stopVpnInternal(updateDesiredState = true)
        stopSelf()
    }

    private fun stopVpnInternal(updateDesiredState: Boolean) {
        isRunning = false
        if (updateDesiredState) {
            settingsRepo.vpnActive = false
        }

        try {
            packetRouter.stop()
            dnsEngine.clearCache()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping engines", e)
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        unregisterNetworkCallback()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        Log.i(TAG, "VPN stopped")
    }

    private fun applyFirewallRules(builder: Builder) {
        // The legacy SharedPreferences whitelist is kept for backward compatibility.
        // New per-app firewall uses SettingsRepository DataStore.
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val legacyWhitelisted = prefs.getStringSet("whitelisted_apps", emptySet()) ?: emptySet()

        // Migrate legacy whitelist entries to ALLOW firewall mode if present
        if (legacyWhitelisted.isNotEmpty()) {
            for (pkg in legacyWhitelisted) {
                try {
                    builder.addDisallowedApplication(pkg)
                    Log.d(TAG, "Firewall ALLOW (legacy): $pkg")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to allow $pkg")
                }
            }
        }

        // Apply new firewall modes from SettingsRepository
        val fwModes = settingsRepo.observeFirewallModes().value
        for ((pkg, mode) in fwModes) {
            when (mode) {
                com.nexusblock.data.model.FirewallMode.ALLOW -> {
                    try {
                        builder.addDisallowedApplication(pkg)
                        Log.d(TAG, "Firewall ALLOW: $pkg")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to allow $pkg")
                    }
                }
                com.nexusblock.data.model.FirewallMode.BLOCK -> {
                    // BLOCK mode: do NOT call addDisallowedApplication.
                    // With the DNS-only VPN route, this denies domains resolved
                    // through NexusBlock. Full IPv4 packet blocking requires a
                    // real full-tunnel forwarder before adding 0.0.0.0/0.
                    // Calling addDisallowedApplication would EXCLUDE the app from
                    // the VPN entirely, giving it unrestricted internet — the opposite
                    // of blocking.
                    Log.d(TAG, "Firewall BLOCK: $pkg (DNS denial enforced in PacketRouter)")
                }
                else -> { /* DEFAULT — traffic goes through VPN normally */ }
            }
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = cm.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                    Log.d(TAG, "Ignoring VPN network callback: $network")
                    return
                }

                val previous = activeUnderlyingNetwork
                activeUnderlyingNetwork = network

                if (previous == null) {
                    Log.i(TAG, "Initial underlying network: $network")
                    return
                }

                if (previous != network) {
                    Log.i(TAG, "Underlying network changed: $previous -> $network")
                    restartVpnDebounced()
                }
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Network lost: $network")
                if (activeUnderlyingNetwork == network) {
                    activeUnderlyingNetwork = null
                }
            }
        }

        cm.registerNetworkCallback(request, networkCallback!!)
    }

    private fun restartVpnDebounced() {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            delay(1500)
            if (isRunning) {
                Log.i(TAG, "Restarting VPN due to real network change")
                stopVpnInternal(updateDesiredState = false)
                delay(750)
                startVpn()
            }
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val cm = getSystemService(ConnectivityManager::class.java)
        try {
            cm?.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback", e)
        }
        networkCallback = null
        activeUnderlyingNetwork = null
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick action: Stop
        val stopIntent = Intent(this, NexusVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick action: Restart
        val restartIntent = Intent(this, NexusVpnService::class.java).apply {
            action = ACTION_RESTART
        }
        val restartPendingIntent = PendingIntent.getService(
            this, 2, restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_VPN)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_notification, getString(R.string.notification_action_stop), stopPendingIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.notification_action_restart), restartPendingIntent)
            .build()
    }
}
