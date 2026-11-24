package com.mp3studio.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.ui.components.*
import com.mp3studio.app.ui.theme.MP3StudioColors
import kotlinx.coroutines.delay

// ── Domain models ──
data class DownloadResult(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnailUrl: String,
    val source: DownloadSource,
    var status: DownloadStatus = DownloadStatus.Idle
)

enum class DownloadSource(val label: String, val color: Color) {
    YouTube("YouTube", Color(0xFFFF0000)),
    Spotify("Spotify", Color(0xFF1DB954)),
    AppleMusic("Apple Music", Color(0xFFFA243C))
}

enum class DownloadStatus { Idle, Fetching, Downloading, Complete, Error }

// ── Mock library entries ──
val sampleResults = listOf(
    DownloadResult("1", "Blinding Lights", "The Weeknd", "3:20", "", DownloadSource.Spotify),
    DownloadResult("2", "Bohemian Rhapsody", "Queen", "5:55", "", DownloadSource.YouTube),
    DownloadResult("3", "As It Was", "Harry Styles", "2:37", "", DownloadSource.AppleMusic),
    DownloadResult("4", "Levitating", "Dua Lipa", "3:23", "", DownloadSource.Spotify),
    DownloadResult("5", "Shape of You", "Ed Sheeran", "3:53", "", DownloadSource.YouTube),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(viewModel: AudioEngineViewModel) {
    var urlInput by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<DownloadResult>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }

    val sources = listOf(DownloadSource.YouTube, DownloadSource.Spotify, DownloadSource.AppleMusic)

    // Simulate a search/fetch
    LaunchedEffect(isSearching) {
        if (isSearching) {
            delay(1500L)
            searchResults = sampleResults.filter {
                it.source == sources[selectedSource] || searchQuery.isEmpty()
            }
            isSearching = false
        }
    }

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
            SectionHeader(title = "Download Studio", subtitle = "Stream & import music")
            Spacer(Modifier.height(20.dp))

            // ── Source Selector ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sources.forEachIndexed { idx, src ->
                    val isSelected = selectedSource == idx
                    val color by animateColorAsState(
                        if (isSelected) src.color else MP3StudioColors.SurfaceElevated,
                        animationSpec = tween(250), label = "src_color_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = if (isSelected) 0.25f else 1f))
                            .border(
                                1.dp,
                                if (isSelected) src.color.copy(alpha = 0.6f) else MP3StudioColors.BorderGlass,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedSource = idx }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val icon = when(src) {
                                DownloadSource.YouTube -> Icons.Rounded.PlayCircle
                                DownloadSource.Spotify -> Icons.Rounded.MusicNote
                                DownloadSource.AppleMusic -> Icons.Rounded.LibraryMusic
                            }
                            Icon(icon, null, tint = if (isSelected) src.color else MP3StudioColors.TextTertiary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(src.label, color = if (isSelected) src.color else MP3StudioColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tabs ──
            AppleSegmentedControl(
                options = listOf("URL Import", "Search"),
                selectedIndex = activeTab,
                onSelected = { activeTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── URL Import / Search input ──
            GlassCard {
                AnimatedContent(targetState = activeTab, label = "tab_anim") { tab ->
                    when (tab) {
                        0 -> UrlImportContent(
                            urlInput = urlInput,
                            onUrlChange = { urlInput = it },
                            source = sources[selectedSource],
                            onFetch = {
                                isSearching = true
                                searchQuery = ""
                            }
                        )
                        1 -> SearchContent(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            source = sources[selectedSource],
                            onSearch = {
                                isSearching = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Search Loading ──
            AnimatedVisibility(visible = isSearching) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MP3StudioColors.AppleRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Fetching audio...", color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // ── Search Results ──
            if (!isSearching && searchResults.isNotEmpty()) {
                Text("Results", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(searchResults) { result ->
                        DownloadResultCard(result = result, sourceColor = sources[selectedSource].color)
                    }
                }
            }

            // ── Empty State ──
            if (!isSearching && searchResults.isEmpty() && urlInput.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = MP3StudioColors.TextTertiary, modifier = Modifier.size(56.dp))
                        Text("Paste a link or search for music", color = MP3StudioColors.TextTertiary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlImportContent(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    source: DownloadSource,
    onFetch: () -> Unit
) {
    Column {
        Text("Paste ${source.label} URL", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://...", color = MP3StudioColors.TextTertiary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Link, null, tint = MP3StudioColors.TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Rounded.Clear, null, tint = MP3StudioColors.TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = source.color,
                unfocusedBorderColor = MP3StudioColors.BorderGlass,
                focusedTextColor = MP3StudioColors.TextPrimary,
                unfocusedTextColor = MP3StudioColors.TextPrimary,
                cursorColor = source.color
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(14.dp))
        ApplePillButton(
            text = "Fetch Audio",
            onClick = onFetch,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.CloudDownload
        )
    }
}

@Composable
private fun SearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    source: DownloadSource,
    onSearch: () -> Unit
) {
    Column {
        Text("Search ${source.label}", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists...", color = MP3StudioColors.TextTertiary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MP3StudioColors.TextSecondary, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = source.color,
                unfocusedBorderColor = MP3StudioColors.BorderGlass,
                focusedTextColor = MP3StudioColors.TextPrimary,
                unfocusedTextColor = MP3StudioColors.TextPrimary,
                cursorColor = source.color
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(14.dp))
        ApplePillButton(
            text = "Search",
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Search
        )
    }
}

@Composable
private fun DownloadResultCard(result: DownloadResult, sourceColor: Color) {
    var status by remember { mutableStateOf(result.status) }

    GlassCard(cornerRadius = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail placeholder
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(MP3StudioColors.GradientPrimary)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.artist, color = MP3StudioColors.TextSecondary, fontSize = 12.sp)
                Text(result.duration, color = MP3StudioColors.TextTertiary, fontSize = 11.sp)
            }

            // Source badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(sourceColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(result.source.label, color = sourceColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))

            // Action button
            when (status) {
                DownloadStatus.Idle -> AppleCircleButton(Icons.Rounded.FileDownload, "Download", {
                    status = DownloadStatus.Downloading
                }, size = 36.dp, tint = sourceColor, isActive = false)
                DownloadStatus.Downloading -> {
                    LaunchedEffect(Unit) { delay(2000L); status = DownloadStatus.Complete }
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = sourceColor, strokeWidth = 2.dp)
                }
                DownloadStatus.Complete -> AppleCircleButton(Icons.Rounded.Check, "Done", {}, size = 36.dp, tint = MP3StudioColors.AppleGreen, isActive = true)
                else -> AppleCircleButton(Icons.Rounded.Refresh, "Retry", { status = DownloadStatus.Idle }, size = 36.dp)
            }
        }
    }
}
