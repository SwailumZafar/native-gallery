package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosScrollPolicyTest {
    @Test
    fun normalScrollingSkipsContinuousTileBoundsTracking() {
        assertFalse(shouldTrackPhotoTileBounds(selectionMode = false, scrolling = true))
    }

    @Test
    fun settledGridRefreshesBoundsForViewerTransitions() {
        assertTrue(shouldTrackPhotoTileBounds(selectionMode = false, scrolling = false))
    }

    @Test
    fun selectionDragKeepsHitTestingBoundsWhileScrolling() {
        assertTrue(shouldTrackPhotoTileBounds(selectionMode = true, scrolling = true))
    }

    @Test
    fun uncachedThumbnailWorkWaitsUntilScrollingSettles() {
        assertTrue(shouldDeferPhotoThumbnailLoads(scrolling = true))
        assertFalse(shouldDeferPhotoThumbnailLoads(scrolling = false))
    }
}
