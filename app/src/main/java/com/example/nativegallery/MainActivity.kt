package com.example.nativegallery

import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nativegallery.ui.GalleryApp
import com.example.nativegallery.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestRefreshRate()
        setContent {
            GalleryTheme {
                GalleryApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferHighestRefreshRate()
    }

    private fun preferHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

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
    }
}
