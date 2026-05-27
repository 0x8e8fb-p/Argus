@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlin.math.roundToLong

/**
 * Counts up to [value] with an ease-out curve. Used for metric tiles.
 */
@Composable
fun AnimatedCounter(
    value: Long,
    modifier: Modifier = Modifier,
    durationMs: Int = 600,
    color: Color = MaterialTheme.colorScheme.primary,
    suffix: String = "",
    formatter: (Long) -> String = { it.toString() }
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatable.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMs, easing = EaseOutCubic)
        )
    }
    Text(
        text = formatter(animatable.value.roundToLong()) + suffix,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
