@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.nativegallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nativegallery.data.AlbumArrangementPolicy
import com.example.nativegallery.data.AlbumArrangementRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryFastScroller
import com.example.nativegallery.ui.components.HeaderActionButton
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.PremiumDropdownMenu
import com.example.nativegallery.ui.components.PremiumDropdownMenuItem
import com.example.nativegallery.ui.components.PremiumOverflowButton
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.VideoBadge
import com.example.nativegallery.ui.components.MediaSelectionActionBar
import com.example.nativegallery.ui.components.MediaSelectionTopBar
import com.example.nativegallery.ui.components.MotionSpecs
import com.example.nativegallery.ui.components.bouncyClickable
import com.example.nativegallery.ui.components.ResourceImage
import com.example.nativegallery.ui.components.ScreenHeader
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class AlbumDetailGridMode(val columns: Int) {
    Single(1),
    Spacious(2),
    Comfortable(3),
    Compact(4),
    Dense(5),
    Micro(6);

    fun label(): String = when (this) {
        Single -> "Single"
        Spacious -> "Spacious"
        Comfortable -> "Comfort"
        Compact -> "Compact"
        Dense -> "Dense"
        Micro -> "Micro"
    }

    fun next(): AlbumDetailGridMode = when (this) {
        Single -> Spacious
        Spacious -> Comfortable
        Comfortable -> Compact
        Compact -> Dense
        Dense -> Micro
        Micro -> Single
    }

    fun zoomIn(): AlbumDetailGridMode = when (this) {
        Micro -> Dense
        Dense -> Compact
        Compact -> Comfortable
        Comfortable -> Spacious
        Spacious -> Single
        Single -> Single
    }

    fun zoomOut(): AlbumDetailGridMode = when (this) {
        Single -> Spacious
        Spacious -> Comfortable
        Comfortable -> Compact
        Compact -> Dense
        Dense -> Micro
        Micro -> Micro
    }
}

enum class AlbumDetailSortMode {
    Newest,
    Oldest,
    Name
}

private class AlbumBoundsRef(var value: Rect = Rect.Zero)

private const val AlbumDropOverlapFraction = 0.16f

private fun Rect.translatedBy(offset: Offset): Rect = Rect(
    left = left + offset.x,
    top = top + offset.y,
    right = right + offset.x,
    bottom = bottom + offset.y
)

private fun hasSufficientAlbumDropOverlap(
    draggedBounds: Rect,
    targetBounds: Rect
): Boolean {
    val draggedArea = draggedBounds.width.coerceAtLeast(0f) * draggedBounds.height.coerceAtLeast(0f)
    val targetArea = targetBounds.width.coerceAtLeast(0f) * targetBounds.height.coerceAtLeast(0f)
    val comparisonArea = min(draggedArea, targetArea)
    if (comparisonArea <= 0f) return false

    val overlapWidth = (
        min(draggedBounds.right, targetBounds.right) -
            max(draggedBounds.left, targetBounds.left)
        ).coerceAtLeast(0f)
    val overlapHeight = (
        min(draggedBounds.bottom, targetBounds.bottom) -
            max(draggedBounds.top, targetBounds.top)
        ).coerceAtLeast(0f)
    val overlapFraction = (overlapWidth * overlapHeight) / comparisonArea
    val center = draggedBounds.center
    val centerInsideTarget =
        center.x >= targetBounds.left &&
            center.x <= targetBounds.right &&
            center.y >= targetBounds.top &&
            center.y <= targetBounds.bottom

    return centerInsideTarget || overlapFraction >= AlbumDropOverlapFraction
}
internal fun selectAlbumDropTargetId(
    draggedAlbumId: String,
    draggedBounds: Rect,
    orderedAlbumIds: List<String>,
    boundsByAlbumId: Map<String, Rect>
): String? {
    val candidates = orderedAlbumIds.asSequence()
        .filterNot { it == draggedAlbumId }
        .mapNotNull { albumId -> boundsByAlbumId[albumId]?.let { albumId to it } }
        .toList()
    if (candidates.isEmpty()) return null

    val directTargets = candidates.filter { (_, bounds) ->
        hasSufficientAlbumDropOverlap(draggedBounds, bounds)
    }
    val targetPool = directTargets.ifEmpty {
        candidates.filter { (_, bounds) ->
            val horizontalTolerance = bounds.width * 0.18f
            val verticalTolerance = bounds.height * 0.18f
            val center = draggedBounds.center
            center.x >= bounds.left - horizontalTolerance &&
                center.x <= bounds.right + horizontalTolerance &&
                center.y >= bounds.top - verticalTolerance &&
                center.y <= bounds.bottom + verticalTolerance
        }
    }
    return targetPool.minByOrNull { (_, bounds) ->
        val distance = bounds.center - draggedBounds.center
        distance.getDistanceSquared()
    }?.first
}


private enum class AlbumDragSelectMode {
    Add,
    Remove
}

internal fun albumReorderFrameRatio(source: Float, target: Float, progress: Float): Float {
    if (!source.isFinite() || !target.isFinite() || source <= 0f || target <= 0f) return 1f
    val fraction = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    return 1f + (target / source - 1f) * fraction
}

