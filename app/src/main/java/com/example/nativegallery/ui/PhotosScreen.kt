@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import kotlin.math.min
import kotlin.math.abs
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.nativegallery.ui.components.rememberGalleryFlingBehavior
import com.example.nativegallery.ui.components.MediaThumbnail
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
    onSearchQueryChange: (String) -> Unit = {},
    selectedMediaIds: Set<String> = emptySet(),
    onMediaLongClick: (MediaItem) -> Unit = {},
    onMediaSelectionToggle: (MediaItem) -> Unit = {},
    onSelectionClear: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onHideSelected: () -> Unit = {},
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
    val listState = rememberLazyListState()
    val headerCollapse = 0f
    val tileBounds = remember { mutableMapOf<String, Rect>() }
    val rootBounds = remember { PhotoBoundsRef() }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val showLoading = isLoading

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
            .onGloballyPositioned { rootBounds.value = it.boundsInRoot() }
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
    ) {
        LazyColumn(
            modifier = Modifier,
            state = listState,
            flingBehavior = rememberGalleryFlingBehavior(),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + if (isSelectionMode) 178.dp else 34.dp
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
                    onHideSelected = onHideSelected
                )
            }

            if (showLoading) {
                loadingPhotoSections()
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
                        activeSharedElementKey = activeSharedElementKey,
                        onMediaBoundsChanged = { mediaItem, bounds -> tileBounds[mediaItem.id] = bounds },
                        onMediaLongClick = onMediaLongClick,
                        onMediaSelectionToggle = onMediaSelectionToggle,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }



        SelectionBottomActionBar(
            visible = isSelectionMode,
            selectedCount = selectedMediaIds.size,
            totalVisibleCount = mediaItems.size,
            onClear = onSelectionClear,
            onSelectAll = onSelectAllVisible,
            contentPadding = contentPadding,
            onShare = onShareSelected,
            onHide = onHideSelected,
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
    collapseProgress: Float,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCount: Int,
    totalVisibleCount: Int,
    onSelectionClear: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onDeleteSelected: () -> Unit,
    onHideSelected: () -> Unit
) {
    val progress = collapseProgress.coerceIn(0f, 1f)
    val topPadding = interpolate(96f, 34f, progress).dp
    val titleScale = interpolate(1.08f, 0.72f, progress)
    val titleAlpha = interpolate(1f, 0.84f, progress)
    val titleSpacing = interpolate(44f, 14f, progress).dp
    val bottomSpacing = interpolate(34f, 18f, progress).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = topPadding, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (selectedCount > 0) "%1$,d selected".format(selectedCount) else "Photos",
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                transformOrigin = TransformOrigin(0.5f, 0f)
                scaleX = titleScale
                scaleY = titleScale
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(titleSpacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier.graphicsLayer {
                    alpha = interpolate(1f, 0.9f, progress)
                    translationY = -8f * progress
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchCircle()
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (selectedCount == 0) {
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
        Spacer(Modifier.height(bottomSpacing))
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
    contentPadding: PaddingValues,
    onShare: () -> Unit,
    onHide: () -> Unit,
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
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = contentPadding.calculateBottomPadding() + 8.dp
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear selection",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "%1$,d selected".format(selectedCount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        enabled = selectedCount < totalVisibleCount,
                        onClick = onSelectAll
                    ) {
                        Text("Select all")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    SelectionBottomAction(
                        label = "Share",
                        icon = Icons.Filled.Share,
                        enabled = selectedCount > 0,
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionBottomAction(
                        label = "Lock",
                        icon = Icons.Filled.Lock,
                        enabled = selectedCount > 0,
                        onClick = onHide,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionBottomAction(
                        label = "Delete",
                        icon = Icons.Filled.Delete,
                        enabled = selectedCount > 0,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(modifier = modifier, enabled = enabled, onClick = onClick) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(6.dp))
        Text(label)
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
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
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
            onMediaSelectionToggle = onMediaSelectionToggle,
            onMediaClick = onMediaClick
        )
        Spacer(Modifier.height(1.dp))
    }
    item(key = "section-end-$title", contentType = "photo-section-end") {
        Spacer(Modifier.height(30.dp))
    }
}

private fun LazyListScope.loadingPhotoSections() {
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
            PhotoSkeletonRow(columns = 4, spacing = 1.dp)
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
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                val sharedElementPrefix = "photos"
                val sharedElementKey = "$sharedElementPrefix-media-${mediaItem.id}"
                val selectionMode = selectedMediaIds.isNotEmpty()
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementKey = sharedElementKey,
                    sharedBoundsTransform = sharedBoundsTransform,
                    isSharedElementSourceHidden = activeSharedElementKey == sharedElementKey,
                    selected = selectedMediaIds.contains(mediaItem.id),
                    onBoundsChanged = if (selectionMode) {
                        { bounds -> onMediaBoundsChanged(mediaItem, bounds) }
                    } else {
                        null
                    },
                    onLongClick = { onMediaLongClick(mediaItem) },
                    onClickWithBounds = { bounds ->
                        if (selectionMode) {
                            onMediaSelectionToggle(mediaItem)
                        } else {
                            onMediaClick(mediaItem, bounds, sharedElementKey, sharedElementPrefix)
                        }
                    }
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}

@Composable
private fun PhotoSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(columns) {
                SkeletonBlock(
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp
                )
            }
        }
    }
}

private fun interpolate(start: Float, end: Float, fraction: Float): Float {
    val boundedFraction = fraction.coerceIn(0f, 1f)
    return start + (end - start) * boundedFraction
}
