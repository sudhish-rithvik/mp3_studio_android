package com.mp3studio.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import com.mp3studio.app.ui.theme.MP3StudioColors
import kotlin.math.*

/**
 * Interactive audio waveform canvas composable.
 *
 * Features:
 *  - Smooth anti-aliased bar waveform rendering
 *  - Animated glowing playhead
 *  - Drag-selectable trim region with Apple-red highlight
 *  - Live decibel-driven bar scaling
 *  - Zoom support (via [zoomLevel])
 */
@Composable
fun AudioWaveformCanvas(
    waveformData: FloatArray,
    playbackFraction: Float = 0f,             // 0..1 position
    trimStart: Float = 0f,                    // 0..1
    trimEnd: Float = 1f,                      // 0..1
    isPlaying: Boolean = false,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier,
    onTrimChanged: (start: Float, end: Float) -> Unit = { _, _ -> },
    onSeek: (fraction: Float) -> Unit = {}
) {
    // ── Animated playhead glow pulse ──
    val infiniteTransition = rememberInfiniteTransition(label = "playhead_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_alpha"
    )

    var dragStartTrim by remember { mutableStateOf(false) }
    var dragEndTrim by remember { mutableStateOf(false) }
    var localTrimStart by remember { mutableStateOf(trimStart) }
    var localTrimEnd by remember { mutableStateOf(trimEnd) }

    LaunchedEffect(trimStart, trimEnd) {
        localTrimStart = trimStart
        localTrimEnd = trimEnd
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = offset.x / size.width.toFloat()
                        val handleSize = 0.03f
                        dragStartTrim = abs(fraction - localTrimStart) < handleSize
                        dragEndTrim = abs(fraction - localTrimEnd) < handleSize
                    },
                    onDrag = { change, _ ->
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        when {
                            dragStartTrim -> {
                                localTrimStart = fraction.coerceAtMost(localTrimEnd - 0.01f)
                                onTrimChanged(localTrimStart, localTrimEnd)
                            }
                            dragEndTrim -> {
                                localTrimEnd = fraction.coerceAtLeast(localTrimStart + 0.01f)
                                onTrimChanged(localTrimStart, localTrimEnd)
                            }
                            else -> onSeek(fraction)
                        }
                    },
                    onDragEnd = {
                        dragStartTrim = false
                        dragEndTrim = false
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val barCount = if (waveformData.isNotEmpty()) waveformData.size else 150
        val barWidth = (canvasWidth / barCount) * zoomLevel
        val barGap = barWidth * 0.25f
        val effectiveBarWidth = barWidth - barGap

        // ── Draw background track ──
        drawRect(
            color = MP3StudioColors.Background,
            size = Size(canvasWidth, canvasHeight)
        )

        // ── Draw time grid ──
        val gridColor = Color.White.copy(alpha = 0.05f)
        for (i in 0..10) {
            val x = (canvasWidth / 10f) * i
            drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = 1f)
        }

        // ── Draw waveform bars ──
        if (waveformData.isNotEmpty()) {
            for (i in waveformData.indices) {
                val x = i * barWidth
                if (x > canvasWidth) break

                val amplitude = waveformData[i].coerceIn(0.05f, 1f)
                val barHeight = amplitude * centerY * 0.9f
                val fraction = i / waveformData.size.toFloat()

                val isSelected = fraction in localTrimStart..localTrimEnd
                val isPlayed = fraction < playbackFraction

                val barColor = when {
                    isSelected && isPlayed -> MP3StudioColors.AppleRed
                    isSelected -> Color(0xFF8E2030)
                    isPlayed -> MP3StudioColors.ApplePink.copy(alpha = 0.6f)
                    else -> MP3StudioColors.WaveformInactive
                }

                // Upper bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x + barGap / 2, centerY - barHeight),
                    size = Size(effectiveBarWidth.coerceAtLeast(2f), barHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Mirror lower bar
                drawRoundRect(
                    color = barColor.copy(alpha = barColor.alpha * 0.45f),
                    topLeft = Offset(x + barGap / 2, centerY),
                    size = Size(effectiveBarWidth.coerceAtLeast(2f), barHeight * 0.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        // ── Selection overlay ──
        val selStart = localTrimStart * canvasWidth
        val selEnd = localTrimEnd * canvasWidth
        drawRect(
            color = MP3StudioColors.WaveformSelection,
            topLeft = Offset(selStart, 0f),
            size = Size(selEnd - selStart, canvasHeight)
        )

        // ── Trim handles ──
        val handleColor = MP3StudioColors.AppleRed
        val handleWidth = 3.dp.toPx()
        // Left handle
        drawLine(handleColor, Offset(selStart, 0f), Offset(selStart, canvasHeight), strokeWidth = handleWidth)
        drawCircle(handleColor, radius = 8.dp.toPx() / 2, center = Offset(selStart, canvasHeight / 2))
        // Right handle
        drawLine(handleColor, Offset(selEnd, 0f), Offset(selEnd, canvasHeight), strokeWidth = handleWidth)
        drawCircle(handleColor, radius = 8.dp.toPx() / 2, center = Offset(selEnd, canvasHeight / 2))

        // ── Playhead ──
        val playheadX = playbackFraction * canvasWidth
        // Glow
        drawLine(
            color = MP3StudioColors.Playhead.copy(alpha = glowAlpha * 0.3f),
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, canvasHeight),
            strokeWidth = 10.dp.toPx()
        )
        // Solid line
        drawLine(
            color = MP3StudioColors.Playhead,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, canvasHeight),
            strokeWidth = 2.dp.toPx()
        )
        // Playhead diamond marker
        val diamond = Path().apply {
            moveTo(playheadX, 0f)
            lineTo(playheadX + 6.dp.toPx(), 10.dp.toPx())
            lineTo(playheadX, 18.dp.toPx())
            lineTo(playheadX - 6.dp.toPx(), 10.dp.toPx())
            close()
        }
        drawPath(diamond, color = MP3StudioColors.Playhead)
    }
}

/**
 * Live recording VU meter visualizer — animated bar spectrum reacting to mic amplitude.
 */
@Composable
fun VUMeterVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    accentColor: Color = MP3StudioColors.AppleRed
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vu")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "vu_pulse"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barCount = amplitudes.size.coerceAtLeast(60)
        val barWidth = canvasWidth / barCount
        val centerY = canvasHeight / 2f

        for (i in 0 until barCount) {
            val amp = if (i < amplitudes.size) amplitudes[i] * pulse else 0.05f
            val barH = amp * centerY
            val x = i * barWidth
            val hue = (i.toFloat() / barCount) * 60f  // red to orange spectrum
            val color = accentColor.copy(alpha = 0.8f + amp * 0.2f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barH),
                size = Size(barWidth - 2f, barH * 2),
                cornerRadius = CornerRadius(3f)
            )
        }
    }
}
