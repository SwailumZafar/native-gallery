package com.example.nativegallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryNavigationViewModelTest {
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