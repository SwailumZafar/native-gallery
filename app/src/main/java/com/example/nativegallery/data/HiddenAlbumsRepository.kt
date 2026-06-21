package com.example.nativegallery.data

class HiddenAlbumsRepository(
    private val startingHiddenAlbumIds: Set<String> = emptySet()
) {
    fun initialHiddenAlbumIds(): Set<String> = startingHiddenAlbumIds
}
