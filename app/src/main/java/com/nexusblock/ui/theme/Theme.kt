
@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nexusblock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66E3D8),
    onPrimary = Color(0xFF061D20),
    primaryContainer = Color(0xFF123E44),
    onPrimaryContainer = Color(0xFFB8FFF7),
    secondary = Color(0xFFFFC857),
    onSecondary = Color(0xFF241800),
    secondaryContainer = Color(0xFF4C3813),
    onSecondaryContainer = Color(0xFFFFE6A3),
    tertiary = Color(0xFF7AA8FF),
    onTertiary = Color(0xFF07162E),
    tertiaryContainer = Color(0xFF243E70),
    onTertiaryContainer = Color(0xFFD8E5FF),
    background = Color(0xFF080B0F),
    onBackground = Color(0xFFEAF1F7),
    surface = Color(0xFF10161D),
    onSurface = Color(0xFFEAF1F7),
    surfaceVariant = Color(0xFF1A232D),
    onSurfaceVariant = Color(0xFF9CADB9),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2B0308),
    errorContainer = Color(0xFF52101A),
    onErrorContainer = Color(0xFFFFD5DA)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCEEEE),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF8A6200),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE1A6),
    onSecondaryContainer = Color(0xFF2B1B00),
    tertiary = Color(0xFF315FA8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E3FF),
    onTertiaryContainer = Color(0xFF001B45),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF10161D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10161D),
    surfaceVariant = Color(0xFFE3EAF0),
    onSurfaceVariant = Color(0xFF50616D),
    error = Color(0xFFB3263A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9DD),
    onErrorContainer = Color(0xFF41000A)
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NexusBlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
