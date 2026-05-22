@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.data.repository.VpnRoutingMode
import com.nexusblock.ui.components.ArgusScreenHeader
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.SegmentedControl
import com.nexusblock.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val autoStart by viewModel.autoStart.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val dnsProfile by viewModel.dnsProfile.collectAsState()
    val vpnRoutingMode by viewModel.vpnRoutingMode.collectAsState()
    var showAdvanced by remember { mutableStateOf(false) }

    if (showAdvanced) {
        AdvancedSettingsScreen()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            ArgusScreenHeader(
                title = "Settings",
                subtitle = "General configuration for ArgusBlock."
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingSwitchRow(
                title = "Auto-start on boot",
                subtitle = "Resume protection after TV reboots",
                checked = autoStart,
                onCheckedChange = viewModel::setAutoStart
            )
        }

        item {
            FocusPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column {
                    Text(
                        text = "DNS Provider",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: ${formatDnsProfile(dnsProfile)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SegmentedControl(
                        options = listOf(
                            "adguard_standard" to "AdGuard",
                            "adguard_family" to "Family",
                            "cloudflare_1111" to "CF",
                            "quad9" to "Quad9"
                        ),
                        selectedKey = dnsProfile,
                        onSelected = viewModel::setDnsProfile
                    )
                }
            }
        }

        item {
            FocusPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column {
                    Text(
                        text = "VPN Routing",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: ${formatVpnRoutingMode(vpnRoutingMode)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SegmentedControl(
                        options = listOf(
                            VpnRoutingMode.DNS_ONLY.storageKey to "DNS",
                            VpnRoutingMode.FULL_ROUTE_SAFE.storageKey to "Safe",
                            VpnRoutingMode.FULL_ROUTE_AGGRESSIVE.storageKey to "Aggressive"
                        ),
                        selectedKey = vpnRoutingMode.storageKey,
                        onSelected = { selected ->
                            viewModel.setVpnRoutingMode(VpnRoutingMode.fromStorageKey(selected))
                        }
                    )
                }
            }
        }

        item {
            SettingActionRow(
                title = "Battery optimization",
                subtitle = if (isBatteryOptimized) "Exemption recorded" else "Request exemption for long sessions",
                buttonText = if (isBatteryOptimized) "Done" else "Request",
                enabled = !isBatteryOptimized,
                onClick = { viewModel.requestBatteryOptimization() }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            FocusPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                onClick = { showAdvanced = true },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Advanced Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Blocking techniques, blocklists, rules & firewall",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onClick, enabled = enabled) {
                Text(buttonText)
            }
        }
    }
}

private fun formatDnsProfile(id: String): String = when (id) {
    "adguard_standard" -> "AdGuard Standard"
    "adguard_family" -> "AdGuard Family"
    "adguard_nonfiltering" -> "AdGuard Non-filtering"
    "cloudflare_1111" -> "Cloudflare 1.1.1.1"
    "cloudflare_security" -> "Cloudflare Security"
    "cloudflare_family" -> "Cloudflare Family"
    "cleanbrowsing_adult" -> "CleanBrowsing Adult"
    "cleanbrowsing_security" -> "CleanBrowsing Security"
    "nextdns" -> "NextDNS"
    "controld_free" -> "ControlD"
    "quad9" -> "Quad9"
    "google" -> "Google DNS"
    else -> id
}

private fun formatVpnRoutingMode(mode: VpnRoutingMode): String = when (mode) {
    VpnRoutingMode.DNS_ONLY -> "DNS Only"
    VpnRoutingMode.FULL_ROUTE_SAFE -> "Full Route Safe"
    VpnRoutingMode.FULL_ROUTE_AGGRESSIVE -> "Full Route Aggressive"
}
