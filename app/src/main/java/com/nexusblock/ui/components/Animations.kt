@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.ExperimentalTvMaterial3Api
import kotlin.math.roundToLong

/**
 * Standard motion spec used across the app to keep animations consistent
 * (D3.5 style — light, snappy, premium).
 */
object ArgusMotion {
    val FocusSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val ColorSpec: AnimationSpec<Color> = tween(durationMillis = 280, easing = EaseOutCubic)
    val EmphasizedSpec: AnimationSpec<Float> = tween(durationMillis = 450, easing = EaseInOutCubic)
}

/**
 * Counts up from 0 (or from the previous value) to [value] over [durationMs]
 * with an ease-out curve. Used for metric tiles to feel alive.
 */
@Composable
fun AnimatedCounter(
    value: Long,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
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
    val display = formatter(animatable.value.roundToLong()) + suffix
    Text(
        text = display,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Subtle shimmer overlay used for skeletons / loading states.
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -800f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.02f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f)
        ),
        start = androidx.compose.ui.geometry.Offset(translate - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(translate + 200f, 0f)
    )
}

/**
 * Soft vignette overlay drawn on top of background to focus the eye on
 * central content. Cheap — done with a single radial gradient.
 */
@Composable
fun ArgusVignette(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.35f)
                    ),
                    radius = size.maxDimension * 0.75f
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}
