@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.nativegallery.R
import androidx.compose.ui.zIndex
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ViewerEnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ViewerExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val ViewerPhotoBackground = Color(0xFF111111)
private val ViewerPhotoStageBackground = Color.Transparent

enum class ViewerActionMode {
    Normal,
    RecentlyDeleted,
    Locked
}

private enum class VideoSideControl {
    Brightness,
    Volume
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoViewerOverlay(
    mediaItems: List<MediaItem>,
    mediaItem: MediaItem?,
    visible: Boolean,
    onClose: (Offset, Float, Float) -> Unit,
    onDelete: (MediaItem, Int) -> Unit,
    onHide: (MediaItem, Int) -> Unit = { _, _ -> },
    onRestore: (MediaItem, Int) -> Unit = { _, _ -> },
    actionMode: ViewerActionMode = ViewerActionMode.Normal,
    favoriteMediaIds: Set<String> = emptySet(),
    onFavoriteChange: (MediaItem, Boolean) -> Unit = { _, _ -> },
    onEdit: (MediaItem) -> Unit = {},
    onCurrentMediaChanged: (MediaItem) -> Unit = {},
    albumNameForMedia: (MediaItem) -> String? = { null },
    autoplayVideos: Boolean = true,
    startVideosMuted: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    predictiveBackProgressProvider: () -> Float = { 0f },
    predictiveBackDirectionProvider: () -> Float = { 1f }
) {
    if (!visible || mediaItem == null) {
        return
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val viewerItems = remember(mediaItems, mediaItem) {
        if (mediaItems.isNotEmpty()) mediaItems else listOf(mediaItem)
    }
    val viewerItemIds = remember(viewerItems) { viewerItems.map { it.id } }
    val selectedIndex = remember(viewerItemIds, mediaItem.id) {
        viewerItemIds.indexOf(mediaItem.id)
            .takeIf { it >= 0 }
            ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { viewerItems.size }
    )
    val scope = rememberCoroutineScope()
    var controlsVisible by remember { mutableStateOf(true) }
    var closeRequested by remember { mutableStateOf(false) }
    var lastSettledPage by rememberSaveable { mutableIntStateOf(selectedIndex) }
    var deleteDirection by rememberSaveable { mutableIntStateOf(1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var detailsDragOffset by remember { mutableFloatStateOf(0f) }
    var isViewerDragging by remember { mutableStateOf(false) }
    var detailsVisible by remember { mutableStateOf(false) }
    var activePhotoZoomed by remember { mutableStateOf(false) }
    val dismissThresholdPx = with(density) { 104.dp.toPx() }
    val detailsThresholdPx = with(density) { 76.dp.toPx() }
    val videoSideControlZonePx = with(density) { 88.dp.toPx() }
    val activePhotoDecodeSize = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density.density
    ) {
        (max(configuration.screenWidthDp, configuration.screenHeightDp) * density.density)
            .roundToInt()
            .coerceIn(1440, 3072)
    }

    LaunchedEffect(visible, selectedIndex, viewerItems.size) {
        if (visible && !pagerState.isScrollInProgress && pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
            lastSettledPage = selectedIndex
        }
    }

    LaunchedEffect(pagerState, viewerItems) {
        snapshotFlow { pagerState.settledPage.coerceIn(0, viewerItems.lastIndex) }.collect { page ->
            if (page != lastSettledPage) {
                deleteDirection = if (page > lastSettledPage) 1 else -1
                lastSettledPage = page
            }
        }
    }

    val currentPageForState = pagerState.settledPage.coerceIn(0, viewerItems.lastIndex)
    val currentItem = viewerItems.getOrNull(currentPageForState) ?: mediaItem
    val currentItemHasVideoPlayer = currentItem.isVideo && currentItem.contentUri != null
    val activity = remember(context) { context.findViewerActivity() }
    val originalWindowBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    val initialVideoBrightness = remember(context, originalWindowBrightness) {
        preferredViewerBrightness(context, originalWindowBrightness)
    }
    val videoBrightnessState = remember { mutableFloatStateOf(initialVideoBrightness) }
    val brightnessOverrideEnabled = remember { mutableStateOf(false) }
    val videoPlaybackVolumeState = remember(startVideosMuted) { mutableFloatStateOf(if (startVideosMuted) 0f else 1f) }
    val lastAudibleVolumeState = remember { mutableFloatStateOf(1f) }
    var activeMediaPlaced by remember(currentItem.id) { mutableStateOf(false) }

    DisposableEffect(activity, originalWindowBrightness) {
        onDispose {
            activity?.applyViewerBrightness(originalWindowBrightness)
        }
    }

    LaunchedEffect(activity, currentItemHasVideoPlayer, originalWindowBrightness) {
        snapshotFlow {
            brightnessOverrideEnabled.value to videoBrightnessState.floatValue
        }.collect { (overrideEnabled, brightness) ->
            val targetBrightness = if (currentItemHasVideoPlayer && overrideEnabled) {
                brightness.coerceIn(0.01f, 1f)
            } else {
                originalWindowBrightness
            }
            activity?.applyViewerBrightness(targetBrightness)
        }
    }

    LaunchedEffect(currentItem.id, visible) {
        if (visible) {
            activeMediaPlaced = false
            onCurrentMediaChanged(currentItem)
            detailsVisible = false
            activePhotoZoomed = false
            dragOffset = Offset.Zero
            detailsDragOffset = 0f
            closeRequested = false
        }
    }

    LaunchedEffect(currentPageForState, viewerItemIds) {
        val nearbyItems = (currentPageForState - 1..currentPageForState + 1)
            .mapNotNull { index -> viewerItems.getOrNull(index) }
        prefetchMediaThumbnails(
            context = context.applicationContext,
            mediaItems = nearbyItems,
            thumbnailSizes = listOf(1440, 512),
            maxItems = 3
        )
    }

    fun shareCurrentMedia() {
        val uri = currentItem.contentUri ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (currentItem.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    fun closeViewerWithChromeFade(closeOffset: Offset = Offset.Zero, closeScale: Float = 1f, closeBackdropAlpha: Float = 1f) {
        if (closeRequested) return
        closeRequested = true
        controlsVisible = false
        detailsVisible = false
        scope.launch {
            delay(GalleryMotion.ViewerChromeCloseDelayMillis)
            onClose(closeOffset, closeScale, closeBackdropAlpha)
        }
    }

    val animatedDragOffset by animateOffsetAsState(
        targetValue = dragOffset,
        animationSpec = if (isViewerDragging) {
            snap()
        } else {
            spring(
                dampingRatio = GalleryMotion.ViewerDismissDamping,
                stiffness = GalleryMotion.ViewerDismissStiffness
            )
        },
        label = "viewer dismiss offset"
    )
    val displayedDragOffset = if (isViewerDragging) {
        dragOffset
    } else {
        animatedDragOffset
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val predictiveProgress = predictiveBackProgressProvider().coerceIn(0f, 1f)
                val direction = predictiveBackDirectionProvider().coerceIn(-1f, 1f)
                val predictiveScale = 1f - 0.04f * predictiveProgress
                scaleX = predictiveScale
                scaleY = predictiveScale
                translationX = direction * 24.dp.toPx() * predictiveProgress
                transformOrigin = TransformOrigin(if (direction < 0f) 1f else 0f, 0.5f)
                shadowElevation = 20.dp.toPx() * predictiveProgress
                shape = RoundedCornerShape((28f * predictiveProgress).dp)
                clip = predictiveProgress > 0f
            }
            .background(
                if (currentItem.isVideo) {
                    Color.Black.copy(alpha = viewerBackdropAlpha(displayedDragOffset, dismissThresholdPx))
                } else {
                    ViewerPhotoBackground.copy(alpha = viewerBackdropAlpha(displayedDragOffset, dismissThresholdPx))
                }
            )
            .pointerInput(
                currentItem.id,
                currentItemHasVideoPlayer,
                controlsVisible,
                detailsVisible,
                dismissThresholdPx,
                detailsThresholdPx,
                videoSideControlZonePx,
                closeRequested,
                activePhotoZoomed
            ) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (closeRequested) return@awaitEachGesture
                    if (activePhotoZoomed) {
                        while (awaitPointerEvent().changes.any { it.pressed }) {
                            // The zoomable photo owns this gesture until every pointer is up.
                        }
                        return@awaitEachGesture
                    }
                    val startedInVideoControlZone = currentItemHasVideoPlayer &&
                        (
                            down.position.x <= videoSideControlZonePx ||
                                down.position.x >= size.width - videoSideControlZonePx
                        )
                    if (startedInVideoControlZone) return@awaitEachGesture
                    var activePointerId = down.id
                    var lastPosition = down.position
                    var lastUptimeMs = down.uptimeMillis
                    var dragVelocity = Offset.Zero
                    var passedTouchSlop = false
                    var handlingViewerDrag = false
                    var closingDetailsGesture = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!handlingViewerDrag && event.changes.count { it.pressed } > 1) break

                        val change = event.changes.firstOrNull { it.id == activePointerId }
                            ?: event.changes.firstOrNull { it.pressed }
                            ?: break
                        activePointerId = change.id
                        if (!change.pressed) break

                        val delta = change.position - lastPosition
                        val elapsedSeconds = ((change.uptimeMillis - lastUptimeMs).coerceAtLeast(1L)) / 1000f
                        dragVelocity = Offset(delta.x / elapsedSeconds, delta.y / elapsedSeconds)
                        lastPosition = change.position
                        lastUptimeMs = change.uptimeMillis

                        if (!passedTouchSlop) {
                            val totalDrag = change.position - down.position
                            if (positionDistance(change.position, down.position) < touchSlop) {
                                continue
                            }
                            val verticalIntent = abs(totalDrag.y) > abs(totalDrag.x) * 0.7f
                            val downwardIntent = totalDrag.y > touchSlop * 0.35f && abs(totalDrag.y) > abs(totalDrag.x) * 0.35f
                            val detailsIntent = totalDrag.y < -touchSlop * 0.35f && abs(totalDrag.y) > abs(totalDrag.x) * 0.7f
                            val dismissDetailsIntent = detailsVisible && downwardIntent
                            if (!verticalIntent && !downwardIntent && !detailsIntent) {
                                break
                            }
                            closingDetailsGesture = dismissDetailsIntent
                            passedTouchSlop = true
                            handlingViewerDrag = true
                            isViewerDragging = true
                            change.consume()
                        }

                        if (handlingViewerDrag) {
                            if (closingDetailsGesture) {
                                dragOffset = Offset.Zero
                                detailsDragOffset = 0f
                                change.consume()
                                continue
                            }
                            val nextOffset = dragOffset + delta
                            val detailsGestureActive = detailsDragOffset < 0f ||
                                (nextOffset.y < 0f && abs(nextOffset.y) > abs(nextOffset.x) * 0.8f)
                            if (detailsGestureActive) {
                                dragOffset = Offset.Zero
                                detailsDragOffset = (detailsDragOffset + delta.y)
                                    .coerceIn(-detailsThresholdPx * 1.8f, 0f)
                            } else {
                                detailsVisible = false
                                detailsDragOffset = 0f
                                dragOffset = Offset(
                                    x = nextOffset.x.coerceIn(-dismissThresholdPx * 1.55f, dismissThresholdPx * 1.55f),
                                    y = nextOffset.y.coerceIn(-dismissThresholdPx * 0.58f, dismissThresholdPx * 1.85f)
                                )
                            }
                            change.consume()
                        }
                    }

                    if (handlingViewerDrag) {
                        if (closingDetailsGesture) {
                            detailsVisible = false
                            controlsVisible = true
                            isViewerDragging = false
                            dragOffset = Offset.Zero
                        } else if (shouldDismissViewerDrag(dragOffset, dragVelocity, dismissThresholdPx)) {
                            isViewerDragging = false
                            closeViewerWithChromeFade(
                                closeOffset = renderedViewerDragOffset(dragOffset),
                                closeScale = viewerDismissScale(dragOffset, dismissThresholdPx),
                                closeBackdropAlpha = viewerBackdropAlpha(dragOffset, dismissThresholdPx)
                            )
                        } else {
                            if (detailsDragOffset < -detailsThresholdPx) {
                                detailsVisible = true
                                controlsVisible = false
                            }
                            isViewerDragging = false
                            dragOffset = Offset.Zero
                        }
                        detailsDragOffset = 0f
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pullProgress = viewerDismissProgress(displayedDragOffset, dismissThresholdPx)
                    val pullScale = viewerDismissScale(displayedDragOffset, dismissThresholdPx)
                    translationX = displayedDragOffset.x * 0.96f
                    translationY = displayedDragOffset.y * 0.96f
                    scaleX = pullScale
                    scaleY = pullScale
                },
            beyondViewportPageCount = 1,
            userScrollEnabled = !activePhotoZoomed &&
                !isViewerDragging &&
                dragOffset == Offset.Zero &&
                !closeRequested,
            key = { page -> viewerItems[page].id }
        ) { page ->
            val pageItem = viewerItems[page]
            ZoomableViewerMedia(
                mediaItem = pageItem,
                isActive = page == currentPageForState,
                controlsVisible = controlsVisible,
                activePhotoDecodeSize = activePhotoDecodeSize,
                videoBrightnessState = videoBrightnessState,
                brightnessOverrideEnabled = brightnessOverrideEnabled,
                videoPlaybackVolumeState = videoPlaybackVolumeState,
                autoplayVideos = autoplayVideos,
                lastAudibleVolumeState = lastAudibleVolumeState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedBoundsTransform = sharedBoundsTransform,
                sharedElementKeyPrefix = sharedElementKeyPrefix,
                isSharedTransitionReady = activeMediaPlaced,
                onActiveMediaPlaced = {
                    if (page == currentPageForState) {
                        activeMediaPlaced = true
                    }
                },
                onClose = { closeViewerWithChromeFade() },
                onToggleControls = {
                    if (activePhotoZoomed) return@ZoomableViewerMedia
                    if (detailsVisible) {
                        detailsVisible = false
                        controlsVisible = true
                    } else {
                        controlsVisible = !controlsVisible
                    }
                },
                onZoomStateChanged = { zoomed ->
                    if (page == currentPageForState) {
                        activePhotoZoomed = zoomed
                        if (zoomed) {
                            detailsVisible = false
                            controlsVisible = false
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(90))
        ) {
            ViewerTopBar(onClose = { closeViewerWithChromeFade() })
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(100)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(GalleryMotion.ViewerActionEnterMillis, easing = ViewerEnterEasing)
            ),
            exit = fadeOut(animationSpec = tween(90)) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(GalleryMotion.ViewerActionExitMillis, easing = ViewerExitEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (viewerItems.size > 1 && !currentItem.isVideo) {
                    ViewerFilmstrip(
                        mediaItems = viewerItems,
                        selectedIndex = pagerState.currentPage,
                        onItemClick = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }
                ViewerActionBar(
                    actionMode = actionMode,
                    isPhoto = !currentItem.isVideo,
                    isFavorite = favoriteMediaIds.contains(currentItem.id),
                    onFavorite = {
                        onFavoriteChange(currentItem, !favoriteMediaIds.contains(currentItem.id))
                    },
                    onEdit = { onEdit(currentItem) },
                    onShare = ::shareCurrentMedia,
                    onInfo = {
                        detailsVisible = true
                        controlsVisible = false
                    },
                    onRestore = { onRestore(currentItem, deleteDirection) },
                    onHide = { onHide(currentItem, deleteDirection) },
                    onDelete = { onDelete(currentItem, deleteDirection) }
                )
            }
        }

        AnimatedVisibility(
            visible = detailsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f),
            enter = fadeIn(animationSpec = tween(120)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(GalleryMotion.ViewerDetailsEnterMillis, easing = ViewerEnterEasing)
            ),
            exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(GalleryMotion.ViewerDetailsExitMillis, easing = ViewerExitEasing)
            )
        ) {
            MediaDetailsPanel(
                mediaItem = currentItem,
                albumName = albumNameForMedia(currentItem),
                onDismiss = {
                    detailsVisible = false
                    controlsVisible = true
                }
            )
        }
    }
}

@Composable
private fun ViewerTopBar(onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(start = 12.dp, top = 42.dp)
            .size(44.dp),
        color = Color.Black.copy(alpha = 0.42f),
        shape = CircleShape,
        shadowElevation = 0.dp
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close media",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ViewerFilmstrip(
    mediaItems: List<MediaItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0)
    )
    LaunchedEffect(selectedIndex, mediaItems.size) {
        if (mediaItems.isEmpty()) return@LaunchedEffect
        val viewportWidth = listState.layoutInfo.viewportSize.width
        val itemWidth = with(density) { 38.dp.roundToPx() }
        val centerOffset = -((viewportWidth - itemWidth) / 2).coerceAtLeast(0)
        listState.animateScrollToItem(
            index = selectedIndex.coerceIn(0, mediaItems.lastIndex),
            scrollOffset = centerOffset
        )
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(
            items = mediaItems,
            key = { _, item -> item.id }
        ) { index, item ->
            val selectedModifier = if (index == selectedIndex) {
                Modifier.border(2.dp, Color(0xFF26A8FF), RoundedCornerShape(8.dp))
            } else {
                Modifier
            }
            MediaThumbnail(
                mediaItem = item,
                modifier = Modifier
                    .size(width = 38.dp, height = 48.dp)
                    .then(selectedModifier),
                cornerRadius = 8.dp,
                onClick = { onItemClick(index) }
            )
        }
    }
}

