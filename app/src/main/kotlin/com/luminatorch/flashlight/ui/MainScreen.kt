package com.luminatorch.flashlight.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminatorch.flashlight.ads.AdMobAdaptiveBanner
import com.luminatorch.flashlight.hardware.TorchMode
import com.luminatorch.flashlight.theme.LuminaColors
import com.luminatorch.flashlight.timer.SleepTimerManager
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MainScreen(
    mode: TorchMode,
    isLedLit: Boolean,
    supportsBrightness: Boolean,
    brightnessRatio: Float,
    strobeSpeed: Float,
    strobeFrequencyHz: Float,
    remainingTimerSeconds: Int,
    hapticEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onPowerClick: () -> Unit,
    onScreenLightClick: () -> Unit,
    onSosClick: () -> Unit,
    onStrobeClick: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onStrobeSpeedChange: (Float) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showTimerDialog by remember { mutableStateOf(false) }

    val triggerHaptic: () -> Unit = {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val isActive = mode != TorchMode.OFF
    val accentColor by animateColorAsState(
        targetValue = when (mode) {
            TorchMode.SOS -> LuminaColors.CrimsonSos
            TorchMode.STEADY, TorchMode.STROBE -> LuminaColors.AmberPrimary
            TorchMode.OFF -> LuminaColors.TextMuted
        },
        animationSpec = tween(250),
        label = "accentColor"
    )

    val ambientGlowAlpha by animateFloatAsState(
        targetValue = if (isActive && isLedLit) 0.24f else if (isActive) 0.10f else 0f,
        animationSpec = tween(260),
        label = "ambientGlow"
    )

    Scaffold(
        containerColor = LuminaColors.Obsidian,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AdMobAdaptiveBanner()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LuminaColors.Obsidian)
        ) {
            // Subtle top-center ambient beam halo
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (ambientGlowAlpha > 0.005f) {
                    val center = Offset(size.width / 2f, size.height * 0.40f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = ambientGlowAlpha),
                                accentColor.copy(alpha = ambientGlowAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.68f
                        ),
                        radius = size.minDimension * 0.68f,
                        center = center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                TopBrandBar(
                    isActive = isActive,
                    accentColor = accentColor,
                    hasTimer = remainingTimerSeconds > 0,
                    onTimerClick = {
                        triggerHaptic()
                        showTimerDialog = true
                    },
                    onSettingsClick = {
                        triggerHaptic()
                        onOpenSettings()
                    }
                )

                Spacer(modifier = Modifier.weight(0.6f))

                // Status badge
                StatusBadge(
                    mode = mode,
                    strobeFrequencyHz = strobeFrequencyHz,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Centerpiece Power Core
                TacticalPowerCore(
                    mode = mode,
                    isLedLit = isLedLit,
                    accentColor = accentColor,
                    onClick = {
                        triggerHaptic()
                        onPowerClick()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Dynamic Slider / Timer Control Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(
                            visible = mode == TorchMode.STEADY && supportsBrightness,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            TacticalSliderCard(
                                label = "BEAM INTENSITY",
                                valueLabel = "${(brightnessRatio * 100).roundToInt()}%",
                                value = brightnessRatio,
                                accentColor = LuminaColors.AmberPrimary,
                                onValueChange = onBrightnessChange
                            )
                        }

                        AnimatedVisibility(
                            visible = mode == TorchMode.STROBE,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            TacticalSliderCard(
                                label = "STROBE RATE",
                                valueLabel = String.format("%.1f Hz", strobeFrequencyHz),
                                value = strobeSpeed,
                                accentColor = LuminaColors.AmberPrimary,
                                onValueChange = onStrobeSpeedChange
                            )
                        }

                        AnimatedVisibility(
                            visible = remainingTimerSeconds > 0,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ActiveTimerPill(
                                remainingSeconds = remainingTimerSeconds,
                                onCancel = {
                                    triggerHaptic()
                                    onCancelSleepTimer()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Mode Dock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TacticalModeButton(
                        modifier = Modifier.weight(1f),
                        label = "SCREEN",
                        icon = Icons.Rounded.WbSunny,
                        isSelected = false,
                        activeColor = LuminaColors.AmberPrimary,
                        onClick = {
                            triggerHaptic()
                            onScreenLightClick()
                        }
                    )

                    TacticalModeButton(
                        modifier = Modifier.weight(1f),
                        label = "BEACON",
                        badgeText = "SOS",
                        isSelected = mode == TorchMode.SOS,
                        activeColor = LuminaColors.CrimsonSos,
                        onClick = {
                            triggerHaptic()
                            onSosClick()
                        }
                    )

                    TacticalModeButton(
                        modifier = Modifier.weight(1f),
                        label = "STROBE",
                        icon = Icons.Rounded.FlashOn,
                        isSelected = mode == TorchMode.STROBE,
                        activeColor = LuminaColors.AmberPrimary,
                        onClick = {
                            triggerHaptic()
                            onStrobeClick()
                        }
                    )
                }
            }
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            isTimerActive = remainingTimerSeconds > 0,
            onDismiss = { showTimerDialog = false },
            onSelectMinutes = { minutes ->
                showTimerDialog = false
                onStartSleepTimer(minutes * 60)
            },
            onCancelTimer = {
                showTimerDialog = false
                onCancelSleepTimer()
            }
        )
    }
}

@Composable
private fun TopBrandBar(
    isActive: Boolean,
    accentColor: Color,
    hasTimer: Boolean,
    onTimerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) accentColor else LuminaColors.SurfaceBorder)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LuminaColors.TextPrimary, fontWeight = FontWeight.ExtraBold)) {
                        append("LUMINA")
                    }
                    append(" ")
                    withStyle(SpanStyle(color = LuminaColors.AmberPrimary, fontWeight = FontWeight.Medium)) {
                        append("TORCH")
                    }
                },
                style = MaterialTheme.typography.titleLarge
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                shape = CircleShape,
                color = if (hasTimer) LuminaColors.AmberPrimary.copy(alpha = 0.16f) else LuminaColors.SurfaceDark,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasTimer) LuminaColors.AmberPrimary else LuminaColors.SurfaceBorder
                )
            ) {
                IconButton(onClick = onTimerClick, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Auto-Off Timer",
                        tint = if (hasTimer) LuminaColors.AmberPrimary else LuminaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = LuminaColors.SurfaceDark,
                border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
            ) {
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = LuminaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    mode: TorchMode,
    strobeFrequencyHz: Float,
    accentColor: Color
) {
    val label = when (mode) {
        TorchMode.OFF -> "STANDBY"
        TorchMode.STEADY -> "BEAM ACTIVE"
        TorchMode.STROBE -> String.format("STROBE • %.1f HZ", strobeFrequencyHz)
        TorchMode.SOS -> "SOS BEACON ACTIVE"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = if (mode == TorchMode.OFF) LuminaColors.SurfaceDark else accentColor.copy(alpha = 0.14f),
        border = BorderStroke(
            width = 1.dp,
            color = if (mode == TorchMode.OFF) LuminaColors.SurfaceBorder else accentColor.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (mode == TorchMode.OFF) LuminaColors.TextSecondary else accentColor,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TacticalPowerCore(
    mode: TorchMode,
    isLedLit: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "coreScale"
    )

    val isActive = mode != TorchMode.OFF
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFactor"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isActive) accentColor else LuminaColors.SurfaceBorder,
        animationSpec = tween(220),
        label = "ringColor"
    )

    Box(
        modifier = Modifier
            .size(252.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer tactical dial + animated glow ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f

            // Glowing halo when active
            if (isActive) {
                val glowAlpha = if (isLedLit) 0.34f * pulseFactor else 0.12f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha),
                            accentColor.copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )
            }

            // 36 tactical perimeter ticks
            val tickCount = 36
            val tickOuter = outerRadius - 6.dp.toPx()
            val tickInnerShort = outerRadius - 14.dp.toPx()
            val tickInnerLong = outerRadius - 19.dp.toPx()
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((i * 360.0 / tickCount) - 90.0)
                val isMajor = i % 9 == 0
                val inner = if (isMajor) tickInnerLong else tickInnerShort
                val start = Offset(
                    x = center.x + (cos(angleRad) * inner).toFloat(),
                    y = center.y + (sin(angleRad) * inner).toFloat()
                )
                val end = Offset(
                    x = center.x + (cos(angleRad) * tickOuter).toFloat(),
                    y = center.y + (sin(angleRad) * tickOuter).toFloat()
                )
                drawLine(
                    color = when {
                        isActive && isMajor -> accentColor
                        isActive -> accentColor.copy(alpha = 0.45f)
                        isMajor -> LuminaColors.TextMuted
                        else -> LuminaColors.SurfaceBorder
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Precision metallic ring
            drawCircle(
                color = ringColor.copy(alpha = if (isActive) 0.9f else 0.55f),
                radius = outerRadius - 29.dp.toPx(),
                center = center,
                style = Stroke(width = if (isActive) 2.5.dp.toPx() else 1.5.dp.toPx())
            )
        }

        // Inner tactile obsidian power button
        Box(
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isActive) {
                            listOf(
                                Color(0xFF252014),
                                Color(0xFF14171F)
                            )
                        } else {
                            listOf(
                                LuminaColors.SurfaceElevated,
                                LuminaColors.SurfaceDark
                            )
                        }
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isActive) accentColor.copy(alpha = 0.9f) else Color(0xFF394358),
                            if (isActive) accentColor.copy(alpha = 0.25f) else Color(0xFF191F2B)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = if (isActive) "Turn flashlight off" else "Turn flashlight on",
                    tint = if (isActive) accentColor else LuminaColors.TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isActive) "ON" else "TAP",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isActive) LuminaColors.AmberBright else LuminaColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun TacticalSliderCard(
    label: String,
    valueLabel: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = LuminaColors.SurfaceDark,
        border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = LuminaColors.TextSecondary
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = LuminaColors.SurfaceBorder
                )
            )
        }
    }
}

