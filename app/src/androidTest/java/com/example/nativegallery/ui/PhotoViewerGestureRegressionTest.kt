package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class PhotoViewerGestureRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun downwardDragDismissesPhotoEvenThoughTapDetectorConsumedDown() {
        var closeRequests = 0
        showViewer(onClose = { closeRequests += 1 })
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.35f), Offset(width * 0.53f, height * 0.8f), 320L)
        }
        composeRule.waitUntil(GestureTimeoutMillis) { closeRequests == 1 }
        composeRule.runOnIdle { assertEquals(1, closeRequests) }
    }

    @Test
    fun upwardDragOpensInformationAndCloseRemainsResponsive() {
        var closeRequests = 0
        showViewer(onClose = { closeRequests += 1 })
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.7f), Offset(width * 0.5f, height * 0.28f), 320L)
        }
        composeRule.onNodeWithContentDescription("Close details").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, closeRequests) }
    }

    @Test
    fun overflowInformationCanBeOpenedAndClosedRepeatedly() {
        showViewer(onClose = {})
        repeat(2) {
            composeRule.onNodeWithContentDescription("More media options").performTouchInput { click(center) }
            composeRule.onNodeWithText("Information").assertIsDisplayed().performClick()
            composeRule.onNodeWithContentDescription("Close details").assertIsDisplayed().performClick()
            composeRule.onNodeWithContentDescription("More media options").assertIsDisplayed()
        }
    }

    @Test
    fun pinchOutSpringsBackWithoutDismissingViewer() {
        var closeRequests = 0
        showViewer(onClose = { closeRequests += 1 })

        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            pinch(
                start0 = center + Offset(-120f, 0f),
                end0 = center + Offset(-18f, 0f),
                start1 = center + Offset(120f, 0f),
                end1 = center + Offset(18f, 0f),
                durationMillis = 320L
            )
        }

        waitForZoomState("Fit")
        composeRule.runOnIdle { assertEquals(0, closeRequests) }
        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).assertIsDisplayed()
    }

    @Test
    fun doubleTapTogglesZoomInAndBackOutWithoutClosing() {
        var closeRequests = 0
        showViewer(onClose = { closeRequests += 1 })
        composeRule.onNodeWithContentDescription("Favorite").assertIsDisplayed()

        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            doubleClick(center)
        }
        waitForZoomState("Zoomed")

        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            doubleClick(center)
        }
        waitForZoomState("Fit")

        composeRule.runOnIdle { assertEquals(0, closeRequests) }
        composeRule.onNodeWithContentDescription("Favorite").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
    }

    @Test
    fun pinchZoomAndSingleFingerPanKeepThePhotoOpen() {
        var closeRequests = 0
        showViewer(onClose = { closeRequests += 1 })
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            pinch(
                start0 = center + Offset(-35f, 0f),
                end0 = center + Offset(-105f, 0f),
                start1 = center + Offset(35f, 0f),
                end1 = center + Offset(105f, 0f),
                durationMillis = 320L
            )
        }
        waitForZoomState("Zoomed")
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput {
            swipe(center, center + Offset(90f, 60f), durationMillis = 240L)
        }
        waitForZoomState("Zoomed")
        composeRule.runOnIdle { assertEquals(0, closeRequests) }
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).performTouchInput { doubleClick(center) }
        waitForZoomState("Fit")
        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
    }

    private fun showViewer(onClose: () -> Unit) {
        val item = MediaItem(
            id = "gesture-photo",
            albumId = "test",
            type = MediaType.Photo,
            title = "Gesture fixture",
            dateLabel = "Test"
        )
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(width = ViewerWidth, height = ViewerHeight)
                        .testTag(ViewportTag)
                ) {
                    PhotoViewerOverlay(
                        mediaItems = listOf(item),
                        mediaItem = item,
                        visible = true,
                        onClose = { _, _, _ -> onClose() },
                        onDelete = { _, _ -> }
                    )
                }
            }
        }
        composeRule.onNodeWithTag(ViewerZoomableMediaTestTag).assertIsDisplayed()
    }

    private fun waitForZoomState(expected: String) {
        composeRule.waitUntil(GestureTimeoutMillis) {
            composeRule.onNodeWithTag(ViewerZoomableMediaTestTag)
                .fetchSemanticsNode()
                .config[SemanticsProperties.StateDescription] == expected
        }
    }

    private companion object {
        val ViewerWidth = 360.dp
        val ViewerHeight = 640.dp
        const val ViewportTag = "viewer-gesture-viewport"
        const val GestureTimeoutMillis = 5_000L
    }
}
