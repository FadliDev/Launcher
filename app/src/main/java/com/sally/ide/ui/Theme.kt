package com.sally.ide.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sally.ide.data.ThemeMode

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    else -> true
}

@Composable
fun SallyTheme(mode: ThemeMode, dynamic: Boolean, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val ctx = LocalContext.current
    var scheme: ColorScheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFFB4C5FF), secondary = Color(0xFFC1C6DD), tertiary = Color(0xFFE2BADB))
        else -> lightColorScheme(primary = Color(0xFF475D92), secondary = Color(0xFF595E72), tertiary = Color(0xFF75546F))
    }
    if (mode == ThemeMode.AMOLED) {
        scheme = scheme.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF0A0A0A), surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1C1C1C),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
