package com.luminatorch.flashlight.timer

import android.content.Context
import com.luminatorch.flashlight.hardware.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SleepTimerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    val isActive: Boolean
        get() = _remainingSeconds.value > 0

    fun start(context: Context, totalSeconds: Int, onExpired: (() -> Unit)? = null) {
        cancel()
        if (totalSeconds <= 0) return

        val appContext = context.applicationContext
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch {
            var left = totalSeconds
            while (isActive && left > 0) {
                delay(1000L)
                left -= 1
                _remainingSeconds.value = left
            }
            if (left <= 0) {
                TorchController.getInstance(appContext).stopAll()
                onExpired?.invoke()
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
    }

    fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