@Composable
private fun ViewerActionBar(
    actionMode: ViewerActionMode,
    isPhoto: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onRestore: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 326.dp)
            .height(54.dp),
        color = Color.Black.copy(alpha = 0.44f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (actionMode == ViewerActionMode.Normal) {
                ViewerActionButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    selected = isFavorite,
                    onClick = onFavorite
                )
                if (isPhoto) {
                    ViewerActionButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = "Edit photo",
                        onClick = onEdit
                    )
                }
                ViewerActionButton(
                    icon = Icons.Filled.Share,
                    contentDescription = "Share",
                    onClick = onShare
                )
            }
            ViewerActionButton(
                icon = Icons.Filled.Info,
                contentDescription = "Info",
                onClick = onInfo
            )
            when (actionMode) {
                ViewerActionMode.Normal -> ViewerActionButton(
                    icon = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    onClick = onHide
                )
                ViewerActionMode.RecentlyDeleted -> ViewerActionButton(
                    icon = Icons.Filled.Restore,
                    contentDescription = "Restore",
                    onClick = onRestore
                )
                ViewerActionMode.Locked -> ViewerActionButton(
                    icon = Icons.Filled.LockOpen,
                    contentDescription = "Show",
                    onClick = onRestore
                )
            }
            ViewerActionButton(
                icon = Icons.Filled.Delete,
                contentDescription = if (actionMode == ViewerActionMode.RecentlyDeleted) "Delete forever" else "Delete",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF72CFFF) else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
