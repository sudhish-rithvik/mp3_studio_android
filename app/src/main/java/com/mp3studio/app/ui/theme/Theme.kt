package com.mp3studio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// MP3 Studio · Apple Glassmorphic Color Palette
// ─────────────────────────────────────────────

object MP3StudioColors {
    // Backgrounds
    val Background        = Color(0xFF0D0D0C)
    val Surface           = Color(0xFF1C1C1E)
    val SurfaceElevated   = Color(0xFF2C2C2E)
    val SurfaceGlass      = Color(0x26FFFFFF)   // white 15%

    // Borders
    val BorderGlass       = Color(0x33FFFFFF)   // white 20%
    val BorderSubtle      = Color(0x1AFFFFFF)   // white 10%

    // Apple Signature Accents
    val AppleRed          = Color(0xFFFA243C)
    val ApplePink         = Color(0xFFFF2D55)
    val ApplePurple       = Color(0xFFBF5AF2)
    val AppleBlue         = Color(0xFF0A84FF)
    val AppleGreen        = Color(0xFF30D158)
    val AppleOrange       = Color(0xFFFF9F0A)

    // Gradients (primary to secondary)
    val GradientPrimary   = listOf(Color(0xFFFA243C), Color(0xFFFF2D55), Color(0xFFBF5AF2))
    val GradientBlue      = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
    val GradientGreen     = listOf(Color(0xFF30D158), Color(0xFF34C759))

    // Text
    val TextPrimary       = Color(0xFFFFFFFF)
    val TextSecondary     = Color(0xFF8E8E93)
    val TextTertiary      = Color(0xFF48484A)

    // Waveform
    val WaveformActive    = Color(0xFFFA243C)
    val WaveformInactive  = Color(0xFF3A3A3C)
    val WaveformSelection = Color(0x55FA243C)
    val Playhead          = Color(0xFFFFFFFF)

    // EQ Bands
    val EQBand1           = Color(0xFFFA243C)
    val EQBand2           = Color(0xFFFF9F0A)
    val EQBand3           = Color(0xFF30D158)
    val EQBand4           = Color(0xFF0A84FF)
    val EQBand5           = Color(0xFFBF5AF2)
}

// ─────────────────────────────────────────────
// Material3 Dark Color Scheme mapped to Apple palette
// ─────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = MP3StudioColors.AppleRed,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF4A0010),
    secondary        = MP3StudioColors.ApplePink,
    onSecondary      = Color.White,
    tertiary         = MP3StudioColors.ApplePurple,
    background       = MP3StudioColors.Background,
    surface          = MP3StudioColors.Surface,
    onBackground     = MP3StudioColors.TextPrimary,
    onSurface        = MP3StudioColors.TextPrimary,
    onSurfaceVariant = MP3StudioColors.TextSecondary,
    outline          = MP3StudioColors.BorderGlass,
    surfaceVariant   = MP3StudioColors.SurfaceElevated,
    error            = Color(0xFFFF453A),
    onError          = Color.White
)

// ─────────────────────────────────────────────
// Typography – Mimics Apple SF Pro scales
// ─────────────────────────────────────────────
val MP3StudioTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 34.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.1).sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 17.sp, letterSpacing = 0.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 15.sp, letterSpacing = 0.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 13.sp, letterSpacing = 0.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 15.sp, letterSpacing = 0.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 11.sp, letterSpacing = 0.5.sp),
)

// ─────────────────────────────────────────────
// App Theme
// ─────────────────────────────────────────────
@Composable
fun MP3StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = MP3StudioTypography,
        content     = content
    )
}
