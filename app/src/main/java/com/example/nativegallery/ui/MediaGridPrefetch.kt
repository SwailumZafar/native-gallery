package com.example.nativegallery.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay

internal val LocalGridPrefetchEnabled = staticCompositionLocalOf { true }

/** Include date headings and spacers so a deep timeline jump warms the actual nearby media. */
internal fun photoGridRowStarts(sectionSizes: List<Int>, columns: Int): List<Int> = buildList {
    val safeColumns = columns.coerceAtLeast(1)
    var mediaIndex = 0
    add(0) // Screen header.
    sectionSizes.filter { it > 0 }.forEach { count ->
        add(mediaIndex) // Date heading.
        repeat((count + safeColumns - 1) / safeColumns) { row -> add(mediaIndex + row * safeColumns) }
        mediaIndex += count
        add(mediaIndex - 1) // Section spacer.
    }
}

internal fun mediaPrefetchRange(anchorIndex: Int, itemCount: Int, columns: Int,
    scrolling: Boolean = false, forward: Boolean = true): IntRange {
    if (itemCount <= 0) return IntRange.EMPTY
    val safeColumns = columns.coerceIn(1, 10)
    // Refill every four rows, keeping a few rows behind and sixteen rows ahead.
    val stride = safeColumns * 4
    val anchor = anchorIndex.coerceIn(0, itemCount - 1) / stride * stride
    if (scrolling) {
        val behind = if (forward) 2 else 8
        val ahead = if (forward) 10 else 4
        return (anchor - safeColumns * behind).coerceAtLeast(0)..
            (anchor + safeColumns * ahead - 1).coerceAtMost(itemCount - 1)
    }
    return (anchor - safeColumns * 4).coerceAtLeast(0)..
        (anchor + safeColumns * 20 - 1).coerceAtMost(itemCount - 1)
}

@Composable
internal fun PrefetchMediaGrid(
    mediaItems: List<MediaItem>,
    listState: LazyListState,
    columns: Int,
    rowStarts: List<Int>? = null
) {
    val context = LocalContext.current.applicationContext
    val enabled = LocalGridPrefetchEnabled.current
    LaunchedEffect(mediaItems, listState, columns, rowStarts, enabled) {
        if (!enabled) return@LaunchedEffect
        var previousRow = listState.firstVisibleItemIndex
        var forward = true
        snapshotFlow {
            val row = listState.firstVisibleItemIndex
            row to listState.isScrollInProgress
        }.map { (row, scrolling) ->
            if (row != previousRow) forward = row > previousRow
            previousRow = row
            val mediaIndex = rowStarts?.getOrNull(row) ?: (row * columns)
            mediaPrefetchRange(mediaIndex, mediaItems.size, columns, scrolling, forward) to scrolling
        }.distinctUntilChanged().collectLatest { (range, scrolling) ->
            if (!range.isEmpty()) {
                if (!scrolling) delay(120L)
                prefetchMediaThumbnails(context, mediaItems.subList(range.first, range.last + 1),
                    thumbnailSizes = listOf(384), maxItems = range.count())
            }
        }
    }
}
