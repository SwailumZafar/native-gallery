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
}