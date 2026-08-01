package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosScrollPolicyTest {
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
    fun uncachedThumbnailWorkWaitsUntilScrollingSettles() {
        assertTrue(shouldDeferPhotoThumbnailLoads(scrolling = true))
        assertFalse(shouldDeferPhotoThumbnailLoads(scrolling = false))
    }
}
