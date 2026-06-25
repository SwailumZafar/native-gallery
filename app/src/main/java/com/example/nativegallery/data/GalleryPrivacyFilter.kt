package com.example.nativegallery.data

import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem

object GalleryPrivacyFilter {
    fun availableMedia(
        mediaItems: List<MediaItem>,
        removedMediaIds: Set<String>
    ): List<MediaItem> {
        if (removedMediaIds.isEmpty()) return mediaItems
        return mediaItems.filterNot { removedMediaIds.contains(it.id) }
    }

    fun visibleMedia(
        mediaItems: List<MediaItem>,
        hiddenAlbumIds: Set<String>,
        lockedMediaIds: Set<String>
    ): List<MediaItem> {
        if (hiddenAlbumIds.isEmpty() && lockedMediaIds.isEmpty()) return mediaItems
        return mediaItems.filterNot { mediaItem ->
            hiddenAlbumIds.contains(mediaItem.albumId) || lockedMediaIds.contains(mediaItem.id)
        }
    }

    fun hiddenManageableAlbums(
        albums: List<Album>,
        mediaItems: List<MediaItem>
    ): List<Album> {
        val mediaByAlbum = mediaItems.groupBy { it.albumId }
        return albums
            .filterNot { it.isAllPhotos }
            .map { album ->
                val albumMedia = mediaByAlbum[album.id].orEmpty()
                if (albumMedia.isEmpty()) {
                    album
                } else {
                    val cover = albumMedia.first()
                    album.copy(
                        itemCount = albumMedia.size,
                        coverMediaIds = albumMedia.take(4).map { it.id },
                        coverRes = cover.imageRes ?: album.coverRes,
                        coverUri = cover.contentUri ?: album.coverUri
                    )
                }
            }
    }

    fun visibleAlbums(
        albums: List<Album>,
        allMedia: List<MediaItem>,
        visibleMedia: List<MediaItem>,
        hiddenAlbumIds: Set<String>
    ): List<Album> {
        val allAlbumIds = allMedia.map { it.albumId }.toSet()
        val mediaByAlbum = visibleMedia.groupBy { it.albumId }
        return albums.mapNotNull { album ->
            when {
                album.isAllPhotos -> {
                    val cover = visibleMedia.firstOrNull()
                    album.copy(
                        itemCount = visibleMedia.size,
                        coverMediaIds = visibleMedia.take(4).map { it.id },
                        coverRes = cover?.imageRes ?: album.coverRes,
                        coverUri = cover?.contentUri ?: album.coverUri
                    )
                }
                hiddenAlbumIds.contains(album.id) -> null
                allAlbumIds.contains(album.id) -> {
                    val albumMedia = mediaByAlbum[album.id].orEmpty()
                    if (albumMedia.isEmpty()) {
                        null
                    } else {
                        val cover = albumMedia.first()
                        album.copy(
                            itemCount = albumMedia.size,
                            coverMediaIds = albumMedia.take(4).map { it.id },
                            coverRes = cover.imageRes ?: album.coverRes,
                            coverUri = cover.contentUri ?: album.coverUri
                        )
                    }
                }
                else -> album
            }
        }
    }

    fun mediaForAlbum(album: Album, mediaItems: List<MediaItem>): List<MediaItem> {
        return if (album.isAllPhotos) {
            mediaItems
        } else {
            mediaItems.filter { it.albumId == album.id }
        }
    }
}
