package com.mindgate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64FFDA),
    onPrimary = Color(0xFF050D1A),
    secondary = Color(0xFF00B4D8),
    background = Color(0xFF050D1A),
    surface = Color(0xFF0D2040),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    error = Color(0xFFFF4081)
)

@Composable
fun MindGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
