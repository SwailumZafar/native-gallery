package com.example.nativegallery.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class VideoDismissGestureTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun draggingBeforeTheFirstDecodedFrameKeepsThePosterThroughTheCloseHandoff() {
        val poster = Bitmap.createBitmap(192, 108, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        var closedWith: Bitmap? = null
        showVideo(poster) { closedWith = it }
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput {
            down(Offset(width * 0.5f, height * 0.3f))
            moveBy(Offset(0f, height * 0.14f), 100)
        }
        composeRule.onNodeWithTag("video-dismiss-frame").assertIsDisplayed()
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput {
            moveBy(Offset(0f, height * 0.4f), 160)
            up()
        }
        composeRule.waitUntil(5_000) { closedWith != null }
        composeRule.runOnIdle { assertSame(poster, closedWith) }
    }

    @Test fun aShortUpwardDragOpensVideoInformationBeforeTheFingerLifts() {
        val poster = Bitmap.createBitmap(192, 108, Bitmap.Config.ARGB_8888)
        var closes = 0
        showVideo(poster) { closes++ }
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput {
            down(Offset(width * 0.5f, height * 0.55f))
            moveBy(Offset(0f, -height * 0.03f), 40)
            moveBy(Offset(0f, -height * 0.08f), 50)
        }
        composeRule.onNodeWithContentDescription("Close details").assertIsDisplayed()
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput { up() }
        composeRule.onNodeWithTag("media-details-panel").performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 0.25f), 100)
        }
        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(0, closes) }
    }

    private fun showVideo(poster: Bitmap, onClose: (Bitmap?) -> Unit) {
        // Intentionally unreadable fixture tests immediate gestures before playback is ready.
        val item = MediaItem("gesture-video", "test", MediaType.Video, "Video gesture fixture", "Test",
            contentUri = Uri.parse("content://unavailable.gesture.fixture/video"), isVideo = true,
            width = 192, height = 108)
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                Box(Modifier.size(360.dp, 640.dp)) {
                    PhotoViewerOverlay(listOf(item), item, true, onClose = { _, _, _ -> },
                        onCloseWithMotion = { _, _, _, _, _, frame -> onClose(frame) }, onDelete = { _, _ -> },
                        initialPreview = poster)
                }
            }
        }
    }
}
