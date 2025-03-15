package tech.capullo.radio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Theme Colors
val primaryLight = Color(0xFF1A73E8) // Ocean blue primary
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD8E8FF)
val onPrimaryContainerLight = Color(0xFF00274C)

val secondaryLight = Color(0xFF0BA396) // Teal secondary
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFB2EEEA)
val onSecondaryContainerLight = Color(0xFF00352F)

val tertiaryLight = Color(0xFF3A6EA5) // Deep sea blue tertiary
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFD6E4FF)
val onTertiaryContainerLight = Color(0xFF001C38)

val errorLight = Color(0xFFBA1A1A)
val surfaceLight = Color(0xFFF2FBFF) // Very light blue surface
val backgroundLight = Color(0xFFF2FBFF)

// Dark Theme Colors
val primaryDark = Color(0xFF89C2FF) // Lighter ocean blue for dark theme
val onPrimaryDark = Color(0xFF003063)
val primaryContainerDark = Color(0xFF00468E)
val onPrimaryContainerDark = Color(0xFFD8E8FF)

val secondaryDark = Color(0xFF86DAD3) // Lighter teal for dark theme
val onSecondaryDark = Color(0xFF00504A)
val secondaryContainerDark = Color(0xFF007067)
val onSecondaryContainerDark = Color(0xFFB2EEEA)

val tertiaryDark = Color(0xFFA8C9FF) // Lighter sea blue for dark theme
val onTertiaryDark = Color(0xFF003061)
val tertiaryContainerDark = Color(0xFF1B4981)
val onTertiaryContainerDark = Color(0xFFD6E4FF)

val errorDark = Color(0xFFFFB4AB)
val surfaceDark = Color(0xFF001E2E) // Deep ocean blue surface
val backgroundDark = Color(0xFF001E2E)

// Material Light Theme Colors
val lightColors = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onPrimaryLight,
    background = backgroundLight,
    onBackground = onPrimaryLight,
    surface = surfaceLight,
    onSurface = onPrimaryLight,
)

// Material Dark Theme Colors
val darkColors = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onPrimaryDark,
    background = backgroundDark,
    onBackground = onPrimaryDark,
    surface = surfaceDark,
    onSurface = onPrimaryDark,
)