package com.luminatorch.flashlight.ui

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminatorch.flashlight.data.LuminaPreferences
import com.luminatorch.flashlight.theme.LuminaColors
import com.luminatorch.flashlight.theme.LuminaTheme

class BrightDisplayActivity : ComponentActivity() {
    private lateinit var preferences: LuminaPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        preferences = LuminaPreferences.getInstance(this)

        setContent {
            LuminaTheme {
                val colorInt by preferences.brightDisplayColorFlow.collectAsStateWithLifecycle()
                ScreenLightView(
                    selectedColor = Color(colorInt),
                    onSelectColor = { newColor ->
                        preferences.brightDisplayColor = newColor.toArgb()
                    },
                    onClose = ::finish
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setScreenBrightness(1.0f)
        requestedOrientation = if (preferences.forcePortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    private fun setScreenBrightness(level: Float) {
        val params = window.attributes
        params.screenBrightness = level
        window.attributes = params
    }
}

private data class LightPreset(val name: String, val color: Color)

@Composable
private fun ScreenLightView(
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    onClose: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }

    val presets = remember {
        listOf(
            LightPreset("Pure White", Color(0xFFFFFFFF)),
            LightPreset("Warm Amber", Color(0xFFFFCA66)),
            LightPreset("Tactical Red", Color(0xFFFF3B30)),
            LightPreset("Cool Cyan", Color(0xFF80E5FF)),
            LightPreset("Night Green", Color(0xFF4ADE80)),
            LightPreset("Soft Sunset", Color(0xFFFF8C42))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(selectedColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Top back button
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = LuminaColors.Obsidian.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LuminaColors.TextPrimary
                    )
                }
            }
        }

        // Bottom color preset dock
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LuminaColors.Obsidian.copy(alpha = 0.84f),
                border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SCREEN LIGHT • TAP ANYWHERE TO HIDE",
                        style = MaterialTheme.typography.labelMedium,
                        color = LuminaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        presets.forEach { preset ->
                            val isSelected = preset.color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(preset.color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) LuminaColors.AmberPrimary else Color.Black.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectColor(preset.color) }
                            )
                        }
                    }
                }
            }
        }
    }
}
