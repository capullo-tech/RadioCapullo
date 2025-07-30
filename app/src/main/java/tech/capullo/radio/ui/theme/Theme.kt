package tech.capullo.radio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
     */
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    secondary = GreenSecondaryDark,
    secondaryContainer = GreenSecondaryContainerDark,
    onSecondaryContainer = GreenOnSecondaryContainerDark,
    tertiary = GreenTertiaryDark,
)

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    secondary = GreenSecondaryLight,
    secondaryContainer = GreenSecondaryContainerLight,
    onSecondaryContainer = GreenOnSecondaryContainerLight,
    tertiary = GreenTertiaryLight,
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    secondary = OrangeSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OrangeOnSecondaryContainerDark,
    tertiary = OrangeTertiaryDark,
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    onPrimary = OrangeOnPrimaryLight,
    secondary = OrangeSecondaryLight,
    secondaryContainer = OrangeSecondaryContainerLight,
    onSecondaryContainer = OrangeOnSecondaryContainerLight,
    tertiary = OrangeTertiaryLight,
)

enum class SchemeChoice(val darkColorScheme: ColorScheme, val lightColorScheme: ColorScheme) {
    GREEN(GreenDarkColorScheme, GreenLightColorScheme),
    ORANGE(OrangeDarkColorScheme, OrangeLightColorScheme),
    DEFAULT(DarkColorScheme, LightColorScheme),
}

@Composable
fun RadioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    schemeChoice: SchemeChoice = SchemeChoice.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> schemeChoice.darkColorScheme
        else -> schemeChoice.lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
