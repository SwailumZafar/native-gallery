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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.view.WindowCompat
import com.example.nativegallery.data.GallerySettings
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
            var renderedDarkTheme by remember { mutableStateOf(darkTheme) }
            var themeTransitionActive by remember { mutableStateOf(false) }
            val themeTransitionAlpha = remember { Animatable(0f) }
            val previousThemeFrame = rememberGraphicsLayer()
            val onGallerySettingsChange: (GallerySettings) -> Unit =
                remember(settingsRepository) {
                    { nextSettings ->
                        gallerySettings = settingsRepository.save(nextSettings)
                    }
                }

            LaunchedEffect(darkTheme) {
                if (renderedDarkTheme == darkTheme) return@LaunchedEffect

                // The graphics layer already contains the most recently rendered frame. Freeze
                // it before replacing MaterialTheme, then reveal the new frame underneath. This
                // mirrors the browser View Transitions approach without re-composing two copies
                // of GalleryApp or animating every individual color consumer.
                themeTransitionAlpha.snapTo(1f)
                themeTransitionActive = true
                renderedDarkTheme = darkTheme

                // Keep the frozen frame opaque until the replacement theme has completed a draw.
                repeat(2) { withFrameNanos { } }
                themeTransitionAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = ThemeCrossfadeMillis,
                        easing = ThemeCrossfadeEasing
                    )
                )
                themeTransitionActive = false
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !renderedDarkTheme
                    isAppearanceLightNavigationBars = !renderedDarkTheme
                }
            }

            LaunchedEffect(gallerySettings.performanceMode) {
                applyPerformanceMode(gallerySettings.performanceMode)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (themeTransitionActive) {
                            drawContent()
                            previousThemeFrame.alpha = themeTransitionAlpha.value
                            drawLayer(previousThemeFrame)
                        } else {
                            previousThemeFrame.alpha = 1f
                            previousThemeFrame.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(previousThemeFrame)
                        }
                    }
            ) {
                GalleryTheme(darkTheme = renderedDarkTheme) {
                    GalleryApp(
                        settings = gallerySettings,
                        onInitialPresentationReady = ::setGalleryPresentationReady,
                        onSettingsChange = onGallerySettingsChange
                    )
                }
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
        const val ThemeCrossfadeMillis = 360
        val ThemeCrossfadeEasing = CubicBezierEasing(0.20f, 0f, 0f, 1f)
    }
}
