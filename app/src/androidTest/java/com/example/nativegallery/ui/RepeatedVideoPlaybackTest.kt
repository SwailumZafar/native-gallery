package com.example.nativegallery.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses a synthetic clip in the app's cache; never touches the user's video library. */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class RepeatedVideoPlaybackTest {
    @org.junit.Before fun keepTestWindowAwake() = TestScreenAwake.enable()
    @get:Rule val composeRule = createComposeRule()

    @Test fun videoStartsAndAdvancesAcrossRepeatedReopens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.cacheDir, "reopen-video-test.mp4")
        instrumentation.context.assets.open("reopen-video.mp4").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val item = MediaItem("reopen-test", "test", MediaType.Video, "Reopen fixture", "Test",
            contentUri = Uri.fromFile(file), isVideo = true, width = 160, height = 96,
            durationMillis = 8_000, mimeType = "video/mp4")
        var visible by mutableStateOf(false)
        try {
            composeRule.setContent {
                GalleryTheme(darkTheme = true) {
                    if (visible) PhotoViewerOverlay(listOf(item), item, true,
                        onClose = { _, _, _ -> visible = false }, onDelete = { _, _ -> },
                        startVideosMuted = true)
                }
            }
            // Playback progress deliberately never becomes idle. Drive Compose frames explicitly
            // instead of asking Espresso to advance the clock until a playing video is idle.
            composeRule.mainClock.autoAdvance = false
            repeat(4) { cycle ->
                composeRule.runOnUiThread { visible = true }
                composeRule.waitUntil(10_000) {
                    composeRule.mainClock.advanceTimeBy(32)
                    composeRule.onAllNodesWithContentDescription("Pause video").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.waitUntil(5_000) {
                    composeRule.mainClock.advanceTimeBy(32)
                    composeRule.onAllNodesWithText("0:01").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.runOnUiThread { visible = false }
                composeRule.mainClock.advanceTimeBy(32)
                composeRule.waitForIdle()
                // Also cover a reopen beyond the old two-second codec-release deadline.
                if (cycle == 1) Thread.sleep(2_500)
            }
        } finally {
            composeRule.runOnUiThread { visible = false }
            composeRule.mainClock.advanceTimeBy(32)
            composeRule.waitForIdle()
            file.delete()
        }
    }

    @Test fun swipingFromPhotoToVideoStartsPlaybackAcrossRepeatedPageChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.cacheDir, "pager-video-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val clip = File(directory, "clip.mp4")
        instrumentation.context.assets.open("reopen-video.mp4").use { input -> clip.outputStream().use { input.copyTo(it) } }
        val photoFile = File(directory, "photo.png")
        val bitmap = android.graphics.Bitmap.createBitmap(160, 96, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        photoFile.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        val photo = MediaItem("pager-photo", "fixture", MediaType.Photo, "Pager photo fixture", "", contentUri = Uri.fromFile(photoFile), width = 160, height = 96)
        val video = MediaItem("pager-video", "fixture", MediaType.Video, "Pager video fixture", "", contentUri = Uri.fromFile(clip),
            isVideo = true, width = 160, height = 96, durationMillis = 8_000, mimeType = "video/mp4")
        var visible by mutableStateOf(true)
        try {
            composeRule.setContent { GalleryTheme(darkTheme = true) {
                if (visible) PhotoViewerOverlay(listOf(photo, video), photo, true, onClose = { _, _, _ -> visible = false }, onDelete = { _, _ -> }, startVideosMuted = true)
            } }
            composeRule.mainClock.autoAdvance = false
            repeat(3) {
                composeRule.mainClock.advanceTimeBy(400)
                composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput { swipeLeft(durationMillis = 350) }
                composeRule.waitUntil(10_000) {
                    composeRule.mainClock.advanceTimeBy(32)
                    composeRule.onAllNodesWithContentDescription("Pause video").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.waitUntil(5_000) {
                    composeRule.mainClock.advanceTimeBy(32)
                    composeRule.onAllNodesWithText("0:01").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput { swipeRight(durationMillis = 350) }
                composeRule.waitUntil(5_000) {
                    composeRule.mainClock.advanceTimeBy(32)
                    composeRule.onAllNodesWithContentDescription("Pause video").fetchSemanticsNodes().isEmpty()
                }
            }
        } finally {
            composeRule.runOnUiThread { visible = false }; composeRule.mainClock.advanceTimeBy(32); composeRule.waitForIdle()
            directory.deleteRecursively()
        }
    }
}
