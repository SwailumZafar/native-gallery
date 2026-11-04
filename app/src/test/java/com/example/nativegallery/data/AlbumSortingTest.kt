package com.example.nativegallery.data

import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumSortingTest {
    private fun album(id: String, name: String, count: Int, timestamp: Long) = Album(
        id, name, count, emptyList(), coverMediaItem = MediaItem(id, id, MediaType.Photo, name, "",
            sortTimestampMillis = timestamp))
    private val albums = listOf(album("z", "Zebra", 3, 30), album("a", "alpha", 8, 10), album("b", "Beta", 1, 20))
    @Test fun sortModesOrderByNameCountAndLatestMedia() {
        assertEquals(listOf("a", "b", "z"), sortAlbums(albums, AlbumSortMode.NameAscending).map { it.id })
        assertEquals(listOf("z", "b", "a"), sortAlbums(albums, AlbumSortMode.NameDescending).map { it.id })
        assertEquals(listOf("z", "b", "a"), sortAlbums(albums, AlbumSortMode.Newest).map { it.id })
        assertEquals(listOf("a", "b", "z"), sortAlbums(albums, AlbumSortMode.Oldest).map { it.id })
        assertEquals(listOf("a", "z", "b"), sortAlbums(albums, AlbumSortMode.MostItems).map { it.id })
        assertEquals(listOf("b", "z", "a"), sortAlbums(albums, AlbumSortMode.FewestItems).map { it.id })
        assertEquals(albums, sortAlbums(albums, AlbumSortMode.Custom))
    }
    @Test fun allPhotosRemainsFirstAndPinnedAlbumsOverrideSortOrder() {
        val all = album("all", "All photos", 12, 0).copy(isAllPhotos = true)
        val sorted = sortAlbums(albums + all, AlbumSortMode.NameDescending)
        assertEquals("all", sorted.first().id)
        assertEquals("b", AlbumArrangementPolicy.arrangedIds(sorted.map { it.id },
            AlbumArrangementState(pinnedAlbumId = "b")).first())
    }
}
