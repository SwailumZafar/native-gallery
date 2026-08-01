@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.GalleryFastScroller
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.PremiumDropdownMenu
import com.example.nativegallery.ui.components.PremiumDropdownMenuItem
import com.example.nativegallery.ui.components.PremiumOverflowButton
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock

private enum class DragSelectMode {
    Add,
    Remove
}

private class PhotoBoundsRef(var value: Rect = Rect.Zero)

private val RefreshSkeletonMillis = GalleryMotion.SkeletonVisibleMillis

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    searchQuery: String = "",
    gridColumns: Int = 4,
    onGridColumnsChange: (Int) -> Unit = {},
    listState: LazyListState,
    revealMediaId: String? = null,
    onSearchQueryChange: (String) -> Unit = {},
    selectedMediaIds: Set<String> = emptySet(),
    onMediaLongClick: (MediaItem) -> Unit = {},
    onMediaSelectionToggle: (MediaItem) -> Unit = {},
    onSelectionClear: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onHideSelected: () -> Unit = {},
    onMoveSelected: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    activeSharedElementKey: Any? = null,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit = { _, _, _, _ -> }
) {
    val sections = remember(mediaItems) {
        mediaItems
            .groupBy { it.dateLabel }
            .entries
            .toList()
    }
    val headerCollapse = remember(listState) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / 280f).coerceIn(0f, 1f)
            }
        }
    }
    val tileBounds = remember { mutableMapOf<String, Rect>() }
    val rootBounds = remember { PhotoBoundsRef() }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val showLoading = isLoading
    val revealOffsetPx = with(LocalDensity.current) { 72.dp.roundToPx() }
    var pinchPreviewScale by remember { mutableStateOf(1f) }
    val isGridScrolling by remember(listState) { derivedStateOf { listState.isScrollInProgress } }
    val trackTileBounds = shouldTrackPhotoTileBounds(isSelectionMode, isGridScrolling)
    val deferThumbnailLoads = shouldDeferPhotoThumbnailLoads(isGridScrolling)

    LaunchedEffect(revealMediaId, sections, gridColumns) {
        val mediaId = revealMediaId ?: return@LaunchedEffect
        val targetIndex = galleryPhotoListIndex(
            mediaItems = mediaItems,
            mediaId = mediaId,
            columns = gridColumns
        ) ?: return@LaunchedEffect
        listState.scrollToItem(targetIndex, scrollOffset = -revealOffsetPx)
    }

    fun rootPoint(localPoint: Offset): Offset = Offset(
        rootBounds.value.left + localPoint.x,
        rootBounds.value.top + localPoint.y
    )

    fun hitMedia(localPoint: Offset): MediaItem? {
        val point = rootPoint(localPoint)
        return mediaItems.firstOrNull { mediaItem -> tileBounds[mediaItem.id]?.contains(point) == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds.value = it.boundsInWindow() }
            .pointerInput(isSelectionMode, mediaItems) {
                var dragMode: DragSelectMode? = null
                var baseSelectedIds = emptySet<String>()
                val visitedMediaIds = mutableSetOf<String>()

                fun applyDragSelectionAt(localPoint: Offset) {
                    val hit = hitMedia(localPoint) ?: return
                    val mode = dragMode ?: return
                    if (!visitedMediaIds.add(hit.id)) return

                    when (mode) {
                        DragSelectMode.Add -> if (!baseSelectedIds.contains(hit.id)) onMediaSelectionToggle(hit)
                        DragSelectMode.Remove -> if (baseSelectedIds.contains(hit.id)) onMediaSelectionToggle(hit)
                    }
                }

                if (isSelectionMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            baseSelectedIds = latestSelectedMediaIds
                            visitedMediaIds.clear()
                            val hit = hitMedia(startOffset)
                            dragMode = hit?.let { if (baseSelectedIds.contains(it.id)) DragSelectMode.Remove else DragSelectMode.Add }
                            applyDragSelectionAt(startOffset)
                        },
                        onDrag = { change, _ ->
                            applyDragSelectionAt(change.position)
                            change.consume()
                        },
                        onDragEnd = {
                            dragMode = null
                            visitedMediaIds.clear()
                        },
                        onDragCancel = {
                            dragMode = null
                            visitedMediaIds.clear()
                        }
                    )
                }
            }
            .pointerInput(gridColumns) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var cumulativeZoom = 1f
                    var densityChanged = false
                    try {
                        var gestureActive = true
                        while (gestureActive) {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                cumulativeZoom *= event.calculateZoom()
                                pinchPreviewScale = (1f + (cumulativeZoom - 1f) * 0.22f)
                                    .coerceIn(0.94f, 1.06f)
                                if (!densityChanged && cumulativeZoom >= 1.16f) {
                                    onGridColumnsChange((gridColumns - 1).coerceAtLeast(2))
                                    densityChanged = true
                                } else if (!densityChanged && cumulativeZoom <= 0.86f) {
                                    onGridColumnsChange((gridColumns + 1).coerceAtMost(10))
                                    densityChanged = true
                                }
                                event.changes.forEach { it.consume() }
                            }
                            gestureActive = event.changes.any { it.pressed }
                        }
                    } finally {
                        pinchPreviewScale = 1f
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.graphicsLayer {
                scaleX = pinchPreviewScale
                scaleY = pinchPreviewScale
            },
            state = listState,
            contentPadding = PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + if (isSelectionMode) 132.dp else 34.dp
            )
        ) {
            item(key = "pictures-header", contentType = "pictures-header") {
                PicturesHeader(
                    mediaAccessNotice = mediaAccessNotice,
                    collapseProgress = headerCollapse,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedCount = selectedMediaIds.size,
                    totalVisibleCount = mediaItems.size,
                    onSelectionClear = onSelectionClear,
                    onSelectAllVisible = onSelectAllVisible,
                    onDeleteSelected = onDeleteSelected,
                    onHideSelected = onHideSelected,
                    onRefresh = onRefresh,
                    onOpenSettings = onOpenSettings
                )
            }

            if (showLoading) {
                loadingPhotoSections(columns = gridColumns)
            } else if (mediaItems.isEmpty()) {
                item(key = "photos-empty", contentType = "photos-empty") {
                    PhotoEmptyState(hasSearchQuery = searchQuery.isNotBlank())
                }
            } else {
                sections.forEach { section ->
                    photoSection(
                        title = section.key,
                        mediaItems = section.value,
                        columns = gridColumns,
                        selectedMediaIds = selectedMediaIds,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        sharedBoundsTransform = sharedBoundsTransform,
                        trackTileBounds = trackTileBounds,
                        deferThumbnailLoads = deferThumbnailLoads,
                        activeSharedElementKey = activeSharedElementKey,
                        onMediaBoundsChanged = { mediaItem, bounds ->
                            tileBounds[mediaItem.id] = bounds
                            onMediaBoundsChanged(mediaItem, bounds)
                        },
                        onMediaLongClick = onMediaLongClick,
                        onMediaSelectionToggle = onMediaSelectionToggle,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }



        if (!showLoading && !isSelectionMode) {
            GalleryFastScroller(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(2f)
            )
        }

        SelectionBottomActionBar(
            visible = isSelectionMode,
            selectedCount = selectedMediaIds.size,
            totalVisibleCount = mediaItems.size,
            onClear = onSelectionClear,
            onSelectAll = onSelectAllVisible,
            onShare = onShareSelected,
            onHide = onHideSelected,
            onMove = onMoveSelected,
            onDelete = onDeleteSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f)
        )
    }
}
@Composable
private fun PicturesHeader(
    mediaAccessNotice: (@Composable () -> Unit)?,
    collapseProgress: State<Float>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCount: Int,
    totalVisibleCount: Int,
    onSelectionClear: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onDeleteSelected: () -> Unit,
    onHideSelected: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 96.dp, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedCount > 0) {
            Text(
                text = "%1$,d selected".format(selectedCount),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(18.dp))
        } else {
            Text(
                text = "Photos",
                modifier = Modifier.graphicsLayer {
                    val progress = collapseProgress.value.coerceIn(0f, 1f)
                    alpha = interpolate(1f, 0.76f, progress)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = interpolate(1f, 0.5f, progress)
                    scaleY = interpolate(1f, 0.5f, progress)
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 52.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(44.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        val progress = collapseProgress.value.coerceIn(0f, 1f)
                        alpha = interpolate(1f, 0.9f, progress)
                        translationY = -8f * progress
                    },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchCircle()
                    Box {
                        PremiumOverflowButton(
                            expanded = menuExpanded,
                            contentDescription = "Photo options",
                            onClick = { menuExpanded = true }
                        )
                        PremiumDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            PremiumDropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRefresh()
                                }
                            )
                            PremiumDropdownMenuItem(
                                text = { Text("Select all") },
                                leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                                enabled = totalVisibleCount > 0,
                                onClick = {
                                    menuExpanded = false
                                    onSelectAllVisible()
                                }
                            )
                            PremiumDropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SearchPill(
                placeholder = "Search photos and videos",
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }
        if (mediaAccessNotice != null) {
            Spacer(Modifier.height(18.dp))
            mediaAccessNotice()
        }
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun SearchCircle() {
    Surface(
        modifier = Modifier.size(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        shadowElevation = 0.dp
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(13.dp)
        )
    }
}



@Composable
private fun SelectionBottomActionBar(
    visible: Boolean,
    selectedCount: Int,
    totalVisibleCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onHide: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(100)) + slideInVertically(initialOffsetY = { it + GalleryMotion.BottomSelectionOffsetPx }),
        exit = fadeOut(animationSpec = tween(GalleryMotion.ViewerChromeFadeMillis)) + slideOutVertically(targetOffsetY = { it + GalleryMotion.BottomSelectionOffsetPx })
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(50.dp),
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cancel selection"
                        )
                    }
                    Text(
                        text = "%1$,d selected".format(selectedCount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        enabled = selectedCount < totalVisibleCount,
                        onClick = onSelectAll
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SelectAll,
                            contentDescription = "Select all"
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    SelectionBottomAction(
                        label = "Share",
                        icon = Icons.Outlined.Share,
                        enabled = selectedCount > 0,
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionBottomAction(
                        label = "Lock",
                        icon = Icons.Outlined.Lock,
                        enabled = selectedCount > 0,
                        onClick = onHide,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionBottomAction(
                        label = "Move",
                        icon = Icons.Outlined.Folder,
                        enabled = selectedCount > 0,
                        onClick = onMove,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionBottomAction(
                        label = "Delete",
                        icon = Icons.Outlined.Delete,
                        enabled = selectedCount > 0,
                        destructive = true,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
@Composable
private fun SelectionBottomAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }.copy(alpha = if (enabled) 1f else 0.38f)
    TextButton(
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PhotoEmptyState(hasSearchQuery: Boolean) {
    Text(
        text = if (hasSearchQuery) "No matching photos or videos." else "No photos yet.",
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun LazyListScope.photoSection(
    title: String,
    mediaItems: List<MediaItem>,
    columns: Int,
    selectedMediaIds: Set<String>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    trackTileBounds: Boolean,
    deferThumbnailLoads: Boolean,
    activeSharedElementKey: Any? = null,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    if (mediaItems.isEmpty()) {
        return
    }

    item(key = "section-$title", contentType = "photo-section-title") {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
    }
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "row-$title-${mediaItems[rowIndex * columns].id}" },
        contentType = { "photo-grid-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        PhotoGridRow(
            mediaItems = rowItems,
            columns = columns,
            spacing = 1.dp,
            selectedMediaIds = selectedMediaIds,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedBoundsTransform = sharedBoundsTransform,
            activeSharedElementKey = activeSharedElementKey,
            onMediaBoundsChanged = onMediaBoundsChanged,
            onMediaLongClick = onMediaLongClick,
            trackTileBounds = trackTileBounds,
            deferThumbnailLoads = deferThumbnailLoads,
            onMediaSelectionToggle = onMediaSelectionToggle,
            onMediaClick = onMediaClick
        )
        Spacer(Modifier.height(1.dp))
    }
    item(key = "section-end-$title", contentType = "photo-section-end") {
        Spacer(Modifier.height(30.dp))
    }
}

private fun LazyListScope.loadingPhotoSections(columns: Int) {
    repeat(4) { sectionIndex ->
        item(key = "loading-section-$sectionIndex", contentType = "loading-section-title") {
            SkeletonBlock(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth(0.28f)
                    .height(18.dp),
                cornerRadius = 9.dp
            )
            Spacer(Modifier.height(10.dp))
        }
        items(
            items = List(if (sectionIndex == 0) 3 else 2) { it },
            key = { rowIndex -> "loading-row-$sectionIndex-$rowIndex" },
            contentType = { "loading-photo-row" }
        ) {
            PhotoSkeletonRow(columns = columns, spacing = 1.dp)
            Spacer(Modifier.height(1.dp))
        }
        item(key = "loading-section-end-$sectionIndex", contentType = "loading-section-end") {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PhotoGridRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    selectedMediaIds: Set<String>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    activeSharedElementKey: Any? = null,
    trackTileBounds: Boolean,
    deferThumbnailLoads: Boolean,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        mediaItems.forEach { mediaItem ->
            val sharedElementPrefix = "photos"
            val sharedElementKey = "$sharedElementPrefix-media-${mediaItem.id}"
            val selectionMode = selectedMediaIds.isNotEmpty()
            MediaThumbnail(
                mediaItem = mediaItem,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                cornerRadius = 0.dp,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedElementKey = sharedElementKey,
                sharedBoundsTransform = sharedBoundsTransform,
                isSharedElementSourceHidden = activeSharedElementKey == sharedElementKey,
                selected = selectedMediaIds.contains(mediaItem.id),
                deferUncachedLoad = deferThumbnailLoads,
                onBoundsChanged = if (trackTileBounds) {
                    { bounds -> onMediaBoundsChanged(mediaItem, bounds) }
                } else {
                    null
                },
                onLongClick = { onMediaLongClick(mediaItem) },
                onClickWithBounds = { bounds ->
                    onMediaBoundsChanged(mediaItem, bounds)
                    if (selectionMode) {
                        onMediaSelectionToggle(mediaItem)
                    } else {
                        onMediaClick(mediaItem, bounds, sharedElementKey, sharedElementPrefix)
                    }
                }
            )
        }
        repeat(columns - mediaItems.size) {
            Spacer(Modifier.weight(1f).aspectRatio(1f))
        }
    }
}

@Composable
private fun PhotoSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(columns) {
            SkeletonBlock(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                cornerRadius = 0.dp
            )
        }
    }
}

internal fun shouldTrackPhotoTileBounds(selectionMode: Boolean, scrolling: Boolean): Boolean {
    return selectionMode || !scrolling
}

internal fun shouldDeferPhotoThumbnailLoads(scrolling: Boolean): Boolean = scrolling

private fun interpolate(start: Float, end: Float, fraction: Float): Float {
    val boundedFraction = fraction.coerceIn(0f, 1f)
    return start + (end - start) * boundedFraction
}
