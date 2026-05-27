package com.nexusblock.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.nexusblock.data.model.FirewallMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active blocking techniques.
 * + dnsProfile → selected DNS provider ID (adguard_standard, cloudflare_family, etc.)
 */
data class BlockingTechniques(
    val dnsFiltering: Boolean = true,
    val ipBlocking: Boolean = true,
    val stealthMode: Boolean = false,
    val appFirewall: Boolean = false
)

enum class VpnRoutingMode(val storageKey: String) {
    DNS_ONLY("dns_only"),
    FULL_ROUTE("full_route");

    companion object {
        fun fromStorageKey(value: String?): VpnRoutingMode = entries.firstOrNull { it.storageKey == value }
            ?: DNS_ONLY
    }
}



@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val legacyPrefs: SharedPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Preference keys
    private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
    private val KEY_VPN_ACTIVE = booleanPreferencesKey("vpn_active")
    private val KEY_VPN_ROUTING_MODE = stringPreferencesKey("vpn_routing_mode")
    private val KEY_DNS_PROFILE = stringPreferencesKey("dns_profile")
    private val KEY_TECH_DNS = booleanPreferencesKey("tech_dns")
    private val KEY_TECH_IP = booleanPreferencesKey("tech_ip")
    private val KEY_TECH_STEALTH = booleanPreferencesKey("tech_stealth")
    private val KEY_TECH_FIREWALL = booleanPreferencesKey("tech_firewall_v2")
    private val KEY_FIREWALL_MODES = stringPreferencesKey("firewall_modes_json")

    // Hot StateFlows for reactive + synchronous access
    private val autoStartFlow = dataStore.data
        .map { it[KEY_AUTO_START] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val vpnActiveFlow = dataStore.data
        .map { it[KEY_VPN_ACTIVE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val dnsProfileFlow = dataStore.data
        .map { it[KEY_DNS_PROFILE] ?: "adguard_standard" }
        .stateIn(scope, SharingStarted.Eagerly, "adguard_standard")

    private val vpnRoutingModeFlow = dataStore.data
        .map { VpnRoutingMode.fromStorageKey(it[KEY_VPN_ROUTING_MODE]) }
        .stateIn(scope, SharingStarted.Eagerly, VpnRoutingMode.DNS_ONLY)

    private val techniquesFlow = dataStore.data
        .map { prefs ->
            BlockingTechniques(
                dnsFiltering = prefs[KEY_TECH_DNS] ?: true,
                ipBlocking = prefs[KEY_TECH_IP] ?: true,
                stealthMode = prefs[KEY_TECH_STEALTH] ?: false,
                appFirewall = prefs[KEY_TECH_FIREWALL] ?: false
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, BlockingTechniques())

    private val firewallModesFlow = dataStore.data
        .map { prefs ->
            val json = prefs[KEY_FIREWALL_MODES] ?: "{}"
            parseFirewallModes(json)
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())



    // ─────────────────────────────────────────────────────────────
    // Synchronous accessors (for Service/Engine code paths)
    // ─────────────────────────────────────────────────────────────

    var autoStart: Boolean
        get() = autoStartFlow.value
        set(value) { scope.launch { dataStore.edit { it[KEY_AUTO_START] = value } } }

    var vpnActive: Boolean
        get() = vpnActiveFlow.value
        set(value) { scope.launch { dataStore.edit { it[KEY_VPN_ACTIVE] = value } } }

    var dnsProfile: String
        get() = dnsProfileFlow.value
        set(value) { scope.launch { dataStore.edit { it[KEY_DNS_PROFILE] = value } } }

    var vpnRoutingMode: VpnRoutingMode
        get() = vpnRoutingModeFlow.value
        set(value) { scope.launch { dataStore.edit { it[KEY_VPN_ROUTING_MODE] = value.storageKey } } }

    var techniques: BlockingTechniques
        get() = techniquesFlow.value
        set(value) {
            scope.launch {
                dataStore.edit {
                    it[KEY_TECH_DNS] = value.dnsFiltering
                    it[KEY_TECH_IP] = value.ipBlocking
                    it[KEY_TECH_STEALTH] = value.stealthMode
                    it[KEY_TECH_FIREWALL] = value.appFirewall
                }
            }
        }

    // ─────────────────────────────────────────────────────────────
    // Firewall modes (per-app ALLOW / BLOCK / DEFAULT)
    // ─────────────────────────────────────────────────────────────

    var firewallModes: Map<String, FirewallMode>
        get() = firewallModesFlow.value
        set(value) {
            scope.launch {
                dataStore.edit {
                    it[KEY_FIREWALL_MODES] = serializeFirewallModes(value)
                }
            }
        }

    fun observeFirewallModes(): StateFlow<Map<String, FirewallMode>> = firewallModesFlow

    // ─────────────────────────────────────────────────────────────
    // Observable flows
    // ─────────────────────────────────────────────────────────────

    fun observeAutoStart(): Flow<Boolean> = autoStartFlow
    fun observeVpnActive(): Flow<Boolean> = vpnActiveFlow
    fun observeTechniques(): StateFlow<BlockingTechniques> = techniquesFlow
    fun observeDnsProfile(): Flow<String> = dnsProfileFlow
    fun observeVpnRoutingMode(): StateFlow<VpnRoutingMode> = vpnRoutingModeFlow

    // ─────────────────────────────────────────────────────────────
    // Default bypass packages for streaming apps
    // ─────────────────────────────────────────────────────────────

    val defaultBypassPackages = setOf(
        "com.netflix.mediaclient",
        "com.disney.disneyplus",
        "com.amazon.aiv.AIVApp",
        "in.startv.hotstar",
        "com.sonyliv",
    )

    fun initDefaultBypassIfEmpty() {
        if (firewallModesFlow.value.isEmpty()) {
            val defaults = defaultBypassPackages.associateWith { FirewallMode.ALLOW }
            firewallModes = defaults
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Serialization helpers
    // ─────────────────────────────────────────────────────────────

    private fun parseFirewallModes(json: String): Map<String, FirewallMode> {
        return try {
            val obj = org.json.JSONObject(json)
            val result = mutableMapOf<String, FirewallMode>()
            obj.keys().forEach { key ->
                result[key] = FirewallMode.valueOf(obj.getString(key))
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeFirewallModes(modes: Map<String, FirewallMode>): String {
        val obj = org.json.JSONObject()
        modes.forEach { (pkg, mode) -> obj.put(pkg, mode.name) }
        return obj.toString()
    }
}
