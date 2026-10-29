package com.example.nativegallery.ui.components

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryImageWorkSchedulerTest {
    @Test
    fun aFailedThumbnailWaitsForALibraryUpdateAndDoesNotMissAnEarlyNotification() = runBlocking {
        val failedAt = GalleryImageWorkScheduler.mediaRevision()
        var retried = false
        val retry = launch(start = CoroutineStart.UNDISPATCHED) {
            GalleryImageWorkScheduler.awaitLibraryChange(failedAt)
            retried = true
        }
        assertFalse(retried)
        GalleryImageWorkScheduler.notifyLibrarySettled()
        retry.join()
        assertTrue(retried)
        GalleryImageWorkScheduler.awaitLibraryChange(failedAt)
    }

    @Test
    fun decodesResumeOnlyAfterBothNavigationAndViewerSettle() = runBlocking {
        GalleryImageWorkScheduler.setNavigationMoving(true)
        GalleryImageWorkScheduler.setViewerMoving(true)
        var resumed = false
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            GalleryImageWorkScheduler.awaitIdle()
            resumed = true
        }
        try {
            assertFalse(resumed)
            GalleryImageWorkScheduler.setNavigationMoving(false)
            assertFalse(resumed)
            GalleryImageWorkScheduler.setViewerMoving(false)
            request.join()
            assertTrue(resumed)
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
            GalleryImageWorkScheduler.setViewerMoving(false)
            request.cancelAndJoin()
        }
    }

    @Test
    fun cancelledOffscreenRequestDoesNotPreventOtherImagesLoading() = runBlocking {
        GalleryImageWorkScheduler.setNavigationMoving(true)
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            GalleryImageWorkScheduler.awaitIdle()
        }
        try {
            request.cancelAndJoin()
            GalleryImageWorkScheduler.setNavigationMoving(false)
            GalleryImageWorkScheduler.awaitIdle()
            assertTrue(request.isCancelled)
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
        }
    }
}
