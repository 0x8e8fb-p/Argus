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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.NexusScreenHeader
import com.nexusblock.ui.components.SegmentedControl
import com.nexusblock.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val autoStart by viewModel.autoStart.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    // CA certificate removed with proxy layer
    val dnsProfile by viewModel.dnsProfile.collectAsState()
    

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            NexusScreenHeader(
                title = "Settings",
                subtitle = "Keep blocking resilient without breaking TV app basics."
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            SettingSwitchRow(
                title = "Auto-start on boot",
                subtitle = "Restart protection after Android TV reboots",
                checked = autoStart,
                onCheckedChange = viewModel::setAutoStart
            )
        }

        item {
            FocusPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DNS Filter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Current: ${formatDnsProfile(dnsProfile)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SegmentedControl(
                        options = listOf(
                            "adguard_standard" to "AdGuard",
                            "adguard_family" to "Family",
                            "cloudflare_1111" to "Cloudflare",
                            "cloudflare_family" to "CF Family",
                            "cleanbrowsing_adult" to "CleanBrowsing",
                            "nextdns" to "NextDNS",
                            "quad9" to "Quad9"
                        ),
                        selectedKey = dnsProfile,
                        onSelected = viewModel::setDnsProfile
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
            .height(86.dp),
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
                    maxLines = 2,
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
            .height(86.dp),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Button(
                onClick = onClick,
                enabled = enabled
            ) {
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
