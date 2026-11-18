package com.luminatorch.flashlight.hardware

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.luminatorch.flashlight.data.LuminaPreferences
import com.luminatorch.flashlight.widgets.TorchWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class TorchMode {
    OFF,
    STEADY,
    STROBE,
    SOS
}

@OptIn(ExperimentalCoroutinesApi::class)
class TorchController private constructor(private val appContext: Context) {
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val preferences = LuminaPreferences.getInstance(appContext)

    // Dedicated single-threaded IO dispatcher so Camera2 IPC never blocks the UI thread
    private val cameraDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + cameraDispatcher)

    @Volatile
    private var flashCameraId: String? = null
    @Volatile
    private var maxStrengthLevel: Int = 1
    private var patternJob: Job? = null
    private var isCallbackRegistered = false

    private val _mode = MutableStateFlow(TorchMode.OFF)
    val mode: StateFlow<TorchMode> = _mode.asStateFlow()

    private val _isLedLit = MutableStateFlow(false)
    val isLedLit: StateFlow<Boolean> = _isLedLit.asStateFlow()

    private val _hasFlash = MutableStateFlow(false)
    val hasFlash: StateFlow<Boolean> = _hasFlash.asStateFlow()

    private val _supportsBrightness = MutableStateFlow(false)
    val supportsBrightness: StateFlow<Boolean> = _supportsBrightness.asStateFlow()

    private val _brightnessRatio = MutableStateFlow(1f)
    val brightnessRatio: StateFlow<Float> = _brightnessRatio.asStateFlow()

    private val _strobeSpeed = MutableStateFlow(preferences.strobeSpeed)
    val strobeSpeed: StateFlow<Float> = _strobeSpeed.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != flashCameraId) return
            if (_mode.value == TorchMode.OFF || _mode.value == TorchMode.STEADY) {
                _isLedLit.value = enabled
                _mode.value = if (enabled) TorchMode.STEADY else TorchMode.OFF
                notifyWidgets(enabled)
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId != flashCameraId) return
            stopAll()
        }
    }

    init {
        scope.launch {
            discoverFlashCameraInternal()
            registerTorchCallback()
        }
    }

    fun discoverFlashCameraAsync() {
        scope.launch {
            discoverFlashCameraInternal()
        }
    }

    private fun discoverFlashCameraInternal() {
        try {
            var fallbackFlashId: String? = null
            var backFlashId: String? = null

            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val available = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (available) {
                    if (fallbackFlashId == null) fallbackFlashId = id
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        backFlashId = id
                        break
                    }
                }
            }

            flashCameraId = backFlashId ?: fallbackFlashId
            _hasFlash.value = flashCameraId != null

            val cid = flashCameraId
            if (cid != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val chars = cameraManager.getCameraCharacteristics(cid)
                maxStrengthLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                _supportsBrightness.value = maxStrengthLevel > 1
                val saved = preferences.brightnessLevel
                val initialLevel = if (saved in 1..maxStrengthLevel) saved else maxStrengthLevel
                _brightnessRatio.value = (initialLevel.toFloat() / maxStrengthLevel.toFloat()).coerceIn(0f, 1f)
            } else {
                maxStrengthLevel = 1
                _supportsBrightness.value = false
                _brightnessRatio.value = 1f
            }
        } catch (_: Exception) {
            flashCameraId = null
            _hasFlash.value = false
            _supportsBrightness.value = false
        }
    }

    private fun registerTorchCallback() {
        if (isCallbackRegistered) return
        try {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            isCallbackRegistered = true
        } catch (_: Exception) {
            isCallbackRegistered = false
        }
    }

    fun toggleSteadyTorch() {
        if (_mode.value == TorchMode.STEADY) {
            turnOff()
        } else {
            turnOnSteady()
        }
    }

    fun turnOnSteady() {
        cancelPattern()
        scope.launch {
            if (!ensureFlashOrNotify()) return@launch
            if (applyHardwareTorch(true)) {
                _mode.value = TorchMode.STEADY
                _isLedLit.value = true
                notifyWidgets(true)
            }
        }
    }

    fun toggleStrobe() {
        if (_mode.value == TorchMode.STROBE) {
            turnOff()
        } else {
            startStrobe()
        }
    }

    fun toggleSos() {
        if (_mode.value == TorchMode.SOS) {
            turnOff()
        } else {
            startSos()
        }
    }

    fun turnOff() {
        stopAll()
    }

    fun stopAll() {
        cancelPattern()
        _mode.value = TorchMode.OFF
        _isLedLit.value = false
        notifyWidgets(false)
        scope.launch {
            applyHardwareTorch(false)
        }
    }

    fun updateBrightness(ratio: Float) {
        val clamped = ratio.coerceIn(0f, 1f)
        _brightnessRatio.value = clamped
        if (maxStrengthLevel > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val level = (1 + clamped * (maxStrengthLevel - 1)).roundToInt().coerceIn(1, maxStrengthLevel)
            preferences.brightnessLevel = level
            if (_mode.value == TorchMode.STEADY) {
                scope.launch {
                    val cid = flashCameraId ?: return@launch
                    try {
                        cameraManager.turnOnTorchWithStrengthLevel(cid, level)
                    } catch (_: Exception) {
                        _errorMessage.tryEmit("Unable to adjust flash brightness")
                    }
                }
            }
        }
    }

    fun updateStrobeSpeed(speed: Float) {
        val clamped = speed.coerceIn(0f, 1f)
        _strobeSpeed.value = clamped
        preferences.strobeSpeed = clamped
    }

    fun getStrobeFrequencyHz(): Float {
        return 1f + (_strobeSpeed.value * 11f)
    }

    private fun startStrobe() {
        cancelPattern()
        patternJob = scope.launch {
            if (!ensureFlashOrNotify()) return@launch
            _mode.value = TorchMode.STROBE
            notifyWidgets(true)
            try {
                while (isActive && _mode.value == TorchMode.STROBE) {
                    val hz = getStrobeFrequencyHz()
                    val halfPeriodMs = (500f / hz).toLong().coerceIn(40L, 500L)
                    if (!applyHardwareTorch(true)) break
                    _isLedLit.value = true
                    delay(halfPeriodMs)
                    if (!isActive) break
                    applyHardwareTorch(false)
                    _isLedLit.value = false
                    delay(halfPeriodMs)
                }
            } finally {
                applyHardwareTorch(false)
                _isLedLit.value = false
                if (_mode.value == TorchMode.STROBE) {
                    _mode.value = TorchMode.OFF
                    notifyWidgets(false)
                }
            }
        }
    }

    private fun startSos() {
        cancelPattern()
        // International Morse SOS: ... --- ...
        val unit = 180L
        val pattern = longArrayOf(
            unit, unit, unit, unit, unit, unit * 3,
            unit * 3, unit, unit * 3, unit, unit * 3, unit * 3,
            unit, unit, unit, unit, unit, unit * 7
        )

        patternJob = scope.launch {
            if (!ensureFlashOrNotify()) return@launch
            _mode.value = TorchMode.SOS
            notifyWidgets(true)
            try {
                var idx = 0
                while (isActive && _mode.value == TorchMode.SOS) {
                    val onDuration = pattern[idx % pattern.size]
                    val offDuration = pattern[(idx + 1) % pattern.size]
                    idx = (idx + 2) % pattern.size

                    if (!applyHardwareTorch(true)) break
                    _isLedLit.value = true
                    delay(onDuration)
                    if (!isActive) break
                    applyHardwareTorch(false)
                    _isLedLit.value = false
                    delay(offDuration)
                }
            } finally {
                applyHardwareTorch(false)
                _isLedLit.value = false
                if (_mode.value == TorchMode.SOS) {
                    _mode.value = TorchMode.OFF
                    notifyWidgets(false)
                }
            }
        }
    }

    private fun cancelPattern() {
        patternJob?.cancel()
        patternJob = null
    }

    private fun ensureFlashOrNotify(): Boolean {
        if (flashCameraId == null) {
            discoverFlashCameraInternal()
        }
        if (flashCameraId == null) {
            _errorMessage.tryEmit("No camera flash detected — try Screen Light mode")
            return false
        }
        return true
    }

    private fun applyHardwareTorch(enable: Boolean): Boolean {
        val cid = flashCameraId ?: return false
        return try {
            if (enable && maxStrengthLevel > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val level = (1 + _brightnessRatio.value * (maxStrengthLevel - 1))
                    .roundToInt()
                    .coerceIn(1, maxStrengthLevel)
                cameraManager.turnOnTorchWithStrengthLevel(cid, level)
            } else {
                cameraManager.setTorchMode(cid, enable)
            }
            true
        } catch (_: Exception) {
            if (enable) {
                _errorMessage.tryEmit("Camera flash is currently unavailable")
            }
            false
        }
    }

    private fun notifyWidgets(isActive: Boolean) {
        val intent = Intent(appContext, TorchWidgetProvider::class.java).apply {
            action = TorchWidgetProvider.ACTION_UPDATE_WIDGET_UI
            putExtra(TorchWidgetProvider.EXTRA_IS_ACTIVE, isActive)
        }
        appContext.sendBroadcast(intent)
    }

    companion object {
        @Volatile
        private var instance: TorchController? = null

        fun getInstance(context: Context): TorchController {
            return instance ?: synchronized(this) {
                instance ?: TorchController(context.applicationContext).also { instance = it }
            }
        }
    }
}
