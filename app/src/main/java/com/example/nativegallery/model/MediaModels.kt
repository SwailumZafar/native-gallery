package com.example.nativegallery.model

import android.net.Uri

data class MediaItem(
    val id: String,
    val albumId: String,
    val type: MediaType,
    val title: String,
    val dateLabel: String,
    val imageRes: Int? = null,
    val contentUri: Uri? = null,
    val isVideo: Boolean = false,
    val durationLabel: String? = null
)

data class RecentlyDeletedMedia(
    val mediaItem: MediaItem,
    val deletedAtMillis: Long
)

enum class MediaType {
    Photo,
    Video
}

data class Album(
    val id: String,
    val name: String,
    val itemCount: Int,
    val coverMediaIds: List<String>,
    val coverRes: Int? = null,
    val coverUri: Uri? = null,
    val isHidden: Boolean = false,
    val isAllPhotos: Boolean = false,
    val hasVideoBadge: Boolean = false
)

enum class AlbumLayoutMode {
    Basic,
    BigTiles
}

data class HiddenAlbumState(
    val albumId: String,
    val isHidden: Boolean
)


