package com.example.nativegallery.ui

import androidx.compose.foundation.lazy.LazyListState

/** Prepare under the opaque viewer; preserve the grid when the row is already fully visible. */
internal suspend fun LazyListState.prepareViewerReturn(row: Int, topInset: Int, bottomInset: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == row }
    val start = item?.let { it.offset - info.viewportStartOffset }
    val available = info.viewportEndOffset - info.viewportStartOffset
    if (item != null && start != null && start >= topInset && start + item.size <= available - bottomInset) return
    scrollToItem(row, scrollOffset = -(topInset - info.beforeContentPadding).coerceAtLeast(0))
}
