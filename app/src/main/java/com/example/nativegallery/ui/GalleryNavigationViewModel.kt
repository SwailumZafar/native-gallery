package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    UsefulScreenshots,
    Moments,
    AlbumCreator,
    PhotoEditor,
    Cleanup,
    Places
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

internal class GalleryNavigationViewModel(
    private val savedState: SavedStateHandle = SavedStateHandle(),
    fallbackLocation: GalleryLocation? = null,
    private val persistLocation: (GalleryLocation) -> Unit = {}
) : ViewModel() {
    private var overlayReturnLocation = restoreLocation("overlay")
    private var editorReturnLocation = restoreLocation("editor")
    private val _uiState = MutableStateFlow(
        restoreLocation("current").let { restored ->
            // Some OEMs return an older task Bundle. The persisted browsing page is newer.
            if (restored == null || restored.destination == GalleryDestination.Main || restored.destination == GalleryDestination.AlbumDetail) {
                fallbackLocation ?: restored ?: GalleryNavigationUiState().location()
            } else restored
        }.let { location ->
            // The editor's working bitmap is intentionally memory-only. Recover its source page.
            if (location.destination == GalleryDestination.PhotoEditor) {
                editorReturnLocation ?: GalleryNavigationUiState().location()
            } else location
        }.toUiState()
    )
    val uiState: StateFlow<GalleryNavigationUiState> = _uiState.asStateFlow()

    private fun updateState(transform: (GalleryNavigationUiState) -> GalleryNavigationUiState) {
        _uiState.value = transform(_uiState.value)
        saveLocation("current", _uiState.value.location())
        persistLocation(_uiState.value.location())
    }

    fun openTab(tab: GalleryTab) {
        updateState {
            it.copy(destination = GalleryDestination.Main, selectedTab = tab)
        }
    }

    fun syncPagerTab(tab: GalleryTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    fun selectAlbumForOpening(albumId: String) {
        updateState {
            it.copy(selectedTab = GalleryTab.Albums, selectedAlbumId = albumId)
        }
    }

    fun showAlbumDetail() {
        updateState { it.copy(destination = GalleryDestination.AlbumDetail) }
    }

    fun closeAlbumDetail() {
        updateState {
            it.copy(destination = GalleryDestination.Main, selectedTab = GalleryTab.Albums)
        }
    }

    fun clearSelectedAlbum() {
        updateState { it.copy(selectedAlbumId = null) }
    }

    fun cancelAlbumOpen() {
        updateState { GalleryNavigationUiState(
            destination = GalleryDestination.Main,
            selectedTab = GalleryTab.Albums,
            selectedAlbumId = null
        ) }
    }

    fun openOverlay(
        destination: GalleryDestination,
        destinationTab: GalleryTab? = null
    ) {
        overlayReturnLocation = _uiState.value.location()
        saveLocation("overlay", overlayReturnLocation)
        updateState {
            it.copy(
                destination = destination,
                selectedTab = destinationTab ?: it.selectedTab
            )
        }
    }

    fun returnFromOverlay(fallbackTab: GalleryTab) {
        val returnLocation = overlayReturnLocation
        overlayReturnLocation = null
        saveLocation("overlay", null)
        updateState { returnLocation?.toUiState() ?: GalleryNavigationUiState(
            destination = GalleryDestination.Main,
            selectedTab = fallbackTab
        ) }
    }

    fun openEditor() {
        editorReturnLocation = _uiState.value.location()
        saveLocation("editor", editorReturnLocation)
        updateState { it.copy(destination = GalleryDestination.PhotoEditor) }
    }

    fun closeEditor() {
        val returnLocation = editorReturnLocation
        editorReturnLocation = null
        saveLocation("editor", null)
        updateState { returnLocation?.toUiState() ?: GalleryNavigationUiState() }
    }

    private fun saveLocation(key: String, location: GalleryLocation?) {
        savedState["$key.destination"] = location?.destination?.name
        savedState["$key.tab"] = location?.selectedTab?.name
        savedState["$key.album"] = location?.selectedAlbumId
    }

    private fun restoreLocation(key: String): GalleryLocation? {
        val destination = savedState.get<String>("$key.destination") ?: return null
        return GalleryLocation(
            GalleryDestination.entries.firstOrNull { it.name == destination } ?: GalleryDestination.Main,
            GalleryTab.entries.firstOrNull { it.name == savedState.get<String>("$key.tab") } ?: GalleryTab.Photos,
            savedState["$key.album"]
        )
    }

    private fun GalleryLocation.toUiState() = GalleryNavigationUiState(
        destination = destination,
        selectedTab = selectedTab,
        selectedAlbumId = selectedAlbumId
    )
}
