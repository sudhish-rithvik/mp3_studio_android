package com.mp3studio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mp3studio.app.audio.AudioEngineViewModel
import com.mp3studio.app.ui.screens.*
import com.mp3studio.app.ui.theme.*

// ─────────────────────────────────────────────
// Navigation destinations
// ─────────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Editor      : Screen("editor",     "Editor",    Icons.Rounded.Tune)
    object Mixer       : Screen("mixer",      "Mixer",     Icons.Rounded.Equalizer)
    object Recorder    : Screen("recorder",   "Record",    Icons.Rounded.Mic)
    object EQ          : Screen("eq",         "EQ",        Icons.Rounded.GraphicEq)
    object Metadata    : Screen("metadata",   "Tags",      Label)
    object Downloader  : Screen("downloader", "Download",  Icons.Rounded.CloudDownload)
}

val bottomNavItems = listOf(
    Screen.Editor,
    Screen.Mixer,
    Screen.Recorder,
    Screen.EQ,
    Screen.Metadata,
    Screen.Downloader
)

// ─────────────────────────────────────────────
// MainActivity
// ─────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MP3StudioTheme {
                val context = LocalContext.current
                val viewModel = remember { AudioEngineViewModel(context) }
                MP3StudioApp(viewModel = viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Root App Composable
// ─────────────────────────────────────────────
@Composable
fun MP3StudioApp(viewModel: AudioEngineViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Editor) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MP3StudioColors.Background)
    ) {
        // ── Status bar spacer ──
        Column(Modifier.fillMaxSize()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MP3StudioColors.Background)
            )

            // ── App header ──
            AppHeader(currentScreen = currentScreen)

            // ── Screen content ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "screen_nav",
                    transitionSpec = {
                        fadeIn(tween(300)) + slideInVertically { it / 30 } togetherWith
                        fadeOut(tween(200))
                    }
                ) { screen ->
                    when (screen) {
                        Screen.Editor     -> AudioEditorScreen(viewModel)
                        Screen.Mixer      -> MultiTrackMixerScreen(viewModel)
                        Screen.Recorder   -> RecorderDubScreen(viewModel)
                        Screen.EQ         -> EqualizerScreen(viewModel)
                        Screen.Metadata   -> MetadataEditorScreen(viewModel)
                        Screen.Downloader -> DownloaderScreen(viewModel)
                    }
                }
            }
        }

        // ── Bottom Navigation Bar ──
        AppleBottomNavBar(
            items = bottomNavItems,
            currentScreen = currentScreen,
            onNavigate = { currentScreen = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────
// App Header
// ─────────────────────────────────────────────
@Composable
private fun AppHeader(currentScreen: Screen) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MP3StudioColors.Background, MP3StudioColors.Background.copy(alpha = 0.95f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App logo + name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(MP3StudioColors.GradientPrimary)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("MP3 Studio", color = MP3StudioColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.3).sp)
                    Text(currentScreen.label, color = MP3StudioColors.TextSecondary, fontSize = 11.sp)
                }
            }
            // Settings button
            Icon(Icons.Rounded.MoreHoriz, "Settings", tint = MP3StudioColors.TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Apple Bottom Navigation Bar
// ─────────────────────────────────────────────
@Composable
private fun AppleBottomNavBar(
    items: List<Screen>,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Frosted glass background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x00000000),
                            MP3StudioColors.Background.copy(alpha = 0.92f),
                            MP3StudioColors.Background
                        )
                    )
                )
                .padding(top = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MP3StudioColors.Surface.copy(alpha = 0.95f))
                    .border(
                        width = 1.dp,
                        color = MP3StudioColors.BorderGlass,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { screen ->
                    NavItem(
                        screen = screen,
                        isSelected = currentScreen == screen,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(screen: Screen, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_scale_${screen.route}"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MP3StudioColors.AppleRed else MP3StudioColors.TextTertiary,
        animationSpec = tween(200),
        label = "nav_color_${screen.route}"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "nav_dot_${screen.route}"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = screen.label,
            color = iconColor,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        // Active dot indicator
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MP3StudioColors.AppleRed.copy(alpha = dotAlpha))
        )
    }
}
