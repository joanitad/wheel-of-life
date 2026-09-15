package com.joanita.wheeloflife.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * A muted, earthy take on a colorblind-safe palette: hues are spaced the
 * way Okabe–Ito spaces them (so deutan/protan/tritan viewers can tell
 * them apart) but desaturated and luminance-staggered so it's easy on the
 * eyes and still prints legibly in grayscale. Segments are always labeled
 * with text, so color is never the only channel of information.
 */
val WheelPalette = listOf(
    Color(0xFFE3B04B), // soft amber
    Color(0xFF89B7D5), // dusty sky blue
    Color(0xFF7FA88F), // sage green
    Color(0xFFF0E0B8), // pale sand
    Color(0xFF4E6E9E), // slate blue
    Color(0xFFC77B57), // terracotta
    Color(0xFFB48EAD), // dusty mauve
    Color(0xFF9A948B)  // warm grey
)

/** Black or white text, whichever contrasts better with the segment color. */
fun onWheelColor(c: Color): Color = if (c.luminance() > 0.4f) Color(0xFF1A1A1A) else Color.White

private val Light = lightColorScheme(
    primary = Color(0xFF4E6E9E),
    secondary = Color(0xFF7FA88F),
    tertiary = Color(0xFFC77B57),
    background = Color(0xFFFAF6EE),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFF0EAE0)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF89B7D5),
    secondary = Color(0xFF7FA88F),
    tertiary = Color(0xFFE3B04B),
    background = Color(0xFF191C1E),
    surface = Color(0xFF22262A)
)

@Composable
fun WheelOfLifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
