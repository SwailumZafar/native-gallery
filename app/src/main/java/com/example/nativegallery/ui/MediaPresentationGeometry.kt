package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem

/** Grid posters may be square crops; they must never override original media dimensions. */
internal fun mediaPresentationGeometry(item: MediaItem, previewWidth: Int?, previewHeight: Int?): MediaItem {
    if ((item.width ?: 0) > 0 && (item.height ?: 0) > 0) return item
    if (item.isVideo) return item.copy(width = 16, height = 9)
    return if ((previewWidth ?: 0) > 0 && (previewHeight ?: 0) > 0) {
        item.copy(width = previewWidth, height = previewHeight)
    } else item
}
