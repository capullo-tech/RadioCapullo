package tech.capullo.radio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Define Colors (These will match the colors from your earlier `color.kt`).
val primaryGreen = Color(0xFF1DB954) // Spotify Green
val secondaryBlack = Color(0xFF121212) // Dark Background (Spotify/Apple)
val tertiaryOrange = Color(0xFFFF5722) // Hint of Orange Accent

val backgroundLight = Color(0xFFFFFFFF) // White (Apple Background)
val surfaceLight = Color(0xFFF5F5F5) // Light Grey (Apple Surface)

val onPrimaryLight = Color(0xFFFFFFFF) // White text on primary green
val onSecondaryLight = Color(0xFFFFFFFF) // White text on secondary black
val onTertiaryLight = Color(0xFFFFFFFF) // White text on orange

val errorColor = Color(0xFFB00020) // Standard Material error color

// Updated Dark and Light Color Schemes using the custom colors

private val DarkColorScheme = darkColorScheme(
    primary = primaryGreen,
    secondary = secondaryBlack,
    tertiary = tertiaryOrange,
    background = secondaryBlack,
    surface = secondaryBlack,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = errorColor,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = primaryGreen,
    secondary = secondaryBlack,
    tertiary = tertiaryOrange,
    background = backgroundLight,
    surface = surfaceLight,
    onPrimary = onPrimaryLight,
    onSecondary = onSecondaryLight,
    onTertiary = onTertiaryLight,
    onBackground = secondaryBlack,
    onSurface = secondaryBlack,
    error = errorColor,
    onError = Color.White
)

@Composable
fun RadioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,  // Assuming Typography is defined elsewhere
        content = content
    )
}
