package com.sally.modforge.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F397),
    onPrimaryContainer = Color(0xFF0B2000),
    secondary = Color(0xFF59624E),
    tertiary = Color(0xFF8B5000),
    background = Color(0xFFF7FAF2),
    surface = Color(0xFFF7FAF2),
    surfaceVariant = Color(0xFFE1E4D9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD77E),
    onPrimary = Color(0xFF163800),
    primaryContainer = Color(0xFF28500C),
    onPrimaryContainer = Color(0xFFB8F397),
    secondary = Color(0xFFC0C9B3),
    tertiary = Color(0xFFFFB95C),
    background = Color(0xFF11140E),
    surface = Color(0xFF11140E),
    surfaceVariant = Color(0xFF42483C)
)

@Composable
fun ModForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
