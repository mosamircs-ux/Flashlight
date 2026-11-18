package com.luminatorch.flashlight.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object LuminaColors {
    val Obsidian = Color(0xFF0B0D11)
    val SurfaceDark = Color(0xFF131720)
    val SurfaceElevated = Color(0xFF1B212E)
    val SurfaceBorder = Color(0xFF283042)

    val AmberPrimary = Color(0xFFFFB300)
    val AmberBright = Color(0xFFFFE082)
    val AmberDeep = Color(0xFFFF8F00)
    val AmberGlow = Color(0x4DFFB300)

    val CrimsonSos = Color(0xFFFF3B30)
    val CrimsonGlow = Color(0x4DFF3B30)

    val TextPrimary = Color(0xFFF4F6FA)
    val TextSecondary = Color(0xFF8E99AD)
    val TextMuted = Color(0xFF5A6478)
}

private val LuminaDarkColorScheme = darkColorScheme(
    primary = LuminaColors.AmberPrimary,
    onPrimary = Color(0xFF140E00),
    primaryContainer = Color(0xFF3A2800),
    onPrimaryContainer = LuminaColors.AmberBright,
    secondary = LuminaColors.AmberDeep,
    onSecondary = Color(0xFF140E00),
    background = LuminaColors.Obsidian,
    onBackground = LuminaColors.TextPrimary,
    surface = LuminaColors.SurfaceDark,
    onSurface = LuminaColors.TextPrimary,
    surfaceVariant = LuminaColors.SurfaceElevated,
    onSurfaceVariant = LuminaColors.TextSecondary,
    outline = LuminaColors.SurfaceBorder,
    error = LuminaColors.CrimsonSos,
    onError = Color.White,
)

private val LuminaTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.6.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.8.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 1.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuminaDarkColorScheme,
        typography = LuminaTypography,
        content = content
    )
}
