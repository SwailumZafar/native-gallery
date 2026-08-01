package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class PhotoViewerCompactLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactLandscapeViewportKeepsEveryViewerActionOnScreen() {
        val mediaItem = MediaItem(
            id = "compact-landscape-photo",
            albumId = "test",
            type = MediaType.Photo,
            title = "Compact landscape fixture",
            dateLabel = "Test"
        )
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .size(width = CompactWidth, height = CompactHeight)
                        .testTag(ViewportTag)
                ) {
                    PhotoViewerOverlay(
                        mediaItems = listOf(mediaItem),
                        mediaItem = mediaItem,
                        visible = true,
                        onClose = { _, _, _ -> },
                        onDelete = { _, _ -> }
                    )
                }
            }
        }

        val viewport = composeRule.onNodeWithTag(ViewportTag).fetchSemanticsNode().boundsInRoot
        val mediaStage = composeRule.onNodeWithTag(ViewerMediaStageTestTag).fetchSemanticsNode().boundsInRoot
        val actionSection = composeRule.onNodeWithTag(ViewerActionSectionTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The media stage must end before the separate action section begins",
            mediaStage.bottom <= actionSection.top
        )
        assertTrue("The action section should span the viewer width", actionSection.width >= viewport.width * 0.95f)

        ViewerActions.forEach { action ->
            composeRule.onNodeWithContentDescription(action).assertIsDisplayed()
            val actionBounds = composeRule
                .onNodeWithContentDescription(action)
                .fetchSemanticsNode()
                .boundsInRoot
            assertTrue("$action must fit inside the compact landscape viewport", viewport.contains(actionBounds))
        }
    }

    private fun Rect.contains(other: Rect): Boolean {
        return other.left >= left &&
            other.top >= top &&
            other.right <= right &&
            other.bottom <= bottom
    }

    private companion object {
        val CompactWidth = 360.dp
        val CompactHeight = 240.dp
        const val ViewportTag = "compact-viewer-viewport"
        val ViewerActions = listOf("Favorite", "Edit photo", "Share", "Info", "Lock", "Delete")
    }
}
