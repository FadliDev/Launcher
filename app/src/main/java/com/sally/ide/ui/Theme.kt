package com.sally.ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sally.ide.data.ThemeMode

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    else -> true
}

/**
 * Skema warna MD3 statis beraksen BIRU (seed ~#0B57D0). Seluruh token sengaja biru: primary, secondary, tertiary,
 * container, dan netral bernuansa biru. Warna dinamis (Material You) dihapus supaya aksen selalu biru.
 * Token latar editor (assets/textmate/themes/ (dark, amoled, light)) memakai nilai surface yang sama.
 */
private val LightBlue = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF), onPrimaryContainer = Color(0xFF001A41),
    inversePrimary = Color(0xFFADC6FF),
    secondary = Color(0xFF565F71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF00629D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFE5FF), onTertiaryContainer = Color(0xFF001D33),
    background = Color(0xFFF9F9FF), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC), onSurfaceVariant = Color(0xFF44474E),
    surfaceTint = Color(0xFF0B57D0),
    inverseSurface = Color(0xFF2E3036), inverseOnSurface = Color(0xFFF0F0F7),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C6D0),
    surfaceBright = Color(0xFFF9F9FF), surfaceDim = Color(0xFFD9D9E0),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF4), surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
)

private val DarkBlue = darkColorScheme(
    primary = Color(0xFFADC6FF), onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494), onPrimaryContainer = Color(0xFFD8E2FF),
    inversePrimary = Color(0xFF0B57D0),
    secondary = Color(0xFFBEC6DC), onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759), onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFF99CBFF), onTertiary = Color(0xFF003354),
    tertiaryContainer = Color(0xFF004B76), onTertiaryContainer = Color(0xFFCFE5FF),
    background = Color(0xFF111318), onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318), onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E), onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceTint = Color(0xFFADC6FF),
    inverseSurface = Color(0xFFE2E2E9), inverseOnSurface = Color(0xFF2E3036),
    outline = Color(0xFF8E9099), outlineVariant = Color(0xFF44474E),
    surfaceBright = Color(0xFF37393E), surfaceDim = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF0C0E13), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

@Composable
fun SallyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    var scheme: ColorScheme = if (isDark(mode)) DarkBlue else LightBlue
    if (mode == ThemeMode.AMOLED) {
        scheme = scheme.copy(
            background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF0A0C10), surfaceContainerHigh = Color(0xFF14171C),
            surfaceContainerHighest = Color(0xFF1D2025),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
