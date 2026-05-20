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
import androidx.compose.ui.geometry.Offset
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

private val PanelShape = RoundedCornerShape(8.dp)

@Composable
fun NexusBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "background")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors.background)
        val grid = 72.dp.toPx()
        val lineColor = colors.primary.copy(alpha = 0.07f)
        var x = -grid + scan * grid
        while (x < size.width + grid) {
            drawLine(lineColor, Offset(x, 0f), Offset(x + size.height * 0.22f, size.height), 1.dp.toPx())
            x += grid
        }
        var y = 0f
        while (y < size.height) {
            drawLine(colors.tertiary.copy(alpha = 0.045f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += grid
        }
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to colors.surface.copy(alpha = 0.72f)
            )
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
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, label = "panelScale")
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "panelBorder"
    )
    val background = if (selected) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PanelShape)
            .background(background)
            .border(
                width = if (focused || selected) 1.5.dp else 1.dp,
                color = borderColor.copy(alpha = if (focused || selected) 0.95f else 0.36f),
                shape = PanelShape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .focusable()
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun NexusScreenHeader(
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
            Spacer(modifier = Modifier.height(6.dp))
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
    FocusPanel(modifier = modifier.height(128.dp)) {
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
            Spacer(modifier = Modifier.height(6.dp))
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            FocusPanel(
                modifier = Modifier
                    .width(128.dp)
                    .height(46.dp),
                selected = key == selectedKey,
                onClick = { onSelected(key) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (key == selectedKey) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
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
                drawCircle(color.copy(alpha = 0.2f), radius = size.minDimension * 1.8f)
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
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
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
fun NexusNavigationRail(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val items = listOf(
        RailItem(Screen.Dashboard, R.drawable.ic_nav_dashboard),
        RailItem(Screen.Blocklists, R.drawable.ic_nav_blocklists),
        RailItem(Screen.CustomRules, R.drawable.ic_nav_rules),
        RailItem(Screen.Firewall, R.drawable.ic_nav_firewall),
        RailItem(Screen.Logs, R.drawable.ic_nav_logs),
        RailItem(Screen.Settings, R.drawable.ic_nav_settings)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(172.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_nexus),
            contentDescription = "NexusBlock",
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "NexusBlock",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "TV Shield",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(22.dp))

        items.forEach { item ->
            NavRailItem(
                title = item.screen.title,
                icon = item.icon,
                selected = currentRoute == item.screen.route,
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
    onClick: () -> Unit
) {
    FocusPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class RailItem(
    val screen: Screen,
    @DrawableRes val icon: Int
)
