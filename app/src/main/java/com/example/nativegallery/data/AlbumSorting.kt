package com.example.nativegallery.data

import com.example.nativegallery.model.Album
import java.util.Locale

enum class AlbumSortMode(val label: String) {
    Custom("Custom order"), NameAscending("Name: A–Z"), NameDescending("Name: Z–A"),
    Newest("Recently updated"), Oldest("Least recently updated"),
    MostItems("Most items"), FewestItems("Fewest items")
}

internal fun sortAlbums(albums: List<Album>, mode: AlbumSortMode): List<Album> {
    if (mode == AlbumSortMode.Custom) return albums
    val byName = compareBy<Album> { it.name.lowercase(Locale.getDefault()) }.thenBy { it.id }
    val comparator = when (mode) {
        AlbumSortMode.NameDescending -> byName.reversed()
        AlbumSortMode.Newest -> compareByDescending<Album> { it.coverMediaItem?.sortTimestampMillis ?: 0L }.then(byName)
        AlbumSortMode.Oldest -> compareBy<Album> { it.coverMediaItem?.sortTimestampMillis ?: 0L }.then(byName)
        AlbumSortMode.MostItems -> compareByDescending<Album> { it.itemCount }.then(byName)
        AlbumSortMode.FewestItems -> compareBy<Album> { it.itemCount }.then(byName)
        else -> byName
    }
    return albums.filter { it.isAllPhotos } + albums.filterNot { it.isAllPhotos }.sortedWith(comparator)
}
