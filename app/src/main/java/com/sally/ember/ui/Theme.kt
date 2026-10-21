package com.sally.ember.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFFFF8A3D), onPrimary = Color(0xFF2B1200),
    secondary = Color(0xFFFFB68A), background = Color(0xFF0F1115), surface = Color(0xFF171A21),
    surfaceVariant = Color(0xFF222733), onSurface = Color(0xFFE7E9EE), onBackground = Color(0xFFE7E9EE)
)

@Composable fun EmberTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
