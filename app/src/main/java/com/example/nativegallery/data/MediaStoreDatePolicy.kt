package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem

internal object MediaStoreDatePolicy {
    fun canonicalTimestampMillis(
        dateTakenMillis: Long,
        dateModifiedSeconds: Long,
        dateAddedSeconds: Long
    ): Long {
        return when {
            dateTakenMillis > 0L -> dateTakenMillis
            dateModifiedSeconds > 0L -> dateModifiedSeconds * 1000L
            dateAddedSeconds > 0L -> dateAddedSeconds * 1000L
            else -> 0L
        }
    }

    fun newestFirst(mediaItems: List<MediaItem>): List<MediaItem> {
        return mediaItems.sortedWith(
            compareByDescending<MediaItem> { it.sortTimestampMillis }
                .thenByDescending { it.id }
        )
    }
}