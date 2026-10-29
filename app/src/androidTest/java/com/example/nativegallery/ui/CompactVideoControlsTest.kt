package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactVideoControlsTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun landscapeTransportUsesOneRowAndKeepsTheSecondaryControlsAvailable() {
        var rewind = 0
        var forward = 0
        var fit = 0
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                Box(Modifier.requiredWidth(640.dp)) {
                    CompactVideoControlDeck(positionMs = 12_000, visualPosition = { 12_000f }, durationMs = 120_000,
                        onPositionChange = {}, onPositionChangeFinished = {}, isMuted = false, onToggleMute = {},
                        isPlaying = true, onPlayPause = {}, onRewind = { rewind++ }, onForward = { forward++ },
                        videoFillFrame = false, onToggleFill = { fit++ })
                }
            }
        }
        val deck = composeRule.onNodeWithTag("compact-video-deck").getUnclippedBoundsInRoot()
        val seek = composeRule.onNodeWithTag("compact-video-seek").getUnclippedBoundsInRoot()
        assertTrue(deck.bottom - deck.top <= 64.dp)
        assertTrue(seek.right - seek.left >= 48.dp)
        val rewindBounds = composeRule.onNodeWithContentDescription("Rewind 10 seconds").getUnclippedBoundsInRoot()
        assertTrue(rewindBounds.left >= seek.right)
        composeRule.onNodeWithContentDescription("Rewind 10 seconds").performClick()
        composeRule.onNodeWithContentDescription("Forward 10 seconds").performClick()
        composeRule.onNodeWithContentDescription("Fill screen").performClick()
        composeRule.runOnIdle {
            assertEquals(1, rewind)
            assertEquals(1, forward)
            assertEquals(1, fit)
        }
    }
}
