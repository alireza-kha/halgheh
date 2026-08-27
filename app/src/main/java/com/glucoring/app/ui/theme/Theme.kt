package com.glucoring.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlucoRingColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF00695C),
    error = Color(0xFFC62828),
)

@Composable
fun GlucoRingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GlucoRingColors, content = content)
}
