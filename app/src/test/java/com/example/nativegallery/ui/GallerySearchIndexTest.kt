package com.example.nativegallery.ui

import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class GallerySearchIndexTest {
    private val cameraPhoto = media(
        id = "camera-photo",
        albumId = "camera",
        title = "Summer Morning",
        dateLabel = "Yesterday",
        mimeType = "image/jpeg"
    )
    private val downloadVideo = media(
        id = "download-video",
        albumId = "downloads",
        title = "Project Demo",
        dateLabel = "June 2026",
        type = MediaType.Video,
        mimeType = "video/mp4"
    )
    private val albums = listOf(
        Album(
            id = "all",
            name = "All photos",
            itemCount = 2,
            coverMediaIds = listOf(cameraPhoto.id),
            isAllPhotos = true
        ),
        Album(
            id = "camera",
            name = "Camera",
            itemCount = 1,
            coverMediaIds = listOf(cameraPhoto.id)
        ),
        Album(
            id = "downloads",
            name = "Downloads",
            itemCount = 1,
            coverMediaIds = listOf(downloadVideo.id)
        )
    )
    private val index = GallerySearchIndex.build(
        mediaItems = listOf(cameraPhoto, downloadVideo),
        albums = albums
    )

    @Test
    fun blankQuery_preservesOriginalMediaAndAlbumOrder() {
        val result = index.search("   ")

        assertEquals(listOf(cameraPhoto.id, downloadVideo.id), result.mediaItems.map { it.id })
        assertEquals(listOf("all", "camera", "downloads"), result.albums.map { it.id })
    }

    @Test
    fun mediaSearch_matchesMetadataAndAlbumNamesCaseInsensitively() {
        assertEquals(
            listOf(cameraPhoto.id),
            index.search("SUMMER").mediaItems.map { it.id }
        )
        assertEquals(
            listOf(downloadVideo.id),
            index.search("video/mp4").mediaItems.map { it.id }
        )
        assertEquals(
            listOf(cameraPhoto.id),
            index.search("camera").mediaItems.map { it.id }
        )
    }

    @Test
    fun albumSearch_includesAllPhotosAndTheMatchingChildAlbum() {
        val result = index.search("project")

        assertEquals(listOf(downloadVideo.id), result.mediaItems.map { it.id })
        assertEquals(listOf("all", "downloads"), result.albums.map { it.id })
    }

    @Test
    fun unmatchedQuery_returnsEmptyResults() {
        val result = index.search("does-not-exist")

        assertEquals(emptyList<MediaItem>(), result.mediaItems)
        assertEquals(emptyList<Album>(), result.albums)
    }

    private fun media(
        id: String,
        albumId: String,
        title: String,
        dateLabel: String,
        type: MediaType = MediaType.Photo,
        mimeType: String
    ) = MediaItem(
        id = id,
        albumId = albumId,
        type = type,
        title = title,
        dateLabel = dateLabel,
        mimeType = mimeType,
        isVideo = type == MediaType.Video
    )
}
