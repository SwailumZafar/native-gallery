package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal enum class GalleryTab {
    Photos,
    Albums,
    Menu
}

internal fun GalleryTab.pageIndex(): Int = when (this) {
    GalleryTab.Photos -> 0
    GalleryTab.Albums -> 1
    GalleryTab.Menu -> 2
}

internal fun pageToGalleryTab(page: Int): GalleryTab {
    return when (page) {
        0 -> GalleryTab.Photos
        1 -> GalleryTab.Albums
        else -> GalleryTab.Menu
    }
}

internal enum class GalleryDestination {
    Main,
    AlbumDetail,
    HiddenItems,
    LockedMedia,
    RecentlyDeleted,
    Documents,
    AlbumCreator,
    PhotoEditor,
    Cleanup
}

@Immutable
internal data class GalleryLocation(
    val destination: GalleryDestination,
    val selectedTab: GalleryTab,
    val selectedAlbumId: String?
)

@Immutable
internal data class GalleryNavigationUiState(
    val destination: GalleryDestination = GalleryDestination.Main,
    val selectedTab: GalleryTab = GalleryTab.Photos,
    val selectedAlbumId: String? = null
) {
    fun location(): GalleryLocation = GalleryLocation(
        destination = destination,
        selectedTab = selectedTab,
        selectedAlbumId = selectedAlbumId
    )
}

internal class GalleryNavigationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryNavigationUiState())
    val uiState: StateFlow<GalleryNavigationUiState> = _uiState.asStateFlow()

    private var overlayReturnLocation: GalleryLocation? = null
    private var editorReturnLocation: GalleryLocation? = null

    fun openTab(tab: GalleryTab) {
        _uiState.update {
            it.copy(destination = GalleryDestination.Main, selectedTab = tab)
        }
    }

    fun syncPagerTab(tab: GalleryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectAlbumForOpening(albumId: String) {
        _uiState.update {
            it.copy(selectedTab = GalleryTab.Albums, selectedAlbumId = albumId)
        }
    }

    fun showAlbumDetail() {
        _uiState.update { it.copy(destination = GalleryDestination.AlbumDetail) }
    }

    fun closeAlbumDetail() {
        _uiState.update {
            it.copy(destination = GalleryDestination.Main, selectedTab = GalleryTab.Albums)
        }
    }

    fun clearSelectedAlbum() {
        _uiState.update { it.copy(selectedAlbumId = null) }
    }

    fun cancelAlbumOpen() {
        _uiState.value = GalleryNavigationUiState(
            destination = GalleryDestination.Main,
            selectedTab = GalleryTab.Albums,
            selectedAlbumId = null
        )
    }

    fun openOverlay(
        destination: GalleryDestination,
        destinationTab: GalleryTab? = null
    ) {
        overlayReturnLocation = _uiState.value.location()
        _uiState.update {
            it.copy(
                destination = destination,
                selectedTab = destinationTab ?: it.selectedTab
            )
        }
    }

    fun returnFromOverlay(fallbackTab: GalleryTab) {
        val returnLocation = overlayReturnLocation
        overlayReturnLocation = null
        _uiState.value = returnLocation?.toUiState() ?: GalleryNavigationUiState(
            destination = GalleryDestination.Main,
            selectedTab = fallbackTab
        )
    }

    fun openEditor() {
        editorReturnLocation = _uiState.value.location()
        _uiState.update { it.copy(destination = GalleryDestination.PhotoEditor) }
    }

    fun closeEditor() {
        val returnLocation = editorReturnLocation
        editorReturnLocation = null
        _uiState.value = returnLocation?.toUiState() ?: GalleryNavigationUiState()
    }

    private fun GalleryLocation.toUiState() = GalleryNavigationUiState(
        destination = destination,
        selectedTab = selectedTab,
        selectedAlbumId = selectedAlbumId
    )
}