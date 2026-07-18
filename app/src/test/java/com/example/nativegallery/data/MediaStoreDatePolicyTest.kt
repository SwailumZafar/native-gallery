package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreDatePolicyTest {
    @Test
    fun captureTimeWinsOverLaterRescanDate() {
        val timestamp = MediaStoreDatePolicy.canonicalTimestampMillis(
            dateTakenMillis = 1_735_689_600_000L,
            dateModifiedSeconds = 1_767_225_600L,
            dateAddedSeconds = 1_767_225_600L
        )

        assertEquals(1_735_689_600_000L, timestamp)
    }

    @Test
    fun modifiedTimeIsUsedWhenCaptureTimeIsMissing() {
        val timestamp = MediaStoreDatePolicy.canonicalTimestampMillis(
            dateTakenMillis = 0L,
            dateModifiedSeconds = 1_767_225_600L,
            dateAddedSeconds = 1_735_689_600L
        )

        assertEquals(1_767_225_600_000L, timestamp)
    }

    @Test
    fun newestFirstIgnoresPageInsertionOrder() {
        val olderRescanned = mediaItem("old", 1_735_689_600_000L)
        val newer = mediaItem("new", 1_767_225_600_000L)

        assertEquals(
            listOf("new", "old"),
            MediaStoreDatePolicy.newestFirst(listOf(olderRescanned, newer)).map { it.id }
        )
    }

    private fun mediaItem(id: String, timestampMillis: Long) = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = id,
        dateLabel = "",
        sortTimestampMillis = timestampMillis
    )
}