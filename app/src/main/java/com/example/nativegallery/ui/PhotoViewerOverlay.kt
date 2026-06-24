@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
private val ViewerPhotoBackground = Color(0xFF111111)
private val ViewerPhotoStageBackground = Color.Black

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
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null
) {
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
    var controlsVisible by remember { mutableStateOf(true) }
    var closeRequested by remember { mutableStateOf(false) }
    var lastSettledPage by rememberSaveable { mutableIntStateOf(selectedIndex) }
    var deleteDirection by rememberSaveable { mutableIntStateOf(1) }
    var verticalDragOffset by remember { mutableStateOf(0f) }
    var detailsDragOffset by remember { mutableStateOf(0f) }
    var isViewerDragging by remember { mutableStateOf(false) }
    var detailsVisible by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val dismissThresholdPx = with(LocalDensity.current) { 104.dp.toPx() }
    val detailsThresholdPx = with(LocalDensity.current) { 76.dp.toPx() }

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
    var activeMediaPlaced by remember(currentItem.id) { mutableStateOf(false) }
    LaunchedEffect(currentItem.id, visible) {
        if (visible) {
            activeMediaPlaced = false
            onCurrentMediaChanged(currentItem)
            controlsVisible = true
            detailsVisible = false
            verticalDragOffset = 0f
            detailsDragOffset = 0f
            closeRequested = false
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

    fun closeViewerWithChromeFade() {
        if (closeRequested) return
        closeRequested = true
        controlsVisible = false
        detailsVisible = false
        scope.launch {
            delay(70)
            onClose()
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
            .background(
                if (currentItem.isVideo) {
                    Color.Black.copy(alpha = viewerBackdropAlpha(displayedVerticalDragOffset, dismissThresholdPx))
                } else {
                    ViewerPhotoBackground.copy(alpha = viewerBackdropAlpha(displayedVerticalDragOffset, dismissThresholdPx))
                }
            )
            .pointerInput(currentItem.id, dismissThresholdPx, detailsThresholdPx) {
                detectVerticalDragGestures(
                    onDragStart = { isViewerDragging = true },
                    onVerticalDrag = { change, dragAmount ->
                        val shouldPullViewer = dragAmount > 0f || verticalDragOffset > 0f
                        val shouldRevealDetails = dragAmount < 0f || detailsDragOffset < 0f
                        when {
                            shouldPullViewer -> {
                                detailsVisible = false
                                verticalDragOffset = (verticalDragOffset + dragAmount)
                                    .coerceIn(0f, dismissThresholdPx * 1.8f)
                                change.consume()
                            }
                            shouldRevealDetails -> {
                                detailsDragOffset = (detailsDragOffset + dragAmount)
                                    .coerceIn(-detailsThresholdPx * 1.8f, 0f)
                                change.consume()
                            }
                        }
                    },
                    onDragEnd = {
                        isViewerDragging = false
                        if (verticalDragOffset > dismissThresholdPx * 0.92f) {
                            closeViewerWithChromeFade()
                        } else {
                            if (detailsDragOffset < -detailsThresholdPx) {
                                detailsVisible = true
                                controlsVisible = false
                            }
                            verticalDragOffset = 0f
                        }
                        detailsDragOffset = 0f
                    },
                    onDragCancel = {
                        isViewerDragging = false
                        verticalDragOffset = 0f
                        detailsDragOffset = 0f
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
                    val pullProgress = (pullDown / (dismissThresholdPx * 1.15f)).coerceIn(0f, 1f)
                    val pullScale = 1f - pullProgress * 0.3f
                    translationY = pullDown * 0.96f
                    scaleX = pullScale
                    scaleY = pullScale
                },
            beyondViewportPageCount = 0,
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
                sharedElementKeyPrefix = sharedElementKeyPrefix,
                isSharedTransitionReady = activeMediaPlaced,
                onActiveMediaPlaced = {
                    if (page == pagerState.currentPage) {
                        activeMediaPlaced = true
                    }
                },
                onClose = ::closeViewerWithChromeFade,
                onToggleControls = {
                    if (detailsVisible) {
                        detailsVisible = false
                        controlsVisible = true
                    } else {
                        controlsVisible = !controlsVisible
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
            ViewerTopBar(onClose = ::closeViewerWithChromeFade)
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
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
                    onInfo = {
                        detailsVisible = true
                        controlsVisible = false
                    },
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
                animationSpec = tween(220, easing = ViewerEnterEasing)
            ),
            exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(160, easing = ViewerExitEasing)
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
    onInfo: () -> Unit,
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
            modifier = Modifier.padding(horizontal = 18.dp),
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
                icon = Icons.Filled.Info,
                contentDescription = "Info",
                onClick = onInfo
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (mediaItem.isVideo) "Video" else "Photo"
    val mimeLabel = mediaItem.mimeType?.uppercase(Locale.US) ?: if (mediaItem.isVideo) "VIDEO" else "IMAGE"
    val resolutionLabel = mediaItem.resolutionLabel() ?: "Unknown"
    val sizeLabel = mediaItem.fileSizeBytes?.let(::formatFileSize) ?: "Unknown"
    val durationLabel = mediaItem.durationLabel?.takeIf { it.isNotBlank() }
    val accent = Color(0xFF004741)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        color = Color(0xFFFAFBF7).copy(alpha = 0.98f),
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
                    .background(Color.Black.copy(alpha = 0.14f), RoundedCornerShape(100.dp))
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
                        color = Color(0xFF111614),
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
                        tint = Color(0xFF202624),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F3EE), RoundedCornerShape(24.dp))
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
                .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(14.dp)),
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
                color = Color(0xFF6B7470),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = Color(0xFF151B18),
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
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    isSharedTransitionReady: Boolean,
    onActiveMediaPlaced: () -> Unit,
    onClose: () -> Unit,
    onToggleControls: () -> Unit
) {
    val activeSharedElementKeyPrefix = sharedElementKeyPrefix.takeIf { isActive && isSharedTransitionReady }
    val activeMediaAlpha = if (isActive && !isSharedTransitionReady) 0f else 1f

    if (mediaItem.isVideo && mediaItem.contentUri != null) {
        ViewerVideoPlayer(
            mediaItem = mediaItem,
            isActive = isActive,
            controlsVisible = controlsVisible,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedBoundsTransform = sharedBoundsTransform,
            sharedElementKeyPrefix = activeSharedElementKeyPrefix,
            isSharedTransitionReady = isSharedTransitionReady,
            onActiveMediaPlaced = onActiveMediaPlaced,
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
    val mediaModifier = Modifier
        .fillMaxSize()
        .mediaSharedElement(
        mediaItem = mediaItem,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedBoundsTransform = sharedBoundsTransform,
        sharedElementKeyPrefix = activeSharedElementKeyPrefix
    )

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
                    onTap = { onToggleControls() },
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
            thumbnailSize = 4096,
            loadQuality = ImageLoadQuality.HighQuality,
            backgroundColor = ViewerPhotoStageBackground
        )
    }
}
@Composable
private fun ViewerVideoPlayer(
    mediaItem: MediaItem,
    isActive: Boolean,
    controlsVisible: Boolean,
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
                .graphicsLayer { alpha = if (isPrepared) 0f else 1f },
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


private fun viewerBackdropAlpha(offset: Float, dismissThresholdPx: Float): Float {
    if (dismissThresholdPx <= 0f) return 1f
    val progress = (offset / (dismissThresholdPx * 1.15f)).coerceIn(0f, 1f)
    return 1f - progress
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