private class AlbumCardInteraction(
    val enabled: Boolean,
    val suppressPlacementAnimation: Boolean,
    val pinnedAlbumId: String?,
    val draggedAlbumId: String?,
    val dropTargetAlbumId: String?,
    val dragOffset: Offset,
    val dropSettling: Boolean,
    val dropSettleProgress: State<Float>,
    val dropSettleStartOffset: Offset,
    val dropSettleTargetOffset: Offset,
    val dropTargetSwapOffset: Offset,
    val draggedBounds: Rect?,
    val dropTargetBounds: Rect?,
    val contextAlbumId: String?,
    val canDelete: (Album) -> Boolean,
    val onPinToggle: (Album) -> Unit,
    val onContextMenuRequest: (Album) -> Unit,
    val onContextMenuDismiss: () -> Unit,
    val onDeleteRequest: (Album) -> Unit,
    val onDragStart: (Album) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDragEnd: (commit: Boolean) -> Unit
)

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    layoutMode: AlbumLayoutMode,
    onLayoutModeChange: (AlbumLayoutMode) -> Unit,
    onOpenHiddenItems: () -> Unit,
    onOpenLockedMedia: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onCreateAlbum: (String) -> Unit = {},
    onDeleteAlbum: (Album) -> Unit = {},
    canDeleteAlbum: (Album) -> Boolean = { !it.isAllPhotos },
    onOpenSettings: () -> Unit = {},
    onRefresh: () -> Unit = {},
    hiddenAlbumCount: Int = 0,
    hiddenItemCount: Int = 0,
    lockedItemCount: Int = 0,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyListState,
    activeTransitionAlbumId: State<String?>? = null,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    bigTileColumns: Int = 2,
    basicTileColumns: Int = 3,
    heroHeight: androidx.compose.ui.unit.Dp = 176.dp
) {
    val hapticFeedback = LocalHapticFeedback.current
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    var layoutExpanded by rememberSaveable { mutableStateOf(false) }
    var sortAlphabetically by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createAlbumName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current.applicationContext
    val arrangementRepository = remember(context) { AlbumArrangementRepository(context) }
    var arrangementState by remember { mutableStateOf(arrangementRepository.load()) }
    val availableAlbumIds = remember(albums) { albums.map(Album::id) }
    val albumById = remember(albums) { albums.associateBy(Album::id) }
    val sourceAlbumIds = remember(albums, sortAlphabetically) {
        if (sortAlphabetically) {
            albums.sortedBy { it.name.lowercase() }.map(Album::id)
        } else {
            availableAlbumIds
        }
    }
    val effectiveArrangement = remember(arrangementState, sortAlphabetically) {
        if (sortAlphabetically) {
            arrangementState.copy(orderIds = emptyList())
        } else {
            arrangementState
        }
    }
    val sortedAlbums = remember(sourceAlbumIds, effectiveArrangement, albumById) {
        AlbumArrangementPolicy.arrangedIds(sourceAlbumIds, effectiveArrangement)
            .mapNotNull(albumById::get)
    }
    val leadingAlbum = sortedAlbums.firstOrNull()
    val remainingAlbums = sortedAlbums.drop(1)
    val reorderEnabled = !sortAlphabetically && searchQuery.isBlank() && sortedAlbums.size > 1
    val deferAlbumCoverLoads = remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    // Coordinates change on every fling frame. Keeping them out of snapshot state prevents
    // album scrolling from invalidating and recomposing the whole screen.
    val albumBounds = remember { mutableMapOf<String, Rect>() }
    val latestOnAlbumBoundsChanged by rememberUpdatedState(onAlbumBoundsChanged)
    var draggedAlbumId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetAlbumId by remember { mutableStateOf<String?>(null) }
    var contextAlbumId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteAlbum by remember { mutableStateOf<Album?>(null) }
    val reorderScope = rememberCoroutineScope()
    val dropSettleProgress = remember { Animatable(1f) }
    var dropSettling by remember { mutableStateOf(false) }
    var suppressPlacementAnimation by remember { mutableStateOf(false) }
    var dropSettleStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dropSettleTargetOffset by remember { mutableStateOf(Offset.Zero) }
    val isRefreshing = remember { mutableStateOf(false) }


    fun recordAlbumBounds(album: Album, bounds: Rect) {
        if (draggedAlbumId == null) {
            albumBounds[album.id] = bounds
        }
        // The click path already carries the card's current bounds. The external cache is only
        // needed for returning to an album tile, so avoid mutating app-level state mid-fling.
        if (draggedAlbumId == null && !listState.isScrollInProgress) {
            latestOnAlbumBoundsChanged(album, bounds)
        }
    }

    LaunchedEffect(listState, availableAlbumIds) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { isScrolling ->
                if (!isScrolling) {
                    albumBounds.toMap().forEach { (albumId, bounds) ->
                        albumById[albumId]?.let { latestOnAlbumBoundsChanged(it, bounds) }
                    }
                }
            }
    }

    val draggedBounds = draggedAlbumId?.let(albumBounds::get)
    val dropTargetBounds = dropTargetAlbumId?.let(albumBounds::get)
    val dropTargetSwapOffset = if (
        draggedBounds != null && dropTargetBounds != null && draggedAlbumId != dropTargetAlbumId
    ) {
        draggedBounds.center - dropTargetBounds.center
    } else {
        Offset.Zero
    }

    fun clearAlbumDrag() {
        draggedAlbumId = null
        dropTargetAlbumId = null
        dragOffset = Offset.Zero
        dropSettling = false
        dropSettleStartOffset = Offset.Zero
        dropSettleTargetOffset = Offset.Zero
    }

    val cardInteraction = AlbumCardInteraction(
        enabled = reorderEnabled,
        suppressPlacementAnimation = suppressPlacementAnimation,
        pinnedAlbumId = arrangementState.pinnedAlbumId,
        draggedAlbumId = draggedAlbumId,
        dropTargetAlbumId = dropTargetAlbumId,
        dragOffset = dragOffset,
        dropSettling = dropSettling,
        dropSettleProgress = dropSettleProgress.asState(),
        dropSettleStartOffset = dropSettleStartOffset,
        dropSettleTargetOffset = dropSettleTargetOffset,
        dropTargetSwapOffset = dropTargetSwapOffset,
        draggedBounds = draggedBounds,
        dropTargetBounds = dropTargetBounds,
        contextAlbumId = contextAlbumId,
        canDelete = canDeleteAlbum,
        onPinToggle = { album ->
            sortAlphabetically = false
            arrangementState = arrangementRepository.save(
                AlbumArrangementPolicy.togglePin(
                    availableIds = availableAlbumIds,
                    state = arrangementState,
                    albumId = album.id
                )
            )
        },
        onContextMenuRequest = { album -> contextAlbumId = album.id },
        onContextMenuDismiss = { contextAlbumId = null },
        onDeleteRequest = { album ->
            contextAlbumId = null
            if (canDeleteAlbum(album)) pendingDeleteAlbum = album
        },
        onDragStart = { album ->
            if (reorderEnabled && !dropSettling) {
                suppressPlacementAnimation = true
                contextAlbumId = null
                draggedAlbumId = album.id
                dragOffset = Offset.Zero
                dropTargetAlbumId = album.id
            }
        },
        onDrag = { delta ->
            val draggedId = draggedAlbumId
            val origin = draggedId?.let(albumBounds::get)
            if (draggedId != null && origin != null && !dropSettling) {
                dragOffset += delta
                if (dragOffset.getDistanceSquared() > 36f) {
                    contextAlbumId = null
                }
                val translatedBounds = origin.translatedBy(dragOffset)
                val nextTargetId = selectAlbumDropTargetId(
                    draggedAlbumId = draggedId,
                    draggedBounds = translatedBounds,
                    orderedAlbumIds = sortedAlbums.map(Album::id),
                    boundsByAlbumId = albumBounds
                ) ?: draggedId
                if (nextTargetId != dropTargetAlbumId && nextTargetId != draggedId) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                dropTargetAlbumId = nextTargetId
            }
        },
        onDragEnd = { commit ->
            val movedId = draggedAlbumId
            val origin = movedId?.let(albumBounds::get)
            if (movedId == null || origin == null || dropSettling) {
                if (!dropSettling) clearAlbumDrag()
            } else {
                val translatedBounds = origin.translatedBy(dragOffset)
                val targetId = if (commit) selectAlbumDropTargetId(
                    draggedAlbumId = movedId,
                    draggedBounds = translatedBounds,
                    orderedAlbumIds = sortedAlbums.map(Album::id),
                    boundsByAlbumId = albumBounds
                ) ?: movedId else movedId
                val targetBounds = albumBounds[targetId]
                val settleTarget = if (targetId != movedId && targetBounds != null) {
                    targetBounds.center - origin.center
                } else {
                    Offset.Zero
                }
                val nextArrangement = if (targetId != movedId) {
                    AlbumArrangementPolicy.swap(
                        availableIds = availableAlbumIds,
                        state = arrangementState,
                        movedId = movedId,
                        targetId = targetId
                    )
                } else {
                    null
                }

                if (targetId == movedId && dragOffset.getDistanceSquared() <= 36f) {
                    clearAlbumDrag()
                    suppressPlacementAnimation = false
                    if (commit) contextAlbumId = movedId
                } else {
                    dropTargetAlbumId = targetId
                    dropSettleStartOffset = dragOffset
                    dropSettleTargetOffset = settleTarget
                    reorderScope.launch {
                        dropSettleProgress.snapTo(0f)
                        dropSettling = true
                        dropSettleProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 320f
                            )
                        )
                        if (draggedAlbumId == movedId && dropTargetAlbumId == targetId) {
                            nextArrangement?.let {
                                arrangementState = arrangementRepository.save(it)
                            }
                            clearAlbumDrag()
                            // Do not replay LazyColumn placement after our cards have already
                            // sprung into their final slots. Re-enable it after layout commits.
                            withFrameNanos { }
                            withFrameNanos { }
                            if (draggedAlbumId == null) suppressPlacementAnimation = false
                        }
                    }
                }
            }
        }
    )

    val pinchScaleAnim = remember { Animatable(1f) }
    val layoutModeOrder = remember {
        listOf(
            AlbumLayoutMode.Hero,
            AlbumLayoutMode.BigTiles,
            AlbumLayoutMode.Medium,
            AlbumLayoutMode.Basic,
            AlbumLayoutMode.Compact,
            AlbumLayoutMode.List
        )
    }
    val currentLayoutMode by rememberUpdatedState(layoutMode)
    val currentOnLayoutModeChange by rememberUpdatedState(onLayoutModeChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = pinchScaleAnim.value
                scaleY = pinchScaleAnim.value
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
                                val targetScale = (1f + (cumulativeZoom - 1f) * 0.12f).coerceIn(0.95f, 1.05f)
                                reorderScope.launch { pinchScaleAnim.snapTo(targetScale) }
                                if (cumulativeZoom >= 1.09f) {
                                    val currIdx = layoutModeOrder.indexOf(currentLayoutMode)
                                    if (currIdx > 0) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnLayoutModeChange(layoutModeOrder[currIdx - 1])
                                        cumulativeZoom = 1f
                                        reorderScope.launch { pinchScaleAnim.snapTo(1f) }
                                    }
                                } else if (cumulativeZoom <= 0.91f) {
                                    val currIdx = layoutModeOrder.indexOf(currentLayoutMode)
                                    if (currIdx < layoutModeOrder.lastIndex) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnLayoutModeChange(layoutModeOrder[currIdx + 1])
                                        cumulativeZoom = 1f
                                        reorderScope.launch { pinchScaleAnim.snapTo(1f) }
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                            gestureActive = event.changes.any { it.pressed }
                        }
                    } finally {
                        reorderScope.launch {
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
                reorderScope.launch {
                    delay(800)
                    isRefreshing.value = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = when (layoutMode) {
                AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> 8.dp
                AlbumLayoutMode.List -> 14.dp
                else -> 18.dp
            },
            top = 58.dp,
            end = when (layoutMode) {
                AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> 8.dp
                AlbumLayoutMode.List -> 14.dp
                else -> 18.dp
            },
            bottom = contentPadding.calculateBottomPadding() + 6.dp
        )
    ) {
        item(key = "albums-header", contentType = "albums-header") {
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
                Column {
                    ScreenHeader(title = "Albums") {
                HeaderActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Create album",
                    onClick = { showCreateDialog = true }
                )
                HeaderActionButton(
                    icon = when (layoutMode) {
                        AlbumLayoutMode.Hero, AlbumLayoutMode.BigTiles -> Icons.Filled.GridView
                        AlbumLayoutMode.Medium, AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> Icons.Filled.Apps
                        AlbumLayoutMode.List -> Icons.Filled.Folder
                    },
                    contentDescription = "Switch layout",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val modes = AlbumLayoutMode.entries
                        val nextIndex = (modes.indexOf(layoutMode) + 1) % modes.size
                        onLayoutModeChange(modes[nextIndex])
                    }
                )
                Box {
                    PremiumOverflowButton(
                        expanded = overflowExpanded,
                        contentDescription = "Album options",
                        onClick = { overflowExpanded = true }
                    )
                    PremiumDropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        PremiumDropdownMenuItem(
                            text = { Text("Sort albums") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                            trailingIcon = {
                                if (sortAlphabetically) Icon(Icons.Filled.Check, contentDescription = null)
                            },
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sortAlphabetically = !sortAlphabetically
                                overflowExpanded = false
                            }
                        )
                        PremiumDropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SearchPill(
                placeholder = "Search albums",
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
            if (mediaAccessNotice != null) {
                Spacer(Modifier.height(18.dp))
                mediaAccessNotice()
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LayoutSelector(
                    layoutMode = layoutMode,
                    expanded = layoutExpanded,
                    onExpandedChange = { layoutExpanded = it },
                    onLayoutModeChange = { nextMode ->
                        if (nextMode != layoutMode) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLayoutModeChange(nextMode)
                        }
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
                }
            }
        }

        if (isLoading) {
            item(key = "albums-loading", contentType = "albums-loading") {
                AlbumsLoadingState(layoutMode = layoutMode)
            }
        } else if (sortedAlbums.isEmpty()) {
            item(key = "albums-empty", contentType = "albums-empty") {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching albums." else "No albums yet.",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 22.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            when (layoutMode) {
                AlbumLayoutMode.Hero -> {
                    heroAlbumRows(
                        albums = sortedAlbums,
                        height = 200.dp,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
                AlbumLayoutMode.BigTiles -> {
                    if (leadingAlbum != null) {
                        item(key = "album-hero", contentType = "album-hero") {
                            Box(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = if (cardInteraction.suppressPlacementAnimation) null else spring(dampingRatio = 0.82f, stiffness = 350f)
                                ).zIndex(albumReorderRowZIndex(listOf(leadingAlbum), cardInteraction))
                            ) {
                                key(leadingAlbum.id) {
                                    AlbumHeroCard(
                                        album = leadingAlbum,
                                        height = heroHeight,
                                        activeTransitionAlbumId = activeTransitionAlbumId,
                                        cardInteraction = cardInteraction,
                                        deferThumbnailLoads = deferAlbumCoverLoads,
                                        onAlbumClick = onAlbumClick,
                                        onAlbumBoundsChanged = ::recordAlbumBounds
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    bigAlbumRows(
                        albums = remainingAlbums,
                        columns = bigTileColumns,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
                AlbumLayoutMode.Medium -> {
                    bigAlbumRows(
                        albums = sortedAlbums,
                        columns = 2,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
                AlbumLayoutMode.Basic -> {
                    basicAlbumRows(
                        albums = sortedAlbums,
                        columns = basicTileColumns,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
                AlbumLayoutMode.Compact -> {
                    basicAlbumRows(
                        albums = sortedAlbums,
                        columns = 4,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
                AlbumLayoutMode.List -> {
                    listAlbumRows(
                        albums = sortedAlbums,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferAlbumCoverLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = ::recordAlbumBounds
                    )
                }
            }
        }
        if (!isLoading) {
            item(key = "recently-deleted-pill", contentType = "album-list-action") {
                Spacer(Modifier.height(10.dp))
                RecentlyDeletedPill(onClick = onOpenRecentlyDeleted)
            }
        }
    }
        }
        if (!isLoading) {
            GalleryFastScroller(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(2f)
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createAlbumName = ""
            },
            modifier = Modifier
                .widthIn(min = 340.dp, max = 460.dp)
                .fillMaxWidth(0.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Create album") },
            text = {
                OutlinedTextField(
                    value = createAlbumName,
                    onValueChange = { createAlbumName = it },
                    singleLine = true,
                    label = { Text("Album name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = createAlbumName.isNotBlank(),
                    onClick = {
                        val albumName = createAlbumName.trim()
                        showCreateDialog = false
                        createAlbumName = ""
                        onCreateAlbum(albumName)
                    }
                ) {
                    Text("Choose media")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    createAlbumName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingDeleteAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAlbum = null },
            modifier = Modifier
                .widthIn(min = 340.dp, max = 460.dp)
                .fillMaxWidth(0.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Delete ${album.name}?") },
            text = { Text("The album’s photos and videos will move to Recently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteAlbum = null
                        onDeleteAlbum(album)
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAlbum = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

}


@Composable
private fun HiddenItemsPill(
    hiddenAlbumCount: Int,
    hiddenItemCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bouncyClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(34.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hidden albums",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hiddenItemsPillLabel(hiddenAlbumCount, hiddenItemCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Manage",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LockedMediaPill(
    lockedItemCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bouncyClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(34.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Locked media",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lockedMediaPillLabel(lockedItemCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RecentlyDeletedPill(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .bouncyClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                shape = RoundedCornerShape(15.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recently deleted",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Review items before they are removed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open recently deleted",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LayoutSelector(
    layoutMode: AlbumLayoutMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLayoutModeChange: (AlbumLayoutMode) -> Unit
) {
    val layoutModes = listOf(
        Triple(AlbumLayoutMode.Hero, "Hero", "Single large card"),
        Triple(AlbumLayoutMode.BigTiles, "Big tiles", "Featured hero + large tiles"),
        Triple(AlbumLayoutMode.Medium, "Medium", "2-column album covers"),
        Triple(AlbumLayoutMode.Basic, "Basic", "Standard 3-column view"),
        Triple(AlbumLayoutMode.Compact, "Compact", "Dense 4-column tiles"),
        Triple(AlbumLayoutMode.List, "List", "Detailed vertical list")
    )
    val currentLabel = layoutModes.firstOrNull { it.first == layoutMode }?.second ?: "Layout"

    Box {
        Surface(
            modifier = Modifier.clickable { onExpandedChange(true) },
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (layoutMode) {
                        AlbumLayoutMode.Hero, AlbumLayoutMode.BigTiles -> Icons.Filled.GridView
                        AlbumLayoutMode.Medium, AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> Icons.Filled.Apps
                        AlbumLayoutMode.List -> Icons.Filled.Folder
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        PremiumDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier.width(230.dp)
        ) {
            layoutModes.forEach { (mode, label, desc) ->
                PremiumDropdownMenuItem(
                    text = {
                        Column {
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                AlbumLayoutMode.Hero, AlbumLayoutMode.BigTiles -> Icons.Filled.GridView
                                AlbumLayoutMode.Medium, AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> Icons.Filled.Apps
                                AlbumLayoutMode.List -> Icons.Filled.Folder
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (layoutMode == mode) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        onLayoutModeChange(mode)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

private fun LazyListScope.heroAlbumRows(
    albums: List<Album>,
    height: androidx.compose.ui.unit.Dp,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    items(
        count = albums.size,
        key = { index -> "hero-album-${albums[index].id}" },
        contentType = { "hero-album-row" }
    ) { index ->
        val album = albums[index]
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = if (cardInteraction.suppressPlacementAnimation) null else spring(dampingRatio = 0.82f, stiffness = 350f)
            ).zIndex(albumReorderRowZIndex(listOf(album), cardInteraction))
        ) {
            AlbumHeroCard(
                album = album,
                height = height,
                activeTransitionAlbumId = activeTransitionAlbumId,
                cardInteraction = cardInteraction,
                deferThumbnailLoads = deferThumbnailLoads,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

private fun LazyListScope.bigAlbumRows(
    albums: List<Album>,
    columns: Int,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val rowCount = (albums.size + safeColumns - 1) / safeColumns
    items(
        count = rowCount,
        key = { rowIndex ->
            "big-album-row-$safeColumns-$rowIndex"
        },
        contentType = { "big-album-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * safeColumns
        val rowAlbums = albums.subList(startIndex, minOf(startIndex + safeColumns, albums.size))
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = if (cardInteraction.suppressPlacementAnimation) null else spring(dampingRatio = 0.82f, stiffness = 350f)
            ).zIndex(albumReorderRowZIndex(rowAlbums, cardInteraction))
        ) {
            BigAlbumRow(
                albums = rowAlbums,
                columns = safeColumns,
                activeTransitionAlbumId = activeTransitionAlbumId,
                cardInteraction = cardInteraction,
                deferThumbnailLoads = deferThumbnailLoads,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun LazyListScope.basicAlbumRows(
    albums: List<Album>,
    columns: Int,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val rowCount = (albums.size + safeColumns - 1) / safeColumns
    items(
        count = rowCount,
        key = { rowIndex ->
            "basic-album-row-$safeColumns-$rowIndex"
        },
        contentType = { "basic-album-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * safeColumns
        val rowAlbums = albums.subList(startIndex, minOf(startIndex + safeColumns, albums.size))
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = if (cardInteraction.suppressPlacementAnimation) null else spring(dampingRatio = 0.82f, stiffness = 350f)
            ).zIndex(albumReorderRowZIndex(rowAlbums, cardInteraction))
        ) {
            BasicAlbumRow(
                albums = rowAlbums,
                columns = safeColumns,
                activeTransitionAlbumId = activeTransitionAlbumId,
                cardInteraction = cardInteraction,
                deferThumbnailLoads = deferThumbnailLoads,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        }
        Spacer(Modifier.height(if (columns >= 4) 14.dp else 20.dp))
    }
}

private fun LazyListScope.listAlbumRows(
    albums: List<Album>,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    items(
        count = albums.size,
        key = { index -> "list-album-${albums[index].id}" },
        contentType = { "list-album-row" }
    ) { index ->
        val album = albums[index]
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = if (cardInteraction.suppressPlacementAnimation) null else spring(dampingRatio = 0.82f, stiffness = 350f)
            ).zIndex(albumReorderRowZIndex(listOf(album), cardInteraction))
        ) {
            AlbumListRow(
                album = album,
                activeTransitionAlbumId = activeTransitionAlbumId,
                cardInteraction = cardInteraction,
                deferThumbnailLoads = deferThumbnailLoads,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AlbumHeroCard(
    album: Album,
    height: androidx.compose.ui.unit.Dp,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    val animatedHeight by animateDpAsState(
        targetValue = height,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "heroAlbumHeight"
    )
    AlbumImageCard(
        album = album,
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight),
        cornerRadius = 24.dp,
        activeTransitionAlbumId = activeTransitionAlbumId,
        cardInteraction = cardInteraction,
        deferThumbnailLoads = deferThumbnailLoads,
        onAlbumClick = onAlbumClick,
        onAlbumBoundsChanged = onAlbumBoundsChanged
    )
}

@Composable
private fun BigAlbumRow(
    albums: List<Album>,
    columns: Int,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 12.dp
        val targetCellWidth = (maxWidth - spacing * (columns - 1)) / columns
        val cellWidth by animateDpAsState(
            targetValue = targetCellWidth,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            label = "bigAlbumCellWidth"
        )
        val cellHeight by animateDpAsState(
            targetValue = targetCellWidth.coerceAtMost(176.dp),
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            label = "bigAlbumCellHeight"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            albums.forEach { album ->
                key(album.id) {
                    AlbumImageCard(
                        album = album,
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight),
                        cornerRadius = 22.dp,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        cardInteraction = cardInteraction,
                        deferThumbnailLoads = deferThumbnailLoads,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = onAlbumBoundsChanged
                    )
                }
            }
            repeat(columns - albums.size) {
                Spacer(Modifier.width(cellWidth))
            }
        }
    }
}

@Composable
private fun BasicAlbumRow(
    albums: List<Album>,
    columns: Int,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 6.dp
        val targetCellWidth = (maxWidth - spacing * (columns - 1)) / columns
        val cellWidth by animateDpAsState(
            targetValue = targetCellWidth,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            label = "basicAlbumCellWidth"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            albums.forEach { album ->
                key(album.id) {
                    val albumBounds = remember(album.id) { AlbumBoundsRef() }
                    Column(
                        modifier = Modifier
                            .width(cellWidth)
                            .albumReorderMotion(album, cardInteraction)
                            .albumReorderGesture(album, cardInteraction)
                            .bouncyClickable { onAlbumClick(album, albumBounds.value) }
                            .hideForAlbumTransition(album.id, activeTransitionAlbumId)
                    ) {
                        Box {
                            GalleryImage(
                                imageRes = album.coverRes,
                                imageUri = album.coverUri,
                                contentDescription = album.name,
                                modifier = Modifier
                                    .size(cellWidth)
                                    .onGloballyPositioned { coordinates ->
                                        val bounds = coordinates.boundsInWindow()
                                        albumBounds.value = bounds
                                        onAlbumBoundsChanged(album, bounds)
                                    },
                                cornerRadius = if (columns >= 4) 14.dp else 18.dp,
                                thumbnailSize = 384,
                                animatedPlaceholder = false,
                                isVideo = album.hasVideoBadge,
                                deferUncachedLoad = deferThumbnailLoads
                            )
                            AlbumContextMenu(
                                album = album,
                                interaction = cardInteraction,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(if (columns >= 4) 4.dp else 6.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (columns >= 4) 12.sp else 14.sp,
                                lineHeight = if (columns >= 4) 15.sp else 18.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Text(
                            text = album.itemCount.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (columns >= 4) 10.sp else 11.5.sp,
                                lineHeight = if (columns >= 4) 13.sp else 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            repeat(columns - albums.size) {
                Spacer(Modifier.width(cellWidth))
            }
        }
    }
}

@Composable
private fun AlbumListRow(
    album: Album,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit
) {
    val albumBounds = remember(album.id) { AlbumBoundsRef() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .albumReorderMotion(album, cardInteraction)
            .albumReorderGesture(album, cardInteraction)
            .bouncyClickable { onAlbumClick(album, albumBounds.value) }
            .hideForAlbumTransition(album.id, activeTransitionAlbumId),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryImage(
                imageRes = album.coverRes,
                imageUri = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier
                    .size(60.dp)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        albumBounds.value = bounds
                        onAlbumBoundsChanged(album, bounds)
                    },
                cornerRadius = 12.dp,
                thumbnailSize = 256,
                animatedPlaceholder = false,
                isVideo = album.hasVideoBadge,
                deferUncachedLoad = deferThumbnailLoads
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (cardInteraction.pinnedAlbumId == album.id) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AlbumContextMenu(
                album = album,
                interaction = cardInteraction,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    listState: LazyListState,
    revealMediaId: String? = null,
    onGridViewportChanged: (Rect) -> Unit = {},
    columnBoost: Int = 0,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    activeSharedElementKey: Any? = null,
    albumEnterProgress: Float = 1f,
    gridMode: AlbumDetailGridMode,
    onGridModeChange: (AlbumDetailGridMode) -> Unit,
    sortMode: AlbumDetailSortMode,
    onSortModeChange: (AlbumDetailSortMode) -> Unit,
    selectedMediaIds: Set<String> = emptySet(),
    onMediaLongClick: (MediaItem) -> Unit = {},
    onMediaSelectionToggle: (MediaItem) -> Unit = {},
    onSelectionClear: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onHideSelected: () -> Unit = {},
    onMoveSelected: () -> Unit = {},
    onHideAlbum: (() -> Unit)? = null,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit = { _, _ -> },
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val currentGridMode by rememberUpdatedState(gridMode)
    val currentOnGridModeChange by rememberUpdatedState(onGridModeChange)
    val sortedMediaItems = remember(mediaItems, sortMode) {
        sortAlbumMedia(mediaItems, sortMode)
    }
    val columns = (gridMode.columns + columnBoost.coerceAtLeast(0)).coerceIn(1, 10)
    val context = LocalContext.current.applicationContext
    val mediaGeneration = remember(sortedMediaItems) {
        listOf(
            sortedMediaItems.size.toString(),
            sortedMediaItems.firstOrNull()?.id.orEmpty(),
            sortedMediaItems.lastOrNull()?.id.orEmpty(),
            sortedMediaItems.firstOrNull()?.sortTimestampMillis?.toString().orEmpty()
        ).joinToString(separator = ":")
    }
    val revealOffsetPx = with(LocalDensity.current) { 172.dp.roundToPx() }
    val tileBounds = remember(album.id) { mutableMapOf<String, Rect>() }
    val rootBounds = remember(album.id) { AlbumBoundsRef() }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val deferThumbnailLoads = remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    val trackMediaBounds = selectedMediaIds.isNotEmpty() || revealMediaId != null
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val pinchScaleAnim = remember(album.id) { Animatable(1f) }

    fun rootPoint(localPoint: Offset): Offset = Offset(
        rootBounds.value.left + localPoint.x,
        rootBounds.value.top + localPoint.y
    )

    fun hitMedia(localPoint: Offset): MediaItem? {
        val point = rootPoint(localPoint)
        return sortedMediaItems.firstOrNull { mediaItem ->
            tileBounds[mediaItem.id]?.contains(point) == true
        }
    }

    var lastColumns by remember(album.id) { mutableStateOf(columns) }
    LaunchedEffect(columns) {
        if (lastColumns != columns) {
            val targetRow = calculateAlbumGridAnchorRow(
                firstVisibleRow = listState.firstVisibleItemIndex,
                oldColumns = lastColumns,
                newColumns = columns,
                totalItems = sortedMediaItems.size
            )
            val scaledOffset = if (lastColumns > 0 && columns > 0) {
                (listState.firstVisibleItemScrollOffset.toLong() * lastColumns / columns).toInt()
            } else {
                listState.firstVisibleItemScrollOffset
            }
            listState.scrollToItem(targetRow, scaledOffset)
            lastColumns = columns
        }
    }

    LaunchedEffect(revealMediaId, sortedMediaItems, columns) {
        val mediaId = revealMediaId ?: return@LaunchedEffect
        val mediaIndex = sortedMediaItems.indexOfFirst { it.id == mediaId }
        if (mediaIndex >= 0) {
            listState.scrollToItem(mediaIndex / columns, scrollOffset = -revealOffsetPx)
        }
    }
    LaunchedEffect(album.id, sortMode, columns, mediaGeneration) {
        if (sortedMediaItems.isEmpty()) return@LaunchedEffect
        prefetchMediaThumbnails(
            context = context,
            mediaItems = sortedMediaItems,
            thumbnailSizes = listOf(384),
            maxItems = minOf(sortedMediaItems.size, columns * 4, 28)
        )
    }
    LaunchedEffect(album.id, sortMode, columns, mediaGeneration, listState) {
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to listState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collectLatest { (lastVisibleRow, isScrolling) ->
                if (isScrolling) return@collectLatest
                delay(120)
                val startIndex = ((lastVisibleRow + 1) * columns)
                    .coerceIn(0, sortedMediaItems.size)
                val endIndex = (startIndex + columns * 3)
                    .coerceAtMost(sortedMediaItems.size)
                if (startIndex < endIndex) {
                    prefetchMediaThumbnails(
                        context = context,
                        mediaItems = sortedMediaItems.subList(startIndex, endIndex),
                        thumbnailSizes = listOf(384),
                        maxItems = endIndex - startIndex
                    )
                }
            }
    }
    val revealProgress = albumEnterProgress.coerceIn(0f, 1f)
    val gridTopPadding = 150.dp
    val gridTopInsetPx = with(LocalDensity.current) { gridTopPadding.toPx() }
    val gridBottomInsetPx = with(LocalDensity.current) { contentPadding.calculateBottomPadding().toPx() }
    val interactiveGridReady = revealProgress >= 0.95f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned {
                rootBounds.value = it.boundsInWindow()
                onGridViewportChanged(gridReturnViewport(rootBounds.value, gridTopInsetPx, gridBottomInsetPx))
            }
            .pointerInput(isSelectionMode, sortedMediaItems) {
                var dragMode: AlbumDragSelectMode? = null
                var baseSelectedIds = emptySet<String>()
                val visitedMediaIds = mutableSetOf<String>()

                fun applyDragSelectionAt(localPoint: Offset) {
                    val hit = hitMedia(localPoint) ?: return
                    val mode = dragMode ?: return
                    if (!visitedMediaIds.add(hit.id)) return

                    when (mode) {
                        AlbumDragSelectMode.Add -> if (!baseSelectedIds.contains(hit.id)) onMediaSelectionToggle(hit)
                        AlbumDragSelectMode.Remove -> if (baseSelectedIds.contains(hit.id)) onMediaSelectionToggle(hit)
                    }
                }

                if (isSelectionMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            baseSelectedIds = latestSelectedMediaIds
                            visitedMediaIds.clear()
                            val hit = hitMedia(startOffset)
                            dragMode = hit?.let {
                                if (baseSelectedIds.contains(it.id)) AlbumDragSelectMode.Remove else AlbumDragSelectMode.Add
                            }
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
                                val mode = currentGridMode
                                if (cumulativeZoom >= 1.09f) {
                                    val nextMode = mode.zoomIn()
                                    if (nextMode != mode) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnGridModeChange(nextMode)
                                        cumulativeZoom = 1f
                                        coroutineScope.launch { pinchScaleAnim.snapTo(1f) }
                                    }
                                } else if (cumulativeZoom <= 0.91f) {
                                    val nextMode = mode.zoomOut()
                                    if (nextMode != mode) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnGridModeChange(nextMode)
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
        val gridTransformModifier = Modifier.graphicsLayer {
            alpha = GalleryMotion.smoothstep(0.48f, 0.78f, revealProgress)
            val scale = pinchScaleAnim.value
            scaleX = scale
            scaleY = scale
            transformOrigin = if (listState.firstVisibleItemIndex == 0) {
                TransformOrigin(0.5f, 0f)
            } else {
                TransformOrigin(0.5f, 0.5f)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(gridTransformModifier),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = gridTopPadding,
                end = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + if (selectedMediaIds.isNotEmpty()) 142.dp else 34.dp
            )
        ) {
            if (!interactiveGridReady && sortedMediaItems.isNotEmpty()) {
                albumDetailPreviewRows(mediaItems = sortedMediaItems, columns = columns)
            } else if (sortedMediaItems.isEmpty() && album.itemCount > 0) {
                albumDetailSkeletonRows(columns = columns)
            } else if (sortedMediaItems.isEmpty()) {
                item(key = "album-detail-empty", contentType = "album-detail-empty") {
                    Text(
                        text = "No photos here yet.",
                        modifier = Modifier.padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                albumDetailRows(
                    mediaItems = sortedMediaItems,
                    columns = columns,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedBoundsTransform = sharedBoundsTransform,
                    sharedElementPrefix = "album-${album.id}",
                    activeSharedElementKey = activeSharedElementKey,
                    selectedMediaIds = selectedMediaIds,
                    trackMediaBounds = trackMediaBounds,
                    deferThumbnailLoads = deferThumbnailLoads,
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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = GalleryMotion.smoothstep(0.40f, 0.70f, revealProgress) },
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 3.dp
        ) {
            AlbumDetailHeader(
                album = album,
                itemCount = sortedMediaItems.size,
                sortMode = sortMode,
                gridMode = gridMode,
                onSortModeChange = onSortModeChange,
                onGridModeChange = onGridModeChange,
                selectedCount = selectedMediaIds.size,
                totalVisibleCount = sortedMediaItems.size,
                onSelectionClear = onSelectionClear,
                onSelectAllVisible = onSelectAllVisible,
                onDeleteSelected = onDeleteSelected,
                onShareSelected = onShareSelected,
                onHideSelected = onHideSelected,
                onHideAlbum = onHideAlbum,
                onBack = onBack,
                modifier = Modifier.padding(start = 10.dp, top = 48.dp, end = 10.dp, bottom = 14.dp)
            )
        }
        if (selectedMediaIds.isEmpty() && sortedMediaItems.isNotEmpty()) {
            GalleryFastScroller(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(3f)
            )
        }
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f),
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(180),
                initialOffsetY = { -it / 2 }
            ),
            exit = fadeOut(tween(100)) + slideOutVertically(targetOffsetY = { -it / 2 })
        ) {
            MediaSelectionTopBar(
                selectedCount = selectedMediaIds.size,
                canSelectAll = selectedMediaIds.size < sortedMediaItems.size,
                onClear = onSelectionClear,
                onSelectAll = onSelectAllVisible,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
        }
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f),
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(190),
                initialOffsetY = { it / 2 + 24 }
            ),
            exit = fadeOut(tween(100)) + slideOutVertically(targetOffsetY = { it / 2 + 24 })
        ) {
            AlbumSelectionToolbar(
                onShare = onShareSelected,
                onMove = onMoveSelected,
                onDelete = onDeleteSelected,
                onHide = onHideSelected,
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 4.dp
                )
            )
        }
    }
}

internal fun calculateAlbumGridAnchorRow(
    firstVisibleRow: Int,
    oldColumns: Int,
    newColumns: Int,
    totalItems: Int
): Int {
    if (totalItems <= 0 || oldColumns <= 0 || newColumns <= 0) return 0
    val anchorItem = (firstVisibleRow * oldColumns).coerceIn(0, totalItems - 1)
    return anchorItem / newColumns
}

internal fun sortAlbumMedia(
    mediaItems: List<MediaItem>,
    sortMode: AlbumDetailSortMode
): List<MediaItem> {
    return when (sortMode) {
        AlbumDetailSortMode.Newest -> mediaItems
        AlbumDetailSortMode.Oldest -> mediaItems.asReversed()
        AlbumDetailSortMode.Name -> mediaItems.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
    }
}

@Composable
fun AlbumDetailTransitionPreview(
    album: Album,
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    gridMode: AlbumDetailGridMode,
    columnBoost: Int = 0,
    sortMode: AlbumDetailSortMode = AlbumDetailSortMode.Newest,
    initialFirstVisibleRow: Int = 0,
    initialFirstVisibleRowOffset: Int = 0,
    onBack: () -> Unit = {}
) {
    val columns = (gridMode.columns + columnBoost.coerceAtLeast(0)).coerceIn(1, 10)
    val sortedMediaItems = remember(mediaItems, sortMode) { sortAlbumMedia(mediaItems, sortMode) }
    val rowCount = (sortedMediaItems.size + columns - 1) / columns
    val previewListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleRow.coerceIn(0, (rowCount - 1).coerceAtLeast(0)),
        initialFirstVisibleItemScrollOffset = initialFirstVisibleRowOffset.coerceAtLeast(0)
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val horizontalPadding = 16.dp
    val spacing = 1.dp
    val safeColumns = columns.coerceAtLeast(1)
    val cellSize = (configuration.screenWidthDp.dp - horizontalPadding - spacing * (safeColumns - 1)) / safeColumns

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = previewListState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 150.dp,
                end = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 34.dp
            )
        ) {
            albumDetailTransitionRows(
                mediaItems = sortedMediaItems,
                columns = safeColumns,
                cellSize = cellSize
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 3.dp
        ) {
            AlbumDetailHeader(
                album = album,
                itemCount = sortedMediaItems.size,
                sortMode = sortMode,
                gridMode = gridMode,
                onSortModeChange = {},
                onGridModeChange = {},
                selectedCount = 0,
                totalVisibleCount = sortedMediaItems.size,
                onSelectionClear = {},
                onSelectAllVisible = {},
                onDeleteSelected = {},
                onShareSelected = {},
                onHideSelected = {},
                onHideAlbum = null,
                onBack = onBack,
                modifier = Modifier.padding(start = 10.dp, top = 48.dp, end = 10.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun AlbumDetailHeader(
    album: Album,
    itemCount: Int,
    sortMode: AlbumDetailSortMode,
    gridMode: AlbumDetailGridMode,
    onSortModeChange: (AlbumDetailSortMode) -> Unit,
    onGridModeChange: (AlbumDetailGridMode) -> Unit,
    selectedCount: Int,
    totalVisibleCount: Int,
    onSelectionClear: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onHideSelected: () -> Unit,
    onHideAlbum: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        GalleryScreenHeader(
            title = album.name,
            onBack = onBack,
            trailingContent = {
                Box {
                    PremiumOverflowButton(
                        expanded = menuExpanded,
                        contentDescription = "Album options",
                        onClick = { menuExpanded = true }
                    )
                    PremiumDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                    PremiumDropdownMenuItem(
                        text = { Text("Select all") },
                        leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                        enabled = totalVisibleCount > 0 && selectedCount < totalVisibleCount,
                        onClick = {
                            menuExpanded = false
                            onSelectAllVisible()
                        }
                    )
                    PremiumDropdownMenuItem(
                        text = { Text("Sort: ${sortMode.label()}") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSortModeChange(
                                when (sortMode) {
                                    AlbumDetailSortMode.Newest -> AlbumDetailSortMode.Oldest
                                    AlbumDetailSortMode.Oldest -> AlbumDetailSortMode.Name
                                    AlbumDetailSortMode.Name -> AlbumDetailSortMode.Newest
                                }
                            )
                        }
                    )
                    PremiumDropdownMenuItem(
                        text = { Text("Grid: ${gridMode.label()}") },
                        leadingIcon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGridModeChange(gridMode.next())
                        }
                    )
                    if (onHideAlbum != null && !album.isAllPhotos) {
                        PremiumDropdownMenuItem(
                            text = { Text("Hide album") },
                            leadingIcon = { Icon(Icons.Filled.Security, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onHideAlbum()
                            }
                        )
                    }
                    }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "%1$,d items, %2\$s, %3\$s".format(
                itemCount,
                sortMode.label(),
                gridMode.label()
            ),
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun AlbumSelectionToolbar(
    onShare: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    MediaSelectionActionBar(
        onShare = onShare,
        onLock = onHide,
        onMove = onMove,
        onDelete = onDelete,
        modifier = modifier
    )
}
private fun LazyListScope.albumDetailRows(
    mediaItems: List<MediaItem>,
    columns: Int,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementPrefix: String,
    activeSharedElementKey: Any? = null,
    selectedMediaIds: Set<String>,
    trackMediaBounds: Boolean,
    deferThumbnailLoads: State<Boolean>,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    val spacing = 1.dp
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "album-detail-row-$columns-$rowIndex" },
        contentType = { "album-media-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        Box(
            modifier = Modifier.animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = spring(dampingRatio = 0.82f, stiffness = 350f)
            )
        ) {
            AlbumDetailRow(
                mediaItems = rowItems,
                columns = columns,
                spacing = spacing,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedBoundsTransform = sharedBoundsTransform,
                sharedElementPrefix = sharedElementPrefix,
                activeSharedElementKey = activeSharedElementKey,
                selectedMediaIds = selectedMediaIds,
                trackMediaBounds = trackMediaBounds,
                deferThumbnailLoads = deferThumbnailLoads,
                onMediaBoundsChanged = onMediaBoundsChanged,
                onMediaLongClick = onMediaLongClick,
                onMediaSelectionToggle = onMediaSelectionToggle,
                onMediaClick = onMediaClick
            )
        }
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementPrefix: String,
    activeSharedElementKey: Any? = null,
    selectedMediaIds: Set<String>,
    trackMediaBounds: Boolean,
    deferThumbnailLoads: State<Boolean>,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        mediaItems.forEach { mediaItem ->
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
                onBoundsChanged = if (trackMediaBounds || activeSharedElementKey == sharedElementKey) {
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

private fun LazyListScope.albumDetailPreviewRows(
    mediaItems: List<MediaItem>,
    columns: Int
) {
    val spacing = 1.dp
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex ->
            "album-detail-preview-row-$columns-${mediaItems[rowIndex * columns].id}"
        },
        contentType = { "album-detail-preview-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        AlbumDetailPreviewRow(mediaItems = rowItems, columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

/**
 * Non-interactive, cache-only cells for the short album container transition.
 *
 * Building the regular [MediaThumbnail] grid here used to install click/long-click handlers,
 * bounds listeners, shared-element bookkeeping, and new decode work underneath the animated
 * overlay. A small cache-backed preview keeps the motion visually connected without competing
 * with the navigation animation or leaving interactive layers alive after it.
 */
private fun LazyListScope.albumDetailTransitionRows(
    mediaItems: List<MediaItem>,
    columns: Int,
    cellSize: androidx.compose.ui.unit.Dp
) {
    val spacing = 1.dp
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex ->
            "album-transition-row-$columns-${mediaItems[rowIndex * columns].id}"
        },
        contentType = { "album-transition-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(
            startIndex,
            minOf(startIndex + columns, mediaItems.size)
        )
        AlbumDetailTransitionRow(
            mediaItems = rowItems,
            columns = columns,
            cellSize = cellSize,
            spacing = spacing
        )
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailTransitionRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        mediaItems.forEach { mediaItem ->
            Box(modifier = Modifier.size(cellSize)) {
                GalleryImage(
                    imageRes = mediaItem.imageRes,
                    imageUri = mediaItem.contentUri,
                    contentDescription = mediaItem.title,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    thumbnailSize = 384,
                    cachedOnly = true,
                    isVideo = mediaItem.isVideo,
                    cacheRevision = mediaThumbnailCacheRevision(mediaItem)
                )
                if (mediaItem.isVideo && mediaItem.contentUri != null) {
                    VideoBadge(
                        duration = mediaItem.durationLabel.orEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    )
                }
            }
        }
        repeat(columns - mediaItems.size) {
            Spacer(Modifier.size(cellSize))
        }
    }
}
@Composable
private fun AlbumDetailPreviewRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        mediaItems.forEach { mediaItem ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            ) {
                GalleryImage(
                    imageRes = mediaItem.imageRes,
                    imageUri = mediaItem.contentUri,
                    contentDescription = mediaItem.title,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    thumbnailSize = 384,
                    cachedOnly = true,
                    isVideo = mediaItem.isVideo,
                    cacheRevision = mediaThumbnailCacheRevision(mediaItem)
                )
                if (mediaItem.isVideo && mediaItem.contentUri != null) {
                    VideoBadge(
                        duration = mediaItem.durationLabel.orEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    )
                }
            }
        }
        repeat(columns - mediaItems.size) {
            Spacer(Modifier.weight(1f).aspectRatio(1f))
        }
    }
}
private fun LazyListScope.albumDetailOpeningRows(columns: Int) {
    val spacing = 1.dp
    items(
        items = List(4) { it },
        key = { rowIndex -> "album-detail-opening-row-$columns-$rowIndex" },
        contentType = { "album-detail-opening-row" }
    ) {
        AlbumDetailOpeningRow(columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailOpeningRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(columns) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            )
        }
    }
}

private fun LazyListScope.albumDetailSkeletonRows(columns: Int) {
    val spacing = 1.dp
    items(
        items = List(5) { it },
        key = { rowIndex -> "album-detail-loading-row-$columns-$rowIndex" },
        contentType = { "album-detail-loading-row" }
    ) {
        AlbumDetailSkeletonRow(columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
@Composable
private fun AlbumsLoadingState(layoutMode: AlbumLayoutMode) {
    when (layoutMode) {
        AlbumLayoutMode.Hero -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        cornerRadius = 24.dp
                    )
                }
            }
        }
        AlbumLayoutMode.BigTiles -> {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                cornerRadius = 24.dp
            )
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = 12.dp
                val cellWidth = (maxWidth - spacing) / 2
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            repeat(2) {
                                SkeletonBlock(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .height(176.dp),
                                    cornerRadius = 22.dp
                                )
                            }
                        }
                    }
                }
            }
        }
        AlbumLayoutMode.Medium -> {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = 12.dp
                val cellWidth = (maxWidth - spacing) / 2
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            repeat(2) {
                                SkeletonBlock(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .height(176.dp),
                                    cornerRadius = 22.dp
                                )
                            }
                        }
                    }
                }
            }
        }
        AlbumLayoutMode.List -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        cornerRadius = 16.dp
                    )
                }
            }
        }
        AlbumLayoutMode.Basic, AlbumLayoutMode.Compact -> {
            val cols = if (layoutMode == AlbumLayoutMode.Compact) 4 else 3
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = if (cols == 4) 6.dp else 14.dp
                val cellWidth = (maxWidth - spacing * (cols - 1)) / cols
                Column(verticalArrangement = Arrangement.spacedBy(if (cols == 4) 14.dp else 20.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            repeat(cols) {
                                Column(modifier = Modifier.width(cellWidth)) {
                                    SkeletonBlock(
                                        modifier = Modifier.size(cellWidth),
                                        cornerRadius = if (cols == 4) 14.dp else 18.dp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SkeletonBlock(
                                        modifier = Modifier
                                            .fillMaxWidth(0.82f)
                                            .height(14.dp),
                                        cornerRadius = 7.dp
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
private fun AlbumImageCard(
    album: Album,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
    activeTransitionAlbumId: State<String?>?,
    cardInteraction: AlbumCardInteraction,
    deferThumbnailLoads: State<Boolean>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    val albumBounds = remember(album.id) { AlbumBoundsRef() }

    Box(
        modifier = modifier
            .albumReorderMotion(album, cardInteraction, morphFrame = true)
            .albumReorderGesture(album, cardInteraction)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                albumBounds.value = bounds
                onAlbumBoundsChanged(album, bounds)
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bouncyClickable { onAlbumClick(album, albumBounds.value) }
            .hideForAlbumTransition(album.id, activeTransitionAlbumId)
    ) {
        GalleryImage(
            imageRes = album.coverRes,
            imageUri = album.coverUri,
            contentDescription = album.name,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
            thumbnailSize = 512,
            animatedPlaceholder = false,
            isVideo = album.hasVideoBadge,
            deferUncachedLoad = deferThumbnailLoads
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.66f)),
                        startY = 120f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = album.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = "%1$,d".format(album.itemCount),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        AlbumContextMenu(
            album = album,
            interaction = cardInteraction,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun Modifier.albumReorderMotion(
    album: Album,
    interaction: AlbumCardInteraction,
    morphFrame: Boolean = false
): Modifier {
    val isDragging = interaction.draggedAlbumId == album.id
    val isDropTarget = interaction.dropTargetAlbumId == album.id && !isDragging
    // Retain spring state between idle/drag/settle. Creating it only after pickup starts
    // every animation at its end value, which causes a snap instead of a spring.
    val animatedScale = animateFloatAsState(
        targetValue = when {
            isDragging && !interaction.dropSettling -> 1.035f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 360f
        ),
        label = "album reorder scale"
    )
    val swapX = animateFloatAsState(
        targetValue = if (isDropTarget) interaction.dropTargetSwapOffset.x else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
        label = "album swap x"
    )
    val swapY = animateFloatAsState(
        targetValue = if (isDropTarget) interaction.dropTargetSwapOffset.y else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
        label = "album swap y"
    )
    val animatedRotation = animateFloatAsState(
        targetValue = if (isDragging && !interaction.dropSettling) {
            (interaction.dragOffset.x / 220f).coerceIn(-1.2f, 1.2f)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "album reorder rotation"
    )
    val shadowElevationDp = animateFloatAsState(
        targetValue = when {
            isDragging && !interaction.dropSettling -> 18f
            isDropTarget && !interaction.dropSettling -> 8f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 380f),
        label = "album shadow elevation"
    )
    val sourceBounds = interaction.draggedBounds
    val targetBounds = interaction.dropTargetBounds
    val targetWidthRatio = animateFloatAsState(
        targetValue = if (morphFrame && isDropTarget && sourceBounds != null && targetBounds != null) {
            sourceBounds.width / targetBounds.width.coerceAtLeast(1f)
        } else 1f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "album reorder frame width"
    )
    val targetHeightRatio = animateFloatAsState(
        targetValue = if (morphFrame && isDropTarget && sourceBounds != null && targetBounds != null) {
            sourceBounds.height / targetBounds.height.coerceAtLeast(1f)
        } else 1f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "album reorder frame height"
    )
    // A committed swap has already reached the destination. Remove all old-slot transforms
    // in the same frame as the new order; animating them back to zero would play a second
    // unwanted move. Spring state stays composed, while idle cards release their layers.
    if (interaction.draggedAlbumId == null && !interaction.dropSettling) return this
    val density = LocalDensity.current
    return this
        .zIndex(
            when {
                isDragging -> 6f
                isDropTarget -> 3f
                else -> 0f
            }
        )
        .graphicsLayer {
            val settledDragOffset = if (interaction.dropSettling) {
                interaction.dropSettleStartOffset +
                    (interaction.dropSettleTargetOffset - interaction.dropSettleStartOffset) *
                    interaction.dropSettleProgress.value
            } else interaction.dragOffset
            scaleX = animatedScale.value
            scaleY = animatedScale.value
            rotationZ = animatedRotation.value
            translationX = if (isDragging) settledDragOffset.x else swapX.value
            translationY = if (isDragging) settledDragOffset.y else swapY.value
            shadowElevation = with(density) { shadowElevationDp.value.dp.toPx() }
        }
        .layout { measurable, constraints ->
            if (!morphFrame || !constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            } else {
                // Re-measure the card frame, not its pixels: Crop reflows the cover and the
                // labels keep their natural size when a square trades places with the hero.
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val progress = interaction.dropSettleProgress.value.coerceIn(0f, 1f)
                val widthRatio = if (isDragging && interaction.dropSettling && sourceBounds != null && targetBounds != null) {
                    albumReorderFrameRatio(sourceBounds.width, targetBounds.width, progress)
                } else if (isDragging) 1f else targetWidthRatio.value
                val heightRatio = if (isDragging && interaction.dropSettling && sourceBounds != null && targetBounds != null) {
                    albumReorderFrameRatio(sourceBounds.height, targetBounds.height, progress)
                } else if (isDragging) 1f else targetHeightRatio.value
                val placeable = measurable.measure(Constraints.fixed(
                    (width * widthRatio).roundToInt().coerceAtLeast(1),
                    (height * heightRatio).roundToInt().coerceAtLeast(1)
                ))
                layout(width, height) {
                    placeable.placeRelative((width - placeable.width) / 2, (height - placeable.height) / 2)
                }
            }
        }
}

private fun albumReorderRowZIndex(albums: List<Album>, interaction: AlbumCardInteraction): Float = when {
    albums.any { it.id == interaction.draggedAlbumId } -> 6f
    albums.any { it.id == interaction.dropTargetAlbumId } -> 3f
    else -> 0f
}

private fun Modifier.hideForAlbumTransition(
    albumId: String,
    activeAlbumIdState: State<String?>?
): Modifier {
    if (activeAlbumIdState == null) return this
    return graphicsLayer {
        alpha = if (activeAlbumIdState.value == albumId) 0f else 1f
    }
}

@Composable
private fun AlbumContextMenu(
    album: Album,
    interaction: AlbumCardInteraction,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        PremiumDropdownMenu(
            expanded = interaction.contextAlbumId == album.id,
            onDismissRequest = interaction.onContextMenuDismiss
        ) {
            PremiumDropdownMenuItem(
                text = { Text(if (interaction.pinnedAlbumId == album.id) "Unpin album" else "Pin album") },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                onClick = {
                    interaction.onContextMenuDismiss()
                    interaction.onPinToggle(album)
                }
            )
            PremiumDropdownMenuItem(
                text = { Text("Delete album") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                enabled = interaction.canDelete(album),
                onClick = { interaction.onDeleteRequest(album) }
            )
        }
    }
}

@Composable
private fun Modifier.albumReorderGesture(
    album: Album,
    interaction: AlbumCardInteraction
): Modifier {
    val latestInteraction by rememberUpdatedState(interaction)
    val hapticFeedback = LocalHapticFeedback.current
    return pointerInput(album.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (latestInteraction.enabled) {
                    latestInteraction.onDragStart(album)
                } else {
                    latestInteraction.onContextMenuRequest(album)
                }
            },
            onDragEnd = {
                if (latestInteraction.enabled) latestInteraction.onDragEnd(true)
            },
            onDragCancel = {
                if (latestInteraction.enabled) latestInteraction.onDragEnd(false)
            },
            onDrag = { change, dragAmount ->
                if (latestInteraction.enabled) {
                    change.consume()
                    latestInteraction.onDrag(dragAmount)
                }
            }
        )
    }
}

private fun AlbumDetailSortMode.label(): String {
    return when (this) {
        AlbumDetailSortMode.Newest -> "Newest"
        AlbumDetailSortMode.Oldest -> "Oldest"
        AlbumDetailSortMode.Name -> "Name"
    }
}

private fun hiddenItemsPillLabel(hiddenAlbumCount: Int, hiddenItemCount: Int): String {
    return if (hiddenAlbumCount > 0) {
        "%1$,d albums, %2$,d items hidden".format(hiddenAlbumCount, hiddenItemCount)
    } else {
        "Choose albums to hide"
    }
}

private fun lockedMediaPillLabel(lockedItemCount: Int): String {
    return if (lockedItemCount > 0) {
        "%1$,d locked photos and videos".format(lockedItemCount)
    } else {
        "PIN, face, or fingerprint protected"
    }
}
