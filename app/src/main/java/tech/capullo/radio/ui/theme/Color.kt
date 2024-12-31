import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val primaryGreen = Color(0xFF1DB954) // Spotify Green
val secondaryBlack = Color(0xFF121212) // Deep black (Spotify / Apple Dark Mode)
val tertiaryOrange = Color(0xFFFF5722) // Orange accent

val backgroundLight = Color(0xFFFFFFFF) // White (Apple background)
val surfaceLight = Color(0xFFF5F5F5) // Light grey (Apple surface)

val onPrimaryLight = Color(0xFFFFFFFF) // White text on primary green
val onSecondaryLight = Color(0xFFFFFFFF) // White text on secondary black
val onTertiaryLight = Color(0xFFFFFFFF) // White text on orange

val errorColor = Color(0xFFB00020) // Standard Material error color

// Material Light Theme Colors
val lightColors = lightColorScheme(
    primary = primaryGreen,
    onPrimary = onPrimaryLight,
    secondary = secondaryBlack,
    onSecondary = onSecondaryLight,
    tertiary = tertiaryOrange,
    onTertiary = onTertiaryLight,
    background = backgroundLight,
    onBackground = secondaryBlack,
    surface = surfaceLight,
    onSurface = secondaryBlack,
    error = errorColor,
    onError = Color.White
)

// Material Dark Theme Colors
val darkColors = darkColorScheme(
    primary = primaryGreen,
    onPrimary = Color.Black,
    secondary = secondaryBlack,
    onSecondary = Color.White,
    tertiary = tertiaryOrange,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    error = errorColor,
    onError = Color.Black
)
