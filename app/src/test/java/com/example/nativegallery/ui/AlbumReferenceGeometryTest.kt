package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class AlbumReferenceGeometryTest {
    // Actual screen is x=46..434 inside a 478x850 letterboxed video.
    private val cover = Rect(16f, 304f, 128f, 416f)

    @Test fun closingTracksMeasuredThumbnailLandmarksFromTheVideo() {
        // elapsed fraction, observed first-thumbnail scale and y in the source video.
        val landmarks = listOf(
            Triple(.168f, .8475f, 164.5f), Triple(.336f, .6675f, 221.5f),
            Triple(.392f, .5800f, 246f), Triple(.504f, .4900f, 277f),
            Triple(.616f, .3900f, 300.5f)
        )
        for ((elapsed, scale, thumbnailY) in landmarks) {
            val p = 1f - ReferenceAlbumClosing.transform(elapsed)
            val frame = albumPageFrame(388f, 850f, cover, p, 22f)
            assertEquals(scale, frame.scale, .02f)
            assertEquals(thumbnailY, frame.bounds.top + 115f * frame.scale, 6f)
        }
    }

    @Test fun openingTracksTheMeasuredExpansionRatherThanTheOldSpringTail() {
        for ((elapsed, scale) in listOf(.215f to .55f, .465f to .8375f, .715f to .9775f)) {
            val frame = albumPageFrame(388f, 850f, cover, ReferenceAlbumOpening.transform(elapsed), 22f)
            assertEquals(scale, frame.scale, .025f)
        }
    }

    @Test fun curvesAndBoundsStayMonotonicAndLandExactlyOnBothEndpoints() {
        for (curve in listOf(ReferenceAlbumOpening, ReferenceAlbumClosing)) {
            var previous = 0f
            repeat(1001) {
                val p = curve.transform(it / 1000f)
                assertTrue(p >= previous)
                assertTrue(p in 0f..1f)
                previous = p
            }
            assertEquals(0f, curve.transform(0f), 0f)
            assertEquals(1f, curve.transform(1f), 0f)
        }
        for ((w, h) in listOf(388f to 850f, 850f to 388f)) {
            val start = albumPageFrame(w, h, cover, 1f, 22f)
            assertEquals(Rect(0f, 0f, w, h), start.bounds)
            assertEquals(1f, start.scale, 0f)
            val end = albumPageFrame(w, h, cover, 0f, 22f)
            assertEquals(cover, end.bounds)
            assertEquals(0f, end.alpha, 0f)
            repeat(101) {
                val frame = albumPageFrame(w, h, cover, it / 100f, 22f)
                assertTrue(frame.bounds.width / frame.scale <= w + .001f)
                assertTrue(frame.bounds.height / frame.scale <= h + .001f)
            }
        }
    }
}
