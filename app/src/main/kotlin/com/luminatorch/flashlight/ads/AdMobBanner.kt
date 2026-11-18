package com.luminatorch.flashlight.ads

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.luminatorch.flashlight.BuildConfig
import com.luminatorch.flashlight.LuminaApp
import com.luminatorch.flashlight.theme.LuminaColors

@Composable
fun AdMobAdaptiveBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
) {
    val adsReady by LuminaApp.adsInitialized.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)

    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    val bannerHeightDp = remember(adSize) {
        adSize.height.coerceAtLeast(50).dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(LuminaColors.Obsidian)
            .navigationBarsPadding()
            .height(bannerHeightDp),
        contentAlignment = Alignment.Center
    ) {
        if (adsReady) {
            AdMobBannerContent(
                adUnitId = adUnitId,
                adSize = adSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeightDp)
            )
        }
    }
}

@Composable
private fun AdMobBannerContent(
    adUnitId: String,
    adSize: AdSize,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val adView = remember(adUnitId, adSize) {
        AdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.adUnitId = adUnitId
            setAdSize(adSize)
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = {
            adView.apply {
                post { loadAd(AdRequest.Builder().build()) }
            }
        },
        modifier = modifier
    )
}
