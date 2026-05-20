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
    val caInstalled by viewModel.caInstalled.collectAsState()
    val dnsMode by viewModel.dnsMode.collectAsState()
    val youtubeRecommendations by viewModel.youtubeRecommendations.collectAsState()

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
            SettingSwitchRow(
                title = "Allow YouTube recommendations",
                subtitle = if (youtubeRecommendations) {
                    "Keeps home, related videos, and playback APIs available"
                } else {
                    "Stricter mode may disrupt YouTube feeds or playback"
                },
                checked = youtubeRecommendations,
                onCheckedChange = viewModel::setYoutubeRecommendations
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
                            text = "DNS Upstream",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Current: ${formatDnsMode(dnsMode)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SegmentedControl(
                        options = listOf(
                            "PLAIN" to "Plain",
                            "DOH_CLOUDFLARE" to "Cloudflare",
                            "DOH_GOOGLE" to "Google",
                            "DOH_QUAD9" to "Quad9"
                        ),
                        selectedKey = dnsMode,
                        onSelected = viewModel::setDnsMode
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
            SettingActionRow(
                title = "CA Certificate",
                subtitle = if (caInstalled) "Certificate flow completed" else "Optional proxy mode for compatible clients",
                buttonText = if (caInstalled) "Done" else "Open",
                enabled = !caInstalled,
                onClick = { viewModel.openCaInstallScreen() }
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

private fun formatDnsMode(mode: String): String = when (mode) {
    "PLAIN" -> "Plain DNS"
    "DOH_CLOUDFLARE" -> "Cloudflare DoH"
    "DOH_GOOGLE" -> "Google DoH"
    "DOH_QUAD9" -> "Quad9 DoH"
    else -> mode
}
