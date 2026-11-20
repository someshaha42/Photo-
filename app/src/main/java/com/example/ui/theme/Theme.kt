package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PixelForgeDarkColorScheme = darkColorScheme(
    primary = StudioAzure,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF19325A),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = StudioTeal,
    onSecondary = Color(0xFF00201B),
    secondaryContainer = Color(0xFF0D3D36),
    onSecondaryContainer = Color(0xFFA6F7EA),
    tertiary = StudioAmber,
    onTertiary = Color(0xFF281800),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    error = StudioCoral,
    onError = Color.White
)

private val PixelForgeLightColorScheme = lightColorScheme(
    primary = StudioAzureDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFF),
    onPrimaryContainer = Color(0xFF0B254B),
    secondary = Color(0xFF008975),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBF7EE),
    onSecondaryContainer = Color(0xFF002B24),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerHigh = LightSurfaceElevated,
    outline = LightOutline,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun PixelForgeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PixelForgeDarkColorScheme else PixelForgeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelForgeTypography,
        content = content
    )
}
