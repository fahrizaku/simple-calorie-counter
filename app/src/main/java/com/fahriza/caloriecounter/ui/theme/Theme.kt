package com.fahriza.caloriecounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF388E3C),
    background = Color(0xFFF9FBF9),
    surface = Color.White,
    error = Color(0xFFD32F2F)
)

@Composable
fun CalorieCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
