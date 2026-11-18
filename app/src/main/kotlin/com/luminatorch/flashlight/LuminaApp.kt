package com.luminatorch.flashlight

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LuminaApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Initialize Google Mobile Ads SDK off the main thread and signal readiness
        appScope.launch {
            MobileAds.initialize(this@LuminaApp) {
                _adsInitialized.value = true
            }
        }
    }

    companion object {
        private val _adsInitialized = MutableStateFlow(false)
        val adsInitialized: StateFlow<Boolean> = _adsInitialized.asStateFlow()
    }
}
