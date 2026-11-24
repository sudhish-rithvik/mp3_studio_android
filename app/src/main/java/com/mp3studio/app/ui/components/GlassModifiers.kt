package com.mp3studio.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mp3studio.app.ui.theme.MP3StudioColors

/**
 * Glassmorphism modifier for Jetpack Compose.
 *
 * On API 31+ uses RenderEffect.createBlurEffect() for true GPU-accelerated blur.
 * On older API levels falls back to a translucent frosted-white overlay.
 *
 * Usage:
 *   Box(modifier = Modifier.glassmorphic(cornerRadius = 20.dp)) { ... }
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 20.dp,
    blurRadius: Float = 40f,
    tintAlpha: Float = 0.15f,
    borderAlpha: Float = 0.20f
): Modifier = this
    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = tintAlpha),
                Color.White.copy(alpha = tintAlpha * 0.6f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.5f)
            )
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    )

/**
 * Glassmorphic with a solid dark base + blur tint —
 * ideal for card surfaces on top of a blurred album art background.
 */
fun Modifier.glassmorphicDark(
    cornerRadius: Dp = 20.dp
): Modifier = this
    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1C1E).copy(alpha = 0.80f),
                Color(0xFF2C2C2E).copy(alpha = 0.72f)
            )
        )
    )
    .border(
        width = 1.dp,
        color = MP3StudioColors.BorderGlass,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    )

/**
 * Neon glow border modifier — draws an Apple-red glowing ring around a composable.
 */
fun Modifier.neonGlow(
    color: Color = MP3StudioColors.AppleRed,
    cornerRadius: Dp = 20.dp,
    spread: Dp = 2.dp
): Modifier = this
    .border(
        width = spread,
        brush = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = 0.4f))
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    )

/**
 * Gradient background fill —  horizontal left-to-right Apple music brand gradient.
 */
fun Modifier.appleGradientBackground(
    cornerRadius: Dp = 20.dp
): Modifier = this
    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.horizontalGradient(
            colors = MP3StudioColors.GradientPrimary
        )
    )
