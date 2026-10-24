package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HighDensityBlue,
    onPrimary = Color(0xFF1E293B), // High contrast slate text on #EFBF04
    primaryContainer = HighDensityBlueLight,
    onPrimaryContainer = HighDensityBlueDark,
    secondary = HighDensityBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Blue100,
    onSecondaryContainer = HighDensityBlueDark,
    tertiary = Teal600,
    onTertiary = Color.White,
    tertiaryContainer = Teal50,
    onTertiaryContainer = Color(0xFF134E4A),
    background = HighDensityBg,
    onBackground = HighDensityText,
    surface = Color.White,
    onSurface = HighDensityText,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose50,
    onErrorContainer = Color(0xFF881337)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Blue900,
    primaryContainer = Blue800,
    onPrimaryContainer = Blue50,
    secondary = Blue100,
    onSecondary = Blue900,
    secondaryContainer = Blue700,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF134E4A),
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    error = Color(0xFFFDA4AF),
    onError = Color(0xFF881337)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Keep high density light theme consistent across all device modes
    dynamicColor: Boolean = false, // Keep consistent professional corporate blue
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
