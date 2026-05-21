package com.nexusblock.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.nexusblock.Constants
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
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BlockingTechniques(
    val dnsFiltering: Boolean = true,
    val sniInspection: Boolean = true,
    val mitmProxy: Boolean = true,
    val headerFilter: Boolean = true,
    val ipBlocking: Boolean = true,
    val stealthMode: Boolean = false,
    val appFirewall: Boolean = false,
    val fullTunnel: Boolean = false,
    val albaniaMode: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val legacyPrefs: SharedPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Preference keys
    private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
    private val KEY_BATTERY_OPT = booleanPreferencesKey("battery_opt")
    private val KEY_VPN_ACTIVE = booleanPreferencesKey("vpn_active")
    private val KEY_CA_INSTALLED = booleanPreferencesKey("ca_installed")
    private val KEY_DNS_MODE = stringPreferencesKey("dns_mode")
    private val KEY_TECH_DNS = booleanPreferencesKey("tech_dns")
    private val KEY_TECH_SNI = booleanPreferencesKey("tech_sni")
    private val KEY_TECH_MITM = booleanPreferencesKey("tech_mitm")
    private val KEY_TECH_HEADER = booleanPreferencesKey("tech_header")
    private val KEY_TECH_IP = booleanPreferencesKey("tech_ip")
    private val KEY_TECH_STEALTH = booleanPreferencesKey("tech_stealth")
    private val KEY_TECH_FIREWALL = booleanPreferencesKey("tech_firewall_v2")
    private val KEY_TECH_FULL_TUNNEL = booleanPreferencesKey("tech_full_tunnel")
    private val KEY_TECH_ALBANIA_MODE = booleanPreferencesKey("tech_albania_mode")
    private val KEY_YOUTUBE_RECOMMENDATIONS = booleanPreferencesKey("youtube_recommendations")
    private val KEY_FIREWALL_MODES = stringPreferencesKey("firewall_modes_json")

    // Hot StateFlows for reactive + synchronous access (populated eagerly in IO scope)
    private val autoStartFlow = dataStore.data
        .map { it[KEY_AUTO_START] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val batteryOptFlow = dataStore.data
        .map { it[KEY_BATTERY_OPT] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val vpnActiveFlow = dataStore.data
        .map { it[KEY_VPN_ACTIVE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val caInstalledFlow = dataStore.data
        .map { it[KEY_CA_INSTALLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val dnsModeFlow = dataStore.data
        .map { it[KEY_DNS_MODE] ?: "PLAIN" }
        .stateIn(scope, SharingStarted.Eagerly, "PLAIN")

    private val techniquesFlow = dataStore.data
        .map { prefs ->
            BlockingTechniques(
                dnsFiltering = prefs[KEY_TECH_DNS] ?: true,
                sniInspection = prefs[KEY_TECH_SNI] ?: true,
                mitmProxy = prefs[KEY_TECH_MITM] ?: true,
                headerFilter = prefs[KEY_TECH_HEADER] ?: true,
                ipBlocking = prefs[KEY_TECH_IP] ?: true,
                stealthMode = prefs[KEY_TECH_STEALTH] ?: false,
                appFirewall = prefs[KEY_TECH_FIREWALL] ?: true,
                fullTunnel = prefs[KEY_TECH_FULL_TUNNEL] ?: false,
                albaniaMode = prefs[KEY_TECH_ALBANIA_MODE] ?: false
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, BlockingTechniques())

    private val youtubeRecommendationsFlow = dataStore.data
        .map { it[KEY_YOUTUBE_RECOMMENDATIONS] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val firewallModesFlow = dataStore.data
        .map { prefs ->
            val jsonStr = prefs[KEY_FIREWALL_MODES] ?: "{}"
            parseFirewallModes(jsonStr)
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    // Synchronous getters return the cached StateFlow.value (instant, non-blocking)
    var autoStart: Boolean
        get() = autoStartFlow.value
        set(value) {
            legacyPrefs.edit().putBoolean(Constants.PREF_AUTO_START, value).apply()
            scope.launch { dataStore.edit { it[KEY_AUTO_START] = value } }
        }

    var batteryOptimized: Boolean
        get() = batteryOptFlow.value
        set(value) {
            scope.launch { dataStore.edit { it[KEY_BATTERY_OPT] = value } }
        }

    var vpnActive: Boolean
        get() = vpnActiveFlow.value
        set(value) {
            legacyPrefs.edit().putBoolean(Constants.PREF_VPN_ACTIVE, value).apply()
            scope.launch { dataStore.edit { it[KEY_VPN_ACTIVE] = value } }
        }

    var caInstalled: Boolean
        get() = caInstalledFlow.value
        set(value) {
            scope.launch { dataStore.edit { it[KEY_CA_INSTALLED] = value } }
        }

    /**
     * Tracks whether Argus CA is installed in the system/user trust store.
     * Distinct from [caInstalled] which tracked legacy certificate manager state.
     */
    var isSystemCaInstalled: Boolean
        get() = caInstalledFlow.value
        set(value) {
            scope.launch { dataStore.edit { it[KEY_CA_INSTALLED] = value } }
        }

    var dnsMode: String
        get() = dnsModeFlow.value
        set(value) {
            scope.launch { dataStore.edit { it[KEY_DNS_MODE] = value } }
        }

    var techniques: BlockingTechniques
        get() = techniquesFlow.value
        set(value) {
            scope.launch {
                dataStore.edit {
                    it[KEY_TECH_DNS] = value.dnsFiltering
                    it[KEY_TECH_SNI] = value.sniInspection
                    it[KEY_TECH_MITM] = value.mitmProxy
                    it[KEY_TECH_HEADER] = value.headerFilter
                    it[KEY_TECH_IP] = value.ipBlocking
                    it[KEY_TECH_STEALTH] = value.stealthMode
                    it[KEY_TECH_FIREWALL] = value.appFirewall
                    it[KEY_TECH_FULL_TUNNEL] = value.fullTunnel
                    it[KEY_TECH_ALBANIA_MODE] = value.albaniaMode
                }
            }
        }

    var youtubeRecommendationsEnabled: Boolean
        get() = youtubeRecommendationsFlow.value
        set(value) {
            scope.launch { dataStore.edit { it[KEY_YOUTUBE_RECOMMENDATIONS] = value } }
        }

    // --- Per-App Firewall ---

    /** Get the firewall mode for a specific package. Defaults to [FirewallMode.DEFAULT]. */
    fun getFirewallMode(packageName: String): FirewallMode {
        return firewallModesFlow.value[packageName] ?: FirewallMode.DEFAULT
    }

    /** Set the firewall mode for a specific package. */
    fun setFirewallMode(packageName: String, mode: FirewallMode) {
        scope.launch {
            setFirewallModeNow(packageName, mode)
        }
    }

    suspend fun setFirewallModeNow(packageName: String, mode: FirewallMode) {
        dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_FIREWALL_MODES] ?: "{}"
            val json = JSONObject(jsonStr)
            if (mode == FirewallMode.DEFAULT) {
                json.remove(packageName)
            } else {
                json.put(packageName, mode.name)
            }
            prefs[KEY_FIREWALL_MODES] = json.toString()
        }
    }

    /** Read all firewall modes as a map (synchronous, reads from SharedPreferences fallback). */
    fun getAllFirewallModes(): Map<String, FirewallMode> {
        return firewallModesFlow.value
    }

    /** Set of packages currently in BLOCK mode — for fast sync lookup in PacketRouter. */
    fun getBlockedPackages(): Set<String> {
        return firewallModesFlow.value
            .filter { it.value == FirewallMode.BLOCK }
            .keys
    }

    /** Flow of all firewall modes — suitable for UI observation. */
    fun observeFirewallModes(): StateFlow<Map<String, FirewallMode>> = firewallModesFlow

    private fun parseFirewallModes(jsonStr: String): Map<String, FirewallMode> {
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, FirewallMode>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = FirewallMode.valueOf(json.getString(key))
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseFirewallMode(jsonStr: String, packageName: String): FirewallMode {
        return try {
            val json = JSONObject(jsonStr)
            val modeName = json.optString(packageName, FirewallMode.DEFAULT.name)
            FirewallMode.valueOf(modeName)
        } catch (_: Exception) {
            FirewallMode.DEFAULT
        }
    }

    // Flow observers
    fun observeAutoStart(): Flow<Boolean> = autoStartFlow
    fun observeVpnActive(): Flow<Boolean> = vpnActiveFlow
    fun observeDnsMode(): Flow<String> = dnsModeFlow
    fun observeTechniques(): Flow<BlockingTechniques> = techniquesFlow
    fun observeYoutubeRecommendations(): Flow<Boolean> = youtubeRecommendationsFlow
}
