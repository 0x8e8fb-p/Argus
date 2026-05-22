@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nexusblock.ui.components.AnimatedStateText
import com.nexusblock.ui.components.FocusPanel
import com.nexusblock.ui.components.MetricTile
import com.nexusblock.ui.theme.tvDimensions
import com.nexusblock.ui.viewmodel.DashboardViewModel

private val EmeraldGreen = Color(0xFF00E676)
private val EmeraldDark = Color(0xFF00C853)
private val ErrorRed = Color(0xFFFF5252)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onRequestVpnPermission: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val dims = tvDimensions()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(dims.spacingMedium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShieldHero(
                active = state.vpnActive,
                shieldSize = dims.shieldSize
            )

            Spacer(modifier = Modifier.height(dims.spacingLarge))

            Text(
                text = if (state.vpnActive) "Protected" else "Unprotected",
                style = MaterialTheme.typography.displaySmall,
                color = if (state.vpnActive) EmeraldGreen else ErrorRed,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dims.spacingSmall))

            AnimatedStateText(
                text = if (state.vpnActive) {
                    "Filtering ads & trackers across all apps"
                } else {
                    "Tap below to activate protection"
                }
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
                    style = MaterialTheme.typography.titleMedium
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
                accent = EmeraldGreen
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
private fun ShieldHero(
    active: Boolean,
    shieldSize: androidx.compose.ui.unit.Dp
) {
    val transition = rememberInfiniteTransition(label = "shieldBreath")
    val breathScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "breathScale"
    )
    val glowPulse by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "glowPulse"
    )

    val scale by animateFloatAsState(
        if (active) breathScale else 1f, tween(500), label = "heroScale"
    )
    val iconColor by animateColorAsState(
        if (active) EmeraldGreen else ErrorRed, tween(600), label = "iconColor"
    )
    val ringAlpha by animateFloatAsState(
        if (active) glowPulse else 0f, tween(600), label = "ringAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(shieldSize * 1.7f)
            .drawBehind {
                if (active) {
                    drawCircle(
                        color = EmeraldGreen.copy(alpha = ringAlpha * 0.25f),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color = EmeraldGreen.copy(alpha = ringAlpha * 0.5f),
                        radius = size.minDimension / 2.6f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = EmeraldDark.copy(alpha = ringAlpha * 0.3f),
                        radius = size.minDimension / 3.2f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = if (active) "Protected" else "Unprotected",
            tint = iconColor,
            modifier = Modifier
                .size(shieldSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}
