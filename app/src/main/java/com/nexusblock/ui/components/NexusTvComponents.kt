@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nexusblock.R
import com.nexusblock.ui.Screen
import com.nexusblock.ui.theme.tvDimensions

private val GlassShape = RoundedCornerShape(16.dp)
private val GlassWhite = Color.White
private val Emerald = Color(0xFF00E676)

@Composable
fun ArgusBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // Static composition — three radial orbs baked into a single Canvas
    // pass. Previously this used three infinite transitions driving full
    // screen redraws every frame; on low-end Android TV GPUs that pegged
    // the render thread and made the whole system feel laggy.
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors.background)

        val orb1Center = Offset(size.width * 0.15f, size.height * 0.25f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Emerald.copy(alpha = 0.10f), Color.Transparent),
                center = orb1Center,
                radius = size.minDimension * 0.45f
            ),
            center = orb1Center,
            radius = size.minDimension * 0.45f
        )

        val orb2Center = Offset(size.width * 0.82f, size.height * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Emerald.copy(alpha = 0.07f), Color.Transparent),
                center = orb2Center,
                radius = size.minDimension * 0.5f
            ),
            center = orb2Center,
            radius = size.minDimension * 0.5f
        )

        val orb3Center = Offset(size.width * 0.5f, size.height * 0.1f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF69F0AE).copy(alpha = 0.05f), Color.Transparent),
                center = orb3Center,
                radius = size.minDimension * 0.35f
            ),
            center = orb3Center,
            radius = size.minDimension * 0.35f
        )
    }
}

@Composable
fun FocusPanel(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, focusSpring, label = "glassScale")
    val glowAlpha by animateFloatAsState(
        if (focused) 0.32f else if (selected) 0.12f else 0f,
        tween(durationMillis = 280, easing = EaseOutCubic),
        label = "glowAlpha"
    )
    val liftY by animateFloatAsState(
        if (focused) -2f else 0f, focusSpring, label = "glassLift"
    )
    val borderAlpha by animateFloatAsState(
        if (focused) 0.85f else if (selected) 0.45f else 0.18f,
        tween(220), label = "borderAlpha"
    )

    val glassBg = when {
        selected -> Brush.linearGradient(
            listOf(
                Emerald.copy(alpha = 0.16f),
                GlassWhite.copy(alpha = 0.05f)
            )
        )
        else -> Brush.linearGradient(
            listOf(
                GlassWhite.copy(alpha = 0.08f),
                GlassWhite.copy(alpha = 0.03f)
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        listOf(
            Emerald.copy(alpha = borderAlpha),
            Emerald.copy(alpha = borderAlpha * 0.25f)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = liftY
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    // Single-layer glow — lighter than 3 layers for TV GPU budget
                    drawRoundRect(
                        color = Emerald.copy(alpha = glowAlpha * 0.6f),
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                        topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                    )
                }
            }
            .clip(GlassShape)
            .background(glassBg)
            .border(width = 1.dp, brush = borderBrush, shape = GlassShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun ArgusScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(16.dp))
            action()
        }
    }
}

@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val dims = tvDimensions()
    // Parse pure numbers for count-up animation; preserves units (KB/MB/GB) as suffix.
    val (numeric, suffix) = remember(value) { parseMetric(value) }

    FocusPanel(modifier = modifier.height(dims.statTileHeight)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            if (numeric != null) {
                AnimatedCounter(
                    value = numeric,
                    color = accent,
                    suffix = suffix
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Splits a display string like "12.4 MB" into (12, " MB") for numeric
 * count-up animation. Fractional units are floored to keep the animation
 * monotonic; the unit suffix is preserved.
 */
private fun parseMetric(value: String): Pair<Long?, String> {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null to ""
    val firstNonNum = trimmed.indexOfFirst { !it.isDigit() && it != '.' && it != '-' }
    val numPart: String
    val rest: String
    if (firstNonNum < 0) {
        numPart = trimmed
        rest = ""
    } else {
        numPart = trimmed.substring(0, firstNonNum)
        rest = trimmed.substring(firstNonNum)
    }
    val asLong = numPart.toDoubleOrNull()?.toLong() ?: return null to value
    return asLong to rest
}

@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selectedKey
            FocusPanel(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                selected = isSelected,
                onClick = { onSelected(key) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Emerald else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color)
            .drawBehind {
                drawCircle(color.copy(alpha = 0.25f), radius = size.minDimension * 2.2f)
            }
    )
}

@Composable
fun AnimatedStateText(
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(150)) },
        label = "stateText",
        modifier = modifier
    ) { value ->
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArgusNavigationRail(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val dims = tvDimensions()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val items = listOf(
        RailItem(Screen.Home, R.drawable.ic_nav_dashboard),
        RailItem(Screen.Activity, R.drawable.ic_nav_logs),
        RailItem(Screen.Settings, R.drawable.ic_nav_settings)
    )
    val selectedIndex = items.indexOfFirst { it.screen.route == currentRoute }
        .coerceAtLeast(0)
    val itemHeight = 52.dp
    val itemSpacing = 8.dp
    val pillOffset by animateDpAsState(
        targetValue = (itemHeight + itemSpacing) * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navPill"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(dims.navRailWidth)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassWhite.copy(alpha = 0.06f),
                        GlassWhite.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )
            .drawWithCache {
                // Right-edge accent line — subtle separator from content
                onDrawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Emerald.copy(alpha = 0.18f),
                                Emerald.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(size.width - 1f, 0f),
                        size = Size(1f, size.height)
                    )
                }
            }
            .padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_argus),
            contentDescription = "ArgusBlock",
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (dims.navRailWidth > 100.dp) {
            Text(
                text = "ArgusBlock",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "TV Shield",
                style = MaterialTheme.typography.labelSmall,
                color = Emerald.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Container that hosts the sliding pill underneath the items.
        Box(modifier = Modifier.fillMaxWidth()) {
            // Sliding selection pill
            Box(
                modifier = Modifier
                    .offset(y = pillOffset)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Emerald.copy(alpha = 0.22f),
                                Emerald.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Emerald.copy(alpha = 0.55f),
                                Emerald.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    NavRailItem(
                        title = item.screen.title,
                        icon = item.icon,
                        selected = index == selectedIndex,
                        showLabel = dims.navRailWidth > 100.dp,
                        itemHeight = itemHeight,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(itemSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRailItem(
    title: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    showLabel: Boolean = true,
    itemHeight: androidx.compose.ui.unit.Dp = 52.dp,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val iconTint by animateColorAsState(
        if (selected) Emerald
        else if (focused) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220), label = "navIconTint"
    )
    val textColor by animateColorAsState(
        if (selected || focused) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220), label = "navTextColor"
    )
    val focusScale by animateFloatAsState(
        if (focused && !selected) 1.04f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "navItemScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (showLabel) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class RailItem(
    val screen: Screen,
    @DrawableRes val icon: Int
)
