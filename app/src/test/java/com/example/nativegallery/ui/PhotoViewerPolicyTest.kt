package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Offset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerPolicyTest {
    @Test
    fun filmstripWindowIsStableAndBoundedAroundSelection() {
        assertEquals(0..6, viewerFilmstripWindow(itemCount = 20, selectedIndex = 0))
        assertEquals(0..6, viewerFilmstripWindow(itemCount = 20, selectedIndex = 3))
        assertEquals(7..13, viewerFilmstripWindow(itemCount = 20, selectedIndex = 10))
        assertEquals(13..19, viewerFilmstripWindow(itemCount = 20, selectedIndex = 19))
        assertEquals(0..2, viewerFilmstripWindow(itemCount = 3, selectedIndex = 1))
        assertTrue(viewerFilmstripWindow(itemCount = 0, selectedIndex = 0).isEmpty())
    }
    @Test
    fun videoSideGestureNeedsClearVerticalIntent() {
        assertFalse(shouldActivateVideoSideGesture(Offset(2f, 14f), touchSlop = 8f))
        assertFalse(shouldActivateVideoSideGesture(Offset(28f, 24f), touchSlop = 8f))
        assertTrue(shouldActivateVideoSideGesture(Offset(4f, 22f), touchSlop = 8f))
    }
    @Test
    fun videoSeekIgnoresMatroskaFallbackToStart() {
        assertFalse(
            isVideoSeekAcknowledged(
                requestedPositionMs = 120_000,
                reportedPositionMs = 0,
                durationMs = 600_000
            )
        )
        assertTrue(
            isVideoSeekAcknowledged(
                requestedPositionMs = 120_000,
                reportedPositionMs = 116_000,
                durationMs = 600_000
            )
        )
        assertTrue(
            isVideoSeekAcknowledged(
                requestedPositionMs = 0,
                reportedPositionMs = 0,
                durationMs = 600_000
            )
        )
    }

    @Test
    fun videoVolumeUsesTheCurrentMediaStreamAndCanRaiseItFromZero() {
        assertEquals(0f, normalizedMediaVolume(currentVolume = 0, maximumVolume = 16), 0.001f)
        assertEquals(0.25f, normalizedMediaVolume(currentVolume = 4, maximumVolume = 16), 0.001f)
        assertEquals(8, mediaStreamVolumeLevel(targetVolume = 0.5f, maximumVolume = 16))
        assertEquals(1f, localPlayerGain(targetVolume = 0.5f, systemVolumeApplied = true), 0.001f)
        assertEquals(0.5f, localPlayerGain(targetVolume = 0.5f, systemVolumeApplied = false), 0.001f)
    }

    @Test
    fun backgroundedVideoCannotKeepPlayingOrAutoplayOnResume() {
        assertFalse(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = true,
                lifecycleResumed = false
            )
        )
        assertFalse(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = false,
                lifecycleResumed = true
            )
        )
        assertTrue(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = true,
                lifecycleResumed = true
            )
        )
    }

    @Test
    fun compactLandscapeKeepsActionsAndFilmstripOutOfTheMediaStage() {
        assertTrue(isViewerCompactLandscape(screenWidthDp = 800, screenHeightDp = 360))
        assertFalse(isViewerCompactLandscape(screenWidthDp = 360, screenHeightDp = 800))
        assertFalse(isViewerCompactLandscape(screenWidthDp = 1000, screenHeightDp = 700))

        assertFalse(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = false,
                compactLandscape = true
            )
        )
        assertTrue(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = false,
                compactLandscape = false
            )
        )
        assertFalse(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = true,
                compactLandscape = false
            )
        )
        assertEquals(62, viewerBottomActionContentHeightDp(showFilmstrip = false))
        assertEquals(122, viewerBottomActionContentHeightDp(showFilmstrip = true))
    }

    @Test
    fun viewerControlsStayCenteredAndBoundedOnExpandedWidths() {
        assertEquals(336, viewerVideoControlMaxWidthDp(screenWidthDp = 360))
        assertEquals(560, viewerVideoControlMaxWidthDp(screenWidthDp = 700))
        assertEquals(640, viewerVideoControlMaxWidthDp(screenWidthDp = 1200))
    }
}
