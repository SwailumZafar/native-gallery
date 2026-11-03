package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class AlbumCloseMotionTest {
    private val cover = Rect(60f, 280f, 180f, 400f)

    @Test fun pageStartsAtItsExactFullScreenSizeAndEndsAtItsOwnCover() {
        val start = albumCloseFrame(480f, 850f, cover, 1f, 22f)
        assertEquals(0f, start.left, 0f)
        assertEquals(0f, start.top, 0f)
        assertEquals(1f, start.scale, 0f)
        assertEquals(1f, start.alpha, 0f)
        assertEquals(0f, start.cornerRadius, 0f)
        val end = albumCloseFrame(480f, 850f, cover, 0f, 22f)
        assertEquals(cover.left, end.left, 0f)
        assertEquals(cover.top, end.top, 0f)
        assertEquals(cover.width, end.width, 0f)
        assertEquals(cover.height, end.height, 0f)
        assertEquals(0f, end.alpha, 0f)
    }

    @Test fun gridScalesAsAWholeAndOnlyCrossfadesAtTheFinalHandoff() {
        val middle = albumCloseFrame(480f, 850f, cover, 0.5f, 22f)
        assertEquals(0.625f, middle.scale, 0.0001f)
        assertEquals(1f, middle.alpha, 0f)
        assertTrue(middle.width < 480f)
        // The clipping rectangle is expressed in scaled page coordinates, not full-size pixels.
        assertTrue(middle.width / middle.scale <= 480f)
        assertTrue(middle.height / middle.scale <= 850f)
        val handoff = albumCloseFrame(480f, 850f, cover, 0.1f, 22f)
        assertEquals(0.5f, handoff.alpha, 0.0001f)
        assertTrue(handoff.scale < middle.scale)
    }

    @Test fun portraitLandscapeAndOffscreenReturnTargetsStayMonotonicAndCovered() {
        for ((w, h) in listOf(480f to 850f, 850f to 480f)) {
            for (bounds in listOf(cover, Rect(-30f, 80f, 240f, 180f), Rect(200f, 100f, 300f, 400f))) {
                var lastScale = 0f
                repeat(101) { step ->
                    val frame = albumCloseFrame(w, h, bounds, step / 100f, 22f)
                    assertTrue(frame.scale >= lastScale)
                    assertTrue(frame.width / frame.scale <= w + 0.001f)
                    assertTrue(frame.height / frame.scale <= h + 0.001f)
                    assertTrue(frame.alpha in 0f..1f)
                    lastScale = frame.scale
                }
            }
        }
    }
}
