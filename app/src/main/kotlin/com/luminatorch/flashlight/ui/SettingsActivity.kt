package com.luminatorch.flashlight.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminatorch.flashlight.BuildConfig
import com.luminatorch.flashlight.data.LuminaPreferences
import com.luminatorch.flashlight.theme.LuminaColors
import com.luminatorch.flashlight.theme.LuminaTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val preferences = LuminaPreferences.getInstance(this)

        setContent {
            LuminaTheme {
                val turnOnStartup by preferences.turnOnAtStartupFlow.collectAsStateWithLifecycle()
                val hapticFeedback by preferences.hapticFeedbackFlow.collectAsStateWithLifecycle()
                val forcePortrait by preferences.forcePortraitFlow.collectAsStateWithLifecycle()

                SettingsScreen(
                    turnOnAtStartup = turnOnStartup,
                    hapticFeedback = hapticFeedback,
                    forcePortrait = forcePortrait,
                    onTurnOnAtStartupChange = { preferences.turnOnAtStartup = it },
                    onHapticFeedbackChange = { preferences.hapticFeedback = it },
                    onForcePortraitChange = { preferences.forcePortrait = it },
                    onBack = ::finish
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    turnOnAtStartup: Boolean,
    hapticFeedback: Boolean,
    forcePortrait: Boolean,
    onTurnOnAtStartupChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onForcePortraitChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = LuminaColors.Obsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        color = LuminaColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = LuminaColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LuminaColors.Obsidian
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BEHAVIOR",
                style = MaterialTheme.typography.labelMedium,
                color = LuminaColors.AmberPrimary
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LuminaColors.SurfaceDark,
                border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
            ) {
                Column {
                    SettingsSwitchRow(
                        title = "Instant Light on Launch",
                        subtitle = "Turn on the rear flashlight immediately when opening Lumina Torch",
                        checked = turnOnAtStartup,
                        onCheckedChange = onTurnOnAtStartupChange
                    )
                    HorizontalDivider(color = LuminaColors.SurfaceBorder.copy(alpha = 0.6f))
                    SettingsSwitchRow(
                        title = "Tactile Haptics",
                        subtitle = "Subtle vibration feedback when toggling modes",
                        checked = hapticFeedback,
                        onCheckedChange = onHapticFeedbackChange
                    )
                    HorizontalDivider(color = LuminaColors.SurfaceBorder.copy(alpha = 0.6f))
                    SettingsSwitchRow(
                        title = "Lock Portrait Orientation",
                        subtitle = "Keep controls anchored upright while holding your phone",
                        checked = forcePortrait,
                        onCheckedChange = onForcePortraitChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelMedium,
                color = LuminaColors.AmberPrimary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = LuminaColors.SurfaceDark,
                border = BorderStroke(1.dp, LuminaColors.SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Lumina Torch",
                        style = MaterialTheme.typography.titleMedium,
                        color = LuminaColors.TextPrimary
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LuminaColors.AmberPrimary
                    )
                    Text(
                        text = "Fast, reliable tactical flashlight with adjustable brightness, SOS Morse beacon, stroboscope, auto-off timer, and full-screen light.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LuminaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LuminaColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LuminaColors.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LuminaColors.Obsidian,
                checkedTrackColor = LuminaColors.AmberPrimary,
                uncheckedThumbColor = LuminaColors.TextSecondary,
                uncheckedTrackColor = LuminaColors.SurfaceElevated,
                uncheckedBorderColor = LuminaColors.SurfaceBorder
            )
        )
    }
}
