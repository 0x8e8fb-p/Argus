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
import com.nexusblock.ui.components.ArgusScreenHeader
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.SegmentedControl
import com.nexusblock.ui.viewmodel.BlocklistViewModel
import com.nexusblock.ui.viewmodel.CustomRulesViewModel
import com.nexusblock.ui.viewmodel.DashboardViewModel
import com.nexusblock.ui.viewmodel.FirewallViewModel
import com.nexusblock.ui.viewmodel.BlocklistSource
import com.nexusblock.data.model.CustomRule
import com.nexusblock.data.model.FirewallMode
import com.nexusblock.data.model.description
import com.nexusblock.ui.viewmodel.AppFirewallInfo

private enum class AdvancedTab(val label: String) {
    TECHNIQUES("Techniques"),
    BLOCKLISTS("Blocklists"),
    RULES("Rules"),
    FIREWALL("Firewall")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    blocklistViewModel: BlocklistViewModel = hiltViewModel(),
    customRulesViewModel: CustomRulesViewModel = hiltViewModel(),
    firewallViewModel: FirewallViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(AdvancedTab.TECHNIQUES) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ArgusScreenHeader(
            title = "Advanced Configuration",
            subtitle = "Fine-tune blocking techniques, lists, rules, and per-app firewall."
        )

        SegmentedControl(
            options = AdvancedTab.entries.map { it.name to it.label },
            selectedKey = selectedTab.name,
            onSelected = { selectedTab = AdvancedTab.valueOf(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        when (selectedTab) {
            AdvancedTab.TECHNIQUES -> TechniquesContent(dashboardViewModel)
            AdvancedTab.BLOCKLISTS -> BlocklistsContent(blocklistViewModel)
            AdvancedTab.RULES -> RulesContent(customRulesViewModel)
            AdvancedTab.FIREWALL -> FirewallContent(firewallViewModel)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TechniquesContent(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(TECHNIQUES, key = { it.key }) { technique ->
            val enabled = state.techniques.isEnabled(technique.key)
            TechniqueToggleRow(
                title = technique.title,
                subtitle = technique.subtitle,
                enabled = enabled,
                onToggle = { viewModel.setTechnique(technique.key, it) }
            )
        }
    }
}

@Composable
private fun TechniqueToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        selected = enabled,
        onClick = { onToggle(!enabled) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BlocklistsContent(viewModel: BlocklistViewModel) {
    val sources by viewModel.sources.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val updateResult by viewModel.lastUpdateResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Update status pill
                if (!updateResult.isIdle) {
                    val statusColor = when (updateResult.status) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.secondary
                        androidx.work.WorkInfo.State.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val statusText = when (updateResult.status) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val parts = buildList {
                                if (updateResult.updated.isNotBlank()) add("Updated: ${updateResult.updated}")
                                if (updateResult.skipped.isNotBlank()) add("No change: ${updateResult.skipped}")
                                if (updateResult.failed.isNotBlank()) add("Failed: ${updateResult.failed}")
                            }
                            if (parts.isEmpty()) "Up to date" else parts.joinToString("  •  ")
                        }
                        androidx.work.WorkInfo.State.FAILED -> "Update failed"
                        androidx.work.WorkInfo.State.RUNNING -> "Updating blocklists..."
                        else -> "Checking for updates..."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = { viewModel.updateNow() },
                    enabled = !isUpdating
                ) {
                    Text(if (isUpdating) "Updating..." else "Update Now")
                }
            }
        }
        items(sources, key = { it.id }) { source ->
            BlocklistRow(
                source = source,
                onToggle = { viewModel.toggleSource(source.id, it) }
            )
        }
    }
}

@Composable
private fun BlocklistRow(source: BlocklistSource, onToggle: (Boolean) -> Unit) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        selected = source.enabled,
        onClick = { onToggle(!source.enabled) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
                    text = if (source.count > 0) "${source.count} rules" else "Ready after next update",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (source.enabled) "Active" else "Paused",
                style = MaterialTheme.typography.labelLarge,
                color = if (source.enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = source.enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RulesContent(viewModel: CustomRulesViewModel) {
    val rules by viewModel.rules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { showDialog = true }) {
                    Text("Add Rule")
                }
            }
        }
        if (rules.isEmpty()) {
            item {
                FocusPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No custom rules yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add block or allow rules for specific domains.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(count = rules.size, key = { rules[it].id }) { index ->
                val rule = rules[index]
                RuleRow(
                    rule = rule,
                    onToggle = { viewModel.toggleRule(rule) },
                    onDelete = { viewModel.deleteRule(rule) }
                )
            }
        }
    }

    if (showDialog) {
        AddRuleDialogAdvanced(
            onDismiss = { showDialog = false },
            onConfirm = { rule, isAllow, desc ->
                viewModel.addRule(rule, isAllow, desc)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RuleRow(rule: CustomRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        selected = rule.enabled,
        onClick = onToggle,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.rule,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (rule.isAllow) "Allow" else "Block",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rule.isAllow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onDelete) { Text("Del") }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddRuleDialogAdvanced(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit
) {
    var ruleText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BLOCK") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Add Custom Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedControl(
                    options = listOf("BLOCK" to "Block", "ALLOW" to "Allow"),
                    selectedKey = type,
                    onSelected = { type = it }
                )
                if (error != null) {
                    androidx.compose.material3.Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                androidx.compose.material3.OutlinedTextField(
                    value = ruleText,
                    onValueChange = { ruleText = it; error = null },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Rule") },
                    placeholder = { androidx.compose.material3.Text("||ads.example.com^") },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ruleText.isBlank()) {
                        error = "Rule cannot be empty"
                    } else {
                        onConfirm(ruleText, type == "ALLOW", description.takeIf { it.isNotBlank() })
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FirewallContent(viewModel: FirewallViewModel) {
    val apps by viewModel.apps.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            FirewallRow(
                app = app,
                onModeSelected = { viewModel.setMode(app.packageName, it) }
            )
        }
    }
}

@Composable
private fun FirewallRow(app: AppFirewallInfo, onModeSelected: (FirewallMode) -> Unit) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp),
        selected = app.mode != FirewallMode.DEFAULT,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
                    text = app.mode.description(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (app.mode) {
                        FirewallMode.ALLOW -> MaterialTheme.colorScheme.secondary
                        FirewallMode.BLOCK -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
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

private data class Technique(
    val title: String,
    val subtitle: String,
    val key: String
)

private val TECHNIQUES = listOf(
    Technique("DNS Filtering", "Blocks ad domains before apps connect", "dns"),
    Technique("IP Blocking", "Drops known ad server IP ranges when visible", "ip"),
    Technique("Stealth Mode", "Blocks ICMP probes from the TV profile", "stealth"),
    Technique("App Firewall", "Applies per-app VPN bypass and block modes", "firewall")
)

private fun com.nexusblock.data.repository.BlockingTechniques.isEnabled(key: String): Boolean {
    return when (key) {
        "dns" -> dnsFiltering
        "ip" -> ipBlocking
        "stealth" -> stealthMode
        "firewall" -> appFirewall
        else -> false
    }
}
