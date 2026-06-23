@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaThumbnail
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ViewerEnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ViewerExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoViewerOverlay(
    mediaItems: List<MediaItem>,
    mediaItem: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    onDelete: (MediaItem, Int) -> Unit,
    onCurrentMediaChanged: (MediaItem) -> Unit = {},
    albumNameForMedia: (MediaItem) -> String? = { null },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null
) {
    BackHandler(enabled = visible && mediaItem != null, onBack = onClose)

    if (!visible || mediaItem == null) {
        return
    }

    val context = LocalContext.current
    val viewerItems = if (mediaItems.isNotEmpty()) mediaItems else listOf(mediaItem)
    val selectedIndex = viewerItems.indexOfFirst { it.id == mediaItem.id }
        .takeIf { it >= 0 }
        ?: 0
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { viewerItems.size }
    )
    val scope = rememberCoroutineScope()
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var lastSettledPage by rememberSaveable { mutableIntStateOf(selectedIndex) }
    var deleteDirection by rememberSaveable { mutableIntStateOf(1) }
    var verticalDragOffset by remember { mutableStateOf(0f) }
    var isViewerDragging by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val dismissThresholdPx = with(LocalDensity.current) { 132.dp.toPx() }

    LaunchedEffect(visible, selectedIndex, viewerItems.size) {
        if (visible && pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != lastSettledPage) {
                deleteDirection = if (page > lastSettledPage) 1 else -1
                lastSettledPage = page
            }
        }
    }

    val currentItem = viewerItems.getOrNull(pagerState.currentPage) ?: mediaItem
    LaunchedEffect(currentItem.id, visible) {
        if (visible) {
            onCurrentMediaChanged(currentItem)
            controlsVisible = true
            verticalDragOffset = 0f
        }
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

    val animatedVerticalDragOffset by animateFloatAsState(
        targetValue = verticalDragOffset,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
        label = "viewer dismiss offset"
    )
    val displayedVerticalDragOffset = if (isViewerDragging) {
        verticalDragOffset
    } else {
        animatedVerticalDragOffset
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (currentItem.isVideo) Color.Black else Color.White)
            .pointerInput(currentItem.id, dismissThresholdPx) {
                detectVerticalDragGestures(
                    onDragStart = { isViewerDragging = true },
                    onVerticalDrag = { change, dragAmount ->
                        val shouldPullViewer = dragAmount > 0f || verticalDragOffset > 0f
                        if (shouldPullViewer) {
                            verticalDragOffset = (verticalDragOffset + dragAmount)
                                .coerceIn(0f, dismissThresholdPx * 2.4f)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        isViewerDragging = false
                        if (verticalDragOffset > dismissThresholdPx) {
                            onClose()
                        }
                        verticalDragOffset = 0f
                    },
                    onDragCancel = {
                        isViewerDragging = false
                        verticalDragOffset = 0f
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pullDown = displayedVerticalDragOffset.coerceAtLeast(0f)
                    val pullScale = 1f - (pullDown / (dismissThresholdPx * 5f)).coerceIn(0f, 0.08f)
                    translationY = pullDown
                    scaleX = pullScale
                    scaleY = pullScale
                },
            beyondViewportPageCount = 1,
            key = { page -> viewerItems[page].id }
        ) { page ->
            val pageItem = viewerItems[page]
            ZoomableViewerMedia(
                mediaItem = pageItem,
                isActive = page == pagerState.currentPage,
                controlsVisible = controlsVisible,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedBoundsTransform = sharedBoundsTransform,
                onToggleControls = { controlsVisible = !controlsVisible }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(90))
        ) {
            ViewerTopBar(onClose = onClose)
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(100)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(180, easing = ViewerEnterEasing)
            ),
            exit = fadeOut(animationSpec = tween(90)) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(140, easing = ViewerExitEasing)
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
                    isFavorite = favoriteIds.contains(currentItem.id),
                    onFavorite = {
                        favoriteIds = if (favoriteIds.contains(currentItem.id)) {
                            favoriteIds - currentItem.id
                        } else {
                            favoriteIds + currentItem.id
                        }
                    },
                    onShare = ::shareCurrentMedia,
                    onDelete = { onDelete(currentItem, deleteDirection) }
                )
            }
        }
    }
}
@Composable
private fun ViewerTopBar(onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(start = 14.dp, top = 44.dp)
            .size(56.dp),
        color = Color.White,
        shape = CircleShape,
        shadowElevation = 10.dp
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close media",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
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
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    .size(width = 44.dp, height = 56.dp)
                    .then(selectedModifier),
                cornerRadius = 8.dp,
                onClick = { onItemClick(index) }
            )
        }
    }
}

