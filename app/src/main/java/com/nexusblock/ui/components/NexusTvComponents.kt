@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.focus.onFocusChanged
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

private val CardShape = RoundedCornerShape(12.dp)
private val Emerald = Color(0xFF00E676)

@Composable
fun ArgusBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
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
    val borderColor by animateColorAsState(
        when {
            focused -> Emerald.copy(alpha = 0.8f)
            selected -> Emerald.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        },
        tween(160),
        label = "panelBorder"
    )
    val bgColor by animateColorAsState(
        when {
            focused -> MaterialTheme.colorScheme.surfaceVariant
            selected -> Emerald.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        tween(160),
        label = "panelBg"
    )

    Box(
        modifier = modifier
            .clip(CardShape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
    val metricParts = remember(value) { parseMetric(value) }
    val numeric = metricParts.first
    val suffix = metricParts.second

    FocusPanel(modifier = modifier.height(dims.statTileHeight)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            if (numeric != null) {
                AnimatedCounter(value = numeric, color = accent, suffix = suffix)
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

private fun parseMetric(value: String): Pair<Long?, String> {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null to ""
    val firstNonNum = trimmed.indexOfFirst { !it.isDigit() && it != '.' && it != '-' }
    val numPart: String
    val rest: String
    if (firstNonNum < 0) {
        numPart = trimmed; rest = ""
    } else {
        numPart = trimmed.substring(0, firstNonNum); rest = trimmed.substring(firstNonNum)
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label) ->
            val isSelected = key == selectedKey
            FocusPanel(
                modifier = Modifier.width(120.dp).height(40.dp),
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
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_argus),
            contentDescription = "ArgusBlock",
            tint = Color.Unspecified,
            modifier = Modifier.size(36.dp)
        )
        if (dims.navRailWidth > 100.dp) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ArgusBlock",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        items.forEach { item ->
            val isSelected = item.screen.route == currentRoute
            NavRailItem(
                title = item.screen.title,
                icon = item.icon,
                selected = isSelected,
                showLabel = dims.navRailWidth > 100.dp,
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
            Spacer(modifier = Modifier.height(8.dp))
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
    var focused by remember { mutableStateOf(false) }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = tween(160),
        label = "navScale"
    )

    val bgColor by animateColorAsState(
        when {
            selected -> Emerald.copy(alpha = 0.18f)
            focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        tween(160), label = "navBg"
    )

    val borderColor by animateColorAsState(
        when {
            selected -> Emerald.copy(alpha = 0.7f)
            focused -> Emerald.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        tween(160), label = "navBorder"
    )

    val iconTint by animateColorAsState(
        when {
            selected -> Emerald
            focused -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        tween(160), label = "navTint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (selected || focused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .padding(horizontal = 10.dp),
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
                color = iconTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class RailItem(val screen: Screen, @DrawableRes val icon: Int)

