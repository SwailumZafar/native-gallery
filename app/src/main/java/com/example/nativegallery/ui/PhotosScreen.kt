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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock

private enum class DragSelectMode {
    Add,
    Remove
}

private val RefreshSkeletonMillis = GalleryMotion.SkeletonVisibleMillis

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    searchQuery: String = "",
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
    val sections = mediaItems
        .groupBy { it.dateLabel }
        .entries
        .toList()
    val listState = rememberLazyListState()
    val rawHeaderCollapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 220f).coerceIn(0f, 1f)
            }
        }
    }
    val headerCollapse = rawHeaderCollapse
    val tileBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var localRefreshing by remember { mutableStateOf(false) }
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val showLoading = isLoading || localRefreshing
    val pullThresholdPx = with(LocalDensity.current) { GalleryMotion.PullThresholdDp.dp.toPx() }
    val refreshProgress = (pullDistance / pullThresholdPx).coerceIn(0f, 1f)

    LaunchedEffect(localRefreshing) {
        if (localRefreshing) {
            delay(RefreshSkeletonMillis)
            localRefreshing = false
            pullDistance = 0f
        }
    }

    fun rootPoint(localPoint: Offset): Offset = Offset(rootBounds.left + localPoint.x, rootBounds.top + localPoint.y)

    fun hitMedia(localPoint: Offset): MediaItem? {
        val point = rootPoint(localPoint)
        return mediaItems.firstOrNull { mediaItem -> tileBounds[mediaItem.id]?.contains(point) == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
            .pointerInput(isSelectionMode, mediaItems, pullThresholdPx, showLoading) {
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
                } else {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchSlop = viewConfiguration.touchSlop
                        var activePointerId = down.id
                        var lastPosition = down.position
                        var handlingPull = false
                        var endedNormally = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break
                            activePointerId = change.id
                            if (!change.pressed) {
                                endedNormally = true
                                break
                            }

                            val delta = change.position - lastPosition
                            lastPosition = change.position
                            if (!handlingPull) {
                                val totalDrag = change.position - down.position
                                val passedSlop = abs(totalDrag.x) > touchSlop || abs(totalDrag.y) > touchSlop
                                if (!passedSlop) continue
                                if (abs(totalDrag.y) <= abs(totalDrag.x) * 1.15f) break
                                handlingPull = true
                            }

                            if (handlingPull) {
                                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                                val pullingDown = delta.y > 0f || pullDistance > 0f
                                if (atTop && pullingDown && !showLoading) {
                                    val rawPull = (pullDistance + delta.y).coerceAtLeast(0f)
                                    pullDistance = min(rawPull / (1f + rawPull / 100f), pullThresholdPx * 1.3f)
                                    change.consume()
                                }
                            }
                        }

                        if (handlingPull || endedNormally) {
                            if (pullDistance >= pullThresholdPx && !showLoading) {
                                localRefreshing = true
                            } else if (!localRefreshing) {
                                pullDistance = 0f
                            }
                        }
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.graphicsLayer { translationY = if (showLoading) 0f else pullDistance },
            state = listState,
            contentPadding = PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 34.dp
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
                        columns = 4,
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

        PullRefreshIndicator(
            progress = refreshProgress,
            refreshing = localRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp)
                .zIndex(2f)
        )

        SelectionBottomActionBar(
            visible = isSelectionMode,
            selectedCount = selectedMediaIds.size,
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
            text = if (selectedCount > 0) "%1$,d selected".format(selectedCount) else "Pictures",
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
        if (selectedCount > 0) {
            Spacer(Modifier.height(12.dp))
            SelectionToolbar(
                selectedCount = selectedCount,
                totalVisibleCount = totalVisibleCount,
                onClear = onSelectionClear,
                onSelectAll = onSelectAllVisible,
                onDelete = onDeleteSelected,
                onHide = onHideSelected
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
        color = Color(0xFFF3F3F3),
        shape = CircleShape,
        shadowElevation = 0.dp
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = Color.Black,
            modifier = Modifier.padding(13.dp)
        )
    }
}

@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    totalVisibleCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "%1$,d selected".format(selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                enabled = selectedCount < totalVisibleCount,
                onClick = onSelectAll
            ) {
                Text("Select all")
            }
            IconButton(onClick = onHide) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Hide selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PullRefreshIndicator(
    progress: Float,
    refreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val visible = progress > 0f || refreshing
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(90)) + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(targetOffsetY = { -it / 2 })
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = CircleShape,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (refreshing) 1f else progress.coerceIn(0.08f, 1f) },
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun SelectionBottomActionBar(
    visible: Boolean,
    selectedCount: Int,
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
                    bottom = contentPadding.calculateBottomPadding() + 84.dp
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 14.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%1$,d selected".format(selectedCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(enabled = selectedCount > 0, onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share selected",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                IconButton(enabled = selectedCount > 0, onClick = onHide) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Hide selected",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                IconButton(enabled = selectedCount > 0, onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete selected",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
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
    items(
        items = mediaItems.chunked(columns),
        key = { rowItems -> "row-$title-${rowItems.first().id}" },
        contentType = { "photo-grid-row" }
    ) { rowItems ->
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
                var mediaBounds by remember(mediaItem.id) { mutableStateOf(Rect.Zero) }
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
                    onBoundsChanged = { bounds ->
                        mediaBounds = bounds
                        onMediaBoundsChanged(mediaItem, bounds)
                    },
                    onLongClick = { onMediaLongClick(mediaItem) },
                    onClick = {
                        if (selectionMode) {
                            onMediaSelectionToggle(mediaItem)
                        } else {
                            onMediaClick(mediaItem, mediaBounds, sharedElementKey, sharedElementPrefix)
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
