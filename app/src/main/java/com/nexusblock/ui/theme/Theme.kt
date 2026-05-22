@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

private val EmeraldGlassScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF0A3D1E),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFF69F0AE),
    onSecondary = Color(0xFF003D20),
    secondaryContainer = Color(0xFF145232),
    onSecondaryContainer = Color(0xFFC8FDD8),
    tertiary = Color(0xFFB2FF59),
    onTertiary = Color(0xFF1B3A00),
    tertiaryContainer = Color(0xFF2E5C00),
    onTertiaryContainer = Color(0xFFD9FF99),
    background = Color(0xFF050A08),
    onBackground = Color(0xFFF1F8F4),
    surface = Color(0xFF0A1F12),
    onSurface = Color(0xFFF1F8F4),
    surfaceVariant = Color(0xFF122A1A),
    onSurfaceVariant = Color(0xFF7DA889),
    error = Color(0xFFFF5252),
    onError = Color(0xFF2B0308),
    errorContainer = Color(0xFF5C1014),
    onErrorContainer = Color(0xFFFFCDD2)
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArgusBlockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EmeraldGlassScheme,
        typography = Typography,
        content = content
    )
}
