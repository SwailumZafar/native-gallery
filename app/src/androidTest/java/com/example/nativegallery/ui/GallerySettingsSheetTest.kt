package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import com.example.nativegallery.data.GallerySettings
import com.example.nativegallery.data.GalleryThemeMode
import com.example.nativegallery.data.MediaManagementAccessState
import com.example.nativegallery.data.MediaManagementAccessStatus
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GallerySettingsSheetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun groupedSettingsKeepThemePlaybackAndDismissalWorking() {
        var settings by mutableStateOf(GallerySettings())
        var visible by mutableStateOf(true)
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize()) {
                    if (visible) GallerySettingsSheet(settings,
                        MediaManagementAccessStatus(MediaManagementAccessState.UnsupportedAndroidVersion), {},
                        { settings = it }, { visible = false })
                }
            }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        saveSheet("settings-dark.png")
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.runOnIdle { assertEquals(GalleryThemeMode.Dark, settings.themeMode) }
        composeRule.onNodeWithText("Autoplay videos").performScrollTo().performClick()
        composeRule.runOnIdle { assertFalse(settings.autoplayVideos) }
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertFalse(visible) }
    }

    @Test fun lightSettingsStayReadableAndDismiss() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            GalleryTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    if (visible) GallerySettingsSheet(GallerySettings(themeMode = GalleryThemeMode.Light),
                        MediaManagementAccessStatus(MediaManagementAccessState.UnsupportedAndroidVersion), {}, {},
                        { visible = false })
                }
            }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        saveSheet("settings-light.png")
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertFalse(visible) }
    }

    private fun saveSheet(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = composeRule.onNodeWithTag("gallery-settings-sheet").captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
