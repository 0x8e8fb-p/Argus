package com.nexusblock.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Ikev2VpnProfile
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnManager
import android.os.Build
import android.util.Log
import java.net.InetAddress
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

enum class LunaState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    UNSUPPORTED
}

/**
 * Manages the IKEv2 VPN connection to Luna's ad-blocking server (vpn.adbl0ck.com).
 * Uses Android's built-in [Ikev2VpnProfile] API (requires API 30+).
 *
 * Luna's server provides:
 * - DNS-level ad blocking via 10.16.0.1
 * - MITM manifest cleaning for Prime Video SSAI (if CA cert is trusted)
 * - System-wide ad blocking for all apps
 */
@Singleton
class LunaVpnManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NexusBlock/LunaVPN"
    }

    private val _state = MutableStateFlow(LunaState.DISCONNECTED)
    val state: StateFlow<LunaState> = _state

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Provision and start the Luna IKEv2 VPN profile.
     * Returns true if provisioning was accepted, false on failure.
     */
    fun connect(): Boolean {
        if (!isSupported) {
            _state.value = LunaState.UNSUPPORTED
            Log.w(TAG, "IKEv2 VPN requires API 30+, current: ${Build.VERSION.SDK_INT}")
            return false
        }
        return connectInternal()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun connectInternal(): Boolean {
        _state.value = LunaState.CONNECTING

        try {
            val config = loadConfig()
            val profile = buildProfile(config)
            val vpnManager = context.getSystemService(VpnManager::class.java)

            if (vpnManager == null) {
                Log.e(TAG, "VpnManager not available")
                _state.value = LunaState.FAILED
                return false
            }

            // Provision the profile — this registers it with the system.
            // If another VPN app is active, Android will ask user consent.
            vpnManager.provisionVpnProfile(profile)
            Log.i(TAG, "Luna VPN profile provisioned")

            // Start the provisioned profile
            vpnManager.startProvisionedVpnProfile()
            Log.i(TAG, "Luna VPN connection initiated to ${config.optString("server")}")

            // Register callback to track actual connection state
            registerNetworkCallback()

            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "VPN permission denied — user must consent", e)
            _state.value = LunaState.FAILED
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Luna VPN", e)
            _state.value = LunaState.FAILED
            return false
        }
    }

    /**
     * Disconnect and remove the Luna VPN profile.
     */
    fun disconnect() {
        if (!isSupported) return
        disconnectInternal()
    }

    /**
     * Quick health check: ping Luna's internal DNS (10.16.0.1) over ICMP.
     * Returns true if the Luna VPN tunnel is actually carrying traffic.
     */
    fun isHealthy(): Boolean {
        if (_state.value != LunaState.CONNECTED) return false
        return try {
            val reachable = InetAddress.getByName("10.16.0.1").isReachable(3000)
            Log.d(TAG, "Luna health ping 10.16.0.1 reachable=$reachable")
            reachable
        } catch (e: Exception) {
            Log.w(TAG, "Luna health ping failed", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun disconnectInternal() {
        try {
            val vpnManager = context.getSystemService(VpnManager::class.java)
            vpnManager?.deleteProvisionedVpnProfile()
            Log.i(TAG, "Luna VPN profile deleted")
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting Luna VPN", e)
        }
        unregisterNetworkCallback()
        _state.value = LunaState.DISCONNECTED
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildProfile(config: JSONObject): Ikev2VpnProfile {
        val server = config.getString("server")
        val username = config.getString("username")
        val password = config.getString("password")
        val remoteId = config.optString("remoteIdentifier", server)

        // Load Luna CA cert for IKE server validation (prevents MITM of the VPN tunnel itself)
        val caCert = loadLunaCaCert()
        if (caCert == null) {
            Log.w(TAG, "Luna CA cert not found; using system trust store for server validation")
        } else {
            Log.i(TAG, "Luna CA cert loaded for IKE server validation")
        }

        return Ikev2VpnProfile.Builder(server, remoteId)
            .setAuthUsernamePassword(username, password, caCert)
            .setBypassable(false)
            .setMetered(false)
            .setMaxMtu(1400)
            .build()
    }

    /**
     * Load the bundled Luna CA certificate from assets for IKEv2 server validation.
     * This ensures we only connect to the real Luna server, not an impersonator.
     */
    private fun loadLunaCaCert(): X509Certificate? {
        return try {
            val pemBytes = context.assets.open("luna/ca_cert.pem").use { it.readBytes() }
            val cf = CertificateFactory.getInstance("X.509")
            cf.generateCertificate(pemBytes.inputStream()) as X509Certificate
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Luna CA cert from assets", e)
            null
        }
    }

    private fun loadConfig(): JSONObject {
        val json = context.assets.open("luna/vpn_config.json").use {
            it.bufferedReader().readText()
        }
        return JSONObject(json)
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Luna VPN connected (network: $network)")
                _state.value = LunaState.CONNECTED
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Luna VPN disconnected (network lost: $network)")
                if (_state.value == LunaState.CONNECTED || _state.value == LunaState.CONNECTING) {
                    _state.value = LunaState.DISCONNECTED
                }
            }

            override fun onUnavailable() {
                Log.w(TAG, "Luna VPN unavailable")
                _state.value = LunaState.FAILED
            }
        }

        cm.registerNetworkCallback(request, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let { cb ->
            try {
                val cm = context.getSystemService(ConnectivityManager::class.java)
                cm?.unregisterNetworkCallback(cb)
            } catch (_: Exception) {}
        }
        networkCallback = null
    }
}
