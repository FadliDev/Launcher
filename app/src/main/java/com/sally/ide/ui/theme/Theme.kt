package com.sally.ide.ui.theme

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

private val SallyLight = lightColorScheme(
    primary = Color(0xFF6250A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF201047),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    surfaceVariant = Color(0xFFE7E0EC),
    outline = Color(0xFF797582)
)

private val SallyDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    secondaryContainer = Color(0xFF4A4458),
    background = Color(0xFF111117),
    surface = Color(0xFF17171F),
    surfaceVariant = Color(0xFF2B2933),
    outline = Color(0xFF938F9D)
)

@Composable
fun SallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        amoled && darkTheme -> SallyDark.copy(background = Color.Black, surface = Color.Black, surfaceContainer = Color(0xFF08080A))
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> SallyDark
        else -> SallyLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
