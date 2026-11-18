package com.luminatorch.flashlight.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LuminaPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _turnOnAtStartup = MutableStateFlow(prefs.getBoolean(KEY_TURN_ON_STARTUP, false))
    val turnOnAtStartupFlow: StateFlow<Boolean> = _turnOnAtStartup.asStateFlow()

    var turnOnAtStartup: Boolean
        get() = _turnOnAtStartup.value
        set(value) {
            prefs.edit().putBoolean(KEY_TURN_ON_STARTUP, value).apply()
            _turnOnAtStartup.value = value
        }

    private val _forcePortrait = MutableStateFlow(prefs.getBoolean(KEY_FORCE_PORTRAIT, true))
    val forcePortraitFlow: StateFlow<Boolean> = _forcePortrait.asStateFlow()

    var forcePortrait: Boolean
        get() = _forcePortrait.value
        set(value) {
            prefs.edit().putBoolean(KEY_FORCE_PORTRAIT, value).apply()
            _forcePortrait.value = value
        }

    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true))
    val hapticFeedbackFlow: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    var hapticFeedback: Boolean
        get() = _hapticFeedback.value
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
            _hapticFeedback.value = value
        }

    private val _strobeSpeed = MutableStateFlow(prefs.getFloat(KEY_STROBE_SPEED, 0.45f))
    val strobeSpeedFlow: StateFlow<Float> = _strobeSpeed.asStateFlow()

    var strobeSpeed: Float
        get() = _strobeSpeed.value
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            prefs.edit().putFloat(KEY_STROBE_SPEED, clamped).apply()
            _strobeSpeed.value = clamped
        }

    var brightnessLevel: Int
        get() = prefs.getInt(KEY_BRIGHTNESS_LEVEL, DEFAULT_BRIGHTNESS)
        set(value) = prefs.edit().putInt(KEY_BRIGHTNESS_LEVEL, value).apply()

    private val _brightDisplayColor = MutableStateFlow(prefs.getInt(KEY_BRIGHT_DISPLAY_COLOR, Color.WHITE))
    val brightDisplayColorFlow: StateFlow<Int> = _brightDisplayColor.asStateFlow()

    var brightDisplayColor: Int
        get() = _brightDisplayColor.value
        set(value) {
            prefs.edit().putInt(KEY_BRIGHT_DISPLAY_COLOR, value).apply()
            _brightDisplayColor.value = value
        }

    var lastSleepTimerMinutes: Int
        get() = prefs.getInt(KEY_LAST_SLEEP_MINUTES, 10)
        set(value) = prefs.edit().putInt(KEY_LAST_SLEEP_MINUTES, value).apply()

    companion object {
        private const val PREFS_NAME = "lumina_torch_prefs"
        private const val KEY_TURN_ON_STARTUP = "turn_on_startup"
        private const val KEY_FORCE_PORTRAIT = "force_portrait"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_STROBE_SPEED = "strobe_speed"
        private const val KEY_BRIGHTNESS_LEVEL = "brightness_level"
        private const val KEY_BRIGHT_DISPLAY_COLOR = "bright_display_color"
        private const val KEY_LAST_SLEEP_MINUTES = "last_sleep_minutes"
        const val DEFAULT_BRIGHTNESS = -1

        @Volatile
        private var instance: LuminaPreferences? = null

        fun getInstance(context: Context): LuminaPreferences {
            return instance ?: synchronized(this) {
                instance ?: LuminaPreferences(context).also { instance = it }
            }
        }
    }
}
