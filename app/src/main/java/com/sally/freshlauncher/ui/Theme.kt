package com.sally.freshlauncher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3A9D23),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5C4),
    onPrimaryContainer = Color(0xFF0F3A05),
    secondary = Color(0xFF4FA8E8),
    tertiary = Color(0xFFE0A030),
    background = Color(0xFFF6FAF3),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8F1E4)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8BD66B),
    onPrimary = Color(0xFF0F3A05),
    primaryContainer = Color(0xFF225015),
    onPrimaryContainer = Color(0xFFD7F5C4),
    secondary = Color(0xFF8CC8F5),
    tertiary = Color(0xFFF2C36B)
)

@Composable
fun FreshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}
