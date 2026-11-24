package com.mp3studio.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.audio.TrimRange
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEditorScreen(viewModel: AudioEngineViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val trimRange by viewModel.trimRange.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()
    val pitchShift by viewModel.pitchShift.collectAsState()
    val volumeLevel by viewModel.volumeLevel.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var showFadeDialog by remember { mutableStateOf(false) }
    var showSilenceDialog by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(0) }
    val tools = listOf("Trim", "Speed", "Pitch", "Volume", "Fade", "Denoise")

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.loadAudioFile(it, "Track") }
    }

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
                .padding(top = 16.dp, bottom = 100.dp)
        ) {
            // ── Header ──
            SectionHeader(
                title = "Audio Editor",
                subtitle = currentTrack?.name ?: "No track loaded"
            )
            Spacer(Modifier.height(20.dp))

            // ── Load Track Card ──
            if (currentTrack == null) {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MP3StudioColors.AppleRed,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "Load an audio file to start editing",
                            color = MP3StudioColors.TextSecondary,
                            fontSize = 15.sp
                        )
                        ApplePillButton(
                            text = "Browse Files",
                            onClick = { filePicker.launch("audio/*") },
                            icon = Icons.Rounded.FolderOpen
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Waveform Editor Card ──
            GlassCard {
                // Track info
                currentTrack?.let { track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Album art placeholder
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(MP3StudioColors.GradientPrimary)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.name, color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatDuration(track.duration), color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                        }
                        AppleCircleButton(
                            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            onClick = {
                                if (isPlaying) { viewModel.pause(); isPlaying = false }
                                else { viewModel.play(); isPlaying = true }
                            },
                            size = 44.dp,
                            tint = MP3StudioColors.AppleRed,
                            isActive = isPlaying
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Waveform canvas
                AudioWaveformCanvas(
                    waveformData = waveformData,
                    playbackFraction = if ((currentTrack?.duration ?: 0L) > 0)
                        playbackPosition.toFloat() / currentTrack!!.duration else 0f,
                    trimStart = if ((currentTrack?.duration ?: 0L) > 0)
                        trimRange.startMs.toFloat() / currentTrack!!.duration else 0f,
                    trimEnd = if ((currentTrack?.duration ?: 0L) > 0)
                        trimRange.endMs.toFloat() / currentTrack!!.duration else 1f,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth(),
                    onTrimChanged = { start, end ->
                        val dur = currentTrack?.duration ?: 0L
                        viewModel.setTrimRange((start * dur).toLong(), (end * dur).toLong())
                    },
                    onSeek = { fraction ->
                        viewModel.seekTo(((currentTrack?.duration ?: 0L) * fraction).toLong())
                    }
                )

                // Trim time display
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(trimRange.startMs), color = MP3StudioColors.AppleRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(formatDuration(trimRange.endMs), color = MP3StudioColors.AppleRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(14.dp))
                // Apply Trim button
                ApplePillButton(
                    text = "Apply Trim",
                    onClick = { viewModel.applyTrim() },
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = currentTrack != null
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Tool Selector ──
            AppleSegmentedControl(
                options = tools,
                selectedIndex = selectedTool,
                onSelected = { selectedTool = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Tool-specific card ──
            AnimatedContent(targetState = selectedTool, label = "tool_card",
                transitionSpec = {
                    fadeIn(tween(220)) + slideInHorizontally { it / 4 } togetherWith
                    fadeOut(tween(150)) + slideOutHorizontally { -it / 4 }
                }
            ) { tool ->
                GlassCard {
                    when (tool) {
                        0 -> TrimToolContent()
                        1 -> SpeedToolContent(speedMultiplier) { viewModel.setSpeed(it) }
                        2 -> PitchToolContent(pitchShift) { viewModel.setPitchShift(it) }
                        3 -> VolumeToolContent(volumeLevel) { viewModel.setVolume(it) }
                        4 -> FadeToolContent(
                            onFadeIn = { viewModel.applyFadeIn(it) },
                            onFadeOut = { viewModel.applyFadeOut(it) }
                        )
                        5 -> DenoiseToolContent(
                            onApply = { viewModel.applyDenoise() },
                            onRemoveSilence = { viewModel.removeSilence() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick Action Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ApplePillButton(
                    text = "Load New", onClick = { filePicker.launch("audio/*") },
                    modifier = Modifier.weight(1f), isPrimary = false,
                    icon = Icons.Rounded.FileOpen
                )
                ApplePillButton(
                    text = "Export", onClick = { /* Navigate to export dialog */ },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.FileDownload
                )
            }
        }
    }
}

@Composable private fun TrimToolContent() {
    Text("✂️  Trim & Cut", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Text("Drag the waveform handles above to set your trim range.\nPress 'Apply Trim' to cut the audio to that selection.", color = MP3StudioColors.TextSecondary, fontSize = 14.sp)
}

@Composable private fun SpeedToolContent(speed: Float, onSpeedChange: (Float) -> Unit) {
    val speedLabel = "${"%.2f".format(speed)}x"
    Text("⚡  Speed Control", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(16.dp))
    AppleSlider(
        value = speed, onValueChange = onSpeedChange,
        valueRange = 0.25f..3.0f,
        label = "Playback Speed", valueLabel = speedLabel
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (speed == preset) MP3StudioColors.AppleRed else MP3StudioColors.SurfaceElevated)
                    .clickable { onSpeedChange(preset) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("${preset}x", color = if (speed == preset) Color.White else MP3StudioColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable private fun PitchToolContent(pitchShift: Int, onPitchChange: (Int) -> Unit) {
    Text("🎵  Pitch Shift", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(16.dp))
    Text("${if (pitchShift >= 0) "+" else ""}$pitchShift semitones", color = MP3StudioColors.ApplePurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Slider(
        value = pitchShift.toFloat(),
        onValueChange = { onPitchChange(it.toInt()) },
        valueRange = -12f..12f, steps = 23,
        colors = SliderDefaults.colors(thumbColor = MP3StudioColors.ApplePurple, activeTrackColor = MP3StudioColors.ApplePurple, inactiveTrackColor = MP3StudioColors.SurfaceElevated)
    )
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text("-12", color = MP3StudioColors.TextTertiary, fontSize = 11.sp)
        Text("0", color = MP3StudioColors.TextTertiary, fontSize = 11.sp)
        Text("+12", color = MP3StudioColors.TextTertiary, fontSize = 11.sp)
    }
}

@Composable private fun VolumeToolContent(volume: Float, onVolumeChange: (Float) -> Unit) {
    val volPct = (volume * 100).toInt()
    Text("🔊  Volume & Gain Boost", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(16.dp))
    Text("$volPct%", color = MP3StudioColors.AppleBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    AppleSlider(value = volume, onValueChange = onVolumeChange, valueRange = 0f..3f, label = "Volume (up to 300%)", valueLabel = "$volPct%", accentColor = MP3StudioColors.AppleBlue)
}

@Composable private fun FadeToolContent(onFadeIn: (Long) -> Unit, onFadeOut: (Long) -> Unit) {
    var fadeInMs by remember { mutableStateOf(1000f) }
    var fadeOutMs by remember { mutableStateOf(1000f) }

    Text("📈  Fade In / Fade Out", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(16.dp))
    AppleSlider(value = fadeInMs, onValueChange = { fadeInMs = it }, valueRange = 100f..10000f, label = "Fade In Duration", valueLabel = "${(fadeInMs / 1000).toInt()}s", accentColor = MP3StudioColors.AppleGreen)
    Spacer(Modifier.height(12.dp))
    AppleSlider(value = fadeOutMs, onValueChange = { fadeOutMs = it }, valueRange = 100f..10000f, label = "Fade Out Duration", valueLabel = "${(fadeOutMs / 1000).toInt()}s", accentColor = MP3StudioColors.AppleOrange)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ApplePillButton(text = "Apply Fade In", onClick = { onFadeIn(fadeInMs.toLong()) }, modifier = Modifier.weight(1f), isPrimary = false)
        ApplePillButton(text = "Apply Fade Out", onClick = { onFadeOut(fadeOutMs.toLong()) }, modifier = Modifier.weight(1f))
    }
}

@Composable private fun DenoiseToolContent(onApply: () -> Unit, onRemoveSilence: () -> Unit) {
    Text("🧹  Denoise & Silence Removal", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(16.dp))
    Text("Apply noise gate and high-pass filter to clean up background hiss, room noise, and microphone static.", color = MP3StudioColors.TextSecondary, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    ApplePillButton(text = "Apply Denoise", onClick = onApply, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.CleaningServices)
    Spacer(Modifier.height(10.dp))
    ApplePillButton(text = "Remove Silence", onClick = onRemoveSilence, modifier = Modifier.fillMaxWidth(), isPrimary = false, icon = VolumeOff)
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val millis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, millis)
}
