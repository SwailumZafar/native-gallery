package com.example.nativegallery.data

import androidx.compose.runtime.Immutable
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem

@Immutable
data class GallerySnapshot(
    val mediaItems: List<MediaItem>,
    val albums: List<Album>
)
