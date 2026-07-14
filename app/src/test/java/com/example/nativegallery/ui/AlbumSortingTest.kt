package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumSortingTest {
    private val mediaItems = listOf(
        media("3", "zebra.jpg"),
        media("2", "Alpha.jpg"),
        media("1", "middle.jpg")
    )

    @Test
    fun albumSort_preservesNewestAndReversesOldest() {
        assertEquals(listOf("3", "2", "1"), sortAlbumMedia(mediaItems, AlbumDetailSortMode.Newest).map { it.id })
        assertEquals(listOf("1", "2", "3"), sortAlbumMedia(mediaItems, AlbumDetailSortMode.Oldest).map { it.id })
    }

    @Test
    fun albumSort_nameIsCaseInsensitive() {
        assertEquals(listOf("2", "1", "3"), sortAlbumMedia(mediaItems, AlbumDetailSortMode.Name).map { it.id })
    }

    private fun media(id: String, title: String) = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = title,
        dateLabel = "Today"
    )
}
