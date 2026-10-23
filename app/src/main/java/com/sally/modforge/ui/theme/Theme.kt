package com.sally.modforge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Token warna dari DESIGN.md §2.1. Satu aksen (primary), sisanya netral.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF3DDC84),
    onPrimary = Color(0xFF06220F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF16181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF1F2F4),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE2E4E8),
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3DDC84),
    onPrimary = Color(0xFF06220F),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFECEEF2),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFECEEF2),
    surfaceVariant = Color(0xFF1F232C),
    onSurfaceVariant = Color(0xFF8B93A1),
    outline = Color(0xFF2B303B),
    error = Color(0xFFFF6369),
)

@Composable
fun ModForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content,
    )
}
