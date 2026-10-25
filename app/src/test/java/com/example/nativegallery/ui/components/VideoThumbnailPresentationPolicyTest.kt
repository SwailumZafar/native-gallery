package com.example.nativegallery.ui.components

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoThumbnailPresentationPolicyTest {
    @Test
    fun genericVideoIconNeverFlashesDuringLoadingOrTransitionPreview() {
        assertFalse(shouldShowVideoThumbnailFallback(isVideo = true, loadComplete = false, cachedOnly = false))
        assertFalse(shouldShowVideoThumbnailFallback(isVideo = true, loadComplete = true, cachedOnly = true))
        assertTrue(shouldShowVideoThumbnailFallback(isVideo = true, loadComplete = true, cachedOnly = false))
        assertFalse(shouldShowVideoThumbnailFallback(isVideo = false, loadComplete = true, cachedOnly = false))
    }

    @Test
    fun previewAndGridShareTheSameRevisionButChangedMediaInvalidatesIt() {
        val video = MediaItem(
            id = "video", albumId = "camera", type = MediaType.Video,
            title = "Video.mp4", dateLabel = "Today", fileSizeBytes = 4096L,
            dateModifiedMillis = 100L
        )
        assertEquals(mediaThumbnailCacheRevision(video), mediaThumbnailCacheRevision(video.copy()))
        assertFalse(mediaThumbnailCacheRevision(video) == mediaThumbnailCacheRevision(video.copy(dateModifiedMillis = 101L)))
        assertFalse(mediaThumbnailCacheRevision(video) == mediaThumbnailCacheRevision(video.copy(fileSizeBytes = 4097L)))
    }
}
