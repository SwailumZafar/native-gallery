package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryLogicTest {
    @Test
    fun photoListIndex_accountsForHeadersRowsAndSectionSpacing() {
        val mediaItems = listOf(
            media("1", "Today"),
            media("2", "Today"),
            media("3", "Today"),
            media("4", "Today"),
            media("5", "Today"),
            media("6", "Yesterday")
        )

        assertEquals(2, galleryPhotoListIndex(mediaItems, "1", columns = 4))
        assertEquals(3, galleryPhotoListIndex(mediaItems, "5", columns = 4))
        assertEquals(6, galleryPhotoListIndex(mediaItems, "6", columns = 4))
        assertNull(galleryPhotoListIndex(mediaItems, "missing", columns = 4))
    }

    @Test
    fun nextMediaAfterDelete_prefersSwipeDirectionAndClampsAtEdges() {
        val remaining = listOf(media("a"), media("b"), media("c"))

        assertEquals("b", nextMediaAfterDelete(remaining, deletedIndex = 1, direction = 1)?.id)
        assertEquals("a", nextMediaAfterDelete(remaining, deletedIndex = 1, direction = -1)?.id)
        assertEquals("c", nextMediaAfterDelete(remaining, deletedIndex = 8, direction = 1)?.id)
        assertNull(nextMediaAfterDelete(emptyList(), deletedIndex = 0, direction = 1))
    }

    @Test
    fun backRouter_leavesPhotosRootToTheSystem() {
        assertEquals(
            GalleryBackAction.System,
            backAction(destination = GalleryDestination.Main, selectedTab = GalleryTab.Photos)
        )
        assertEquals(
            GalleryBackAction.OpenPhotos,
            backAction(destination = GalleryDestination.Main, selectedTab = GalleryTab.Albums)
        )
    }

    @Test
    fun backRouter_prioritizesViewerThenTransitionsThenSelection() {
        assertEquals(
            GalleryBackAction.CloseViewer,
            backAction(
                destination = GalleryDestination.AlbumDetail,
                viewerVisible = true,
                albumTransitionActive = true,
                hasSelection = true
            )
        )
        assertEquals(
            GalleryBackAction.CancelAlbumOpen,
            backAction(
                destination = GalleryDestination.AlbumDetail,
                albumTransitionActive = true,
                albumTransitionCanCancel = true,
                hasSelection = true
            )
        )
        assertEquals(
            GalleryBackAction.ClearSelection,
            backAction(destination = GalleryDestination.AlbumDetail, hasSelection = true)
        )
    }

    @Test
    fun backRouter_blocksClosingTransitionsAndRepeatedViewerClose() {
        assertEquals(
            GalleryBackAction.BlockTransition,
            backAction(destination = GalleryDestination.Main, viewerClosing = true)
        )
        assertEquals(
            GalleryBackAction.BlockTransition,
            backAction(destination = GalleryDestination.Main, mediaTransitionActive = true)
        )
    }

    @Test
    fun backRouter_returnsSecondaryScreensToTheirOwners() {
        assertEquals(
            GalleryBackAction.CloseAlbumDetail,
            backAction(destination = GalleryDestination.AlbumDetail)
        )
        assertEquals(
            GalleryBackAction.ReturnToAlbums,
            backAction(destination = GalleryDestination.RecentlyDeleted)
        )
        assertEquals(
            GalleryBackAction.ReturnToMenu,
            backAction(destination = GalleryDestination.Cleanup)
        )
        assertEquals(
            GalleryBackAction.ClosePhotoEditor,
            backAction(destination = GalleryDestination.PhotoEditor)
        )
    }

    @Test
    fun adaptivePolicy_keepsPortraitPhonesCompact() {
        val policy = galleryAdaptivePolicy(widthDp = 412f, heightDp = 915f)

        assertEquals(GalleryWindowWidthClass.Compact, policy.widthClass)
        assertEquals(false, policy.isLandscape)
        assertEquals(false, policy.useNavigationRail)
        assertEquals(0, policy.photoColumnBoost)
        assertEquals(3, policy.basicAlbumColumns)
        assertEquals(4, policy.utilityGridColumns)
        assertEquals(false, policy.useCompactEditorLayout)
    }

    @Test
    fun adaptivePolicy_usesCompactLandscapeDensityBelowRailBreakpoint() {
        val policy = galleryAdaptivePolicy(widthDp = 580f, heightDp = 360f)

        assertEquals(GalleryWindowWidthClass.Compact, policy.widthClass)
        assertEquals(true, policy.isLandscape)
        assertEquals(false, policy.useNavigationRail)
        assertEquals(1, policy.photoColumnBoost)
        assertEquals(4, policy.basicAlbumColumns)
        assertEquals(5, policy.utilityGridColumns)
        assertEquals(true, policy.useCompactEditorLayout)
    }

    @Test
    fun adaptivePolicy_usesNavigationRailForMediumWindows() {
        val policy = galleryAdaptivePolicy(widthDp = 800f, heightDp = 600f)

        assertEquals(GalleryWindowWidthClass.Medium, policy.widthClass)
        assertEquals(true, policy.useNavigationRail)
        assertEquals(2, policy.photoColumnBoost)
        assertEquals(3, policy.bigAlbumColumns)
        assertEquals(6, policy.utilityGridColumns)
    }

    @Test
    fun adaptivePolicy_scalesExpandedGalleryGrids() {
        val policy = galleryAdaptivePolicy(widthDp = 1280f, heightDp = 800f)

        assertEquals(GalleryWindowWidthClass.Expanded, policy.widthClass)
        assertEquals(true, policy.useNavigationRail)
        assertEquals(4, policy.photoColumnBoost)
        assertEquals(4, policy.albumDetailColumnBoost)
        assertEquals(4, policy.bigAlbumColumns)
        assertEquals(6, policy.basicAlbumColumns)
        assertEquals(8, policy.utilityGridColumns)
    }

    private fun backAction(
        destination: GalleryDestination,
        selectedTab: GalleryTab = GalleryTab.Photos,
        viewerVisible: Boolean = false,
        viewerClosing: Boolean = false,
        albumTransitionActive: Boolean = false,
        albumTransitionCanCancel: Boolean = false,
        mediaTransitionActive: Boolean = false,
        mediaTransitionCanCancel: Boolean = false,
        hasSelection: Boolean = false
    ) = resolveGalleryBackAction(
        destination = destination,
        selectedTab = selectedTab,
        viewerVisible = viewerVisible,
        viewerClosing = viewerClosing,
        albumTransitionActive = albumTransitionActive,
        albumTransitionCanCancel = albumTransitionCanCancel,
        mediaTransitionActive = mediaTransitionActive,
        mediaTransitionCanCancel = mediaTransitionCanCancel,
        hasSelection = hasSelection
    )

    private fun media(id: String, dateLabel: String = "Today") = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = id,
        dateLabel = dateLabel
    )
}
