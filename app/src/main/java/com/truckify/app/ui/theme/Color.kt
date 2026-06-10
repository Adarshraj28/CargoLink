package com.truckify.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Brand Colors
val PrimaryBlue = Color(0xFF0066FF)
val PrimaryBlueLight = Color(0xFF4D94FF)
val PrimaryBlueDark = Color(0xFF004DBF)

// Dark Theme Palette (SaaS / Premium)
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF151921)
val DarkCard = Color(0xFF1C222B)
val DarkBorder = Color(0xFF2D3643)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextDisabled = Color(0xFF475569)

// Accent / Status Colors
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)
val WarningOrange = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3B82F6)

// Gradients
val BlueGradient = Brush.verticalGradient(
    colors = listOf(PrimaryBlue, PrimaryBlueDark)
)

val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(DarkSurface, DarkBackground)
)

// Compatibility aliases
val DarkBlue = PrimaryBlueDark
val LightBlue = PrimaryBlueLight
val Background = Color(0xFFF8FAFC)
val Beige = Color(0xFFE8D5C4)
val BackgroundWhite = Color.White
val AccentGreen = SuccessGreen
val BackgroundDark = DarkBackground
val CardDark = DarkCard
val TextGray = TextSecondary