@Composable
private fun MediaDetailsPanel(
    mediaItem: MediaItem,
    albumName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (mediaItem.isVideo) "Video" else "Photo"
    val mimeLabel = mediaItem.mimeType?.uppercase(Locale.US) ?: if (mediaItem.isVideo) "VIDEO" else "IMAGE"
    val resolutionLabel = mediaItem.resolutionLabel() ?: "Unknown"
    val sizeLabel = mediaItem.fileSizeBytes?.let(::formatFileSize) ?: "Unknown"
    val durationLabel = mediaItem.durationLabel?.takeIf { it.isNotBlank() }
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 24.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), RoundedCornerShape(100.dp))
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaItem.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailTypeChip(
                        icon = if (mediaItem.isVideo) Icons.Filled.Movie else Icons.Filled.Image,
                        text = "$typeLabel - $mimeLabel",
                        accent = accent
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                PremiumDetailRow(
                    icon = Icons.Filled.CalendarToday,
                    label = "Date",
                    value = mediaItem.dateLabel,
                    accent = accent
                )
                if (!albumName.isNullOrBlank()) {
                    PremiumDetailRow(
                        icon = Icons.Filled.Folder,
                        label = "Album",
                        value = albumName,
                        accent = accent
                    )
                }
                if (mediaItem.isVideo && durationLabel != null) {
                    PremiumDetailRow(
                        icon = Icons.Filled.AccessTime,
                        label = "Duration",
                        value = durationLabel,
                        accent = accent
                    )
                }
                PremiumDetailRow(
                    icon = Icons.Filled.AspectRatio,
                    label = "Resolution",
                    value = resolutionLabel,
                    accent = accent
                )
                PremiumDetailRow(
                    icon = Icons.Filled.Storage,
                    label = "File size",
                    value = sizeLabel,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun DetailTypeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = accent,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PremiumDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
private fun ZoomableViewerMedia(
    mediaItem: MediaItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    activePhotoDecodeSize: Int,
    videoBrightnessState: MutableFloatState,
    brightnessOverrideEnabled: MutableState<Boolean>,
    videoPlaybackVolumeState: MutableFloatState,
    autoplayVideos: Boolean,
    lastAudibleVolumeState: MutableFloatState,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    isSharedTransitionReady: Boolean,
    onActiveMediaPlaced: () -> Unit,
    onClose: () -> Unit,
    onToggleControls: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    val activeSharedElementKeyPrefix: String? = null
    val activeMediaAlpha = 1f

    if (mediaItem.isVideo && mediaItem.contentUri != null) {
        if (isActive) {
            ViewerVideoPlayer(
                mediaItem = mediaItem,
                isActive = true,
                controlsVisible = controlsVisible,
                brightnessState = videoBrightnessState,
                brightnessOverrideEnabled = brightnessOverrideEnabled,
                playbackVolumeState = videoPlaybackVolumeState,
                autoplay = autoplayVideos,
                lastAudibleVolumeState = lastAudibleVolumeState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedBoundsTransform = sharedBoundsTransform,
                sharedElementKeyPrefix = activeSharedElementKeyPrefix,
                isSharedTransitionReady = isSharedTransitionReady,
                onActiveMediaPlaced = onActiveMediaPlaced,
                onToggleControls = onToggleControls
            )
        } else {
            ViewerVideoPoster(mediaItem = mediaItem)
        }
        if (isActive) {
            LaunchedEffect(mediaItem.id) { onZoomStateChanged(false) }
        }
        return
    }

    var targetScale by remember(mediaItem.id) { mutableFloatStateOf(1f) }
    var targetOffset by remember(mediaItem.id) { mutableStateOf(Offset.Zero) }
    var gestureActive by remember(mediaItem.id) { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = GalleryMotion.ViewerTransformDamping,
            stiffness = GalleryMotion.ViewerTransformStiffness
        ),
        label = "viewer photo zoom scale"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = GalleryMotion.ViewerTransformDamping,
            stiffness = GalleryMotion.ViewerTransformStiffness
        ),
        label = "viewer photo pan offset"
    )
    val displayScale = if (gestureActive) targetScale else animatedScale
    val displayOffset = if (gestureActive) targetOffset else animatedOffset
    val mediaModifier = Modifier.fillMaxSize()

    LaunchedEffect(isActive, targetScale) {
        if (isActive) {
            onZoomStateChanged(targetScale > 1.02f)
        }
    }

    DisposableEffect(isActive) {
        onDispose {
            if (isActive) onZoomStateChanged(false)
        }
    }

    Box(
        modifier = mediaModifier
            .background(ViewerPhotoStageBackground)
            .graphicsLayer { alpha = activeMediaAlpha }
            .onGloballyPositioned {
                if (isActive) {
                    onActiveMediaPlaced()
                }
            }
            .pointerInput(mediaItem.id) {
                detectTapGestures(
                    onTap = {
                        if (targetScale <= 1.02f) onToggleControls()
                    },
                    onDoubleTap = { tapOffset ->
                        gestureActive = false
                        if (targetScale > 1.05f) {
                            targetScale = 1f
                            targetOffset = Offset.Zero
                        } else {
                            val nextScale = 2.8f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val desiredOffset = (center - tapOffset) * (nextScale - 1f)
                            val maxPanX = size.width * (nextScale - 1f) * 0.5f
                            val maxPanY = size.height * (nextScale - 1f) * 0.5f
                            targetScale = nextScale
                            targetOffset = desiredOffset.coercePan(maxPanX, maxPanY)
                        }
                    }
                )
            }
            .pointerInput(mediaItem.id) {
                awaitPointerEventScope {
                    var previousDistance = 0f
                    var previousCentroid = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.size > 1) {
                            gestureActive = true
                            val centroid = pressedChanges
                                .map { it.position }
                                .fold(Offset.Zero) { total, position -> total + position } / pressedChanges.size.toFloat()
                            val distance = pressedChanges
                                .map { positionDistance(it.position, centroid) }
                                .average()
                                .toFloat()
                            if (previousDistance > 0f) {
                                val zoom = (distance / previousDistance).takeIf { it.isFinite() } ?: 1f
                                val nextScale = (targetScale * zoom).coerceIn(0.55f, 5f)
                                val pan = centroid - previousCentroid
                                val maxPanX = size.width * (nextScale - 1f) * 0.5f
                                val maxPanY = size.height * (nextScale - 1f) * 0.5f
                                targetScale = nextScale
                                targetOffset = if (nextScale <= 1.02f) {
                                    Offset.Zero
                                } else {
                                    (targetOffset + pan).coercePan(maxPanX, maxPanY)
                                }
                                pressedChanges.forEach { it.consume() }
                            }
                            previousDistance = distance
                            previousCentroid = centroid
                        } else if (pressedChanges.size == 1 && targetScale > 1.02f) {
                            gestureActive = true
                            previousDistance = 0f
                            previousCentroid = Offset.Zero
                            val change = pressedChanges.single()
                            val pan = change.position - change.previousPosition
                            val maxPanX = size.width * (targetScale - 1f) * 0.5f
                            val maxPanY = size.height * (targetScale - 1f) * 0.5f
                            targetOffset = (targetOffset + pan).coercePan(maxPanX, maxPanY)
                            change.consume()
                        } else {
                            previousDistance = 0f
                            previousCentroid = Offset.Zero
                            if (gestureActive) {
                                gestureActive = false
                            }
                            if (targetScale < 0.62f) {
                                onClose()
                            } else if (targetScale <= 1.02f) {
                                targetScale = 1f
                                targetOffset = Offset.Zero
                            }
                        }
                    }
                }
            }
    ) {
        GalleryImage(
            imageRes = mediaItem.imageRes,
            imageUri = mediaItem.contentUri,
            contentDescription = mediaItem.title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = displayScale,
                    scaleY = displayScale,
                    translationX = displayOffset.x,
                    translationY = displayOffset.y
            ),
            cornerRadius = 0.dp,
            contentScale = ContentScale.Fit,
            thumbnailSize = if (isActive) activePhotoDecodeSize else 1440,
            loadQuality = if (isActive) ImageLoadQuality.HighQuality else ImageLoadQuality.Thumbnail,
            backgroundColor = ViewerPhotoStageBackground
        )
    }
}

