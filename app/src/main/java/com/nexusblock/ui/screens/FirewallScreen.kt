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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.data.model.FirewallMode
import com.nexusblock.data.model.description
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.NexusScreenHeader
import com.nexusblock.ui.components.SegmentedControl
import com.nexusblock.ui.viewmodel.AppFirewallInfo
import com.nexusblock.ui.viewmodel.FirewallViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FirewallScreen(
    viewModel: FirewallViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            NexusScreenHeader(
                title = "App Firewall",
                subtitle = "Launchable TV apps are included, even when preinstalled as system apps."
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(apps, key = { it.packageName }) { app ->
            FirewallItem(
                app = app,
                onModeSelected = { mode -> viewModel.setMode(app.packageName, mode) }
            )
        }
    }
}

@Composable
private fun FirewallItem(
    app: AppFirewallInfo,
    onModeSelected: (FirewallMode) -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp),
        selected = app.mode != FirewallMode.DEFAULT,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.mode.description(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (app.mode) {
                        FirewallMode.ALLOW -> MaterialTheme.colorScheme.secondary
                        FirewallMode.BLOCK -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SegmentedControl(
                options = listOf(
                    FirewallMode.DEFAULT.name to "VPN",
                    FirewallMode.ALLOW.name to "Bypass",
                    FirewallMode.BLOCK.name to "DNS"
                ),
                selectedKey = app.mode.name,
                onSelected = { onModeSelected(FirewallMode.valueOf(it)) }
            )
        }
    }
}
