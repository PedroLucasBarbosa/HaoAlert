package com.haoalert.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta Hao Alert: grafite quase preto com um único destaque vermelho.
val HaoBackground = Color(0xFF0F0B0C)
val HaoSurface = Color(0xFF1A1415)
val HaoSurfaceHigh = Color(0xFF251D1E)
val HaoOutline = Color(0xFF3A2E30)
val HaoAccent = Color(0xFFE5383B)
val HaoAccentSoft = Color(0xFF3A1417)
val HaoOnBackground = Color(0xFFF3EDEE)
val HaoMuted = Color(0xFFA09396)
val HaoRose = Color(0xFFFFB4B0)

private val DarkColors = darkColorScheme(
    primary = HaoAccent,
    onPrimary = Color.White,
    primaryContainer = HaoAccentSoft,
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = HaoRose,
    onSecondary = Color(0xFF3A0A0C),
    background = HaoBackground,
    onBackground = HaoOnBackground,
    surface = HaoSurface,
    onSurface = HaoOnBackground,
    surfaceVariant = HaoSurfaceHigh,
    onSurfaceVariant = HaoMuted,
    surfaceContainer = HaoSurface,
    surfaceContainerHigh = HaoSurfaceHigh,
    surfaceContainerHighest = HaoSurfaceHigh,
    outline = HaoOutline,
    outlineVariant = HaoOutline,
    error = Color(0xFFFF7A7A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF8E3A3C),
    background = Color(0xFFFFFBFB),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFF1F0),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF6DEDD),
    onSurfaceVariant = Color(0xFF6F5A5C),
    surfaceContainer = Color(0xFFFFF1F0),
    surfaceContainerHigh = Color(0xFFF6DEDD),
    surfaceContainerHighest = Color(0xFFF6DEDD),
    outline = Color(0xFFE2C4C3),
    outlineVariant = Color(0xFFE2C4C3),
)

private val HaoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HaoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = HaoShapes,
        content = content,
    )
}
