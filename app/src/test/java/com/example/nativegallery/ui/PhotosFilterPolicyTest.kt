package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosFilterPolicyTest {

    private val photo1 = MediaItem(
        id = "p1",
        albumId = "camera",
        type = MediaType.Photo,
        title = "IMG_001.jpg",
        dateLabel = "Today",
        isVideo = false
    )

    private val photo2 = MediaItem(
        id = "p2",
        albumId = "camera",
        type = MediaType.Photo,
        title = "IMG_002.jpg",
        dateLabel = "Today",
        isVideo = false
    )

    private val video1 = MediaItem(
        id = "v1",
        albumId = "camera",
        type = MediaType.Video,
        title = "VID_001.mp4",
        dateLabel = "Today",
        isVideo = true
    )

    private val allItems = listOf(photo1, photo2, video1)
    private val favoriteIds = setOf("p1", "v1")

    @Test
    fun filterAllReturnsAllItems() {
        val result = filterMediaItems(allItems, PhotoGridFilter.All, favoriteIds)
        assertEquals(3, result.size)
        assertEquals(allItems, result)
    }

    @Test
    fun filterPhotosReturnsOnlyPhotos() {
        val result = filterMediaItems(allItems, PhotoGridFilter.Photos, favoriteIds)
        assertEquals(listOf(photo1, photo2), result)
    }

    @Test
    fun filterVideosReturnsOnlyVideos() {
        val result = filterMediaItems(allItems, PhotoGridFilter.Videos, favoriteIds)
        assertEquals(listOf(video1), result)
    }

    @Test
    fun filterFavoritesReturnsOnlyFavoritedItems() {
        val result = filterMediaItems(allItems, PhotoGridFilter.Favorites, favoriteIds)
        assertEquals(listOf(photo1, video1), result)
    }

    @Test
    fun filterChipsHaveAllFourCategories() {
        val labels = PhotoGridFilter.entries.map { it.label }
        assertEquals(listOf("All", "Photos", "Videos", "Favorites"), labels)
    }

    private fun filterMediaItems(
        items: List<MediaItem>,
        filter: PhotoGridFilter,
        favorites: Set<String>
    ): List<MediaItem> = when (filter) {
        PhotoGridFilter.All -> items
        PhotoGridFilter.Photos -> items.filter { !it.isVideo }
        PhotoGridFilter.Videos -> items.filter { it.isVideo }
        PhotoGridFilter.Favorites -> items.filter { favorites.contains(it.id) }
    }
}