@Composable
private fun ActiveTimerPill(
    remainingSeconds: Int,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = LuminaColors.AmberPrimary.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, LuminaColors.AmberPrimary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = LuminaColors.AmberPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AUTO-OFF ${SleepTimerManager.formatDuration(remainingSeconds)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = LuminaColors.AmberBright
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel timer",
                    tint = LuminaColors.AmberBright,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TacticalModeButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    badgeText: String? = null,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.18f) else LuminaColors.SurfaceDark,
        animationSpec = tween(200),
        label = "modeContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else LuminaColors.SurfaceBorder,
        animationSpec = tween(200),
        label = "modeBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else LuminaColors.TextPrimary,
        animationSpec = tween(200),
        label = "modeContent"
    )

    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (badgeText != null) {
                Text(
                    text = badgeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) contentColor else LuminaColors.TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerDialog(
    isTimerActive: Boolean,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val presets = listOf(1, 5, 10, 15, 30, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuminaColors.SurfaceElevated,
        titleContentColor = LuminaColors.TextPrimary,
        textContentColor = LuminaColors.TextSecondary,
        title = {
            Text(
                text = "Auto-Off Timer",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Automatically shut off the flashlight after:",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { mins ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelectMinutes(mins) },
                            label = {
                                Text(
                                    text = if (mins == 60) "1 hr" else "$mins min",
                                    color = LuminaColors.TextPrimary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = LuminaColors.SurfaceDark,
                                labelColor = LuminaColors.TextPrimary
                            ),
                            border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isTimerActive) {
                TextButton(onClick = onCancelTimer) {
                    Text("TURN OFF TIMER", color = LuminaColors.CrimsonSos)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = LuminaColors.AmberPrimary)
            }
        }
    )
}
