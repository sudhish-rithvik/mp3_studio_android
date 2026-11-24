package com.mp3studio.app.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors
import kotlinx.coroutines.*
import kotlin.random.Random

@Composable
fun RecorderDubScreen(viewModel: AudioEngineViewModel) {
    val isRecording by viewModel.isRecording.collectAsState()
    var localRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var isDubMode by remember { mutableStateOf(false) }
    var amplitudes by remember { mutableStateOf(FloatArray(60) { 0.05f }) }
    var savedRecordings by remember { mutableStateOf(listOf<String>()) }
    var inputGain by remember { mutableStateOf(0.8f) }
    var monitorEcho by remember { mutableStateOf(false) }

    // Simulate live amplitude updates while recording
    LaunchedEffect(localRecording) {
        while (localRecording) {
            amplitudes = FloatArray(60) {
                if (localRecording) 0.1f + Random.nextFloat() * 0.85f else 0.05f
            }
            recordingSeconds++
            delay(1000L)
        }
    }

    // Animate VU bars continuously
    val infiniteTransition = rememberInfiniteTransition(label = "vu_anim")
    val vuPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "vu"
    )

    // Pulsing record button
    val recScale by animateFloatAsState(
        targetValue = if (localRecording) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "rec_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MP3StudioColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "Recorder & Dub", subtitle = "Studio quality microphone recording")
            Spacer(Modifier.height(20.dp))

            // ── Mode Selector ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isDubMode) "Dub Mode" else "Record Mode", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            if (isDubMode) "Record over a background track" else "Standalone audio recording",
                            color = MP3StudioColors.TextSecondary, fontSize = 13.sp
                        )
                    }
                    CupertinoToggle(
                        checked = isDubMode,
                        onCheckedChange = { isDubMode = it },
                        accentColor = MP3StudioColors.AppleRed,
                        label = ""
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── VU Meter ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Live Input Level", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                VUMeterVisualizer(
                    amplitudes = if (localRecording) amplitudes else FloatArray(60) { 0.02f + it * 0.003f },
                    accentColor = if (localRecording) MP3StudioColors.AppleRed else MP3StudioColors.TextTertiary
                )
                Spacer(Modifier.height(14.dp))
                // Input gain slider
                AppleSlider(
                    value = inputGain, onValueChange = { inputGain = it },
                    valueRange = 0f..2f,
                    label = "Input Gain", valueLabel = "${(inputGain * 100).toInt()}%",
                    accentColor = MP3StudioColors.AppleOrange
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CupertinoToggle(checked = monitorEcho, onCheckedChange = { monitorEcho = it }, label = "Monitor / Echo", accentColor = MP3StudioColors.AppleBlue)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Record Timer ──
            AnimatedVisibility(visible = localRecording) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val min = recordingSeconds / 60
                    val sec = recordingSeconds % 60
                    Text(
                        "%02d:%02d".format(min, sec),
                        color = MP3StudioColors.AppleRed,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MP3StudioColors.AppleRed))
                        Text("Recording...", color = MP3StudioColors.AppleRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Record Button ──
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(recScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (localRecording) listOf(Color(0xFF8B0000), MP3StudioColors.AppleRed)
                            else listOf(MP3StudioColors.AppleRed, MP3StudioColors.ApplePink)
                        )
                    )
                    .clickable {
                        localRecording = !localRecording
                        if (!localRecording && recordingSeconds > 0) {
                            savedRecordings = savedRecordings + "Recording_${savedRecordings.size + 1} (${recordingSeconds}s)"
                            recordingSeconds = 0
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (localRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (localRecording) "Stop" else "Record",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (localRecording) "Tap to stop recording" else "Tap to start recording",
                color = MP3StudioColors.TextSecondary, fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Dub background track picker (visible in dub mode) ──
            AnimatedVisibility(visible = isDubMode) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Background Track", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Load a track to record your voice over", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    ApplePillButton(
                        text = "Browse Background Track",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false,
                        icon = Icons.Rounded.FolderOpen
                    )
                }
            }

            // ── Saved Recordings ──
            if (savedRecordings.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Recordings", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    savedRecordings.forEachIndexed { i, name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(MP3StudioColors.AppleRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Mic, null, tint = MP3StudioColors.AppleRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(name, color = MP3StudioColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            AppleCircleButton(
                                icon = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                onClick = {},
                                size = 34.dp,
                                tint = MP3StudioColors.AppleGreen
                            )
                            Spacer(Modifier.width(6.dp))
                            AppleCircleButton(
                                icon = Icons.Rounded.FileDownload,
                                contentDescription = "Export",
                                onClick = {},
                                size = 34.dp
                            )
                        }
                        if (i < savedRecordings.lastIndex) HorizontalDivider(color = MP3StudioColors.BorderSubtle)
                    }
                }
            }
        }
    }
}
