@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaSelectionActionBar
import com.example.nativegallery.ui.components.MediaSelectionTopBar
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.GalleryFastScroller
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.MotionSpecs
import com.example.nativegallery.ui.components.PremiumDropdownMenu
import com.example.nativegallery.ui.components.PremiumDropdownMenuItem
import com.example.nativegallery.ui.components.PremiumOverflowButton
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock
import com.example.nativegallery.ui.components.solarTactile

private enum class DragSelectMode {
    Add,
    Remove
}

private class PhotoBoundsRef(var value: Rect = Rect.Zero)

private val RefreshSkeletonMillis = GalleryMotion.SkeletonVisibleMillis

enum class PhotoGridFilter(val label: String) {
    All("All"),
    Photos("Photos"),
    Videos("Videos"),
    Favorites("Favorites")
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    favoriteMediaIds: Set<String> = emptySet(),
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    searchQuery: String = "",
    gridColumns: Int = 4,
    onGridColumnsChange: (Int) -> Unit = {},
    listState: LazyListState,
    revealMediaId: String? = null,
    onGridViewportChanged: (Rect) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    selectedMediaIds: Set<String> = emptySet(),
    onMediaLongClick: (MediaItem) -> Unit = {},
    onMediaSelectionToggle: (MediaItem) -> Unit = {},
    onSelectionClear: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onSelectItems: ((List<MediaItem>) -> Unit)? = null,
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
    onMediaClickWithItems: ((MediaItem, List<MediaItem>, Rect, String, String) -> Unit)? = null,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit = { _, _, _, _ -> }
) {
    var selectedFilter by rememberSaveable { mutableStateOf(PhotoGridFilter.All) }

    val filteredMediaItems = remember(mediaItems, selectedFilter, favoriteMediaIds) {
        when (selectedFilter) {
            PhotoGridFilter.All -> mediaItems
            PhotoGridFilter.Photos -> mediaItems.filter { !it.isVideo }
            PhotoGridFilter.Videos -> mediaItems.filter { it.isVideo }
            PhotoGridFilter.Favorites -> mediaItems.filter { favoriteMediaIds.contains(it.id) }
        }
    }
    val sections = remember(filteredMediaItems) {
        filteredMediaItems
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
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val revealOffsetPx = with(density) { 72.dp.roundToPx() }
    val gridTopInsetPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val gridBottomInsetPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
    val pinchScaleAnim = remember { Animatable(1f) }
    val trackTileBounds = shouldTrackPhotoTileBounds(isSelectionMode, revealMediaId != null)
    val deferThumbnailLoads = remember(listState) {
        derivedStateOf { shouldDeferPhotoThumbnailLoads(listState.isScrollInProgress) }
    }

    val currentGridColumns by rememberUpdatedState(gridColumns)
    val currentOnGridColumnsChange by rememberUpdatedState(onGridColumnsChange)
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSearchVisible by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    val isRefreshing = remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchVisible) {
        keyboardController?.hide()
        onSearchQueryChange("")
        isSearchVisible = false
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && !isSearchVisible) {
            isSearchVisible = true
        }
    }

    LaunchedEffect(revealMediaId, sections, gridColumns) {
        val mediaId = revealMediaId ?: return@LaunchedEffect
        val targetIndex = galleryPhotoListIndex(
            mediaItems = filteredMediaItems,
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
        return filteredMediaItems.firstOrNull { mediaItem -> tileBounds[mediaItem.id]?.contains(point) == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootBounds.value = it.boundsInWindow()
                onGridViewportChanged(gridReturnViewport(rootBounds.value, gridTopInsetPx, gridBottomInsetPx))
            }
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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var cumulativeZoom = 1f
                    try {
                        var gestureActive = true
                        while (gestureActive) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                cumulativeZoom *= zoomChange
                                val targetScale = (1f + (cumulativeZoom - 1f) * 0.12f)
                                    .coerceIn(0.95f, 1.05f)
                                coroutineScope.launch { pinchScaleAnim.snapTo(targetScale) }
                                if (cumulativeZoom >= 1.09f) {
                                    val cols = currentGridColumns
                                    if (cols > 1) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnGridColumnsChange(cols - 1)
                                        cumulativeZoom = 1f
                                        coroutineScope.launch { pinchScaleAnim.snapTo(1f) }
                                    }
                                } else if (cumulativeZoom <= 0.91f) {
                                    val cols = currentGridColumns
                                    if (cols < 6) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnGridColumnsChange(cols + 1)
                                        cumulativeZoom = 1f
                                        coroutineScope.launch { pinchScaleAnim.snapTo(1f) }
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                            gestureActive = event.changes.any { it.pressed }
                        }
                    } finally {
                        coroutineScope.launch {
                            pinchScaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = MotionSpecs.AppleSpringPinch
                            )
                        }
                    }
                }
            }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing.value,
            onRefresh = {
                isRefreshing.value = true
                onRefresh()
                coroutineScope.launch {
                    delay(800)
                    isRefreshing.value = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = pinchScaleAnim.value
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = if (listState.firstVisibleItemIndex == 0) {
                            TransformOrigin(0.5f, 0f)
                        } else {
                            TransformOrigin(0.5f, 0.5f)
                        }
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val scale = pinchScaleAnim.value
                                if (scale != 1f && scale > 0f) {
                                    val inv = 1f / scale
                                    scaleX = inv
                                    scaleY = inv
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }
                            }
                    ) {
                        PicturesHeader(
                            mediaAccessNotice = mediaAccessNotice,
                            collapseProgress = headerCollapse,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            selectedCount = selectedMediaIds.size,
                            totalVisibleCount = filteredMediaItems.size,
                            allPhotosCount = remember(mediaItems) { mediaItems.count { !it.isVideo } },
                            allVideosCount = remember(mediaItems) { mediaItems.count { it.isVideo } },
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it },
                            onSelectAllVisible = {
                                if (onSelectItems != null) {
                                    onSelectItems(filteredMediaItems)
                                } else {
                                    onSelectAllVisible()
                                }
                            },
                            onRefresh = onRefresh,
                            onOpenSettings = onOpenSettings,
                            searchFocusRequester = searchFocusRequester,
                            isSearchVisible = isSearchVisible,
                            onSearchClick = {
                                isSearchVisible = true
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                    delay(120)
                                    searchFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            },
                            onCloseSearch = {
                                keyboardController?.hide()
                                onSearchQueryChange("")
                                isSearchVisible = false
                            }
                        )
                    }
                }

            if (showLoading) {
                loadingPhotoSections(columns = gridColumns)
            } else if (filteredMediaItems.isEmpty()) {
                item(key = "photos-empty", contentType = "photos-empty") {
                    PhotoEmptyState(hasSearchQuery = searchQuery.isNotBlank(), filter = selectedFilter)
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
                        onMediaClick = { mediaItem, bounds, key, prefix ->
                            onMediaClickWithItems?.invoke(mediaItem, filteredMediaItems, bounds, key, prefix)
                                ?: onMediaClick(mediaItem, bounds, key, prefix)
                        }
                    )
                }
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

        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f),
            enter = fadeIn(MotionSpecs.AppleSpringSnappy) + slideInVertically(
                animationSpec = MotionSpecs.AppleSpringSheetOffset,
                initialOffsetY = { -it / 2 }
            ),
            exit = fadeOut(tween(100)) + slideOutVertically(
                animationSpec = MotionSpecs.AppleSpringSheetOffset,
                targetOffsetY = { -it / 2 }
            )
        ) {
            MediaSelectionTopBar(
                selectedCount = selectedMediaIds.size,
                canSelectAll = selectedMediaIds.size < filteredMediaItems.size,
                onClear = onSelectionClear,
                onSelectAll = {
                    if (onSelectItems != null) {
                        onSelectItems(filteredMediaItems)
                    } else {
                        onSelectAllVisible()
                    }
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
        }

        SelectionBottomActionBar(
            visible = isSelectionMode,
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
    allPhotosCount: Int,
    allVideosCount: Int,
    selectedFilter: PhotoGridFilter,
    onFilterSelected: (PhotoGridFilter) -> Unit,
    onSelectAllVisible: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    searchFocusRequester: FocusRequester? = null,
    isSearchVisible: Boolean = false,
    onSearchClick: () -> Unit = {},
    onCloseSearch: () -> Unit = {}
) {
    val density = LocalDensity.current
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 88.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (selectedCount > 0) {
            Spacer(Modifier.height(28.dp))
        } else {
            val mediaSummary = remember(allPhotosCount, allVideosCount) {
                val numberFormat = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                val photoPart = "${numberFormat.format(allPhotosCount)} ${if (allPhotosCount == 1) "Photo" else "Photos"}"
                val videoPart = "${numberFormat.format(allVideosCount)} ${if (allVideosCount == 1) "Video" else "Videos"}"
                if (allPhotosCount > 0 && allVideosCount > 0) {
                    "$photoPart • $videoPart"
                } else if (allPhotosCount > 0) {
                    photoPart
                } else if (allVideosCount > 0) {
                    videoPart
                } else ""
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Photos",
                        modifier = Modifier.graphicsLayer {
                            val progress = collapseProgress.value.coerceIn(0f, 1f)
                            val easedProgress = GalleryMotion.easeOutCubic(progress)
                            alpha = interpolate(1f, 0.76f, easedProgress)
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleX = interpolate(1f, 0.58f, easedProgress)
                            scaleY = interpolate(1f, 0.58f, easedProgress)
                            translationY = -6f * easedProgress
                            cameraDistance = 16f * density.density
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 40.sp,
                            lineHeight = 46.sp,
                            letterSpacing = (-1).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (mediaSummary.isNotBlank()) {
                        Text(
                            text = mediaSummary,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .graphicsLayer {
                                    val progress = collapseProgress.value.coerceIn(0f, 1f)
                                    alpha = interpolate(1f, 0f, (progress * 2.5f).coerceIn(0f, 1f))
                                    translationY = -4f * progress
                                },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.graphicsLayer {
                        val progress = collapseProgress.value.coerceIn(0f, 1f)
                        alpha = interpolate(1f, 0.9f, progress)
                        translationY = -8f * progress
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchCircle(
                        onClick = {
                            if (isSearchVisible) {
                                onCloseSearch()
                            } else {
                                onSearchClick()
                            }
                        }
                    )
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
            Spacer(Modifier.height(24.dp))
            PhotoFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected
            )
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    expandFrom = Alignment.Top
                ) + fadeIn(spring(dampingRatio = 0.85f, stiffness = 400f)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 450f),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(tween(140))
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchPill(
                            modifier = Modifier.weight(1f),
                            placeholder = "Search photos and videos",
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            focusRequester = searchFocusRequester
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .solarTactile(scaleDown = 0.88f, onClick = onCloseSearch),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
        if (mediaAccessNotice != null) {
            Spacer(Modifier.height(18.dp))
            mediaAccessNotice()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun PhotoFilterBar(
    selectedFilter: PhotoGridFilter,
    onFilterSelected: (PhotoGridFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Keep every label and touch target usable on narrow screens and at larger text sizes.
        val filtersPerRow = if (maxWidth / fontScale < 320.dp) 2 else 4
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(4.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PhotoGridFilter.entries.chunked(filtersPerRow).forEach { filters ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        filters.forEach { filterOption ->
                            val isSelected = selectedFilter == filterOption
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .solarTactile(scaleDown = 0.94f) { onFilterSelected(filterOption) }
                                    .semantics {
                                        role = Role.Tab
                                        selected = isSelected
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filterOption.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCircle(
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .size(50.dp)
            .solarTactile(scaleDown = 0.90f, onClick = onClick),
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
    onShare: () -> Unit,
    onHide: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = MotionSpecs.AppleSpringSnappy) + slideInVertically(
            animationSpec = MotionSpecs.AppleSpringSheetOffset,
            initialOffsetY = { it / 2 + 24 }
        ),
        exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(
            animationSpec = MotionSpecs.AppleSpringSheetOffset,
            targetOffsetY = { it / 2 + 24 }
        )
    ) {
        MediaSelectionActionBar(
            onShare = onShare,
            onLock = onHide,
            onMove = onMove,
            onDelete = onDelete,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 2.dp)
        )
    }
}

@Composable
private fun PhotoEmptyState(
    hasSearchQuery: Boolean,
    filter: PhotoGridFilter = PhotoGridFilter.All
) {
    val message = when {
        hasSearchQuery -> "No matching photos or videos."
        filter == PhotoGridFilter.Photos -> "No photos yet."
        filter == PhotoGridFilter.Videos -> "No videos yet."
        filter == PhotoGridFilter.Favorites -> "No favorite photos or videos yet."
        else -> "No photos yet."
    }
    Text(
        text = message,
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
    deferThumbnailLoads: State<Boolean>,
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
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 380f),
                fadeOutSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 380f),
                placementSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 350f)
            )
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "photo-row-$title-$rowIndex" },
        contentType = { "photo-grid-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 380f),
                fadeOutSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 380f),
                placementSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 350f)
            )
        ) {
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
        }
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
    deferThumbnailLoads: State<Boolean>,
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
                onBoundsChanged = if (trackTileBounds || activeSharedElementKey == sharedElementKey) {
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

internal fun shouldTrackPhotoTileBounds(selectionMode: Boolean, revealInProgress: Boolean): Boolean {
    return selectionMode || revealInProgress
}

internal fun shouldDeferPhotoThumbnailLoads(scrolling: Boolean): Boolean = scrolling

private fun interpolate(start: Float, end: Float, fraction: Float): Float {
    val boundedFraction = fraction.coerceIn(0f, 1f)
    return start + (end - start) * boundedFraction
}
