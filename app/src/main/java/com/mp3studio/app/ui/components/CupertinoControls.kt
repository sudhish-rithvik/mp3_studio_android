package com.mp3studio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.mp3studio.app.ui.theme.MP3StudioColors

// ─────────────────────────────────────────────
// Apple Pill Button
// ─────────────────────────────────────────────

@Composable
fun ApplePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_scale"
    )

    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(MP3StudioColors.GradientPrimary)
    } else {
        Brush.horizontalGradient(listOf(MP3StudioColors.SurfaceElevated, MP3StudioColors.SurfaceElevated))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(bgBrush)
            .then(if (!isPrimary) Modifier.border(1.dp, MP3StudioColors.BorderGlass, RoundedCornerShape(50)) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                color = if (isPrimary) Color.White else MP3StudioColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// Icon Circular Button (Apple style)
// ─────────────────────────────────────────────

@Composable
fun AppleCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MP3StudioColors.AppleRed,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "circ_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) tint.copy(alpha = 0.25f) else MP3StudioColors.SurfaceElevated,
        animationSpec = tween(200),
        label = "circ_bg"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, if (isActive) tint.copy(alpha = 0.6f) else MP3StudioColors.BorderSubtle, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (isActive) tint else MP3StudioColors.TextSecondary, modifier = Modifier.size(size * 0.45f))
    }
}

// ─────────────────────────────────────────────
// Cupertino Toggle Switch
// ─────────────────────────────────────────────

@Composable
fun CupertinoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = MP3StudioColors.AppleGreen,
    label: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(label, color = MP3StudioColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MP3StudioColors.SurfaceElevated,
                uncheckedBorderColor = MP3StudioColors.BorderGlass
            )
        )
    }
}

// ─────────────────────────────────────────────
// Apple Segmented Control
// ─────────────────────────────────────────────

@Composable
fun <T> AppleSegmentedControl(
    options: List<T>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String = { it.toString() }
) {
    val selected by animateIntAsState(selectedIndex, label = "seg_sel")

    Box(
        modifier = modifier
            .glassmorphicDark(cornerRadius = 12.dp)
            .padding(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MP3StudioColors.AppleRed else Color.Transparent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "seg_bg_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onSelected(index) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFor(option),
                        color = if (isSelected) Color.White else MP3StudioColors.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Glass Card
// ─────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassmorphicDark(cornerRadius = cornerRadius)
            .padding(20.dp)
    ) {
        content()
    }
}

// ─────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, color = MP3StudioColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MP3StudioColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────
// Apple Red Accent Slider
// ─────────────────────────────────────────────

@Composable
fun AppleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    accentColor: Color = MP3StudioColors.AppleRed,
    label: String = "",
    valueLabel: String = ""
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = MP3StudioColors.TextSecondary, fontSize = 12.sp)
                Text(valueLabel, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MP3StudioColors.SurfaceElevated
            )
        )
    }
}

// ─────────────────────────────────────────────
// Vertical EQ Fader
// ─────────────────────────────────────────────

@Composable
fun VerticalEQFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    color: Color = MP3StudioColors.AppleRed,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${if (value >= 0) "+" else ""}${(value * 12).toInt()}dB",
            color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -1f..1f,
            modifier = Modifier
                .height(140.dp)
                .width(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = MP3StudioColors.SurfaceElevated
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = MP3StudioColors.TextSecondary, fontSize = 10.sp)
    }
}
