package com.sally.modforge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF3DDC84)
private val OnGreen = Color(0xFF06220F)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = OnGreen,
    secondary = Color(0xFF2A7550),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F2F4),
    onSurface = Color(0xFF16181D),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE2E4E8),
    error = Color(0xFFE5484D),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = OnGreen,
    secondary = Color(0xFF81DDA8),
    onSecondary = Color(0xFF06220F),
    background = Color(0xFF0F1115),
    surface = Color(0xFF171A21),
    surfaceVariant = Color(0xFF1F232C),
    onSurface = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF8B93A1),
    outline = Color(0xFF2B303B),
    error = Color(0xFFFF6369),
    onError = Color(0xFF2D0002)
)

@Composable
fun ModForgeTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
