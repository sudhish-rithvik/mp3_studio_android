package com.mp3studio.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.audio.AudioTrack
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors

@Composable
fun MultiTrackMixerScreen(viewModel: AudioEngineViewModel) {
    val mixerTracks by viewModel.mixerTracks.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MP3StudioColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp)
        ) {
            SectionHeader(title = "Multi-Track Mixer", subtitle = "${mixerTracks.size} tracks loaded")
            Spacer(Modifier.height(20.dp))

            // ── Add Track Button ──
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Mix Studio", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Layer multiple tracks together", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                    }
                    AppleCircleButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add Track",
                        onClick = {
                            val demoTrack = AudioTrack(
                                id = System.currentTimeMillis().toString(),
                                name = "Track ${mixerTracks.size + 1}",
                                uri = null,
                                duration = 180000L
                            )
                            viewModel.addMixerTrack(demoTrack)
                        },
                        tint = MP3StudioColors.AppleRed,
                        isActive = true
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tracks ──
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(mixerTracks) { index, track ->
                    MixerTrackRow(
                        track = track,
                        trackIndex = index,
                        onUpdate = { viewModel.updateMixerTrack(it) },
                        onRemove = { viewModel.removeMixerTrack(track.id) }
                    )
                }
                if (mixerTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.LibraryMusic, null, tint = MP3StudioColors.TextTertiary, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Tap + to add your first track", color = MP3StudioColors.TextTertiary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Mixdown Button ──
            ApplePillButton(
                text = "Mix Down to Single Track",
                onClick = { viewModel.mixDown() },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Merge,
                isPrimary = mixerTracks.size >= 2
            )
        }
    }
}

@Composable
private fun MixerTrackRow(
    track: AudioTrack,
    trackIndex: Int,
    onUpdate: (AudioTrack) -> Unit,
    onRemove: () -> Unit
) {
    var volume by remember(track.volume) { mutableStateOf(track.volume) }
    var muted by remember(track.muted) { mutableStateOf(track.muted) }
    var soloed by remember(track.soloed) { mutableStateOf(track.soloed) }
    var expanded by remember { mutableStateOf(false) }

    // Track color from index
    val trackColors = listOf(
        MP3StudioColors.AppleRed, MP3StudioColors.AppleBlue, MP3StudioColors.ApplePurple,
        MP3StudioColors.AppleGreen, MP3StudioColors.AppleOrange, MP3StudioColors.ApplePink
    )
    val trackColor = trackColors[trackIndex % trackColors.size]

    GlassCard(cornerRadius = 16.dp) {
        // ── Track header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color indicator
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(trackColor.copy(alpha = 0.2f))
                    .border(1.dp, trackColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${trackIndex + 1}", color = trackColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.name, color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (track.uri != null) "Loaded" else "Empty", color = MP3StudioColors.TextTertiary, fontSize = 11.sp)
            }
            // Mute
            AppleCircleButton(
                icon = if (muted) VolumeOff else VolumeUp,
                contentDescription = "Mute",
                onClick = { muted = !muted; onUpdate(track.copy(muted = muted)) },
                size = 36.dp, tint = MP3StudioColors.AppleOrange, isActive = muted
            )
            Spacer(Modifier.width(8.dp))
            // Solo
            AppleCircleButton(
                icon = Icons.Rounded.StarOutline,
                contentDescription = "Solo",
                onClick = { soloed = !soloed; onUpdate(track.copy(soloed = soloed)) },
                size = 36.dp, tint = MP3StudioColors.AppleBlue, isActive = soloed
            )
            Spacer(Modifier.width(8.dp))
            // Expand
            AppleCircleButton(
                icon = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = "Expand",
                onClick = { expanded = !expanded },
                size = 36.dp
            )
            Spacer(Modifier.width(4.dp))
            // Remove
            AppleCircleButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "Remove",
                onClick = onRemove,
                size = 36.dp, tint = MP3StudioColors.AppleRed
            )
        }

        // ── Volume fader ──
        Spacer(Modifier.height(12.dp))
        AppleSlider(
            value = volume,
            onValueChange = { volume = it; onUpdate(track.copy(volume = it)) },
            valueRange = 0f..1.5f,
            label = "Volume",
            valueLabel = "${(volume * 100).toInt()}%",
            accentColor = if (muted) MP3StudioColors.TextTertiary else trackColor
        )

        // ── Expanded controls ──
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MP3StudioColors.BorderSubtle)
                Spacer(Modifier.height(12.dp))
                var offset by remember { mutableStateOf(0f) }
                AppleSlider(
                    value = offset, onValueChange = { offset = it },
                    valueRange = 0f..60000f,
                    label = "Start Offset",
                    valueLabel = "${(offset / 1000).toInt()}s",
                    accentColor = trackColor
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApplePillButton(text = "Load File", onClick = {}, modifier = Modifier.weight(1f), isPrimary = false, icon = Icons.Rounded.FileOpen)
                    ApplePillButton(text = "Record Dub", onClick = {}, modifier = Modifier.weight(1f), icon = Icons.Rounded.Mic)
                }
            }
        }
    }
}
