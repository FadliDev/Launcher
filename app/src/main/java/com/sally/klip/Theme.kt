package com.sally.klip

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    background = Color(0xFF0F0F12), surface = Color(0xFF1A1A20), surfaceVariant = Color(0xFF26262E),
    primary = Color(0xFF7C9CFF), onPrimary = Color(0xFF0B1230), secondary = Color(0xFF5EE0C0),
    tertiary = Color(0xFFFFB86B), error = Color(0xFFFF6B6B), outline = Color(0xFF3A3A45),
    onBackground = Color(0xFFE6E6EC), onSurface = Color(0xFFE6E6EC), onSurfaceVariant = Color(0xFFC4C4CE),
)

@Composable fun KlipTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Dark, content = content)
