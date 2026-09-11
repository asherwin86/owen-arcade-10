package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Dedicated Retro Arcade Dark Color Scheme
private val ArcadeDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF004D43),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = Color(0xFF490022),
    secondaryContainer = Color(0xFF650033),
    onSecondaryContainer = Color(0xFFFFD8E4),
    tertiary = NeonYellow,
    onTertiary = Color(0xFF383000),
    tertiaryContainer = Color(0xFF504600),
    onTertiaryContainer = NeonYellow,
    background = ArcadeBackground,
    onBackground = ArcadeTextPrimary,
    surface = ArcadeSurface,
    onSurface = ArcadeTextPrimary,
    surfaceVariant = ArcadeSurfaceVariant,
    onSurfaceVariant = ArcadeTextSecondary,
    outline = ArcadeBorder,
    outlineVariant = Color(0xFF26223E),
    error = Color(0xFFEF4444),
    onError = Color(0xFF450A0A)
)

private val ArcadeLightColorScheme = lightColorScheme(
    primary = NeonCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF70F6DF),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = NeonPinkDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E4),
    onSecondaryContainer = Color(0xFF3D001B),
    tertiary = Color(0xFF856E00),
    onTertiary = Color.White,
    background = Color(0xFFF3F1FA),
    onBackground = Color(0xFF19162A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF19162A),
    surfaceVariant = Color(0xFFE6E2F3),
    onSurfaceVariant = Color(0xFF49455B),
    outline = Color(0xFF7A758E)
)

@Composable
fun Arcade10Theme(
    darkTheme: Boolean = true, // Retro Arcade aesthetic defaults to dark canvas
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ArcadeDarkColorScheme else ArcadeLightColorScheme
    val arcadeColors = ArcadeColors()

    CompositionLocalProvider(
        LocalArcadeColors provides arcadeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Global accessor for custom arcade tokens
object ArcadeTheme {
    val colors: ArcadeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalArcadeColors.current

    val textStyles: ArcadeTextStyles
        get() = ArcadeTextStyles
}

// Backward compatibility alias for template references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    Arcade10Theme(darkTheme = true, content = content)
}
