package tech.capullo.radio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val primaryBlack = Color(0xFF121212) // Deep black (Spotify / Apple Dark Mode)
val secondaryOrange = Color(0xFFFF5722) // Orange accent

val backgroundLight = Color(0xFFFFFFFF) // White (Apple background)
val surfaceLight = Color(0xFFF5F5F5) // Light grey (Apple surface)

val onPrimaryLight = Color(0xFFFFFFFF)
val onSecondaryLight = Color(0xFFFFFFFF)

val errorColor = Color(0xFFB00020)

// Material Light Theme Colors
val lightColors = lightColorScheme(
    primary = primaryBlack,
    onPrimary = onPrimaryLight,
    secondary = secondaryOrange,
    onSecondary = onSecondaryLight,
    background = backgroundLight,
    onBackground = primaryBlack,
    surface = surfaceLight,
    onSurface = secondaryOrange,
    error = errorColor,
    onError = Color.White
)

// Material Dark Theme Colors
val darkColors = darkColorScheme(
    primary = primaryBlack,
    onPrimary = onPrimaryLight,
    secondary = secondaryOrange,
    onSecondary = onSecondaryLight,
    background = backgroundLight,
    onBackground = primaryBlack,
    surface = surfaceLight,
    onSurface = secondaryOrange,
    error = errorColor,
    onError = Color.White
)
