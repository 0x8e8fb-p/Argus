@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.R
import com.nexusblock.ui.components.MetricTile
import com.nexusblock.ui.theme.tvDimensions
import com.nexusblock.ui.viewmodel.DashboardViewModel

private val ActiveGreen = Color(0xFF00E676)
private val InactiveRed = Color(0xFFFF5252)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onRequestVpnPermission: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val bandwidth by viewModel.bandwidthTotal.collectAsState()
    val dims = tvDimensions()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status bar — only shown when active
        if (state.vpnActive) {
            StatusBar(bandwidth = bandwidth)
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShieldIcon(active = state.vpnActive, size = dims.shieldSize)

            Spacer(modifier = Modifier.height(dims.spacingLarge))

            val statusColor by animateColorAsState(
                if (state.vpnActive) ActiveGreen else InactiveRed,
                tween(300),
                label = "statusColor"
            )
            Text(
                text = if (state.vpnActive) "Protected" else "Unprotected",
                style = MaterialTheme.typography.displaySmall,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (state.vpnActive) "Filtering ads & trackers across all apps"
                       else "Tap below to activate protection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dims.spacingLarge))

            Button(
                onClick = { viewModel.toggleVpn(onRequestVpnPermission) },
                modifier = Modifier
                    .width(dims.buttonWidth)
                    .height(dims.buttonHeight)
            ) {
                Text(
                    text = if (state.vpnActive) "Stop Protection" else "Start Protection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.spacingMedium)
        ) {
            MetricTile(
                value = state.blockedCountText,
                label = "Ads Blocked",
                modifier = Modifier.weight(1f),
                accent = ActiveGreen
            )
            MetricTile(
                value = state.dataSavedText,
                label = "Data Saved",
                modifier = Modifier.weight(1f),
                accent = Color(0xFF69F0AE)
            )
            MetricTile(
                value = state.domainCountText,
                label = "Active Rules",
                modifier = Modifier.weight(1f),
                accent = Color(0xFFB2FF59)
            )
        }
    }
}

@Composable
private fun StatusBar(bandwidth: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ActiveGreen.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ActiveGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelMedium,
            color = ActiveGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Bandwidth  $bandwidth",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShieldIcon(active: Boolean, size: androidx.compose.ui.unit.Dp) {
    val iconColor by animateColorAsState(
        if (active) ActiveGreen else InactiveRed,
        tween(300),
        label = "shieldColor"
    )
    Icon(
        painter = painterResource(R.drawable.ic_shield),
        contentDescription = if (active) "Protected" else "Unprotected",
        tint = iconColor,
        modifier = Modifier.size(size)
    )
}

