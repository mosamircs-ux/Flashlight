package com.luminatorch.flashlight.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminatorch.flashlight.data.LuminaPreferences
import com.luminatorch.flashlight.hardware.TorchController
import com.luminatorch.flashlight.hardware.TorchMode
import com.luminatorch.flashlight.theme.LuminaTheme
import com.luminatorch.flashlight.timer.SleepTimerManager

class MainActivity : ComponentActivity() {
    private lateinit var torchController: TorchController
    private lateinit var preferences: LuminaPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        torchController = TorchController.getInstance(this)
        preferences = LuminaPreferences.getInstance(this)

        if (preferences.turnOnAtStartup && torchController.mode.value == TorchMode.OFF) {
            torchController.turnOnSteady()
        }

        setContent {
            LuminaTheme {
                val mode by torchController.mode.collectAsStateWithLifecycle()
                val isLedLit by torchController.isLedLit.collectAsStateWithLifecycle()
                val supportsBrightness by torchController.supportsBrightness.collectAsStateWithLifecycle()
                val brightnessRatio by torchController.brightnessRatio.collectAsStateWithLifecycle()
                val strobeSpeed by torchController.strobeSpeed.collectAsStateWithLifecycle()
                val remainingTimer by SleepTimerManager.remainingSeconds.collectAsStateWithLifecycle()
                val hapticEnabled by preferences.hapticFeedbackFlow.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    torchController.errorMessage.collect { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                MainScreen(
                    mode = mode,
                    isLedLit = isLedLit,
                    supportsBrightness = supportsBrightness,
                    brightnessRatio = brightnessRatio,
                    strobeSpeed = strobeSpeed,
                    strobeFrequencyHz = torchController.getStrobeFrequencyHz(),
                    remainingTimerSeconds = remainingTimer,
                    hapticEnabled = hapticEnabled,
                    snackbarHostState = snackbarHostState,
                    onPowerClick = { torchController.toggleSteadyTorch() },
                    onScreenLightClick = {
                        startActivity(Intent(this@MainActivity, BrightDisplayActivity::class.java))
                    },
                    onSosClick = { torchController.toggleSos() },
                    onStrobeClick = { torchController.toggleStrobe() },
                    onBrightnessChange = { torchController.updateBrightness(it) },
                    onStrobeSpeedChange = { torchController.updateStrobeSpeed(it) },
                    onStartSleepTimer = { seconds ->
                        SleepTimerManager.start(this@MainActivity, seconds)
                    },
                    onCancelSleepTimer = {
                        SleepTimerManager.cancel()
                    },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        torchController.discoverFlashCameraAsync()
        requestedOrientation = if (preferences.forcePortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
