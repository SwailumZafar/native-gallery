package com.example.nativegallery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/** Last browsing location survives OEM task eviction even when Android drops the saved Bundle. */
internal class GalleryNavigationRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("photo_lane_navigation", Context.MODE_PRIVATE)

    fun load(): GalleryLocation? {
        val tab = GalleryTab.entries.firstOrNull { it.name == preferences.getString("tab", null) } ?: return null
        val albumId = preferences.getString("album", null)
        val destination = if (preferences.getBoolean("album_open", false) && albumId != null) {
            GalleryDestination.AlbumDetail
        } else GalleryDestination.Main
        return GalleryLocation(destination, tab, albumId)
    }

    fun save(location: GalleryLocation) {
        // Sensitive overlays and the editor must be reopened explicitly; retain their source page.
        if (location.destination != GalleryDestination.Main && location.destination != GalleryDestination.AlbumDetail) return
        preferences.edit().putString("tab", location.selectedTab.name)
            .putBoolean("album_open", location.destination == GalleryDestination.AlbumDetail)
            .putString("album", location.selectedAlbumId.takeIf { location.destination == GalleryDestination.AlbumDetail })
            .apply()
    }
}

internal class GalleryNavigationViewModelFactory(private val repository: GalleryNavigationRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        GalleryNavigationViewModel(extras.createSavedStateHandle(), repository.load(), repository::save) as T
}
