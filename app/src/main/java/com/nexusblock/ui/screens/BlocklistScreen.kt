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
import androidx.compose.foundation.lazy.items
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
import com.nexusblock.ui.viewmodel.BlocklistSource
import com.nexusblock.ui.viewmodel.BlocklistViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlocklistScreen(
    viewModel: BlocklistViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            NexusScreenHeader(
                title = "Blocklists",
                subtitle = "Public DNS filters, OTT safety rules, and encrypted DNS defenses.",
                action = {
                    Button(
                        onClick = { viewModel.updateNow() },
                        enabled = !isUpdating
                    ) {
                        Text(if (isUpdating) "Updating" else "Update Now")
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(sources, key = { it.id }) { source ->
            BlocklistItem(
                source = source,
                onToggle = { enabled -> viewModel.toggleSource(source.id, enabled) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BlocklistItem(
    source: BlocklistSource,
    onToggle: (Boolean) -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp),
        selected = source.enabled,
        onClick = { onToggle(!source.enabled) },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (source.count > 0) {
                        "${source.count} rules"
                    } else {
                        "Ready after next update"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (source.enabled) "Active" else "Paused",
                style = MaterialTheme.typography.labelLarge,
                color = if (source.enabled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(18.dp))
            Switch(
                checked = source.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
