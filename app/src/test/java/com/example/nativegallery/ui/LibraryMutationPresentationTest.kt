package com.example.nativegallery.ui

import com.example.nativegallery.data.GalleryPrivacyFilter
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.model.RecentlyDeletedMedia
import org.junit.Assert.*
import org.junit.Test

class LibraryMutationPresentationTest {
    private val photo = MediaItem("photo", "camera", MediaType.Photo, "Photo", "Today")
    private val video = MediaItem("video", "camera", MediaType.Video, "Video", "Today", isVideo = true,
        fileSizeBytes = 512_000, dateModifiedMillis = 123_000)
    private val album = Album("camera", "Camera", 2, listOf(photo.id, video.id), coverMediaItem = photo)

    @Test fun deletingOrLockingAPhotoPromotesTheVideoCoverWithItsDecoderAndFingerprint() {
        val filtered = GalleryPrivacyFilter.visibleAlbums(listOf(album), listOf(photo, video), listOf(video), emptySet()).single()
        assertEquals(listOf(video.id), filtered.coverMediaIds)
        assertTrue(filtered.hasVideoBadge)
        assertSame(video, filtered.coverMediaItem)
        assertEquals(123_000L, filtered.coverMediaItem?.dateModifiedMillis)
    }

    @Test fun restoringAPhotoReplacesTheVideoCoverTypeAgain() {
        val videoAlbum = album.copy(hasVideoBadge = true, coverMediaItem = video)
        val restored = GalleryPrivacyFilter.visibleAlbums(listOf(videoAlbum), listOf(photo, video), listOf(photo, video), emptySet()).single()
        assertFalse(restored.hasVideoBadge)
        assertSame(photo, restored.coverMediaItem)
    }

    @Test fun anEmptyAllPhotosAlbumDoesNotKeepADeletedVideoIdentity() {
        val empty = GalleryPrivacyFilter.visibleAlbums(listOf(album.copy(isAllPhotos = true, hasVideoBadge = true)),
            listOf(video), emptyList(), emptySet()).single()
        assertEquals(0, empty.itemCount)
        assertNull(empty.coverMediaItem)
        assertFalse(empty.hasVideoBadge)
    }

    @Test fun confirmedTrashRemovalRetainsDepartingRowsAlongsideUpdatedSurvivors() {
        val deletedPhoto = RecentlyDeletedMedia(photo, 30)
        val deletedVideo = RecentlyDeletedMedia(video, 20)
        val updatedVideo = deletedVideo.copy(mediaItem = video.copy(title = "Updated video"))
        val animationRows = retainDepartingTrashItems(listOf(deletedPhoto, deletedVideo), listOf(updatedVideo))
        assertEquals(listOf(deletedPhoto, updatedVideo), animationRows)
        assertEquals(listOf(deletedPhoto, deletedVideo), retainDepartingTrashItems(listOf(deletedPhoto, deletedVideo), emptyList()))
    }

    @Test fun videoClosingUsesExactlyTheHeldDragCornerRadiusAfterScaling() {
        assertEquals(0f, videoDismissStartRadius(1f), 0.001f)
        assertEquals(28f * 0.5f * 0.86f, videoDismissStartRadius(0.86f), 0.001f)
        assertEquals(28f * 0.72f, videoDismissStartRadius(0.72f), 0.001f)
    }

    @Test fun frozenVideoFrameMatchesFillCropInBothOrientations() {
        assertEquals(VideoFrameCrop(656, 0, 608, 1080), videoFrameCrop(1920, 1080, 9f / 16f))
        assertEquals(VideoFrameCrop(0, 236, 1920, 608), videoFrameCrop(1920, 1080, 1920f / 608f))
        assertEquals(VideoFrameCrop(0, 0, 1920, 1080), videoFrameCrop(1920, 1080, 16f / 9f))
    }
}