@Composable
private fun ViewerActionBar(
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(78.dp),
        color = Color.White,
        shape = RoundedCornerShape(40.dp),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerActionButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                selected = isFavorite,
                onClick = onFavorite
            )
            ViewerActionButton(
                icon = Icons.Filled.Share,
                contentDescription = "Share",
                onClick = onShare
            )
            ViewerActionButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Delete",
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
    IconButton(onClick = onClick, modifier = Modifier.size(58.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF26A8FF) else Color.Black,
            modifier = Modifier.size(30.dp)
        )
    }
}
@Composable
private fun MediaDetailsPanel(
    mediaItem: MediaItem,
    albumName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        color = Color.Black.copy(alpha = 0.86f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 26.dp, bottomEnd = 26.dp),
        tonalElevation = 0.dp,
        shadowElevation = 18.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(
                text = mediaItem.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(Modifier.height(16.dp))
            DetailLine(label = "Type", value = if (mediaItem.isVideo) "Video" else "Photo")
            DetailLine(label = "Date", value = mediaItem.dateLabel)
            if (!albumName.isNullOrBlank()) {
                DetailLine(label = "Album", value = albumName)
            }
            mediaItem.durationLabel?.takeIf { it.isNotBlank() }?.let { duration ->
                DetailLine(label = "Duration", value = duration)
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
@Composable
private fun ZoomableViewerMedia(
    mediaItem: MediaItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    onToggleControls: () -> Unit
) {
    if (mediaItem.isVideo && mediaItem.contentUri != null) {
        ViewerVideoPlayer(
            mediaItem = mediaItem,
            isActive = isActive,
            controlsVisible = controlsVisible,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedBoundsTransform = sharedBoundsTransform,
            onToggleControls = onToggleControls
        )
        return
    }

    var targetScale by remember(mediaItem.id) { mutableStateOf(1f) }
    var targetOffset by remember(mediaItem.id) { mutableStateOf(Offset.Zero) }
    var gestureActive by remember(mediaItem.id) { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        label = "viewer photo zoom scale"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        label = "viewer photo pan offset"
    )
    val displayScale = if (gestureActive) targetScale else animatedScale
    val displayOffset = if (gestureActive) targetOffset else animatedOffset
    val mediaModifier = Modifier.mediaSharedElement(
        mediaItem = mediaItem,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedBoundsTransform = sharedBoundsTransform
    )

    GalleryImage(
        imageRes = mediaItem.imageRes,
        imageUri = mediaItem.contentUri,
        contentDescription = mediaItem.title,
        modifier = mediaModifier
            .fillMaxSize()
            .pointerInput(mediaItem.id) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        gestureActive = false
                        if (targetScale > 1.05f) {
                            targetScale = 1f
                            targetOffset = Offset.Zero
                        } else {
                            targetScale = 2.35f
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
                                val nextScale = (targetScale * zoom).coerceIn(1f, 5f)
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
                        } else {
                            previousDistance = 0f
                            previousCentroid = Offset.Zero
                            if (gestureActive) {
                                gestureActive = false
                            }
                            if (targetScale <= 1.02f) {
                                targetScale = 1f
                                targetOffset = Offset.Zero
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = displayScale,
                scaleY = displayScale,
                translationX = displayOffset.x,
                translationY = displayOffset.y
            ),
        cornerRadius = 0.dp,
        contentScale = ContentScale.Fit,
        thumbnailSize = 4096,
        loadQuality = ImageLoadQuality.HighQuality
    )
}
@Composable
private fun ViewerVideoPlayer(
    mediaItem: MediaItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    val uri = mediaItem.contentUri ?: return
    val playerState = remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    val surfaceState = remember(uri) { mutableStateOf<Surface?>(null) }
    var textureView by remember(uri) { mutableStateOf<TextureView?>(null) }
    var isPrepared by remember(uri) { mutableStateOf(false) }
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var durationMs by remember(uri) { mutableStateOf(0) }
    var positionMs by remember(uri) { mutableStateOf(0) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    var videoWidth by remember(uri) { mutableStateOf(0) }
    var videoHeight by remember(uri) { mutableStateOf(0) }
    val activeState by rememberUpdatedState(isActive)

    fun releasePlayer() {
        playerState.value?.let { player ->
            runCatching {
                player.setOnPreparedListener(null)
                player.setOnVideoSizeChangedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                player.reset()
                player.release()
            }
        }
        playerState.value = null
        surfaceState.value?.release()
        surfaceState.value = null
        isPrepared = false
        isPlaying = false
        durationMs = 0
        positionMs = 0
        isScrubbing = false
    }

    fun seekToPosition(targetMs: Int) {
        val player = playerState.value ?: return
        val boundedTarget = if (durationMs > 0) {
            targetMs.coerceIn(0, durationMs)
        } else {
            targetMs.coerceAtLeast(0)
        }
        runCatching { player.seekTo(boundedTarget) }
        positionMs = boundedTarget
    }

    fun seekBy(deltaMs: Int) {
        seekToPosition(positionMs + deltaMs)
    }

    fun preparePlayer(view: TextureView) {
        if (!view.isAvailable) return
        releasePlayer()
        val surfaceTexture = view.surfaceTexture ?: return
        val surface = Surface(surfaceTexture)
        surfaceState.value = surface

        val player = MediaPlayer()
        playerState.value = player
        runCatching {
            player.setDataSource(context, uri)
            player.setSurface(surface)
            player.setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                view.applyVideoFitTransform(width, height)
            }
            player.setOnPreparedListener { preparedPlayer ->
                isPrepared = true
                durationMs = preparedPlayer.duration.coerceAtLeast(0)
                positionMs = preparedPlayer.currentPosition.coerceAtLeast(0)
                view.applyVideoFitTransform(videoWidth, videoHeight)
                if (activeState) {
                    preparedPlayer.start()
                    isPlaying = true
                }
            }
            player.setOnCompletionListener { completedPlayer ->
                isPlaying = false
                seekToPosition(0)
                runCatching { completedPlayer.seekTo(0) }
            }
            player.setOnErrorListener { _, _, _ ->
                isPrepared = false
                isPlaying = false
                true
            }
            player.prepareAsync()
        }.onFailure {
            releasePlayer()
        }
    }

    val preparePlayerForSurface by rememberUpdatedState<(TextureView) -> Unit>(
        newValue = { view -> preparePlayer(view) }
    )

    LaunchedEffect(isActive, isPrepared) {
        val player = playerState.value ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect

        if (isActive && !player.isPlaying) {
            player.start()
            isPlaying = true
        } else if (!isActive && player.isPlaying) {
            player.pause()
            isPlaying = false
        }
    }

    LaunchedEffect(isActive, isPrepared, uri) {
        while (isActive && isPrepared) {
            val player = playerState.value
            if (player != null) {
                isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
                durationMs = runCatching { player.duration.coerceAtLeast(0) }.getOrDefault(durationMs)
                if (!isScrubbing) {
                    positionMs = runCatching { player.currentPosition.coerceAtLeast(0) }.getOrDefault(positionMs)
                }
            }
            delay(250)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            releasePlayer()
            textureView = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GalleryImage(
            imageRes = mediaItem.imageRes,
            imageUri = mediaItem.contentUri,
            contentDescription = mediaItem.title,
            modifier = Modifier
                .mediaSharedElement(
                    mediaItem = mediaItem,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedBoundsTransform = sharedBoundsTransform
                )
                .fillMaxSize(),
            cornerRadius = 0.dp,
            contentScale = ContentScale.Fit,
            thumbnailSize = 1440,
            loadQuality = ImageLoadQuality.Thumbnail
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isPrepared) 1f else 0f }
                .pointerInput(uri) {
                    detectTapGestures(onTap = { onToggleControls() })
                },
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isOpaque = true
                    tag = uri
                    val texture = this
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            if (playerState.value == null) {
                                preparePlayerForSurface(texture)
                            } else {
                                texture.applyVideoFitTransform(videoWidth, videoHeight)
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            texture.applyVideoFitTransform(videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            releasePlayer()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                    }
                    textureView = this
                }
            },
            update = { view ->
                textureView = view
                if (view.tag != uri) {
                    view.tag = uri
                    if (view.isAvailable) {
                        preparePlayer(view)
                    }
                } else if (view.isAvailable && playerState.value == null) {
                    preparePlayer(view)
                } else {
                    view.applyVideoFitTransform(videoWidth, videoHeight)
                }
            }
        )

        AnimatedVisibility(
            visible = controlsVisible && isPrepared,
            modifier = Modifier.align(Alignment.BottomCenter),
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
                    .padding(start = 22.dp, top = 62.dp, end = 22.dp, bottom = 118.dp)
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoControlButton(
                        icon = Icons.Filled.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        size = 48.dp,
                        iconSize = 27.dp,
                        onClick = { seekBy(-10_000) }
                    )
                    Spacer(Modifier.width(18.dp))
                    VideoControlButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause video" else "Play video",
                        size = 62.dp,
                        iconSize = 36.dp,
                        containerAlpha = 0.48f,
                        onClick = {
                            val player = playerState.value ?: return@VideoControlButton
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    )
                    Spacer(Modifier.width(18.dp))
                    VideoControlButton(
                        icon = Icons.Filled.Forward10,
                        contentDescription = "Forward 10 seconds",
                        size = 48.dp,
                        iconSize = 27.dp,
                        onClick = { seekBy(10_000) }
                    )
                }
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

private fun TextureView.applyVideoFitTransform(videoWidth: Int, videoHeight: Int) {
    if (width == 0 || height == 0 || videoWidth == 0 || videoHeight == 0) return

    val viewWidth = width.toFloat()
    val viewHeight = height.toFloat()
    val scale = min(viewWidth / videoWidth.toFloat(), viewHeight / videoHeight.toFloat())
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    val matrix = Matrix().apply {
        setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f)
    }
    setTransform(matrix)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.mediaSharedElement(
    mediaItem: MediaItem,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedBoundsTransform: BoundsTransform?
): Modifier {
    val transitionScope = sharedTransitionScope ?: return this
    val visibilityScope = animatedVisibilityScope ?: return this

    return with(transitionScope) {
        val sharedContentState = rememberSharedContentState(key = "media-${mediaItem.id}")
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
