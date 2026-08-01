package com.example.nativegallery

import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.nativegallery.data.GallerySettingsRepository
import com.example.nativegallery.data.GalleryThemeMode
import com.example.nativegallery.ui.GalleryApp
import com.example.nativegallery.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {
    private var performanceModeEnabled = true
    @Volatile
    private var galleryPresentationReady = false
    private var launchPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    private val launchGateTimeout = Runnable { setGalleryPresentationReady(true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            installDirectLaunchGate()
        }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settingsRepository = remember { GallerySettingsRepository(this) }
            var gallerySettings by remember { mutableStateOf(settingsRepository.initialSettings()) }
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (gallerySettings.themeMode) {
                GalleryThemeMode.System -> systemInDarkTheme
                GalleryThemeMode.Light -> false
                GalleryThemeMode.Dark -> true
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            LaunchedEffect(gallerySettings.performanceMode) {
                applyPerformanceMode(gallerySettings.performanceMode)
            }

            GalleryTheme(darkTheme = darkTheme) {
                GalleryApp(
                    settings = gallerySettings,
                    onInitialPresentationReady = ::setGalleryPresentationReady,
                    onSettingsChange = { nextSettings ->
                        gallerySettings = settingsRepository.save(nextSettings)
                    }
                )
            }
        }
    }

    private fun installDirectLaunchGate() {
        val listener = ViewTreeObserver.OnPreDrawListener { galleryPresentationReady }
        launchPreDrawListener = listener
        window.decorView.viewTreeObserver.addOnPreDrawListener(listener)
        window.decorView.postDelayed(launchGateTimeout, LaunchGateTimeoutMillis)
    }

    private fun setGalleryPresentationReady(ready: Boolean) {
        galleryPresentationReady = ready
        if (!ready) return
        window.decorView.removeCallbacks(launchGateTimeout)
        launchPreDrawListener?.let { listener ->
            val observer = window.decorView.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        }
        launchPreDrawListener = null
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(launchGateTimeout)
        launchPreDrawListener?.let { listener ->
            val observer = window.decorView.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        }
        launchPreDrawListener = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        applyPerformanceMode(performanceModeEnabled)
    }

    private fun applyPerformanceMode(enabled: Boolean) {
        performanceModeEnabled = enabled
        if (enabled) {
            preferHighestRefreshRate()
        } else {
            clearRefreshRatePreference()
        }
    }

    private fun clearRefreshRatePreference() {
        val attributes = window.attributes
        attributes.preferredDisplayModeId = 0
        attributes.preferredRefreshRate = 0f
        window.attributes = attributes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRateBoostOnTouchEnabled(false)
            window.setFrameRatePowerSavingsBalanced(true)
            window.decorView.post {
                requestFrameRate(window.decorView, 0f)
            }
        }
    }

    private fun preferHighestRefreshRate() {

        @Suppress("DEPRECATION")
        val bestMode = windowManager.defaultDisplay.supportedModes
            .maxWithOrNull(
                compareBy<Display.Mode> { it.refreshRate }
                    .thenBy { it.physicalWidth * it.physicalHeight }
            )
            ?: return

        val attributes = window.attributes
        attributes.preferredDisplayModeId = bestMode.modeId
        attributes.preferredRefreshRate = bestMode.refreshRate
        window.attributes = attributes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRateBoostOnTouchEnabled(true)
            window.setFrameRatePowerSavingsBalanced(false)
            window.decorView.post {
                requestFrameRate(window.decorView, bestMode.refreshRate)
            }
        }
    }

    private fun requestFrameRate(view: View, frameRate: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        view.setRequestedFrameRate(frameRate)
        if (view is ViewGroup) {
            repeat(view.childCount) { index ->
                requestFrameRate(view.getChildAt(index), frameRate)
            }
        }
    }

    private companion object {
        const val LaunchGateTimeoutMillis = 80L
    }
}
