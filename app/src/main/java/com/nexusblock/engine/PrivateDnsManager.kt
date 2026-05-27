package com.nexusblock.engine

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android's native Private DNS (DNS-over-TLS) settings.
 *
 * This approach writes directly to Settings.Global to set the system-wide
 * Private DNS provider. Zero battery drain, no VPN slot used, survives app kills.
 *
 * Requires WRITE_SECURE_SETTINGS permission granted via ADB:
 *   adb shell pm grant com.nexusblock android.permission.WRITE_SECURE_SETTINGS
 */
@Singleton
class PrivateDnsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NexusBlock/PrivateDNS"

        // Android Settings.Global keys for Private DNS
        private const val MODE_KEY = "private_dns_mode"
        private const val SPECIFIER_KEY = "private_dns_specifier"

        // Mode values
        private const val MODE_OFF = "off"
        private const val MODE_OPPORTUNISTIC = "opportunistic"
        private const val MODE_HOSTNAME = "hostname"

        // Default ad-blocking DNS hostname (AdGuard DoT)
        const val DEFAULT_DNS_HOSTNAME = "dns.adguard-dns.com"
    }

    /**
     * Known DNS providers that support ad-blocking via Private DNS (DoT).
     */
    enum class DnsProvider(val hostname: String, val displayName: String) {
        ADGUARD_STANDARD("dns.adguard-dns.com", "AdGuard Standard"),
        ADGUARD_FAMILY("family.adguard-dns.com", "AdGuard Family"),
        CLOUDFLARE_FAMILY("family.cloudflare-dns.com", "Cloudflare Family"),
        CLEANBROWSING_SECURITY("security-filter-dns.cleanbrowsing.org", "CleanBrowsing Security"),
        QUAD9("dns.quad9.net", "Quad9"),
        CONTROLD_1HOSTS("x-1hosts-lite.freedns.controld.com", "ControlD + 1Hosts Lite"),
        NEXTDNS("dns.nextdns.io", "NextDNS (requires config ID)");
    }

    private val resolver: ContentResolver = context.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isActive = MutableStateFlow(isAdBlockingActive())
    val isActive: StateFlow<Boolean> = _isActive

    private val _hasPermission = MutableStateFlow(checkPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission

    init {
        // Observe changes to Private DNS settings made externally
        scope.launch {
            observeDnsChanges().collect { active ->
                _isActive.value = active
            }
        }
    }

    /**
     * Check if we have WRITE_SECURE_SETTINGS permission.
     */
    fun checkPermission(): Boolean {
        return try {
            // Try reading - if we can write, we definitely can read
            Settings.Global.getString(resolver, MODE_KEY)
            // Try a no-op write to verify write permission
            val current = Settings.Global.getString(resolver, MODE_KEY)
            Settings.Global.putString(resolver, MODE_KEY, current ?: MODE_OPPORTUNISTIC)
            true
        } catch (e: SecurityException) {
            Log.d(TAG, "WRITE_SECURE_SETTINGS not granted")
            false
        }
    }

    /**
     * Check if Private DNS is currently set to our ad-blocking hostname.
     */
    fun isAdBlockingActive(): Boolean {
        return try {
            val mode = Settings.Global.getString(resolver, MODE_KEY)
            val host = Settings.Global.getString(resolver, SPECIFIER_KEY)
            mode == MODE_HOSTNAME && isKnownBlockingDns(host)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied checking DNS settings", e)
            false
        }
    }

    /**
     * Get the currently configured Private DNS hostname (if in hostname mode).
     */
    fun getCurrentHostname(): String? {
        return try {
            val mode = Settings.Global.getString(resolver, MODE_KEY)
            if (mode == MODE_HOSTNAME) {
                Settings.Global.getString(resolver, SPECIFIER_KEY)
            } else null
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Enable ad-blocking by setting Private DNS to the specified hostname.
     * @return true if successful, false if permission denied
     */
    fun enable(hostname: String = DEFAULT_DNS_HOSTNAME): Boolean {
        return try {
            Settings.Global.putString(resolver, SPECIFIER_KEY, hostname)
            Settings.Global.putString(resolver, MODE_KEY, MODE_HOSTNAME)
            _isActive.value = true
            Log.i(TAG, "Private DNS enabled: $hostname")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: WRITE_SECURE_SETTINGS not granted", e)
            _hasPermission.value = false
            false
        }
    }

    /**
     * Disable Private DNS ad-blocking (revert to opportunistic/automatic).
     * @return true if successful, false if permission denied
     */
    fun disable(): Boolean {
        return try {
            Settings.Global.putString(resolver, MODE_KEY, MODE_OPPORTUNISTIC)
            _isActive.value = false
            Log.i(TAG, "Private DNS reverted to opportunistic")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            _hasPermission.value = false
            false
        }
    }

    /**
     * Toggle Private DNS ad-blocking on/off.
     * @return the new state (true = active), or null if permission denied
     */
    fun toggle(): Boolean? {
        return if (isAdBlockingActive()) {
            if (disable()) false else null
        } else {
            if (enable()) true else null
        }
    }

    /**
     * Observe Private DNS state changes (including external changes by user/system).
     */
    private fun observeDnsChanges(): Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(isAdBlockingActive())
            }
        }

        resolver.registerContentObserver(
            Settings.Global.getUriFor(MODE_KEY), false, observer
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(SPECIFIER_KEY), false, observer
        )

        trySend(isAdBlockingActive())

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged()

    /**
     * Refresh permission status (call after user grants via ADB).
     */
    fun refreshPermission() {
        _hasPermission.value = checkPermission()
    }

    private fun isKnownBlockingDns(hostname: String?): Boolean {
        if (hostname.isNullOrBlank()) return false
        return DnsProvider.entries.any { it.hostname == hostname } ||
                hostname == DEFAULT_DNS_HOSTNAME
    }
}
