@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.R
import com.nexusblock.ui.components.AnimatedStateText
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.MetricTile
import com.nexusblock.ui.components.NexusScreenHeader
import com.nexusblock.ui.components.StatusDot
import com.nexusblock.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onRequestVpnPermission: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val bandwidthTotal by viewModel.bandwidthTotal.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        NexusScreenHeader(
            title = "Command Center",
            subtitle = if (state.vpnActive) {
                "System DNS filtering is active across the TV profile."
            } else {
                "Start the shield to route DNS through NexusBlock."
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProtectionPanel(
                active = state.vpnActive,
                statusText = state.statusText,
                onToggle = { viewModel.toggleVpn(onRequestVpnPermission) },
                modifier = Modifier
                    .weight(1.2f)
                    .height(292.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MetricTile(
                        value = state.blockedCountText,
                        label = "Ads Blocked",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    MetricTile(
                        value = state.dataSavedText,
                        label = "Data Saved",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MetricTile(
                        value = bandwidthTotal,
                        label = "Traffic",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                    MetricTile(
                        value = state.domainCountText,
                        label = "Active Rules",
                        modifier = Modifier.weight(1f),
                        accent = Color(0xFFFF8A80)
                    )
                }
            }
        }

        Text(
            text = "Blocking Techniques",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(TECHNIQUES.chunked(2), key = { row -> row.joinToString { it.key } }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { technique ->
                        val enabled = state.techniques.isEnabled(technique.key)
                        TechniqueToggle(
                            title = technique.title,
                            subtitle = technique.subtitle,
                            enabled = enabled,
                            onToggle = { viewModel.setTechnique(technique.key, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtectionPanel(
    active: Boolean,
    statusText: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    FocusPanel(
        modifier = modifier,
        selected = active,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    StatusDot(color = accent, modifier = Modifier.size(18.dp))
                    Icon(
                        painter = painterResource(if (active) R.drawable.ic_vpn_key else R.drawable.ic_shield),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = if (active) "Protected" else "Disabled",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column {
                AnimatedStateText(
                    text = if (active) "Filtering ad and tracker domains" else "DNS protection is paused"
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onToggle,
                    modifier = Modifier
                        .width(260.dp)
                        .height(58.dp)
                ) {
                    Text(
                        text = if (active) "Stop Blocking" else "Start Blocking",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TechniqueToggle(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FocusPanel(
        modifier = modifier.height(86.dp),
        selected = enabled,
        onClick = { onToggle(!enabled) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

private data class Technique(
    val title: String,
    val subtitle: String,
    val key: String
)

private val TECHNIQUES = listOf(
    Technique("DNS Filtering", "Blocks ad domains before apps connect", "dns"),
    Technique("SNI Watch", "Flags encrypted connections to known ad hosts", "sni"),
    Technique("HTTPS Proxy", "Optional browser-level inspection for unpinned clients", "mitm"),
    Technique("Header Filter", "Removes common tracking headers in proxy mode", "header"),
    Technique("IP Blocking", "Drops known ad server IP ranges when visible", "ip"),
    Technique("Stealth Mode", "Blocks ICMP probes from the TV profile", "stealth"),
    Technique("App Firewall", "Applies per-app VPN bypass and block modes", "firewall"),
    Technique("Albania Mode", "YouTube region spoof — fewer ads via Albanian routing", "albania")
)

private fun com.nexusblock.data.repository.BlockingTechniques.isEnabled(key: String): Boolean {
    return when (key) {
        "dns" -> dnsFiltering
        "sni" -> sniInspection
        "mitm" -> mitmProxy
        "header" -> headerFilter
        "ip" -> ipBlocking
        "stealth" -> stealthMode
        "firewall" -> appFirewall
        "albania" -> albaniaMode
        else -> false
    }
}
