@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import kotlin.math.cos
import kotlin.math.sin

private val GlassShape = RoundedCornerShape(16.dp)
private val GlassWhite = Color.White
private val Emerald = Color(0xFF00E676)

@Composable
fun ArgusBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "ambientBg")

    val drift1 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift1"
    )
    val drift2 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift2"
    )
    val drift3 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors.background)

        val orb1Center = Offset(
            x = size.width * 0.15f + cos(drift1) * size.width * 0.05f,
            y = size.height * 0.25f + sin(drift1) * size.height * 0.08f
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Emerald.copy(alpha = 0.12f), Color.Transparent),
                center = orb1Center,
                radius = size.minDimension * 0.45f
            ),
            center = orb1Center,
            radius = size.minDimension * 0.45f
        )

        val orb2Center = Offset(
            x = size.width * 0.82f + cos(drift2) * size.width * 0.04f,
            y = size.height * 0.7f + sin(drift2) * size.height * 0.06f
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Emerald.copy(alpha = 0.08f), Color.Transparent),
                center = orb2Center,
                radius = size.minDimension * 0.5f
            ),
            center = orb2Center,
            radius = size.minDimension * 0.5f
        )

        val orb3Center = Offset(
            x = size.width * 0.5f + sin(drift3) * size.width * 0.06f,
            y = size.height * 0.1f + cos(drift3) * size.height * 0.04f
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF69F0AE).copy(alpha = 0.06f), Color.Transparent),
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
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(220), label = "glassScale")
    val glowAlpha by animateFloatAsState(
        if (focused) 0.18f else 0f, tween(300), label = "glowAlpha"
    )

    val glassBg = when {
        selected -> Brush.linearGradient(
            listOf(
                Emerald.copy(alpha = 0.12f),
                GlassWhite.copy(alpha = 0.04f)
            )
        )
        else -> Brush.linearGradient(
            listOf(
                GlassWhite.copy(alpha = 0.07f),
                GlassWhite.copy(alpha = 0.03f)
            )
        )
    }

    val borderBrush = when {
        focused -> Brush.linearGradient(
            listOf(Emerald.copy(alpha = 0.6f), Emerald.copy(alpha = 0.15f))
        )
        selected -> Brush.linearGradient(
            listOf(Emerald.copy(alpha = 0.3f), GlassWhite.copy(alpha = 0.08f))
        )
        else -> Brush.linearGradient(
            listOf(GlassWhite.copy(alpha = 0.12f), GlassWhite.copy(alpha = 0.04f))
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = Emerald.copy(alpha = glowAlpha),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                        topLeft = Offset(-3.dp.toPx(), -3.dp.toPx())
                    )
                }
            }
            .clip(GlassShape)
            .background(glassBg)
            .border(width = 0.5.dp, brush = borderBrush, shape = GlassShape)
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
    FocusPanel(modifier = modifier.height(dims.statTileHeight)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(dims.navRailWidth)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassWhite.copy(alpha = 0.05f),
                        GlassWhite.copy(alpha = 0.02f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(GlassWhite.copy(alpha = 0.1f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 12.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_argus),
            contentDescription = "ArgusBlock",
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (dims.navRailWidth > 80.dp) {
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
        Spacer(modifier = Modifier.height(28.dp))

        items.forEach { item ->
            NavRailItem(
                title = item.screen.title,
                icon = item.icon,
                selected = currentRoute == item.screen.route,
                showLabel = dims.navRailWidth > 80.dp,
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
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun NavRailItem(
    title: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (showLabel) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = if (selected) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
