package com.mp3studio.app.ui.screens

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(viewModel: AudioEngineViewModel) {
    val eqBands by viewModel.eqBands.collectAsState()
    val volumeLevel by viewModel.volumeLevel.collectAsState()

    var bass by remember(eqBands.bass) { mutableStateOf(eqBands.bass) }
    var lowMid by remember(eqBands.lowMid) { mutableStateOf(eqBands.lowMid) }
    var mid by remember(eqBands.mid) { mutableStateOf(eqBands.mid) }
    var highMid by remember(eqBands.highMid) { mutableStateOf(eqBands.highMid) }
    var treble by remember(eqBands.treble) { mutableStateOf(eqBands.treble) }
    var volume by remember(volumeLevel) { mutableStateOf(volumeLevel) }
    var selectedPreset by remember { mutableStateOf(-1) }
    var selectedFormatIdx by remember { mutableStateOf(0) }
    val formats = listOf("MP3", "WAV", "AAC", "OGG", "FLAC")

    val presets = listOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f),
        "Bass Boost" to listOf(0.75f, 0.4f, 0f, -0.1f, -0.1f),
        "Treble" to listOf(-0.1f, 0f, 0f, 0.4f, 0.75f),
        "Rock" to listOf(0.5f, 0.2f, -0.1f, 0.2f, 0.4f),
        "Pop" to listOf(-0.1f, 0.2f, 0.3f, 0.2f, -0.1f),
        "Jazz" to listOf(0.3f, 0.1f, -0.1f, 0.1f, 0.2f),
        "Vocal" to listOf(-0.2f, 0f, 0.5f, 0.3f, 0f),
        "Electronic" to listOf(0.6f, 0.3f, -0.2f, 0.3f, 0.6f),
        "Classical" to listOf(0f, 0f, 0f, 0.2f, 0.4f)
    )

    fun applyPreset(idx: Int) {
        selectedPreset = idx
        val vals = presets[idx].second
        bass = vals[0]; lowMid = vals[1]; mid = vals[2]; highMid = vals[3]; treble = vals[4]
        viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
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
            SectionHeader(title = "Equalizer", subtitle = "5-Band Graphic EQ + Volume Boost")
            Spacer(Modifier.height(20.dp))

            // ── EQ Faders ──
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VerticalEQFader(
                        value = bass, onValueChange = {
                            bass = it
                            selectedPreset = -1
                            viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
                        },
                        label = "60Hz", color = MP3StudioColors.EQBand1
                    )
                    VerticalEQFader(
                        value = lowMid, onValueChange = {
                            lowMid = it
                            selectedPreset = -1
                            viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
                        },
                        label = "230Hz", color = MP3StudioColors.EQBand2
                    )
                    VerticalEQFader(
                        value = mid, onValueChange = {
                            mid = it
                            selectedPreset = -1
                            viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
                        },
                        label = "910Hz", color = MP3StudioColors.EQBand3
                    )
                    VerticalEQFader(
                        value = highMid, onValueChange = {
                            highMid = it
                            selectedPreset = -1
                            viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
                        },
                        label = "3.6kHz", color = MP3StudioColors.EQBand4
                    )
                    VerticalEQFader(
                        value = treble, onValueChange = {
                            treble = it
                            selectedPreset = -1
                            viewModel.updateEQ(com.mp3studio.app.audio.EQBands(bass, lowMid, mid, highMid, treble))
                        },
                        label = "14kHz", color = MP3StudioColors.EQBand5
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Volume Booster ──
            GlassCard {
                Text("Volume Booster", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                AppleSlider(
                    value = volume,
                    onValueChange = { volume = it; viewModel.setVolume(it) },
                    valueRange = 0f..3f,
                    label = "Master Gain (up to 300%)",
                    valueLabel = "${(volume * 100).toInt()}%",
                    accentColor = MP3StudioColors.AppleBlue
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Presets ──
            GlassCard {
                Text("EQ Presets", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEachIndexed { index, (name, _) ->
                        val isSelected = selectedPreset == index
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MP3StudioColors.AppleRed else MP3StudioColors.SurfaceElevated)
                                .border(1.dp, if (isSelected) Color.Transparent else MP3StudioColors.BorderGlass, RoundedCornerShape(20.dp))
                                .clickable { applyPreset(index) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(name, color = if (isSelected) Color.White else MP3StudioColors.TextSecondary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Format Converter ──
            GlassCard {
                Text("🔄  Format Converter", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("Select output format and export your track", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                AppleSegmentedControl(
                    options = formats,
                    selectedIndex = selectedFormatIdx,
                    onSelected = { selectedFormatIdx = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                ApplePillButton(
                    text = "Export as ${formats[selectedFormatIdx]}",
                    onClick = { /* trigger export */ },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.FileDownload
                )
            }
        }
    }
}
