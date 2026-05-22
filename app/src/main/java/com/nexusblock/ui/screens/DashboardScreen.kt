@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.nexusblock.ui.components.StatusDot
import com.nexusblock.ui.theme.tvDimensions
import com.nexusblock.ui.viewmodel.DashboardViewModel

private val EmeraldGreen = Color(0xFF00E676)
private val EmeraldDark = Color(0xFF00C853)
private val EmeraldGlow = Color(0xFF69F0AE)
private val ErrorRed = Color(0xFFFF5252)

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
        // Top status pill — only appears when VPN is active.
        AnimatedVisibility(
            visible = state.vpnActive,
            enter = fadeIn(tween(400)) + expandVertically(tween(400)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            StatusPill(bandwidth = bandwidth)
        }

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

            ProtectionButton(
                active = state.vpnActive,
                onClick = { viewModel.toggleVpn(onRequestVpnPermission) },
                width = dims.buttonWidth,
                height = dims.buttonHeight
            )
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
                accent = EmeraldGlow
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

/**
 * Top status pill — animated entry, glowing live indicator + bandwidth.
 */
@Composable
private fun StatusPill(bandwidth: String) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        EmeraldGreen.copy(alpha = 0.18f),
                        EmeraldGreen.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        EmeraldGreen.copy(alpha = 0.55f),
                        EmeraldGreen.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        StatusDot(color = EmeraldGreen)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .size(width = 1.dp, height = 14.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Bandwidth",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = bandwidth,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtectionButton(
    active: Boolean,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(height)
    ) {
        Text(
            text = if (active) "Stop Protection" else "Start Protection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Shield hero — a single soft halo + one slow rotating dashed ring when
 * active. Designed to be GPU-cheap so the whole-system frame budget on
 * mid-range Android TVs is preserved.
 *
 * IMPORTANT: The infinite transition only runs when VPN is active. When
 * inactive, no animations tick — zero GPU cost in idle state.
 */
@Composable
private fun ShieldHero(
    active: Boolean,
    shieldSize: androidx.compose.ui.unit.Dp
) {
    // Only animate when VPN is active. When inactive, these values are static
    // (0f / 0.20f) and no infinite transition is allocated, saving GPU frames.
    val rotationSlow: Float
    val pulse: Float
    if (active) {
        val transition = rememberInfiniteTransition(label = "shieldHero")
        rotationSlow = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                tween(24000, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "rotSlow"
        ).value
        pulse = transition.animateFloat(
            initialValue = 0.20f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                tween(2600, easing = EaseInOutCubic),
                RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else {
        rotationSlow = 0f
        pulse = 0.20f
    }

    val scale by animateFloatAsState(
        if (active) 1.0f else 0.98f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "heroScale"
    )
    val iconColor by animateColorAsState(
        if (active) EmeraldGreen else ErrorRed,
        tween(600, easing = EaseOutCubic),
        label = "iconColor"
    )
    val ringIntensity by animateFloatAsState(
        if (active) 1f else 0f,
        tween(500, easing = EaseOutCubic),
        label = "ringIntensity"
    )

    val canvasSize = shieldSize * 2.1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(canvasSize)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (ringIntensity <= 0.01f) return@Canvas
            val cx = size.width / 2f
            val cy = size.height / 2f
            val minDim = size.minDimension

            // Soft halo (pulsing)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        EmeraldGreen.copy(alpha = pulse * 0.30f * ringIntensity),
                        EmeraldGreen.copy(alpha = pulse * 0.08f * ringIntensity),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = minDim * 0.55f
                ),
                center = Offset(cx, cy),
                radius = minDim * 0.55f
            )

            // Single rotating dashed ring
            val outerRadius = minDim * 0.48f
            rotate(rotationSlow, pivot = Offset(cx, cy)) {
                val steps = 24
                val sweepArc = 360f / steps * 0.55f
                for (i in 0 until steps) {
                    drawArc(
                        color = EmeraldGreen.copy(alpha = 0.55f * ringIntensity),
                        startAngle = i * (360f / steps),
                        sweepAngle = sweepArc,
                        useCenter = false,
                        topLeft = Offset(cx - outerRadius, cy - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // Static inner ring
            drawCircle(
                color = EmeraldDark.copy(alpha = (0.20f + pulse * 0.20f) * ringIntensity),
                radius = minDim * 0.32f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

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
