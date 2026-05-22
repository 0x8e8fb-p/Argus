package com.nexusblock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TvDimensions(
    val navRailWidth: Dp = 168.dp,
    val shieldSize: Dp = 120.dp,
    val typeScale: Float = 1f,
    val contentPadding: Dp = 28.dp,
    val panelCorner: Dp = 16.dp,
    val statTileHeight: Dp = 100.dp,
    val buttonHeight: Dp = 56.dp,
    val buttonWidth: Dp = 240.dp,
    val iconNav: Dp = 22.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp
)

val CompactTvDimensions = TvDimensions(
    navRailWidth = 84.dp,
    shieldSize = 72.dp,
    typeScale = 0.85f,
    contentPadding = 16.dp,
    panelCorner = 8.dp,
    statTileHeight = 72.dp,
    buttonHeight = 44.dp,
    buttonWidth = 180.dp,
    iconNav = 20.dp,
    spacingSmall = 6.dp,
    spacingMedium = 10.dp,
    spacingLarge = 16.dp
)

val MediumTvDimensions = TvDimensions(
    navRailWidth = 144.dp,
    shieldSize = 96.dp,
    typeScale = 0.92f,
    contentPadding = 22.dp,
    panelCorner = 10.dp,
    statTileHeight = 86.dp,
    buttonHeight = 50.dp,
    buttonWidth = 210.dp,
    iconNav = 21.dp,
    spacingSmall = 7.dp,
    spacingMedium = 12.dp,
    spacingLarge = 20.dp
)

val ExpandedTvDimensions = TvDimensions()

val LocalTvDimensions = compositionLocalOf { ExpandedTvDimensions }

@Composable
fun tvDimensions(): TvDimensions = LocalTvDimensions.current

fun dimensionsForWidth(screenWidthDp: Int): TvDimensions = when {
    screenWidthDp < 960 -> CompactTvDimensions
    screenWidthDp < 1280 -> MediumTvDimensions
    else -> ExpandedTvDimensions
}
