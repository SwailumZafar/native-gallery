package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotosScrollPolicyTest {
    @Test fun movingPrefetchIsSmallerAndFollowsTheDirectionOfTravel() {
        val resting = mediaPrefetchRange(400, 1000, 4)
        val forward = mediaPrefetchRange(400, 1000, 4, scrolling = true, forward = true)
        val backward = mediaPrefetchRange(400, 1000, 4, scrolling = true, forward = false)
        assertTrue(forward.count() < resting.count())
        assertTrue(backward.count() < resting.count())
        assertTrue(400 in forward && 400 in backward)
        assertTrue(forward.last > backward.last)
        assertTrue(backward.first < forward.first)
        assertEquals(0, mediaPrefetchRange(0, 10, 4, scrolling = true).first)
        assertEquals(999, mediaPrefetchRange(999, 1000, 4, scrolling = true).last)
    }

    @Test
    fun normalGridSkipsContinuousTileBoundsTracking() {
        assertFalse(shouldTrackPhotoTileBounds(selectionMode = false, revealInProgress = false))
    }

    @Test
    fun viewerRevealRefreshesBoundsForCloseTransition() {
        assertTrue(shouldTrackPhotoTileBounds(selectionMode = false, revealInProgress = true))
    }

    @Test
    fun selectionDragKeepsHitTestingBoundsWhileScrolling() {
        assertTrue(shouldTrackPhotoTileBounds(selectionMode = true, revealInProgress = false))
    }

    @Test
    fun nearbyPrefetchIncludesSeveralScreensBeyondTheViewport() {
        assertEquals(0..79, mediaPrefetchRange(0, 1000, 4))
        assertEquals(16..111, mediaPrefetchRange(37, 1000, 4))
    }

    @Test fun deepJumpsAndLibraryEdgesStayBounded() {
        assertEquals(9968..9999, mediaPrefetchRange(9999, 10000, 4))
        assertTrue(mediaPrefetchRange(10, 0, 4).isEmpty())
        assertEquals(0..2, mediaPrefetchRange(100, 3, 4))
    }

    @Test fun timelinePrefetchAccountsForShortDateSections() {
        assertEquals(listOf(0, 0, 0, 4, 4, 5, 5, 7), photoGridRowStarts(listOf(5, 3), 4))
    }
}
