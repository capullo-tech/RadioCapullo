package tech.capullo.radio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with, using the default system font family
val Typography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.ExtraBold,  // Super bold for large headers
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // Title
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.Bold,  // Bold for prominent titles
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.SemiBold,  // SemiBold for secondary titles
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.Normal,  // Regular body text
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.Medium,  // Slightly heavier body text
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Label
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,  // Use the default system font
        fontWeight = FontWeight.Medium,  // Medium weight for small labels
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
