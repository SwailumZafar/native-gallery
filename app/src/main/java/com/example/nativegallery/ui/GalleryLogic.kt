package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem

internal enum class GalleryBackAction {
    System,
    BlockTransition,
    CloseViewer,
    CancelAlbumOpen,
    CancelMediaOpen,
    ClearSelection,
    OpenPhotos,
    CloseAlbumDetail,
    ReturnToAlbums,
    ReturnToMenu,
    CancelAlbumCreator,
    ClosePhotoEditor
}

internal enum class GalleryWindowWidthClass {
    Compact,
    Medium,
    Expanded
}

internal data class GalleryAdaptivePolicy(
    val widthClass: GalleryWindowWidthClass,
    val isLandscape: Boolean,
    val useNavigationRail: Boolean,
    val photoColumnBoost: Int,
    val albumDetailColumnBoost: Int,
    val bigAlbumColumns: Int,
    val basicAlbumColumns: Int,
    val utilityGridColumns: Int,
    val useCompactEditorLayout: Boolean
)

internal fun galleryAdaptivePolicy(widthDp: Float, heightDp: Float): GalleryAdaptivePolicy {
    val safeWidth = widthDp.coerceAtLeast(1f)
    val safeHeight = heightDp.coerceAtLeast(1f)
    val widthClass = when {
        safeWidth >= 840f -> GalleryWindowWidthClass.Expanded
        safeWidth >= 600f -> GalleryWindowWidthClass.Medium
        else -> GalleryWindowWidthClass.Compact
    }
    val isLandscape = safeWidth > safeHeight
    return when (widthClass) {
        GalleryWindowWidthClass.Compact -> GalleryAdaptivePolicy(
            widthClass = widthClass,
            isLandscape = isLandscape,
            useNavigationRail = false,
            photoColumnBoost = if (isLandscape) 1 else 0,
            albumDetailColumnBoost = if (isLandscape) 1 else 0,
            bigAlbumColumns = 2,
            basicAlbumColumns = if (isLandscape) 4 else 3,
            utilityGridColumns = if (isLandscape) 5 else 4,
            useCompactEditorLayout = isLandscape && safeHeight < 600f
        )
        GalleryWindowWidthClass.Medium -> GalleryAdaptivePolicy(
            widthClass = widthClass,
            isLandscape = isLandscape,
            useNavigationRail = true,
            photoColumnBoost = 2,
            albumDetailColumnBoost = 2,
            bigAlbumColumns = 3,
            basicAlbumColumns = 5,
            utilityGridColumns = 6,
            useCompactEditorLayout = isLandscape
        )
        GalleryWindowWidthClass.Expanded -> GalleryAdaptivePolicy(
            widthClass = widthClass,
            isLandscape = isLandscape,
            useNavigationRail = true,
            photoColumnBoost = 4,
            albumDetailColumnBoost = 4,
            bigAlbumColumns = 4,
            basicAlbumColumns = 6,
            utilityGridColumns = 8,
            useCompactEditorLayout = isLandscape
        )
    }
}

internal fun resolveGalleryBackAction(
    destination: GalleryDestination,
    selectedTab: GalleryTab,
    viewerVisible: Boolean,
    viewerClosing: Boolean,
    albumTransitionActive: Boolean,
    albumTransitionCanCancel: Boolean,
    mediaTransitionActive: Boolean,
    mediaTransitionCanCancel: Boolean,
    hasSelection: Boolean
): GalleryBackAction {
    return when {
        viewerClosing -> GalleryBackAction.BlockTransition
        viewerVisible -> GalleryBackAction.CloseViewer
        albumTransitionActive -> if (albumTransitionCanCancel) {
            GalleryBackAction.CancelAlbumOpen
        } else {
            GalleryBackAction.BlockTransition
        }
        mediaTransitionActive -> if (mediaTransitionCanCancel) {
            GalleryBackAction.CancelMediaOpen
        } else {
            GalleryBackAction.BlockTransition
        }
        hasSelection -> GalleryBackAction.ClearSelection
        destination == GalleryDestination.Main && selectedTab != GalleryTab.Photos -> GalleryBackAction.OpenPhotos
        destination == GalleryDestination.AlbumDetail -> GalleryBackAction.CloseAlbumDetail
        destination == GalleryDestination.HiddenItems ||
            destination == GalleryDestination.LockedMedia ||
            destination == GalleryDestination.RecentlyDeleted -> GalleryBackAction.ReturnToAlbums
        destination == GalleryDestination.Cleanup ||
            destination == GalleryDestination.Documents -> GalleryBackAction.ReturnToMenu
        destination == GalleryDestination.AlbumCreator -> GalleryBackAction.CancelAlbumCreator
        destination == GalleryDestination.PhotoEditor -> GalleryBackAction.ClosePhotoEditor
        else -> GalleryBackAction.System
    }
}

internal fun galleryPhotoListIndex(
    mediaItems: List<MediaItem>,
    mediaId: String,
    columns: Int
): Int? {
    val safeColumns = columns.coerceAtLeast(1)
    var lazyListIndex = 1 // The screen header is item 0.
    mediaItems.groupBy { it.dateLabel }.values.forEach { sectionItems ->
        val mediaIndex = sectionItems.indexOfFirst { it.id == mediaId }
        if (mediaIndex >= 0) {
            return lazyListIndex + 1 + mediaIndex / safeColumns
        }
        val rowCount = (sectionItems.size + safeColumns - 1) / safeColumns
        lazyListIndex += rowCount + 2 // Section title, rows, and section spacing.
    }
    return null
}

internal fun nextMediaAfterDelete(
    remainingItems: List<MediaItem>,
    deletedIndex: Int,
    direction: Int
): MediaItem? {
    if (remainingItems.isEmpty()) return null
    val preferredIndex = if (direction < 0) deletedIndex - 1 else deletedIndex
    return remainingItems[preferredIndex.coerceIn(0, remainingItems.lastIndex)]
}
