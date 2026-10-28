package com.cict.tirukkural.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cict.tirukkural.data.Paal

/** Navy taken from the Thiruvalluvar artwork; used wherever the art sits on screen. */
val BrandNight = Color(0xFF0F1225)
val BrandNightDeep = Color(0xFF080A16)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9B6FF),
    onPrimary = Color(0xFF2B1868),
    primaryContainer = Color(0xFF3F2C8A),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF9DB7FF),
    onSecondary = Color(0xFF0E2A6B),
    secondaryContainer = Color(0xFF243D82),
    onSecondaryContainer = Color(0xFFDCE4FF),
    tertiary = Color(0xFFF2C38B),
    onTertiary = Color(0xFF472A00),
    background = Color(0xFF0B0E1C),
    onBackground = Color(0xFFE6E4F2),
    surface = Color(0xFF0B0E1C),
    onSurface = Color(0xFFE6E4F2),
    surfaceVariant = Color(0xFF262A40),
    onSurfaceVariant = Color(0xFFB9B8CC),
    surfaceContainerLowest = Color(0xFF080A15),
    surfaceContainerLow = Color(0xFF11142A),
    surfaceContainer = Color(0xFF151933),
    surfaceContainerHigh = Color(0xFF1C2140),
    surfaceContainerHighest = Color(0xFF242949),
    outline = Color(0xFF4A4E68),
    outlineVariant = Color(0xFF2E3250),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1B0A5E),
    secondary = Color(0xFF2D50C8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4FF),
    onSecondaryContainer = Color(0xFF00174E),
    tertiary = Color(0xFF8C5A14),
    onTertiary = Color.White,
    background = Color(0xFFF8F6FD),
    onBackground = Color(0xFF1A1B26),
    surface = Color(0xFFF8F6FD),
    onSurface = Color(0xFF1A1B26),
    surfaceVariant = Color(0xFFE4E1F0),
    onSurfaceVariant = Color(0xFF4A4A5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F0FA),
    surfaceContainer = Color(0xFFEDEAF6),
    surfaceContainerHigh = Color(0xFFE7E4F1),
    surfaceContainerHighest = Color(0xFFE1DEEC),
    outline = Color(0xFF7A7A8E),
    outlineVariant = Color(0xFFCBC8D8),
)

private val BaseTypography = Typography()

// Tamil glyphs carry tall vowel signs; slightly looser line heights keep them from clipping.
private val AppTypography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold, lineHeight = 46.sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(lineHeight = 22.sp),
    bodySmall = BaseTypography.bodySmall.copy(lineHeight = 18.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = BaseTypography.labelSmall,
)

@Composable
fun TirukkuralTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** Each பால் has its own accent so the listener always knows where they are in the book. */
val Paal.gradientColors: List<Color>
    get() = when (this) {
        Paal.ARAM -> listOf(Color(0xFF6D5BFF), Color(0xFFD091C5))
        Paal.PORUL -> listOf(Color(0xFF1E4FD8), Color(0xFF3FC1E8))
        Paal.KAMAM -> listOf(Color(0xFFE2557A), Color(0xFFF6A35B))
    }

val Paal.brush: Brush get() = Brush.linearGradient(gradientColors)

val Paal.accent: Color get() = gradientColors.last()

val NumberStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.sp)
