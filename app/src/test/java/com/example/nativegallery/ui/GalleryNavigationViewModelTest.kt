package com.example.nativegallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import androidx.lifecycle.SavedStateHandle

class GalleryNavigationViewModelTest {
    @Test fun oldTaskBundleCannotOverwriteTheLatestBrowsingPage() {
        val oldState = SavedStateHandle()
        GalleryNavigationViewModel(oldState).openTab(GalleryTab.Photos)
        val latest = GalleryLocation(GalleryDestination.AlbumDetail, GalleryTab.Albums, "camera")
        val restored = GalleryNavigationViewModel(oldState, fallbackLocation = latest)
        assertEquals(latest, restored.uiState.value.location())
    }
    @Test fun absentActivityBundleRestoresTheLastAlbum() {
        var persisted: GalleryLocation? = null
        val original = GalleryNavigationViewModel(persistLocation = { persisted = it })
        original.openTab(GalleryTab.Albums)
        original.selectAlbumForOpening("camera")
        original.showAlbumDetail()
        val recreated = GalleryNavigationViewModel(fallbackLocation = persisted)
        assertEquals(GalleryDestination.AlbumDetail, recreated.uiState.value.destination)
        assertEquals(GalleryTab.Albums, recreated.uiState.value.selectedTab)
        assertEquals("camera", recreated.uiState.value.selectedAlbumId)
    }

    @Test fun closingAnAlbumRetainsAlbumsWhenTheTaskIsRelaunched() {
        var persisted: GalleryLocation? = null
        val original = GalleryNavigationViewModel(persistLocation = { persisted = it })
        original.selectAlbumForOpening("camera")
        original.showAlbumDetail()
        original.closeAlbumDetail()
        val recreated = GalleryNavigationViewModel(fallbackLocation = persisted)
        assertEquals(GalleryDestination.Main, recreated.uiState.value.destination)
        assertEquals(GalleryTab.Albums, recreated.uiState.value.selectedTab)
    }
    @Test
    fun processRecreationRestoresAlbumAndOverlayReturnLocation() {
        val savedState = SavedStateHandle()
        val original = GalleryNavigationViewModel(savedState)
        original.openTab(GalleryTab.Albums)
        original.selectAlbumForOpening("camera")
        original.showAlbumDetail()
        original.openOverlay(GalleryDestination.RecentlyDeleted)

        val recreated = GalleryNavigationViewModel(copySavedState(savedState))
        assertEquals(GalleryDestination.RecentlyDeleted, recreated.uiState.value.destination)
        assertEquals(GalleryTab.Albums, recreated.uiState.value.selectedTab)
        recreated.returnFromOverlay(GalleryTab.Albums)
        assertEquals(GalleryDestination.AlbumDetail, recreated.uiState.value.destination)
        assertEquals("camera", recreated.uiState.value.selectedAlbumId)
    }

    @Test
    fun processRecreationKeepsMenuInsteadOfResettingToPhotos() {
        val savedState = SavedStateHandle()
        GalleryNavigationViewModel(savedState).openTab(GalleryTab.Menu)
        val recreated = GalleryNavigationViewModel(copySavedState(savedState))
        assertEquals(GalleryTab.Menu, recreated.uiState.value.selectedTab)
    }

    @Test
    fun processRecreationOfEditorReturnsToSourceAlbum() {
        val savedState = SavedStateHandle()
        val original = GalleryNavigationViewModel(savedState)
        original.selectAlbumForOpening("camera")
        original.showAlbumDetail()
        original.openEditor()
        val recreated = GalleryNavigationViewModel(copySavedState(savedState))
        assertEquals(GalleryDestination.AlbumDetail, recreated.uiState.value.destination)
        assertEquals("camera", recreated.uiState.value.selectedAlbumId)
    }

    private fun copySavedState(handle: SavedStateHandle) = SavedStateHandle(
        handle.keys().associateWith { handle.get<Any?>(it) }
    )

    @Test
    fun albumNavigationKeepsAlbumIdentityThroughClose() {
        val viewModel = GalleryNavigationViewModel()

        viewModel.openTab(GalleryTab.Albums)
        viewModel.selectAlbumForOpening("camera")
        viewModel.showAlbumDetail()

        assertEquals(GalleryDestination.AlbumDetail, viewModel.uiState.value.destination)
        assertEquals("camera", viewModel.uiState.value.selectedAlbumId)

        viewModel.closeAlbumDetail()

        assertEquals(GalleryDestination.Main, viewModel.uiState.value.destination)
        assertEquals(GalleryTab.Albums, viewModel.uiState.value.selectedTab)
        assertEquals("camera", viewModel.uiState.value.selectedAlbumId)
    }

    @Test
    fun overlayReturnsToExactAlbumLocation() {
        val viewModel = GalleryNavigationViewModel()
        viewModel.openTab(GalleryTab.Albums)
        viewModel.selectAlbumForOpening("screenshots")
        viewModel.showAlbumDetail()

        viewModel.openOverlay(GalleryDestination.RecentlyDeleted)
        viewModel.returnFromOverlay(GalleryTab.Albums)

        assertEquals(GalleryDestination.AlbumDetail, viewModel.uiState.value.destination)
        assertEquals(GalleryTab.Albums, viewModel.uiState.value.selectedTab)
        assertEquals("screenshots", viewModel.uiState.value.selectedAlbumId)
    }

    @Test
    fun overlayDestinationTabDoesNotChangeReturnLocation() {
        val viewModel = GalleryNavigationViewModel()

        viewModel.openOverlay(
            destination = GalleryDestination.LockedMedia,
            destinationTab = GalleryTab.Albums
        )
        viewModel.returnFromOverlay(GalleryTab.Albums)

        assertEquals(GalleryDestination.Main, viewModel.uiState.value.destination)
        assertEquals(GalleryTab.Photos, viewModel.uiState.value.selectedTab)
        assertNull(viewModel.uiState.value.selectedAlbumId)
    }

    @Test
    fun settledPagerPageKeepsNavigationSelectionInSync() {
        val viewModel = GalleryNavigationViewModel()
        viewModel.openTab(GalleryTab.Albums)

        viewModel.syncPagerTab(GalleryTab.Photos)

        assertEquals(GalleryDestination.Main, viewModel.uiState.value.destination)
        assertEquals(GalleryTab.Photos, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun editorReturnsToItsLaunchingLocation() {
        val viewModel = GalleryNavigationViewModel()
        viewModel.openTab(GalleryTab.Albums)
        viewModel.selectAlbumForOpening("camera")
        viewModel.showAlbumDetail()

        viewModel.openEditor()
        viewModel.closeEditor()

        assertEquals(GalleryDestination.AlbumDetail, viewModel.uiState.value.destination)
        assertEquals("camera", viewModel.uiState.value.selectedAlbumId)
    }
}
