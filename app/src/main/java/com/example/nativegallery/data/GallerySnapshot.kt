package com.example.nativegallery.data

import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem

data class GallerySnapshot(
    val mediaItems: List<MediaItem>,
    val albums: List<Album>
)
