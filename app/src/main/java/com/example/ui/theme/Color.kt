package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Classic Retro Arcade Neon Palette
val NeonCyan = Color(0xFF00F5D4)
val NeonCyanDark = Color(0xFF006658)
val NeonPink = Color(0xFFFF007F)
val NeonPinkDark = Color(0xFF660033)
val NeonYellow = Color(0xFFFFE600)
val NeonGreen = Color(0xFF39FF14)
val NeonPurple = Color(0xFF9D4EDD)
val NeonOrange = Color(0xFFFF6D00)
val NeonBlue = Color(0xFF38BDF8)
val ArcadeScoreGold = Color(0xFFFBBF24)

// Dark Arcade Surfaces & Backgrounds
val ArcadeBackground = Color(0xFF0C0A19)
val ArcadeSurface = Color(0xFF141226)
val ArcadeSurfaceVariant = Color(0xFF1E1B2E)
val ArcadeSurfaceElevated = Color(0xFF2A2744)
val ArcadeBorder = Color(0xFF36325C)

// Semantic Text Colors
val ArcadeTextPrimary = Color(0xFFFFFFFF)
val ArcadeTextSecondary = Color(0xFF94A3B8)
val ArcadeTextMuted = Color(0xFF64748B)

// Default template compatibility colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class ArcadeColors(
    val neonCyan: Color = NeonCyan,
    val neonPink: Color = NeonPink,
    val neonYellow: Color = NeonYellow,
    val neonGreen: Color = NeonGreen,
    val neonPurple: Color = NeonPurple,
    val neonOrange: Color = NeonOrange,
    val neonBlue: Color = NeonBlue,
    val scoreGold: Color = ArcadeScoreGold,
    val surfaceCard: Color = ArcadeSurfaceVariant,
    val surfaceElevated: Color = ArcadeSurfaceElevated,
    val border: Color = ArcadeBorder
)

val LocalArcadeColors = staticCompositionLocalOf { ArcadeColors() }
