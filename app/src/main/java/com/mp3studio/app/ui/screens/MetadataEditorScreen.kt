package com.mp3studio.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.audio.AudioMetadata
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors

@Composable
fun MetadataEditorScreen(viewModel: AudioEngineViewModel) {
    val metadata by viewModel.metadata.collectAsState()
    val context = LocalContext.current

    var title by remember(metadata.title) { mutableStateOf(metadata.title) }
    var artist by remember(metadata.artist) { mutableStateOf(metadata.artist) }
    var album by remember(metadata.album) { mutableStateOf(metadata.album) }
    var year by remember(metadata.year) { mutableStateOf(metadata.year) }
    var genre by remember(metadata.genre) { mutableStateOf(metadata.genre) }
    var trackNum by remember(metadata.trackNumber) { mutableStateOf(metadata.trackNumber) }
    var lyrics by remember(metadata.lyrics) { mutableStateOf(metadata.lyrics) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var showRingtoneSheet by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        coverUri = uri
        viewModel.updateMetadata(metadata.copy(coverArtPath = uri?.toString()))
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
            SectionHeader(title = "Metadata & Tags", subtitle = "Edit ID3v2.3 song information")
            Spacer(Modifier.height(20.dp))

            // ── Cover Art Card ──
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cover art display
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(MP3StudioColors.GradientPrimary)
                            )
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverUri != null) {
                            AsyncImage(
                                model = coverUri,
                                contentDescription = "Cover Art",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Image, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                                Text("Add Cover", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Album Cover Art", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("JPG / PNG up to 3000×3000px", color = MP3StudioColors.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppleCircleButton(Icons.Rounded.Image, "Pick image", { imagePicker.launch("image/*") }, size = 36.dp, tint = MP3StudioColors.AppleRed, isActive = true)
                            if (coverUri != null) {
                                AppleCircleButton(Icons.Rounded.DeleteOutline, "Remove", { coverUri = null }, size = 36.dp, tint = MP3StudioColors.AppleOrange)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Metadata Fields Card ──
            GlassCard {
                Text("Track Information", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))

                MetaTextField(value = title, onValueChange = { title = it }, label = "Title", icon = Icons.Rounded.MusicNote)
                Spacer(Modifier.height(12.dp))
                MetaTextField(value = artist, onValueChange = { artist = it }, label = "Artist", icon = Icons.Rounded.Person)
                Spacer(Modifier.height(12.dp))
                MetaTextField(value = album, onValueChange = { album = it }, label = "Album", icon = Icons.Rounded.Album)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetaTextField(value = year, onValueChange = { year = it }, label = "Year", icon = Icons.Rounded.CalendarToday, modifier = Modifier.weight(1f))
                    MetaTextField(value = trackNum, onValueChange = { trackNum = it }, label = "Track #", icon = Icons.Rounded.Tag, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                MetaTextField(value = genre, onValueChange = { genre = it }, label = "Genre", icon = Icons.Rounded.Category)
            }

            Spacer(Modifier.height(16.dp))

            // ── Lyrics Card ──
            GlassCard {
                Text("Lyrics", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Paste song lyrics here...", color = MP3StudioColors.TextTertiary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MP3StudioColors.AppleRed,
                        unfocusedBorderColor = MP3StudioColors.BorderGlass,
                        focusedTextColor = MP3StudioColors.TextPrimary,
                        unfocusedTextColor = MP3StudioColors.TextPrimary,
                        cursorColor = MP3StudioColors.AppleRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Save Metadata Button ──
            ApplePillButton(
                text = "Save Metadata & Export",
                onClick = {
                    viewModel.updateMetadata(AudioMetadata(title, artist, album, year, genre, trackNum, coverUri?.toString(), lyrics))
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Save
            )

            Spacer(Modifier.height(12.dp))

            // ── Ringtone & System ──
            GlassCard {
                Text("🔔  System Integration", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("Set this track as your device ringtone, alarm, or notification sound.", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    RingtoneOptionButton(label = "Ringtone", icon = Icons.Rounded.PhoneInTalk, color = MP3StudioColors.AppleRed, modifier = Modifier.weight(1f)) {}
                    RingtoneOptionButton(label = "Alarm", icon = Icons.Rounded.Alarm, color = MP3StudioColors.AppleOrange, modifier = Modifier.weight(1f)) {}
                    RingtoneOptionButton(label = "Notif.", icon = Icons.Rounded.Notifications, color = MP3StudioColors.AppleBlue, modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun MetaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = MP3StudioColors.AppleRed, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MP3StudioColors.AppleRed,
            unfocusedBorderColor = MP3StudioColors.BorderGlass,
            focusedTextColor = MP3StudioColors.TextPrimary,
            unfocusedTextColor = MP3StudioColors.TextPrimary,
            cursorColor = MP3StudioColors.AppleRed,
            focusedLabelColor = MP3StudioColors.AppleRed,
            unfocusedLabelColor = MP3StudioColors.TextTertiary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun RingtoneOptionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
