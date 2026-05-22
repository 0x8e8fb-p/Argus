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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.data.model.CustomRule
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.ArgusScreenHeader
import com.nexusblock.ui.components.SegmentedControl
import com.nexusblock.ui.viewmodel.CustomRulesViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CustomRulesScreen(
    viewModel: CustomRulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            ArgusScreenHeader(
                title = "Custom Rules",
                subtitle = "Add targeted block or allow rules when an app reveals a new domain.",
                action = {
                    Button(onClick = { showDialog = true }) {
                        Text("Add Rule")
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (rules.isEmpty()) {
            item {
                FocusPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No custom rules yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use this when logs show a domain you want to force block or force allow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(count = rules.size, key = { rules[it].id }) { index ->
                val rule = rules[index]
                CustomRuleItem(
                    rule = rule,
                    onToggle = { viewModel.toggleRule(rule) },
                    onDelete = { viewModel.deleteRule(rule) }
                )
            }
        }
    }

    if (showDialog) {
        AddRuleDialog(
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
private fun CustomRuleItem(
    rule: CustomRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        selected = rule.enabled,
        onClick = onToggle,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
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
                    text = if (rule.isAllow) "Allow rule" else "Block rule",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (rule.isAllow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
                rule.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit
) {
    var ruleText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BLOCK") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
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
                OutlinedTextField(
                    value = ruleText,
                    onValueChange = {
                        ruleText = it
                        error = null
                    },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Rule") },
                    placeholder = { androidx.compose.material3.Text("||ads.example.com^") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Description") },
                    placeholder = { androidx.compose.material3.Text("Optional") },
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
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
