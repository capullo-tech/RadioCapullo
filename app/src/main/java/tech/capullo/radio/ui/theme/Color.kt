package tech.capullo.radio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val primaryGreen = Color(0xFF81C784) // Verde pastel
val secondaryOrange = Color(0xFFFF7043) // Orange pastel

val backgroundLight = Color(0xFFFFFFFF) // White
val surfaceLight = Color(0xFFF5F5F5) // Light grey

val onPrimaryLight = Color(0xFFFFFFFF)
val onSecondaryLight = Color(0xFFFFFFFF)

val errorColor = Color(0xFFB00020)

// Material Light Theme Colors
val lightColors = lightColorScheme(
    primary = primaryGreen,
    onPrimary = onPrimaryLight,
    secondary = secondaryOrange,
    onSecondary = onSecondaryLight,
    background = backgroundLight,
    onBackground = primaryGreen,
    surface = surfaceLight,
    onSurface = secondaryOrange,
    error = errorColor,
    onError = Color.White,
)

// Material Dark Theme Colors
val darkColors = darkColorScheme(
    primary = primaryGreen,
    onPrimary = onPrimaryLight,
    secondary = secondaryOrange,
    onSecondary = onSecondaryLight,
    background = backgroundLight,
    onBackground = primaryGreen,
    surface = surfaceLight,
    onSurface = secondaryOrange,
    error = errorColor,
    onError = Color.White,
)