@Composable
private fun ViewerVideoPoster(mediaItem: MediaItem) {
    GalleryImage(
        imageRes = mediaItem.imageRes,
        imageUri = mediaItem.contentUri,
        contentDescription = mediaItem.title,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        cornerRadius = 0.dp,
        contentScale = ContentScale.Fit,
        thumbnailSize = 1440,
        loadQuality = ImageLoadQuality.Thumbnail,
        backgroundColor = Color.Black
    )
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun ViewerVideoPlayer(
    mediaItem: MediaItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    brightnessState: MutableFloatState,
    autoplay: Boolean,
    brightnessOverrideEnabled: MutableState<Boolean>,
    playbackVolumeState: MutableFloatState,
    lastAudibleVolumeState: MutableFloatState,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    isSharedTransitionReady: Boolean,
    onActiveMediaPlaced: () -> Unit,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    val uri = mediaItem.contentUri ?: return
    var isPrepared by remember(uri) { mutableStateOf(false) }
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var videoFillFrame by remember(uri) { mutableStateOf(false) }
    var hasPlaybackError by remember(uri) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(uri) { mutableStateOf(false) }
    var durationMs by remember(uri) { mutableIntStateOf(0) }
    var positionMs by remember(uri) { mutableIntStateOf(0) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    val rootView = LocalView.current
    val density = LocalDensity.current
    val sideGestureZonePx = with(density) { 96.dp.toPx() }
    val videoBrightness = brightnessState.floatValue
    val playbackVolume = playbackVolumeState.floatValue
    val isMuted = playbackVolume <= 0.01f
    val player = remember(context, uri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            volume = playbackVolume
            playWhenReady = isActive && autoplay
            prepare()
        }
    }

    var visibleSideControl by remember(uri) { mutableStateOf<VideoSideControl?>(null) }
    var sideControlAutoHideKey by remember(uri) { mutableIntStateOf(0) }

    fun showSideControl(control: VideoSideControl) {
        visibleSideControl = control
        sideControlAutoHideKey += 1
    }

    LaunchedEffect(sideControlAutoHideKey, visibleSideControl) {
        val shownControl = visibleSideControl ?: return@LaunchedEffect
        delay(1050)
        if (visibleSideControl == shownControl) {
            visibleSideControl = null
        }
    }

    fun updatePlaybackVolume(targetVolume: Float) {
        val boundedVolume = targetVolume.coerceIn(0f, 1f)
        if (boundedVolume > 0.01f) {
            lastAudibleVolumeState.floatValue = boundedVolume
        }
        playbackVolumeState.floatValue = boundedVolume
        player.volume = boundedVolume
    }

    fun updateVideoBrightness(targetBrightness: Float) {
        brightnessState.floatValue = targetBrightness.coerceIn(0.01f, 1f)
        brightnessOverrideEnabled.value = true
    }

    fun seekToPosition(targetMs: Int) {
        val boundedTarget = if (durationMs > 0) {
            targetMs.coerceIn(0, durationMs)
        } else {
            targetMs.coerceAtLeast(0)
        }
        player.seekTo(boundedTarget.toLong())
        positionMs = boundedTarget
    }

    fun seekBy(deltaMs: Int) {
        seekToPosition(positionMs + deltaMs)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPrepared = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackError = false
                    durationMs = player.duration
                        .takeIf { it > 0L }
                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                        ?.toInt()
                        ?: durationMs
                } else if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0L)
                    positionMs = 0
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }

            override fun onPlayerError(error: PlaybackException) {
                hasPlaybackError = true
                isPrepared = false
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(playbackVolume, player) {
        player.volume = playbackVolume
    }

    DisposableEffect(isActive, isPrepared, isPlaying, rootView) {
        val keepScreenOn = isActive && isPrepared && isPlaying
        if (keepScreenOn) {
            rootView.keepScreenOn = true
        }
        onDispose {
            if (keepScreenOn) {
                rootView.keepScreenOn = false
            }
        }
    }
    LaunchedEffect(isActive, isPrepared, autoplay) {
        if (!isPrepared) return@LaunchedEffect

        if (isActive && autoplay && !player.isPlaying) {
            player.play()
        } else if (!isActive && player.isPlaying) {
            player.pause()
        }
    }

    LaunchedEffect(isActive, isPrepared, uri) {
        while (isActive && isPrepared) {
            isPlaying = player.isPlaying
            durationMs = player.duration
                .takeIf { it > 0L }
                ?.coerceAtMost(Int.MAX_VALUE.toLong())
                ?.toInt()
                ?: durationMs
            if (!isScrubbing) {
                positionMs = player.currentPosition
                    .coerceIn(0L, Int.MAX_VALUE.toLong())
                    .toInt()
            }
            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned {
                if (isActive) {
                    onActiveMediaPlaced()
                }
            }
    ) {
        GalleryImage(
            imageRes = mediaItem.imageRes,
            imageUri = mediaItem.contentUri,
            contentDescription = mediaItem.title,
            modifier = Modifier
                .fillMaxSize()
                .mediaSharedElement(
                    mediaItem = mediaItem,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedBoundsTransform = sharedBoundsTransform,
                    sharedElementKeyPrefix = sharedElementKeyPrefix
                )
                .graphicsLayer { alpha = if (hasRenderedFirstFrame) 0f else 1f },
            cornerRadius = 0.dp,
            contentScale = ContentScale.Fit,
            thumbnailSize = 1440,
            loadQuality = ImageLoadQuality.Thumbnail,
            backgroundColor = Color.Black
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (hasRenderedFirstFrame) 1f else 0f }
                .pointerInput(uri) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val sideControl = when {
                            down.position.x <= sideGestureZonePx -> VideoSideControl.Brightness
                            down.position.x >= size.width - sideGestureZonePx -> VideoSideControl.Volume
                            else -> null
                        }

                        if (sideControl == null) {
                            val touchSlop = viewConfiguration.touchSlop
                            var activePointerId = down.id
                            var moved = false
                            var released = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break
                                activePointerId = change.id
                                if (!change.pressed) {
                                    released = true
                                    break
                                }
                                if (positionDistance(change.position, down.position) > touchSlop) {
                                    moved = true
                                }
                            }

                            if (released && !moved) {
                                onToggleControls()
                            }
                            return@awaitEachGesture
                        }

                        showSideControl(sideControl)
                        var activePointerId = down.id
                        var lastY = down.position.y
                        var passedTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break
                            activePointerId = change.id
                            if (!change.pressed) break

                            val totalY = change.position.y - down.position.y
                            val deltaY = change.position.y - lastY
                            lastY = change.position.y
                            if (!passedTouchSlop) {
                                if (abs(totalY) < touchSlop) continue
                                passedTouchSlop = true
                            }

                            val valueDelta = (-deltaY / size.height.coerceAtLeast(1).toFloat()) * 1.25f
                            when (sideControl) {
                                VideoSideControl.Brightness -> updateVideoBrightness(brightnessState.floatValue + valueDelta)
                                VideoSideControl.Volume -> updatePlaybackVolume(playbackVolumeState.floatValue + valueDelta)
                            }
                            showSideControl(sideControl)
                            change.consume()
                        }
                        showSideControl(sideControl)
                    }
                },
            factory = { viewContext ->
                (LayoutInflater.from(viewContext).inflate(
                    R.layout.viewer_player_view,
                    FrameLayout(viewContext),
                    false
                ) as PlayerView).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = if (videoFillFrame) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        if (hasPlaybackError) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "This video could not be played here.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        AnimatedVisibility(
            visible = visibleSideControl == VideoSideControl.Brightness,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .zIndex(4f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            VerticalVideoControl(
                value = videoBrightness,
                onValueChange = { value -> updateVideoBrightness(value) },
                icon = Icons.Filled.Brightness6,
                contentDescription = "Video brightness"
            )
        }

        AnimatedVisibility(
            visible = visibleSideControl == VideoSideControl.Volume,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .zIndex(4f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            VerticalVideoControl(
                value = playbackVolume,
                onValueChange = { value -> updatePlaybackVolume(value) },
                icon = if (isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = "Video volume"
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && isPrepared,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(start = 22.dp, top = 42.dp, end = 22.dp, bottom = 86.dp)
            ) {
                Slider(
                    value = positionMs.coerceIn(0, durationMs.coerceAtLeast(1)).toFloat(),
                    onValueChange = { value ->
                        isScrubbing = true
                        positionMs = value.roundToInt()
                    },
                    onValueChangeFinished = {
                        seekToPosition(positionMs)
                        isScrubbing = false
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlaybackTime(positionMs),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatPlaybackTime(durationMs),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoControlButton(
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute video" else "Mute video",
                        size = 38.dp,
                        iconSize = 20.dp,
                        containerAlpha = if (isMuted) 0.56f else 0.34f,
                        onClick = {
                            if (isMuted) {
                                updatePlaybackVolume(lastAudibleVolumeState.floatValue)
                            } else {
                                lastAudibleVolumeState.floatValue = playbackVolume
                                updatePlaybackVolume(0f)
                            }
                        }
                    )
                    VideoControlButton(
                        icon = Icons.Filled.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        size = 42.dp,
                        iconSize = 22.dp,
                        onClick = { seekBy(-10_000) }
                    )
                    VideoControlButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause video" else "Play video",
                        size = 54.dp,
                        iconSize = 30.dp,
                        containerAlpha = 0.48f,
                        onClick = {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        }
                    )
                    VideoControlButton(
                        icon = Icons.Filled.Forward10,
                        contentDescription = "Forward 10 seconds",
                        size = 42.dp,
                        iconSize = 22.dp,
                        onClick = { seekBy(10_000) }
                    )
                    VideoControlButton(
                        icon = Icons.Filled.AspectRatio,
                        contentDescription = if (videoFillFrame) "Fit video" else "Fill screen",
                        size = 38.dp,
                        iconSize = 20.dp,
                        containerAlpha = if (videoFillFrame) 0.56f else 0.34f,
                        onClick = { videoFillFrame = !videoFillFrame }
                    )
                }
        }
    }
}

}

@Composable
private fun VerticalVideoControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val controlledValue = value.coerceIn(0f, 1f)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Surface(
        modifier = modifier
            .size(width = 48.dp, height = 188.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(controlledValue, 0f..1f)
                setProgress { targetValue ->
                    currentOnValueChange(targetValue.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    fun updateValue(positionY: Float) {
                        if (size.height <= 0) return
                        val nextValue = 1f - positionY / size.height.toFloat()
                        currentOnValueChange(nextValue.coerceIn(0f, 1f))
                    }

                    down.consume()
                    updateValue(down.position.y)
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        updateValue(change.position.y)
                        change.consume()
                    }
                }
            },
        color = Color.Black.copy(alpha = 0.52f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(128.dp)
                    .width(5.dp)
                    .background(Color.White.copy(alpha = 0.26f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(controlledValue)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun VideoControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    containerAlpha: Float = 0.34f,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .background(Color.Black.copy(alpha = containerAlpha), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}


private fun viewerBackdropAlpha(offset: Offset, dismissThresholdPx: Float): Float {
    return 1f - viewerDismissProgress(offset, dismissThresholdPx)
}

private fun renderedViewerDragOffset(offset: Offset): Offset {
    return Offset(offset.x * 0.96f, offset.y * 0.96f)
}

private fun viewerDismissScale(offset: Offset, dismissThresholdPx: Float): Float {
    return 1f - viewerDismissProgress(offset, dismissThresholdPx) * 0.3f
}

private fun viewerDismissProgress(offset: Offset, dismissThresholdPx: Float): Float {
    if (dismissThresholdPx <= 0f) return 0f
    val downwardOffset = offset.y.coerceAtLeast(0f)
    val distance = sqrt(offset.x * offset.x + downwardOffset * downwardOffset)
    return (distance / (dismissThresholdPx * 1.15f)).coerceIn(0f, 1f)
}

private fun shouldDismissViewerDrag(
    offset: Offset,
    velocity: Offset,
    dismissThresholdPx: Float
): Boolean {
    if (dismissThresholdPx <= 0f) return false
    val downwardOffset = offset.y.coerceAtLeast(0f)
    val distance = sqrt(offset.x * offset.x + downwardOffset * downwardOffset)
    val velocityMagnitude = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
    return downwardOffset > dismissThresholdPx * 0.88f ||
        (downwardOffset > dismissThresholdPx * 0.32f && distance > dismissThresholdPx * 1.18f) ||
        (downwardOffset > dismissThresholdPx * 0.22f && velocity.y > 900f) ||
        (downwardOffset > dismissThresholdPx * 0.28f && velocityMagnitude > 1300f)
}

private fun MediaItem.resolutionLabel(): String? {
    val mediaWidth = width?.takeIf { it > 0 }
    val mediaHeight = height?.takeIf { it > 0 }
    return if (mediaWidth != null && mediaHeight != null) {
        "%1$,d x %2$,d".format(Locale.US, mediaWidth, mediaHeight)
    } else {
        null
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024.0 && unitIndex < units.lastIndex) {
        size /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0 || size >= 10.0) {
        String.format(Locale.US, "%.0f %s", size, units[unitIndex])
    } else {
        String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }
}
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.mediaSharedElement(
    mediaItem: MediaItem,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedBoundsTransform: BoundsTransform?,
    sharedElementKeyPrefix: String?
): Modifier {
    val transitionScope = sharedTransitionScope ?: return this
    val visibilityScope = animatedVisibilityScope ?: return this
    val keyPrefix = sharedElementKeyPrefix ?: return this

    return with(transitionScope) {
        val sharedContentState = rememberSharedContentState(key = "$keyPrefix-media-${mediaItem.id}")
        if (sharedBoundsTransform != null) {
            this@mediaSharedElement.sharedElement(
                state = sharedContentState,
                animatedVisibilityScope = visibilityScope,
                boundsTransform = sharedBoundsTransform
            )
        } else {
            this@mediaSharedElement.sharedElement(
                state = sharedContentState,
                animatedVisibilityScope = visibilityScope
            )
        }
    }
}

private tailrec fun Context.findViewerActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findViewerActivity()
        else -> null
    }
}

private fun preferredViewerBrightness(context: Context, originalWindowBrightness: Float): Float {
    if (originalWindowBrightness in 0f..1f) {
        return originalWindowBrightness.coerceIn(0.01f, 1f)
    }

    return runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        ) / 255f
    }.getOrDefault(0.5f).coerceIn(0.01f, 1f)
}

private fun Activity.applyViewerBrightness(brightness: Float) {
    if (!brightness.isFinite()) return
    val windowAttributes = window.attributes
    if (windowAttributes.screenBrightness == brightness) return
    windowAttributes.screenBrightness = brightness
    window.attributes = windowAttributes
}

private fun Offset.coercePan(maxX: Float, maxY: Float): Offset {
    return Offset(
        x = x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
        y = y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
    )
}

private fun positionDistance(position: Offset, centroid: Offset): Float {
    val x = position.x - centroid.x
    val y = position.y - centroid.y
    return sqrt(x * x + y * y)
}

private fun formatPlaybackTime(timeMs: Int): String {
    val totalSeconds = (timeMs.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
