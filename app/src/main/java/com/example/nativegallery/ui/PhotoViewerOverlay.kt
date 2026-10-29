@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.Activity
import android.graphics.Bitmap
import com.example.nativegallery.data.VideoPresentationGeometry
import com.example.nativegallery.data.videoDisplayDimensions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MotionPhotosPause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nativegallery.util.MotionPhotoExtractor
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.nativegallery.ui.components.rememberDeferredAnimation
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import com.example.nativegallery.R
import com.example.nativegallery.data.VideoDurationCache
import com.example.nativegallery.data.VideoDurationResolver
import androidx.compose.ui.zIndex
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.player.galleryVideoMediaSourceFactory
import com.example.nativegallery.player.galleryVideoRenderersFactory
import com.example.nativegallery.player.GalleryVideoPlayerStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.nativegallery.player.isMediaTekHardware
import com.example.nativegallery.player.preferMediaTekHardwareAvc
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.MotionSpecs
import com.example.nativegallery.ui.components.PremiumDropdownMenu
import com.example.nativegallery.ui.components.PremiumDropdownMenuItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.nativegallery.ui.components.bouncyClickable
import com.example.nativegallery.ui.components.solarScale
import com.example.nativegallery.ui.components.solarTactile
import com.example.nativegallery.ui.components.cacheVideoFrameThumbnail
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import com.example.nativegallery.ui.components.DisplayedBitmapCache
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ViewerEnterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val ViewerExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val ViewerPhotoBackground = Color.Black
private val ViewerPhotoStageBackground = Color.Transparent
private val ViewerVideoControlTextShadow = Shadow(Color.Black.copy(alpha = 0.90f), Offset(0f, 1f), 3f)
private val ViewerActionLabelShadow = Shadow(Color.Black.copy(alpha = 0.96f), Offset(0f, 1.2f), 5f)
private const val DEFAULT_AUDIBLE_MEDIA_VOLUME = 0.35f
private const val VIDEO_PLAYBACK_LOG_TAG = "NativeGalleryVideo"
internal const val ViewerMediaStageTestTag = "viewer-media-stage"
internal const val ViewerZoomableMediaTestTag = "viewer-zoomable-media"
internal const val ViewerFilmstripSectionTestTag = "viewer-filmstrip-section"
internal const val ViewerActionSectionTestTag = "viewer-action-section"
internal const val ViewerBottomChromeTestTag = "viewer-bottom-chrome"
internal const val ViewerBottomChromeSectionGapDp = 4
internal const val ViewerBottomChromeActionInsetDp = 82
internal const val ViewerActionCapsuleHeightDp = 72
internal const val ViewerVideoSeekHeightDp = 32

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
    onCloseWithMotion: ((Offset, Float, Float, Float, Float, Bitmap?) -> Unit)? = null,
    onDelete: (MediaItem, Int) -> Unit,
    onHide: (MediaItem, Int) -> Unit = { _, _ -> },
    onRestore: (MediaItem, Int) -> Unit = { _, _ -> },
    actionMode: ViewerActionMode = ViewerActionMode.Normal,
    favoriteMediaIds: Set<String> = emptySet(),
    onFavoriteChange: (MediaItem, Boolean) -> Unit = { _, _ -> },
    onEdit: (MediaItem) -> Unit = {},
    onMove: (MediaItem) -> Unit = {},
    onCurrentMediaChanged: (MediaItem) -> Unit = {},
    albumNameForMedia: (MediaItem) -> String? = { null },
    autoplayVideos: Boolean = true,
    startVideosMuted: Boolean = false,
    preferredPhotoDecodeSize: Int? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    predictiveBackProgressProvider: () -> Float = { 0f },
    predictiveBackDirectionProvider: () -> Float = { 1f },
    initialPreview: Bitmap? = null
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
    LaunchedEffect(pagerState) {
        snapshotFlow { isViewerDragging || pagerState.isScrollInProgress || closeRequested }.collect { moving ->
            com.example.nativegallery.ui.components.GalleryImageWorkScheduler.setViewerMoving(moving)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            com.example.nativegallery.ui.components.GalleryImageWorkScheduler.setViewerMoving(false)
        }
    }
    var detailsVisible by remember { mutableStateOf(false) }
    var activePhotoZoomed by remember { mutableStateOf(false) }
    val dismissThresholdPx = with(density) { 104.dp.toPx() }
    val detailsThresholdPx = with(density) { 76.dp.toPx() }
    val videoSideControlZonePx = with(density) { 60.dp.toPx() }
    val calculatedPhotoDecodeSize = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density.density
    ) {
        (max(configuration.screenWidthDp, configuration.screenHeightDp) * density.density)
            .roundToInt()
            .coerceIn(1440, 3072)
    }
    val activePhotoDecodeSize = preferredPhotoDecodeSize ?: calculatedPhotoDecodeSize

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
    LaunchedEffect(currentPageForState) {
        // A zoom flag belongs only to its active page. Never let it disable paging after the
        // viewer settles on another item or a previously zoomed page is revisited.
        activePhotoZoomed = false
    }
    val listedItem = viewerItems.getOrNull(currentPageForState) ?: mediaItem
    val currentItem = if (listedItem.id == mediaItem.id) mediaItem else listedItem
    val currentItemHasVideoPlayer = currentItem.isVideo && currentItem.contentUri != null
    val compactLandscape = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    ) {
        isViewerCompactLandscape(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp
        )
    }
    val showViewerFilmstrip = shouldShowViewerFilmstrip(
        itemCount = viewerItems.size,
        isVideo = currentItem.isVideo,
        compactLandscape = compactLandscape
    )
    val activity = remember(context) { context.findViewerActivity() }
    val originalWindowBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    val initialVideoBrightness = remember(context, originalWindowBrightness) {
        preferredViewerBrightness(context, originalWindowBrightness)
    }
    val videoBrightnessState = remember { mutableFloatStateOf(initialVideoBrightness) }
    val brightnessOverrideEnabled = remember { mutableStateOf(false) }
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val systemMediaVolume = remember(audioManager) {
        normalizedMediaVolume(
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        )
    }
    val videoPlaybackVolumeState = remember(startVideosMuted, systemMediaVolume) {
        mutableFloatStateOf(if (startVideosMuted) 0f else systemMediaVolume)
    }
    val lastAudibleVolumeState = remember(systemMediaVolume) {
        mutableFloatStateOf(systemMediaVolume.coerceAtLeast(DEFAULT_AUDIBLE_MEDIA_VOLUME))
    }
    var activeMediaPlaced by remember(currentItem.id) { mutableStateOf(false) }
    var isPlayingMotionPhoto by remember(currentItem.id) { mutableStateOf(false) }
    var motionVideoUri by remember(currentItem.id) { mutableStateOf<Uri?>(currentItem.motionVideoUri) }
    var isExtractingMotion by remember(currentItem.id) { mutableStateOf(false) }
    var isBurstReviewOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = detailsVisible) {
        detailsVisible = false
        controlsVisible = true
    }
    BackHandler(enabled = isBurstReviewOpen) { isBurstReviewOpen = false }
    var burstShots by remember(currentItem.id) {
        mutableStateOf(currentItem.burstItems.ifEmpty { listOf(currentItem) })
    }

    fun ensureMotionVideo(onReady: (Uri) -> Unit) {
        val existing = motionVideoUri
        if (existing != null) {
            onReady(existing)
            return
        }
        val uri = currentItem.contentUri ?: return
        if (isExtractingMotion) return
        isExtractingMotion = true
        scope.launch {
            val extracted = MotionPhotoExtractor.extractMotionVideo(
                context,
                uri,
                currentItem.motionVideoOffset
            )
            isExtractingMotion = false
            if (extracted != null) {
                motionVideoUri = extracted
                onReady(extracted)
            }
        }
    }

    fun startMotionPlayback() {
        if (!currentItem.isMotionPhoto) return
        ensureMotionVideo {
            isPlayingMotionPhoto = true
        }
    }

    fun stopMotionPlayback() {
        isPlayingMotionPhoto = false
    }

    fun toggleMotionPlayback() {
        if (isPlayingMotionPhoto) {
            stopMotionPlayback()
        } else {
            startMotionPlayback()
        }
    }

    DisposableEffect(activity, originalWindowBrightness) {
        onDispose {
            activity?.applyViewerBrightness(originalWindowBrightness)
        }
    }

    DisposableEffect(activity, actionMode) {
        if (activity == null || actionMode != ViewerActionMode.Locked) {
            return@DisposableEffect onDispose { }
        }
        val viewerWindow = activity.window
        @Suppress("DEPRECATION")
        val previousNavigationBarColor = viewerWindow.navigationBarColor
        val insetsController = WindowCompat.getInsetsController(viewerWindow, viewerWindow.decorView)
        val previousLightNavigationBars = insetsController.isAppearanceLightNavigationBars
        val previousContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewerWindow.isNavigationBarContrastEnforced
        } else {
            false
        }
        @Suppress("DEPRECATION")
        viewerWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewerWindow.isNavigationBarContrastEnforced = false
        }
        insetsController.isAppearanceLightNavigationBars = false

        onDispose {
            @Suppress("DEPRECATION")
            viewerWindow.navigationBarColor = previousNavigationBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewerWindow.isNavigationBarContrastEnforced = previousContrastEnforced
            }
            insetsController.isAppearanceLightNavigationBars = previousLightNavigationBars
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
            isPlayingMotionPhoto = false
            isBurstReviewOpen = false
            burstShots = currentItem.burstItems.ifEmpty { listOf(currentItem) }
        }
    }

    LaunchedEffect(currentPageForState, viewerItemIds, activePhotoDecodeSize) {
        val activeItem = viewerItems.getOrNull(currentPageForState)
        prefetchMediaThumbnails(
            context = context.applicationContext,
            mediaItems = listOfNotNull(activeItem).filterNot { it.isVideo },
            thumbnailSizes = listOf(activePhotoDecodeSize),
            maxItems = 1
        )
        val nearbyItems = listOfNotNull(
            viewerItems.getOrNull(currentPageForState - 1),
            viewerItems.getOrNull(currentPageForState + 1)
        )
        prefetchMediaThumbnails(
            context = context.applicationContext,
            mediaItems = nearbyItems.filterNot { it.isVideo },
            thumbnailSizes = listOf(minOf(activePhotoDecodeSize, 2048)),
            maxItems = 2
        )
        val filmstripItems = viewerFilmstripWindow(viewerItems.size, currentPageForState)
            .mapNotNull(viewerItems::getOrNull)
        prefetchMediaThumbnails(
            context = context.applicationContext,
            mediaItems = filmstripItems,
            thumbnailSizes = listOf(384),
            maxItems = filmstripItems.size,
            pinInMemory = true
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

    var activeVideoTextureView by remember { mutableStateOf<TextureView?>(null) }

    fun closeViewerWithChromeFade(
        closeOffset: Offset = Offset.Zero,
        closeScale: Float = 1f,
        closeBackdropAlpha: Float = 1f,
        closeRotationZ: Float = 0f,
        closeRotationX: Float = 0f
    ) {
        if (closeRequested) return
        closeRequested = true
        controlsVisible = false
        detailsVisible = false
        val closingFrame = if (currentItem.isVideo) {
            captureVideoTransitionFrame(activeVideoTextureView)
        } else null
        closingFrame?.let { frame ->
            currentItem.contentUri?.let { uri ->
                cacheVideoFrameThumbnail(
                    context,
                    uri,
                    frame,
                    mediaThumbnailCacheRevision(currentItem)
                )
            }
        }
        val dispatch = {
            if (onCloseWithMotion != null) {
                onCloseWithMotion(closeOffset, closeScale, closeBackdropAlpha, closeRotationZ, closeRotationX, closingFrame)
            } else {
                onClose(closeOffset, closeScale, closeBackdropAlpha)
            }
        }
        if (closeOffset != Offset.Zero) {
            dispatch()
        } else {
            scope.launch {
                delay(GalleryMotion.ViewerChromeCloseDelayMillis)
                dispatch()
            }
        }
    }

    val animatedDragOffset = rememberDeferredAnimation(
        initialValue = Offset.Zero,
        typeConverter = Offset.VectorConverter,
        targetValue = { dragOffset },
        animationSpec = {
            if (isViewerDragging) {
                snap()
            } else {
                spring(
                    dampingRatio = GalleryMotion.ViewerSpringBackDamping,
                    stiffness = GalleryMotion.ViewerSpringBackStiffness
                )
            }
        },
        label = "viewer dismiss offset",
        animationKey = currentItem.id
    )
    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    val currentDetailsVisible by rememberUpdatedState(detailsVisible)
    val currentCloseRequested by rememberUpdatedState(closeRequested)
    val currentActivePhotoZoomed by rememberUpdatedState(activePhotoZoomed)
    val currentItemHasVideoPlayerState by rememberUpdatedState(currentItemHasVideoPlayer)

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
            .pointerInput(currentItem.id) {
                val touchSlop = viewConfiguration.touchSlop
                val maxDragX = size.width.toFloat() * 0.92f
                val maxDragY = size.height.toFloat() * 1.5f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Tap/double-tap detectors consume DOWN even at fit scale. That does not
                    // claim a drag. Respect consumed movement instead, after child controls
                    // (timeline, zoom/pan, details scrolling, pager) have had their turn.
                    if (currentCloseRequested) return@awaitEachGesture
                    if (currentActivePhotoZoomed) {
                        while (awaitPointerEvent().changes.any { it.pressed }) {
                            // The zoomable photo owns this gesture until every pointer is up.
                        }
                        return@awaitEachGesture
                    }
                    val startedInVideoControlZone = currentItemHasVideoPlayerState &&
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
                    var isDownwardDrag = false
                    var isDetailsDrag = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (!handlingViewerDrag && event.changes.count { it.pressed } > 1) break

                        val change = event.changes.firstOrNull { it.id == activePointerId }
                            ?: event.changes.firstOrNull { it.pressed }
                            ?: break
                        activePointerId = change.id
                        if (!handlingViewerDrag && change.isConsumed) break
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
                            val dismissDetailsIntent = currentDetailsVisible && downwardIntent
                            if (!verticalIntent && !downwardIntent && !detailsIntent) {
                                break
                            }
                            closingDetailsGesture = dismissDetailsIntent
                            isDownwardDrag = !dismissDetailsIntent && downwardIntent
                            isDetailsDrag = !dismissDetailsIntent && detailsIntent
                            passedTouchSlop = true
                            handlingViewerDrag = true
                            isViewerDragging = true
                            if (isDownwardDrag) {
                                val slopOffsetY = totalDrag.y - touchSlop * 0.35f
                                dragOffset = Offset(
                                    x = totalDrag.x.coerceIn(-maxDragX, maxDragX),
                                    y = slopOffsetY.coerceAtLeast(0f)
                                )
                            }
                            change.consume()
                        } else if (handlingViewerDrag) {
                            if (closingDetailsGesture) {
                                dragOffset = Offset.Zero
                                detailsDragOffset = 0f
                                change.consume()
                                continue
                            }
                            if (isDownwardDrag) {
                                val nextOffset = dragOffset + delta
                                val dampedY = if (nextOffset.y < 0f) {
                                    nextOffset.y * 0.28f
                                } else {
                                    nextOffset.y.coerceIn(0f, maxDragY)
                                }
                                detailsVisible = false
                                detailsDragOffset = 0f
                                dragOffset = Offset(
                                    x = nextOffset.x.coerceIn(-maxDragX, maxDragX),
                                    y = dampedY
                                )
                            } else if (isDetailsDrag) {
                                dragOffset = Offset.Zero
                                detailsDragOffset = (detailsDragOffset + delta.y)
                                    .coerceIn(-detailsThresholdPx * 1.8f, 0f)
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
                        } else if (isDownwardDrag) {
                            if (shouldDismissViewerDrag(dragOffset, dragVelocity, dismissThresholdPx)) {
                                val rotZ = (dragOffset.x / 38f).coerceIn(-7f, 7f)
                                closeViewerWithChromeFade(
                                    closeOffset = renderedViewerDragOffset(dragOffset),
                                    closeScale = viewerDismissScale(dragOffset, dismissThresholdPx),
                                    closeBackdropAlpha = viewerBackdropAlpha(dragOffset, dismissThresholdPx),
                                    closeRotationZ = rotZ,
                                    closeRotationX = 0f
                                )
                            } else {
                                isViewerDragging = false
                                dragOffset = Offset.Zero
                            }
                        } else if (isDetailsDrag) {
                            if (detailsDragOffset < -detailsThresholdPx) {
                                detailsVisible = true
                                controlsVisible = false
                            }
                            isViewerDragging = false
                            dragOffset = Offset.Zero
                        } else {
                            isViewerDragging = false
                            dragOffset = Offset.Zero
                        }
                        detailsDragOffset = 0f
                    }
                }
            }
    ) {
        val targetViewerBg = Color.Black
        val viewerBgColor by animateColorAsState(
            targetValue = targetViewerBg,
            animationSpec = tween(180),
            label = "viewerBgColor"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val currentOffset = if (isViewerDragging) dragOffset else animatedDragOffset.value
                    drawRect(viewerBgColor.copy(
                        alpha = viewerBgColor.alpha * viewerBackdropAlpha(currentOffset, dismissThresholdPx)
                    ))
                }
        )
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = MotionSpecs.AppleSpringPager
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag(ViewerMediaStageTestTag)
                .graphicsLayer {
                    val currentOffset = if (isViewerDragging) dragOffset else animatedDragOffset.value
                    val pullScale = viewerDismissScale(currentOffset, dismissThresholdPx)
                    val rotZ = (currentOffset.x / 38f).coerceIn(-7f, 7f)
                    translationX = currentOffset.x * 0.96f
                    translationY = currentOffset.y * 0.96f
                    scaleX = pullScale
                    scaleY = pullScale
                    rotationZ = rotZ
                    rotationX = 0f
                    cameraDistance = 16f * density.density
                },
            beyondViewportPageCount = viewerBeyondViewportPageCount(),
            userScrollEnabled = !activePhotoZoomed &&
                !isViewerDragging &&
                !closeRequested,
            key = { page -> viewerItems[page].id }
        ) { page ->
            val listedPageItem = viewerItems[page]
            val pageItem = if (listedPageItem.id == mediaItem.id) mediaItem else listedPageItem
            val currentDismissFraction = remember(dismissThresholdPx) {
                derivedStateOf {
                    val currentOffset = if (isViewerDragging) dragOffset else animatedDragOffset.value
                    if (isViewerDragging || closeRequested) {
                        ((1f - viewerDismissScale(currentOffset, dismissThresholdPx)) / (1f - 0.72f)).coerceIn(0f, 1f)
                    } else 0f
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        val absOffset = kotlin.math.abs(pageOffset)
                        val scale = 1f - (absOffset * 0.08f).coerceIn(0f, 0.08f)
                        val alphaVal = (1f - absOffset * 0.45f).coerceIn(0.55f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaVal
                        translationX = -pageOffset * 0.22f * size.width
                        cameraDistance = 16f * density.density
                    }
            ) {
                ZoomableViewerMedia(
                    initialPreview = initialPreview.takeIf { pageItem.id == mediaItem.id },
                    mediaItem = pageItem,
                    isActive = page == currentPageForState,
                    isViewerDragging = isViewerDragging || closeRequested,
                    dismissCornerRadius = { if (page == currentPageForState) 28.dp * currentDismissFraction.value else 0.dp },
                    dismissElevation = { if (page == currentPageForState) 22.dp * currentDismissFraction.value else 0.dp },
                    controlsVisible = controlsVisible,
                    compactLandscape = compactLandscape,
                    activePhotoDecodeSize = activePhotoDecodeSize,
                    videoBrightnessState = videoBrightnessState,
                    brightnessOverrideEnabled = brightnessOverrideEnabled,
                    videoPlaybackVolumeState = videoPlaybackVolumeState,
                    autoplayVideos = autoplayVideos,
                    initiallyMuted = startVideosMuted,
                    lastAudibleVolumeState = lastAudibleVolumeState,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedBoundsTransform = sharedBoundsTransform,
                    sharedElementKeyPrefix = sharedElementKeyPrefix,
                    isSharedTransitionReady = activeMediaPlaced,
                    onDimensionsChanged = { w, h ->
                        if (page == currentPageForState) {
                            onCurrentMediaChanged(pageItem.copy(width = w, height = h))
                        }
                    },
                    onActiveMediaPlaced = {
                        if (page == currentPageForState) {
                            activeMediaPlaced = true
                        }
                    },
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
                            val wasZoomed = activePhotoZoomed
                            activePhotoZoomed = zoomed
                            if (zoomed) {
                                detailsVisible = false
                                controlsVisible = false
                            } else if (wasZoomed) {
                                controlsVisible = true
                            }
                        }
                    },
                    isPlayingMotionPhoto = if (page == currentPageForState) isPlayingMotionPhoto else false,
                    motionVideoUri = if (page == currentPageForState) motionVideoUri else null,
                    onStartMotionPlayback = ::startMotionPlayback,
                    onStopMotionPlayback = ::stopMotionPlayback,
                    onVideoTextureViewAvailable = { view ->
                        if (page == currentPageForState) {
                            activeVideoTextureView = view
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !isViewerDragging,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(90))
        ) {
            ViewerTopBar(
                actionMode = actionMode,
                onClose = { closeViewerWithChromeFade() },
                onMove = { onMove(currentItem) },
                onInfo = {
                    isViewerDragging = false
                    dragOffset = Offset.Zero
                    detailsDragOffset = 0f
                    detailsVisible = true
                    controlsVisible = false
                },
                onLock = { onHide(currentItem, deleteDirection) },
                isBurst = currentItem.isBurst,
                burstCount = currentItem.burstCount,
                isMotionPhoto = currentItem.isMotionPhoto,
                isPlayingMotion = isPlayingMotionPhoto,
                onToggleMotion = ::toggleMotionPlayback,
                onOpenBurstReview = { isBurstReviewOpen = true }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isViewerDragging,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(90))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ViewerBottomChromeTestTag)
                    .navigationBarsPadding()
                    .padding(
                        top = if (showViewerFilmstrip) 8.dp else 4.dp,
                        bottom = 4.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showViewerFilmstrip) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ViewerFilmstripSectionTestTag)
                    ) {
                        ViewerFilmstrip(
                            mediaItems = viewerItems,
                            selectedIndex = pagerState.currentPage,
                            onItemClick = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                    Spacer(Modifier.height(ViewerBottomChromeSectionGapDp.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ViewerActionSectionTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    ViewerActionBar(
                        actionMode = actionMode,
                        isPhoto = !currentItem.isVideo,
                        isFavorite = favoriteMediaIds.contains(currentItem.id),
                        onFavorite = {
                            onFavoriteChange(currentItem, !favoriteMediaIds.contains(currentItem.id))
                        },
                        onEdit = { onEdit(currentItem) },
                        onShare = ::shareCurrentMedia,
                        onRestore = { onRestore(currentItem, deleteDirection) },
                        onDelete = { onDelete(currentItem, deleteDirection) }
                    )
                }
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

        if (isBurstReviewOpen && currentItem.isBurst) {
            BurstReviewOverlay(
                burstItems = burstShots,
                currentMainItem = currentItem,
                onClose = { isBurstReviewOpen = false },
                onSetAsMain = { selectedShot ->
                    val reordered = listOf(selectedShot) + burstShots.filterNot { it.id == selectedShot.id }
                    burstShots = reordered
                    val updatedMain = selectedShot.copy(
                        isBurst = true,
                        burstCount = burstShots.size,
                        burstGroupId = currentItem.burstGroupId,
                        burstItems = reordered
                    )
                    onCurrentMediaChanged(updatedMain)
                }
            )
        }
    }
}

@Composable
private fun ViewerTopBar(
    actionMode: ViewerActionMode,
    onClose: () -> Unit,
    onMove: () -> Unit,
    onInfo: () -> Unit,
    onLock: () -> Unit,
    isBurst: Boolean = false,
    burstCount: Int = 0,
    isMotionPhoto: Boolean = false,
    isPlayingMotion: Boolean = false,
    onToggleMotion: () -> Unit = {},
    onOpenBurstReview: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, top = 4.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(38.dp)
                .solarTactile(scaleDown = 0.88f, onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClose()
                }),
            color = Color(0xFF1B1B1B),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close media",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isBurst && burstCount > 1) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenBurstReview()
                        },
                    color = Color(0xFF1B1B1B),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BurstMode,
                            contentDescription = "Burst stack",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Burst ($burstCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
            if (isMotionPhoto) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMotion()
                        },
                    color = if (isPlayingMotion) MaterialTheme.colorScheme.primary else Color(0xFF1B1B1B),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingMotion) Icons.Filled.MotionPhotosPause else Icons.Filled.MotionPhotosOn,
                            contentDescription = "Motion Photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPlayingMotion) "PLAYING" else "MOTION",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Box {
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .solarTactile(scaleDown = 0.88f, onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        menuExpanded = true
                    }),
                color = Color(0xFF1B1B1B),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More media options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            PremiumDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                if (actionMode == ViewerActionMode.Normal) {
                    PremiumDropdownMenuItem(
                        text = { Text("Move to album") },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            menuExpanded = false
                            onMove()
                        }
                    )
                }
                PremiumDropdownMenuItem(
                    text = { Text("Information") },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        menuExpanded = false
                        onInfo()
                    }
                )
                if (actionMode == ViewerActionMode.Normal) {
                    PremiumDropdownMenuItem(
                        text = { Text("Lock") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            menuExpanded = false
                            onLock()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerFilmstrip(
    mediaItems: List<MediaItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit
) {
    val visibleIndices = remember(mediaItems.size, selectedIndex) {
        viewerFilmstripWindow(mediaItems.size, selectedIndex)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleIndices.forEach { index ->
            val item = mediaItems[index]
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

internal fun viewerFilmstripWindow(
    itemCount: Int,
    selectedIndex: Int,
    maximumItems: Int = 7
): IntRange {
    if (itemCount <= 0 || maximumItems <= 0) return 1..0
    val visibleCount = minOf(itemCount, maximumItems)
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val startIndex = (safeSelectedIndex - visibleCount / 2)
        .coerceIn(0, itemCount - visibleCount)
    return startIndex until (startIndex + visibleCount)
}


internal fun isViewerCompactLandscape(screenWidthDp: Int, screenHeightDp: Int): Boolean {
    return screenWidthDp > screenHeightDp && screenHeightDp <= 480
}

internal fun shouldShowViewerFilmstrip(
    itemCount: Int,
    isVideo: Boolean,
    compactLandscape: Boolean
): Boolean = itemCount > 1 && !isVideo && !compactLandscape

internal fun viewerBeyondViewportPageCount(): Int = 0

internal fun viewerPinchScale(currentScale: Float, zoomDelta: Float): Float {
    val safeZoomDelta = zoomDelta.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (currentScale * safeZoomDelta).coerceIn(0.82f, 5f)
}

internal fun viewerDoubleTapTargetScale(currentScale: Float): Float {
    return if (currentScale > 1.05f) 1f else 2.8f
}

internal fun viewerVideoControlMaxWidthDp(screenWidthDp: Int): Int = when {
    screenWidthDp >= 840 -> 640
    screenWidthDp >= 600 -> 560
    else -> (screenWidthDp - 24).coerceAtLeast(280)
}

@Composable
private fun ViewerActionBar(
    actionMode: ViewerActionMode,
    isPhoto: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(ViewerActionCapsuleHeightDp.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (actionMode == ViewerActionMode.Normal) {
            ViewerActionButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                label = "Favorite",
                selected = isFavorite,
                modifier = Modifier.weight(1f),
                onClick = onFavorite
            )
            ViewerActionButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "Edit photo",
                label = "Edit",
                enabled = isPhoto,
                modifier = Modifier.weight(1f),
                onClick = onEdit
            )
            ViewerActionButton(
                icon = Icons.Outlined.Share,
                contentDescription = "Share",
                label = "Share",
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
            ViewerActionButton(
                icon = Icons.Outlined.Delete,
                destructive = true,
                contentDescription = "Delete",
                label = "Delete",
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        } else {
            ViewerActionButton(
                icon = Icons.Outlined.Restore,
                contentDescription = if (actionMode == ViewerActionMode.Locked) "Show" else "Restore",
                label = "Restore",
                modifier = Modifier.weight(1f),
                onClick = onRestore
            )
            ViewerActionButton(
                icon = Icons.Outlined.Delete,
                destructive = true,
                contentDescription = if (actionMode == ViewerActionMode.RecentlyDeleted) "Delete forever" else "Delete",
                label = if (actionMode == ViewerActionMode.RecentlyDeleted) "Delete forever" else "Delete",
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val iconTint = when {
        selected -> Color(0xFF86D7FF)
        destructive -> Color(0xFFFFC1BB)
        else -> Color.White.copy(alpha = 0.96f)
    }
    val iconScaleState = animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = com.example.nativegallery.ui.components.MotionSpecs.AppleSpringBouncy,
        label = "actionIconScale"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(38.dp)
                .solarScale(interactionSource, scaleDown = 0.88f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        val scale = iconScaleState.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        color = Color.Black.copy(alpha = if (enabled) 0.46f else 0.22f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.38f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Text(
            text = label,
            color = if (enabled) iconTint else iconTint.copy(alpha = 0.38f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                shadow = ViewerActionLabelShadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    val maximumHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp

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
        Column(
            modifier = Modifier
                .heightIn(max = maximumHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
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
    isViewerDragging: Boolean = false,
    dismissCornerRadius: () -> Dp = { 0.dp },
    dismissElevation: () -> Dp = { 0.dp },
    controlsVisible: Boolean,
    compactLandscape: Boolean,
    activePhotoDecodeSize: Int,
    videoBrightnessState: MutableFloatState,
    brightnessOverrideEnabled: MutableState<Boolean>,
    videoPlaybackVolumeState: MutableFloatState,
    autoplayVideos: Boolean,
    initiallyMuted: Boolean,
    lastAudibleVolumeState: MutableFloatState,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementKeyPrefix: String? = null,
    isSharedTransitionReady: Boolean,
    onDimensionsChanged: (Int, Int) -> Unit = { _, _ -> },
    onActiveMediaPlaced: () -> Unit,
    onToggleControls: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit,
    isPlayingMotionPhoto: Boolean = false,
    motionVideoUri: Uri? = null,
    onStartMotionPlayback: () -> Unit = {},
    onStopMotionPlayback: () -> Unit = {},
    initialPreview: Bitmap? = null,
    onVideoTextureViewAvailable: (TextureView?) -> Unit = {}
) {
    val activeSharedElementKeyPrefix: String? = null
    val activeMediaAlpha = 1f
    val scope = rememberCoroutineScope()

    if (mediaItem.isVideo && mediaItem.contentUri != null) {
        if (isActive) {
            ViewerVideoPlayer(
                initialPreview = initialPreview,
                mediaItem = mediaItem,
                isActive = true,
                isViewerDragging = isViewerDragging,
                dismissCornerRadius = dismissCornerRadius,
                dismissElevation = dismissElevation,
                controlsVisible = controlsVisible,
                compactLandscape = compactLandscape,
                brightnessState = videoBrightnessState,
                brightnessOverrideEnabled = brightnessOverrideEnabled,
                playbackVolumeState = videoPlaybackVolumeState,
                autoplay = autoplayVideos,
                initiallyMuted = initiallyMuted,
                lastAudibleVolumeState = lastAudibleVolumeState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedBoundsTransform = sharedBoundsTransform,
                sharedElementKeyPrefix = activeSharedElementKeyPrefix,
                isSharedTransitionReady = isSharedTransitionReady,
                onDimensionsChanged = onDimensionsChanged,
                onActiveMediaPlaced = onActiveMediaPlaced,
                onToggleControls = onToggleControls,
                onVideoTextureViewAvailable = onVideoTextureViewAvailable
            )
        } else {
            ViewerVideoPoster(
                mediaItem = mediaItem,
                dismissCornerRadius = dismissCornerRadius,
                dismissElevation = dismissElevation
            )
        }
        if (isActive) {
            LaunchedEffect(mediaItem.id) { onZoomStateChanged(false) }
        }
        return
    }

    var targetScale by remember(mediaItem.id) { mutableFloatStateOf(1f) }
    var targetOffset by remember(mediaItem.id) { mutableStateOf(Offset.Zero) }
    var gestureActive by remember(mediaItem.id) { mutableStateOf(false) }
    val transformSpring = remember {
        spring<Float>(
            dampingRatio = GalleryMotion.ViewerTransformDamping,
            stiffness = GalleryMotion.ViewerTransformStiffness
        )
    }
    val panSpring = remember {
        spring<Offset>(
            dampingRatio = GalleryMotion.ViewerTransformDamping,
            stiffness = GalleryMotion.ViewerTransformStiffness
        )
    }
    val animatedScale = rememberDeferredAnimation(
        initialValue = 1f,
        typeConverter = Float.VectorConverter,
        targetValue = { targetScale },
        animationSpec = { transformSpring },
        label = "viewer photo zoom scale",
        animationKey = mediaItem.id
    )
    val animatedOffset = rememberDeferredAnimation(
        initialValue = Offset.Zero,
        typeConverter = Offset.VectorConverter,
        targetValue = { targetOffset },
        animationSpec = { panSpring },
        label = "viewer photo pan offset",
        animationKey = mediaItem.id
    )
    val photoTransform = Modifier.graphicsLayer {
        val scale = if (gestureActive) targetScale else animatedScale.value
        val offset = if (gestureActive) targetOffset else animatedOffset.value
        scaleX = scale
        scaleY = scale
        translationX = offset.x
        translationY = offset.y
    }
    val mediaModifier = Modifier.fillMaxSize()

    val photoZoomed by remember(mediaItem.id) { derivedStateOf { targetScale > 1.02f } }
    LaunchedEffect(isActive, photoZoomed, gestureActive) {
        if (isActive) {
            onZoomStateChanged(gestureActive || photoZoomed)
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            gestureActive = false
            targetScale = 1f
            targetOffset = Offset.Zero
            onZoomStateChanged(false)
        }
    }

    DisposableEffect(isActive) {
        onDispose {
            if (isActive) onZoomStateChanged(false)
        }
    }

    BoxWithConstraints(
        modifier = mediaModifier
            .testTag(ViewerZoomableMediaTestTag)
            .semantics {
                stateDescription = if (targetScale > 1.02f) "Zoomed" else "Fit"
            }
            .background(ViewerPhotoStageBackground)
            .graphicsLayer { alpha = activeMediaAlpha }
            .onGloballyPositioned {
                if (isActive) {
                    onActiveMediaPlaced()
                }
            }
            .pointerInput(mediaItem.id, mediaItem.isMotionPhoto) {
                detectTapGestures(
                    onPress = {
                        if (mediaItem.isMotionPhoto) {
                            val job = scope.launch {
                                kotlinx.coroutines.delay(180)
                                onStartMotionPlayback()
                            }
                            tryAwaitRelease()
                            job.cancel()
                            onStopMotionPlayback()
                        }
                    },
                    onTap = {
                        if (targetScale <= 1.02f) onToggleControls()
                    },
                    onDoubleTap = { tapOffset ->
                        gestureActive = false
                        val nextScale = viewerDoubleTapTargetScale(targetScale)
                        if (nextScale <= 1.02f) {
                            targetScale = 1f
                            targetOffset = Offset.Zero
                            if (isActive) onZoomStateChanged(false)
                        } else {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val desiredOffset = (center - tapOffset) * (nextScale - 1f)
                            val maxPanX = size.width * (nextScale - 1f) * 0.5f
                            val maxPanY = size.height * (nextScale - 1f) * 0.5f
                            targetScale = nextScale
                            targetOffset = desiredOffset.coercePan(maxPanX, maxPanY)
                            if (isActive) onZoomStateChanged(true)
                        }
                    }
                )
            }
            .pointerInput(mediaItem.id) {
                awaitPointerEventScope {
                    var previousDistance = 0f
                    var previousCentroid = Offset.Zero
                    var singlePointerPan = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        var pointerCount = 0
                        var positionSum = Offset.Zero
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                pointerCount += 1
                                positionSum += change.position
                            }
                        }
                        if (pointerCount > 1) {
                            singlePointerPan = Offset.Zero
                            gestureActive = true
                            val centroid = positionSum / pointerCount.toFloat()
                            var distanceSum = 0.0
                            event.changes.forEach { change ->
                                if (change.pressed) distanceSum += positionDistance(change.position, centroid)
                            }
                            val distance = (distanceSum / pointerCount).toFloat()
                            if (previousDistance > 0f) {
                                val zoom = (distance / previousDistance).takeIf { it.isFinite() } ?: 1f
                                val nextScale = viewerPinchScale(targetScale, zoom)
                                val pan = centroid - previousCentroid
                                val maxPanX = size.width * (nextScale - 1f) * 0.5f
                                val maxPanY = size.height * (nextScale - 1f) * 0.5f
                                targetScale = nextScale
                                targetOffset = if (nextScale <= 1.02f) {
                                    Offset.Zero
                                } else {
                                    (targetOffset + pan).coercePan(maxPanX, maxPanY)
                                }
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                            previousDistance = distance
                            previousCentroid = centroid
                        } else if (pointerCount == 1 && targetScale > 1.02f) {
                            previousDistance = 0f
                            previousCentroid = Offset.Zero
                            val change = event.changes.first { it.pressed }
                            val pan = change.position - change.previousPosition
                            singlePointerPan += pan
                            if (
                                gestureActive ||
                                positionDistance(singlePointerPan, Offset.Zero) > viewConfiguration.touchSlop
                            ) {
                                gestureActive = true
                                val maxPanX = size.width * (targetScale - 1f) * 0.5f
                                val maxPanY = size.height * (targetScale - 1f) * 0.5f
                                targetOffset = (targetOffset + pan).coercePan(maxPanX, maxPanY)
                                if (pan != Offset.Zero) change.consume()
                            }
                        } else {
                            previousDistance = 0f
                            previousCentroid = Offset.Zero
                            singlePointerPan = Offset.Zero
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
    ) {
        val w = mediaItem.width?.takeIf { it > 0 }
        val h = mediaItem.height?.takeIf { it > 0 }
        val isZoomed = (photoZoomed || gestureActive) && !isViewerDragging
        var zoomDecodeRequested by remember(mediaItem.id) { mutableStateOf(false) }
        LaunchedEffect(photoZoomed, gestureActive, isViewerDragging) {
            if (!gestureActive && !isViewerDragging) zoomDecodeRequested = photoZoomed
        }
        val (cardWidth, cardHeight) = if (isZoomed) {
            Pair(maxWidth, maxHeight)
        } else {
            val (wPx, hPx) = fittedMediaDimensions(
                containerWidth = maxWidth.value,
                containerHeight = maxHeight.value,
                mediaWidth = w,
                mediaHeight = h,
                fallbackAspect = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
            )
            Pair(wPx.dp, hPx.dp)
        }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.shadowElevation = dismissElevation().toPx()
                    val radius = dismissCornerRadius()
                    clip = radius.value > 0f
                    shape = RoundedCornerShape(radius)
                }
        ) {
            GalleryImage(
                imageRes = mediaItem.imageRes,
                imageUri = mediaItem.contentUri,
                contentDescription = mediaItem.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(photoTransform),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Fit,
                thumbnailSize = if (isActive) viewerPhotoResolution(
                    mediaItem.width, mediaItem.height, activePhotoDecodeSize, zoomDecodeRequested
                ) else minOf(activePhotoDecodeSize, 2048),
                loadQuality = ImageLoadQuality.HighQuality,
                backgroundColor = ViewerPhotoStageBackground,
                allowApproximateCache = true,
                initialBitmap = initialPreview,
                previewContentScale = if (isZoomed) ContentScale.Fit else ContentScale.Crop,
                cachedOnly = !isActive,
                fallbackImageUri = mediaItem.previewUri,
                cacheRevision = mediaThumbnailCacheRevision(mediaItem)
            )
            if (isPlayingMotionPhoto && motionVideoUri != null) {
                MotionPhotoPlayerOverlay(
                    videoUri = motionVideoUri,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(photoTransform)
                )
            }
        }
    }
}

@Composable
private fun MotionPhotoPlayerOverlay(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(videoUri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose {
            player.pause()
            player.stop()
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun BurstReviewOverlay(
    burstItems: List<MediaItem>,
    currentMainItem: MediaItem,
    onClose: () -> Unit,
    onSetAsMain: (MediaItem) -> Unit
) {
    var selectedItem by remember { mutableStateOf(currentMainItem) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 104.dp),
            contentAlignment = Alignment.Center
        ) {
            GalleryImage(
                imageRes = selectedItem.imageRes,
                imageUri = selectedItem.contentUri,
                contentDescription = selectedItem.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loadQuality = ImageLoadQuality.HighQuality,
                backgroundColor = Color.Black
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "Burst (${burstItems.size} photos)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetAsMain(selectedItem)
                        onClose()
                    },
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Set as main", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(burstItems) { index, item ->
                    val isSelected = item.id == selectedItem.id
                    val isMain = item.id == currentMainItem.id
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 76.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedItem = item
                            }
                    ) {
                        MediaThumbnail(
                            mediaItem = item,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 8.dp
                        )
                        if (isMain) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Main photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp).padding(1.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerVideoPoster(
    mediaItem: MediaItem,
    dismissCornerRadius: () -> Dp = { 0.dp },
    dismissElevation: () -> Dp = { 0.dp }
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val (wPx, hPx) = fittedMediaDimensions(
            containerWidth = maxWidth.value,
            containerHeight = maxHeight.value,
            mediaWidth = mediaItem.width,
            mediaHeight = mediaItem.height,
            fallbackAspect = 16f / 9f
        )
        val cardWidth = wPx.dp
        val cardHeight = hPx.dp
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.shadowElevation = dismissElevation().toPx()
                    val radius = dismissCornerRadius()
                    clip = radius.value > 0f
                    shape = RoundedCornerShape(radius)
                }
                .background(Color.Black)
                .clipToBounds()
        ) {
            GalleryImage(
                imageRes = mediaItem.imageRes,
                imageUri = mediaItem.contentUri,
                contentDescription = mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Fit,
                thumbnailSize = 512,
                loadQuality = ImageLoadQuality.Thumbnail,
                backgroundColor = Color.Transparent,
                fallbackImageUri = mediaItem.previewUri,
                isVideo = true,
                cacheRevision = mediaThumbnailCacheRevision(mediaItem)
            )
        }
    }
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun ViewerVideoPlayer(
    mediaItem: MediaItem,
    isActive: Boolean,
    isViewerDragging: Boolean = false,
    dismissCornerRadius: () -> Dp = { 0.dp },
    dismissElevation: () -> Dp = { 0.dp },
    controlsVisible: Boolean,
    compactLandscape: Boolean,
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
    initiallyMuted: Boolean,
    onDimensionsChanged: (Int, Int) -> Unit = { _, _ -> },
    onActiveMediaPlaced: () -> Unit,
    onToggleControls: () -> Unit,
    initialPreview: Bitmap? = null,
    onVideoTextureViewAvailable: (TextureView?) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val controlMaxWidth = viewerVideoControlMaxWidthDp(configuration.screenWidthDp).dp
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val uri = mediaItem.contentUri ?: return
    var isPrepared by remember(uri) { mutableStateOf(false) }
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var videoFillFrame by remember(uri) { mutableStateOf(false) }
    var hasPlaybackError by remember(uri) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(uri) { mutableStateOf(false) }
    var presentationReady by remember(uri) { mutableStateOf(false) }
    var hasRuntimeVideoSize by remember(uri) { mutableStateOf(false) }
    var playerView by remember(uri) { mutableStateOf<PlayerView?>(null) }
    var cachedPlayerFrame by remember(uri) { mutableStateOf(false) }
    val durationCacheIdentity = remember(
        uri,
        mediaItem.id,
        mediaItem.fileSizeBytes,
        mediaItem.dateModifiedMillis
    ) {
        VideoDurationCache.identity(
            uri = uri,
            mediaId = mediaItem.id,
            fileSizeBytes = mediaItem.fileSizeBytes,
            revisionMillis = mediaItem.dateModifiedMillis
        )
    }
    var durationMs by remember(uri) {
        mutableIntStateOf(mediaItem.durationMillis?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0)
    }
    var positionMs by rememberSaveable(mediaItem.id) { mutableIntStateOf(0) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    var pendingSeekPositionMs by remember(uri) { mutableStateOf<Int?>(null) }
    var pendingSeekRetryCount by remember(uri) { mutableIntStateOf(0) }
    var pendingSeekStartedAt by remember(uri) { mutableStateOf(0L) }
    var hasUserAdjustedVolume by remember(uri) { mutableStateOf(false) }
    var playbackRequested by rememberSaveable(mediaItem.id) { mutableStateOf(autoplay) }
    val currentPlaybackRequested by rememberUpdatedState(playbackRequested)
    var foregroundPlaybackAllowed by remember(uri) {
        mutableStateOf(
            playbackRequested && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }
    val rootView = LocalView.current
    val density = LocalDensity.current
    val sideGestureZonePx = with(density) { 60.dp.toPx() }
    val videoBrightness = brightnessState.floatValue
    val playbackVolume = playbackVolumeState.floatValue
    val isMuted = playbackVolume <= 0.01f
    val isMatroskaVideo = remember(uri, mediaItem.mimeType, mediaItem.title) {
        mediaItem.mimeType?.contains("matroska", ignoreCase = true) == true ||
            mediaItem.title.endsWith(".mkv", ignoreCase = true)
    }
    var playerVideoWidth by remember(uri) { mutableIntStateOf(mediaItem.width ?: 0) }
    var playerVideoHeight by remember(uri) { mutableIntStateOf(mediaItem.height ?: 0) }
    LaunchedEffect(uri) {
        val resolved = VideoPresentationGeometry.resolve(context.applicationContext, mediaItem)
        if (!hasRuntimeVideoSize && (resolved.width ?: 0) > 0 && (resolved.height ?: 0) > 0) {
            playerVideoWidth = resolved.width!!
            playerVideoHeight = resolved.height!!
            onDimensionsChanged(playerVideoWidth, playerVideoHeight)
        }
    }
    val playerStore: GalleryVideoPlayerStore = viewModel()
    val playerLease = remember(uri) { playerStore.acquire(context) }
    val player = remember(playerLease, uri, isMatroskaVideo) {
        playerLease.player.apply {
                setSeekParameters(if (isMatroskaVideo) SeekParameters.CLOSEST_SYNC else SeekParameters.DEFAULT)
                val playerMediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(uri)
                    .apply {
                        mediaItem.mimeType?.takeIf { it.isNotBlank() }?.let(::setMimeType)
                    }
                    .build()
                val reusePreparedMedia = currentMediaItem == playerMediaItem && playbackState != Player.STATE_IDLE && playerError == null
                if (!reusePreparedMedia) setMediaItem(playerMediaItem)
                seekTo(positionMs.toLong())
                setAudioAttributes(AudioAttributes.DEFAULT, true)
                setHandleAudioBecomingNoisy(true)
                volume = localPlayerGain(playbackVolume, systemVolumeApplied = true)
                playWhenReady =
                    isActive &&
                        playbackRequested &&
                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (!reusePreparedMedia) prepare()
        }
    }

    LaunchedEffect(uri, durationCacheIdentity, mediaItem.title, mediaItem.mimeType) {
        val resolvedDuration = withContext(Dispatchers.IO) {
            VideoDurationCache.get(context.applicationContext, durationCacheIdentity)
                .takeIf { it > 0L }
                ?: VideoDurationResolver.resolve(
                    context = context.applicationContext,
                    uri = uri,
                    displayName = mediaItem.title,
                    mimeType = mediaItem.mimeType
                ).also { resolved ->
                    if (resolved > 0L) {
                        VideoDurationCache.put(
                            context.applicationContext,
                            durationCacheIdentity,
                            resolved
                        )
                    }
                }
        }
        if (resolvedDuration > 0L) {
            durationMs = preferredVideoDurationMillis(player.duration, resolvedDuration, durationMs.toLong())
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
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
        hasUserAdjustedVolume = true
        if (boundedVolume > 0.01f) {
            lastAudibleVolumeState.floatValue = boundedVolume
        }
        playbackVolumeState.floatValue = boundedVolume
        applyVideoPlaybackVolume(
            audioManager = audioManager,
            player = player,
            targetVolume = boundedVolume
        )
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
        pendingSeekPositionMs = boundedTarget
        pendingSeekStartedAt = SystemClock.elapsedRealtime()
        pendingSeekRetryCount = 0
        positionMs = boundedTarget
        player.seekTo(boundedTarget.toLong())
    }

    fun seekBy(deltaMs: Int) {
        seekToPosition(positionMs + deltaMs)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            @Suppress("DEPRECATION")
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val dimensions = videoDisplayDimensions(videoSize.width, videoSize.height,
                        videoSize.unappliedRotationDegrees, videoSize.pixelWidthHeightRatio) ?: return
                    hasRuntimeVideoSize = true
                    playerVideoWidth = dimensions.width
                    playerVideoHeight = dimensions.height
                    VideoPresentationGeometry.record(mediaItem, dimensions)
                    onDimensionsChanged(dimensions.width, dimensions.height)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPrepared = videoPreparedStateAfterPlaybackState(isPrepared, playbackState)
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackError = false
                    val runtimeDuration = player.duration
                        .takeIf { it > 0L }
                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                    durationMs = preferredVideoDurationMillis(
                        runtimeDuration ?: 0L,
                        durationMs.toLong(),
                        mediaItem.durationMillis ?: 0L
                    ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    runtimeDuration?.let { resolvedDuration ->
                        VideoDurationCache.put(
                            context.applicationContext,
                            durationCacheIdentity,
                            resolvedDuration
                        )
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0L)
                    positionMs = 0
                    pendingSeekPositionMs = null
                    pendingSeekRetryCount = 0
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    val reportedPositionMs = newPosition.positionMs
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt()
                    val requestedPositionMs = pendingSeekPositionMs
                    if (
                        requestedPositionMs == null ||
                        isVideoSeekAcknowledged(requestedPositionMs, reportedPositionMs, durationMs)
                    ) {
                        positionMs = reportedPositionMs
                        pendingSeekPositionMs = null
                        pendingSeekRetryCount = 0
                    } else {
                        positionMs = requestedPositionMs
                        if (pendingSeekRetryCount < 1) {
                            pendingSeekRetryCount += 1
                            player.seekTo(requestedPositionMs.toLong())
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    VIDEO_PLAYBACK_LOG_TAG,
                    "Playback failed: code=${error.errorCodeName}, " +
                        "mime=${mediaItem.mimeType.orEmpty()}, hardware=${Build.HARDWARE}",
                    error
                )
                hasPlaybackError = true
                isPrepared = false
                isPlaying = false
                pendingSeekPositionMs = null
                pendingSeekRetryCount = 0
            }
        }
        player.addListener(listener)
        // A rapid reopen may reuse an already prepared player without a new READY event.
        isPrepared = videoPreparedStateAfterPlaybackState(false, player.playbackState)
        onDispose {
            positionMs = player.currentPosition
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()
            playerView?.player = null
            playerView = null
            player.removeListener(listener)
            playerStore.park(playerLease)
        }
    }

    LaunchedEffect(hasRenderedFirstFrame, playerVideoWidth, playerVideoHeight, playerView) {
        if (!presentationReady && hasRenderedFirstFrame && playerView != null) {
            // Allow AndroidView and TextureView to adopt the final display geometry before fading in.
            repeat(2) { androidx.compose.runtime.withFrameNanos { } }
            presentationReady = true
        }
    }
    LaunchedEffect(playerView, isActive) {
        if (isActive) {
            onVideoTextureViewAvailable(playerView?.videoSurfaceView as? TextureView)
        } else {
            onVideoTextureViewAvailable(null)
        }
    }
    LaunchedEffect(presentationReady, playerView, uri, cachedPlayerFrame) {
        if (!presentationReady || cachedPlayerFrame) return@LaunchedEffect
        val textureView = playerView?.videoSurfaceView as? TextureView ?: return@LaunchedEffect
        delay(16)
        val frame = captureVideoTransitionFrame(textureView) ?: return@LaunchedEffect
        try {
            cacheVideoFrameThumbnail(
                context,
                uri,
                frame,
                mediaThumbnailCacheRevision(mediaItem)
            )
            cachedPlayerFrame = true
        } finally {
            frame.recycle()
        }
    }

    LaunchedEffect(playbackVolume, player) {
        if (initiallyMuted && !hasUserAdjustedVolume && playbackVolume <= 0.01f) {
            player.volume = 0f
        } else {
            applyVideoPlaybackVolume(
                audioManager = audioManager,
                player = player,
                targetVolume = playbackVolume
            )
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { owner, _ ->
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                foregroundPlaybackAllowed = currentPlaybackRequested
            } else {
                foregroundPlaybackAllowed = false
                player.playWhenReady = false
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            foregroundPlaybackAllowed = false
            player.playWhenReady = false
            player.pause()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(playbackRequested, lifecycleOwner) {
        foregroundPlaybackAllowed = playbackRequested &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
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
    LaunchedEffect(isViewerDragging) {
        if (isViewerDragging) {
            player.playWhenReady = false
            player.pause()
        }
    }

    LaunchedEffect(
        isActive,
        isViewerDragging,
        isPrepared,
        foregroundPlaybackAllowed,
        lifecycleOwner.lifecycle.currentState
    ) {
        if (!isPrepared) return@LaunchedEffect

        val shouldPlay = shouldVideoPlaybackRun(
            isActive = isActive && !isViewerDragging,
            isPrepared = isPrepared,
            foregroundPlaybackAllowed = foregroundPlaybackAllowed,
            lifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
        if (shouldPlay && !player.isPlaying) {
            player.play()
        } else if (!shouldPlay && player.isPlaying) {
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
                val pendingSeek = pendingSeekPositionMs
                val reportedPosition = player.currentPosition
                    .coerceIn(0L, Int.MAX_VALUE.toLong())
                    .toInt()
                if (
                    pendingSeek != null &&
                    (isVideoSeekAcknowledged(pendingSeek, reportedPosition, durationMs) ||
                        SystemClock.elapsedRealtime() - pendingSeekStartedAt >= 3_000L)
                ) {
                    pendingSeekPositionMs = null
                    pendingSeekRetryCount = 0
                    positionMs = reportedPosition
                } else {
                    positionMs = pendingSeek ?: reportedPosition
                }
            }
            delay(250)
        }
    }

    // Draw progress at the display cadence without recomposing the player and controls each frame.
    val smoothPosition = remember(uri) { mutableFloatStateOf(positionMs.toFloat()) }
    LaunchedEffect(isActive, isPrepared, player) {
        while (isActive && isPrepared) {
            androidx.compose.runtime.withFrameNanos { }
            smoothPosition.floatValue = if (isScrubbing || pendingSeekPositionMs != null) positionMs.toFloat()
                else player.currentPosition.coerceIn(0L, durationMs.coerceAtLeast(1).toLong()).toFloat()
        }
    }
    val currentVideoAspect = remember(mediaItem.width, mediaItem.height, playerVideoWidth, playerVideoHeight) {
        val w = playerVideoWidth.takeIf { it > 0 } ?: mediaItem.width?.takeIf { it > 0 }
        val h = playerVideoHeight.takeIf { it > 0 } ?: mediaItem.height?.takeIf { it > 0 }
        if (w != null && h != null && w > 0 && h > 0) w.toFloat() / h.toFloat() else 16f / 9f
    }

    val playerAlpha by animateFloatAsState(
        targetValue = if (presentationReady) 1f else 0f,
        animationSpec = tween(120),
        label = "playerAlpha"
    )
    val thumbnailAlpha by animateFloatAsState(
        targetValue = if (presentationReady) 0f else 1f,
        animationSpec = tween(180, delayMillis = 60),
        label = "thumbnailAlpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                if (isActive) {
                    onActiveMediaPlaced()
                }
            }
    ) {
        val effW = playerVideoWidth.takeIf { it > 0 } ?: mediaItem.width?.takeIf { it > 0 }
        val effH = playerVideoHeight.takeIf { it > 0 } ?: mediaItem.height?.takeIf { it > 0 }
        val shouldFill = videoFillFrame && !isViewerDragging
        val (cardWidth, cardHeight) = if (shouldFill) {
            Pair(maxWidth, maxHeight)
        } else {
            val (wPx, hPx) = fittedMediaDimensions(
                containerWidth = maxWidth.value,
                containerHeight = maxHeight.value,
                mediaWidth = effW,
                mediaHeight = effH,
                fallbackAspect = currentVideoAspect ?: (16f / 9f)
            )
            Pair(wPx.dp, hPx.dp)
        }

        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.shadowElevation = dismissElevation().toPx()
                    val radius = dismissCornerRadius()
                    clip = radius.value > 0f
                    shape = RoundedCornerShape(radius)
                }
                .background(Color.Black)
                .clipToBounds()
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
                    .graphicsLayer { alpha = thumbnailAlpha },
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = 512,
                loadQuality = ImageLoadQuality.Thumbnail,
                backgroundColor = Color.Black,
                fallbackImageUri = mediaItem.previewUri,
                isVideo = true,
                cacheRevision = mediaThumbnailCacheRevision(mediaItem),
                initialBitmap = initialPreview
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = playerAlpha }
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

                        var activePointerId = down.id
                        var lastY = down.position.y
                        var sideGestureActivated = false
                        var moved = false
                        var released = false
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break
                            activePointerId = change.id
                            if (!change.pressed) {
                                released = true
                                break
                            }

                            val totalDrag = change.position - down.position
                            val deltaY = change.position.y - lastY
                            if (positionDistance(change.position, down.position) > touchSlop) {
                                moved = true
                            }
                            if (!sideGestureActivated) {
                                val horizontalClaim = abs(totalDrag.x) > touchSlop * 1.5f &&
                                    abs(totalDrag.x) > abs(totalDrag.y) * 1.15f
                                if (horizontalClaim) break
                                if (!shouldActivateVideoSideGesture(totalDrag, touchSlop)) continue
                                sideGestureActivated = true
                                lastY = change.position.y
                                if (sideControl == VideoSideControl.Volume) {
                                    updatePlaybackVolume(
                                        normalizedMediaVolume(
                                            currentVolume = audioManager.getStreamVolume(
                                                AudioManager.STREAM_MUSIC
                                            ),
                                            maximumVolume = audioManager.getStreamMaxVolume(
                                                AudioManager.STREAM_MUSIC
                                            )
                                        )
                                    )
                                }
                                showSideControl(sideControl)
                                change.consume()
                                continue
                            }

                            lastY = change.position.y
                            val valueDelta = (-deltaY / size.height.coerceAtLeast(1).toFloat()) * 1.05f
                            when (sideControl) {
                                VideoSideControl.Brightness -> updateVideoBrightness(brightnessState.floatValue + valueDelta)
                                VideoSideControl.Volume -> updatePlaybackVolume(playbackVolumeState.floatValue + valueDelta)
                            }
                            showSideControl(sideControl)
                            change.consume()
                        }
                        if (sideGestureActivated) {
                            showSideControl(sideControl)
                        } else if (released && !moved) {
                            onToggleControls()
                        }
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
                    playerView = this
                }
            },
            update = { view ->
                view.player = player
                playerView = view
                onVideoTextureViewAvailable(view.videoSurfaceView as? TextureView)
                view.resizeMode = if (shouldFill) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            onRelease = { view ->
                onVideoTextureViewAvailable(null)
                view.player = null
                if (playerView === view) {
                    playerView = null
                }
            }
        )
        }

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
            visible = !isViewerDragging && visibleSideControl == VideoSideControl.Brightness,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .zIndex(4f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            VerticalVideoControl(
                compact = compactLandscape,
                value = videoBrightness,
                onValueChange = { value -> updateVideoBrightness(value) },
                icon = Icons.Filled.Brightness6,
                contentDescription = "Video brightness"
            )
        }

        AnimatedVisibility(
            visible = !isViewerDragging && visibleSideControl == VideoSideControl.Volume,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .zIndex(4f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            VerticalVideoControl(
                compact = compactLandscape,
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
            visible = controlsVisible && !isViewerDragging && isPrepared,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .navigationBarsPadding()
                .padding(bottom = ViewerBottomChromeActionInsetDp.dp)
                .zIndex(3f),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(140))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = controlMaxWidth)
                    .fillMaxWidth()
                    .padding(
                        start = if (compactLandscape) 8.dp else 18.dp,
                        end = if (compactLandscape) 8.dp else 18.dp,
                        top = if (compactLandscape) 4.dp else 10.dp,
                        bottom = if (compactLandscape) 4.dp else 10.dp
                    )
            ) {
                if (compactLandscape) {
                    CompactVideoControlDeck(
                        positionMs = positionMs,
                        visualPosition = { if (isScrubbing) positionMs.toFloat() else smoothPosition.floatValue },
                        durationMs = durationMs,
                        onPositionChange = { value ->
                            isScrubbing = true
                            positionMs = value.roundToInt()
                        },
                        onPositionChangeFinished = {
                            seekToPosition(positionMs)
                            isScrubbing = false
                        },
                        isMuted = isMuted,
                        onToggleMute = {
                            if (isMuted) {
                                updatePlaybackVolume(lastAudibleVolumeState.floatValue)
                            } else {
                                lastAudibleVolumeState.floatValue = playbackVolume
                                updatePlaybackVolume(0f)
                            }
                        },
                        isPlaying = isPlaying,
                        onPlayPause = {
                            if (player.isPlaying) {
                                playbackRequested = false
                                foregroundPlaybackAllowed = false
                                player.pause()
                            } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                playbackRequested = true
                                foregroundPlaybackAllowed = true
                                player.play()
                            }
                        },
                        onRewind = { seekBy(-10_000) },
                        onForward = { seekBy(10_000) },
                        videoFillFrame = videoFillFrame,
                        onToggleFill = { videoFillFrame = !videoFillFrame }
                    )
                } else {
                    ViewerVideoSeekSlider(
                        value = positionMs.coerceIn(0, durationMs.coerceAtLeast(1)).toFloat(),
                        visualValue = { if (isScrubbing) positionMs.toFloat() else smoothPosition.floatValue },
                        onValueChange = { value ->
                            isScrubbing = true
                            positionMs = value.roundToInt()
                        },
                        onValueChangeFinished = {
                            seekToPosition(positionMs)
                            isScrubbing = false
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                        enabled = durationMs > 0
                    )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlaybackTime(positionMs),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodySmall.copy(shadow = ViewerVideoControlTextShadow)
                    )
                    Text(
                        text = formatPlaybackTime(durationMs),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall.copy(shadow = ViewerVideoControlTextShadow)
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
                                playbackRequested = false
                                foregroundPlaybackAllowed = false
                                player.pause()
                            } else {
                                if (
                                    lifecycleOwner.lifecycle.currentState
                                        .isAtLeast(Lifecycle.State.RESUMED)
                                ) {
                                    playbackRequested = true
                                    foregroundPlaybackAllowed = true
                                    player.play()
                                }
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
}

internal fun isMediaTekCodecPlatform(hardware: String): Boolean = isMediaTekHardware(hardware)

internal fun shouldPreferHardwareAvcDecoder(hardware: String, mimeType: String): Boolean {
    return preferMediaTekHardwareAvc(hardware, mimeType)
}

@Composable
private fun ViewerVideoSeekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    visualValue: () -> Float = { value }
) {
    val currentRange by rememberUpdatedState(valueRange)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinish by rememberUpdatedState(onValueChangeFinished)
    val thumbInset = with(LocalDensity.current) { 10.dp.toPx() }
    Box(
        modifier = modifier
            .height(ViewerVideoSeekHeightDp.dp)
            .semantics {
                contentDescription = "Video seek"
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                setProgress { target ->
                    if (!currentEnabled) false else {
                        currentOnChange(target.coerceIn(currentRange))
                        currentOnFinish()
                        true
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!currentEnabled) return@awaitEachGesture
                    // Freeze this gesture's range; late metadata cannot restart or resize a drag.
                    val dragRange = currentRange
                    fun scrub(x: Float) {
                        val fraction = ((x - thumbInset) / (size.width - thumbInset * 2).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        currentOnChange(dragRange.start + fraction * (dragRange.endInclusive - dragRange.start))
                    }
                    down.consume()
                    scrub(down.position.x)
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            scrub(change.position.x)
                            if (!change.pressed) break
                        }
                    } finally {
                        currentOnFinish()
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val inset = 10.dp.toPx()
            val trackEnd = (size.width - inset).coerceAtLeast(inset)
            val fraction = ((visualValue() - valueRange.start) /
                (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val thumbX = inset + (trackEnd - inset) * fraction
            val centerY = size.height / 2f
            drawLine(Color.White.copy(alpha = if (enabled) 0.28f else 0.12f),
                Offset(inset, centerY), Offset(trackEnd, centerY), 3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color.White, Offset(inset, centerY), Offset(thumbX, centerY),
                3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(Color.Black.copy(alpha = 0.15f), 7.dp.toPx(), Offset(thumbX, centerY + 1.dp.toPx()))
            drawCircle(Color.White, 5.dp.toPx(), Offset(thumbX, centerY))
        }
    }
}
@Composable
private fun CompactVideoControlDeck(
    positionMs: Int,
    visualPosition: () -> Float,
    durationMs: Int,
    onPositionChange: (Float) -> Unit,
    onPositionChangeFinished: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    videoFillFrame: Boolean,
    onToggleFill: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPlaybackTime(positionMs),
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = ViewerVideoControlTextShadow),
                maxLines = 1
            )
            ViewerVideoSeekSlider(
                value = positionMs.coerceIn(0, durationMs.coerceAtLeast(1)).toFloat(),
                visualValue = visualPosition,
                onValueChange = onPositionChange,
                onValueChangeFinished = onPositionChangeFinished,
                modifier = Modifier.weight(1f),
                valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                enabled = durationMs > 0
            )
            Text(formatPlaybackTime(durationMs), color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall.copy(shadow = ViewerVideoControlTextShadow))
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            VideoControlButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute video" else "Mute video",
                size = 48.dp,
                iconSize = 19.dp,
                containerAlpha = if (isMuted) 0.56f else 0.34f,
                onClick = onToggleMute
            )
            VideoControlButton(
                icon = Icons.Filled.Replay10,
                contentDescription = "Rewind 10 seconds",
                size = 48.dp,
                iconSize = 20.dp,
                onClick = onRewind
            )
            VideoControlButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause video" else "Play video",
                size = 48.dp,
                iconSize = 24.dp,
                containerAlpha = 0.48f,
                onClick = onPlayPause
            )
            VideoControlButton(
                icon = Icons.Filled.Forward10,
                contentDescription = "Forward 10 seconds",
                size = 48.dp,
                iconSize = 20.dp,
                onClick = onForward
            )
            VideoControlButton(
                icon = Icons.Filled.AspectRatio,
                contentDescription = if (videoFillFrame) "Fit video" else "Fill screen",
                size = 48.dp,
                iconSize = 19.dp,
                containerAlpha = if (videoFillFrame) 0.56f else 0.34f,
                onClick = onToggleFill
            )
        }
    }
}
@Composable
private fun VerticalVideoControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val controlledValue = value.coerceIn(0f, 1f)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Surface(
        modifier = modifier
            .size(width = 48.dp, height = if (compact) 148.dp else 188.dp)
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
                .padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
            Box(
                modifier = Modifier
                    .height(if (compact) 96.dp else 128.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(size)
            .solarScale(interactionSource, scaleDown = 0.88f)
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


private fun captureVideoTransitionFrame(textureView: TextureView?): Bitmap? {
    if (textureView == null || !textureView.isAvailable) return null
    val width = textureView.width
    val height = textureView.height
    if (width <= 0 || height <= 0) return null
    val scale = (1024f / max(width, height)).coerceAtMost(1f)
    return runCatching {
        textureView.getBitmap((width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1))
    }.getOrNull()
}

private fun viewerBackdropAlpha(offset: Offset, dismissThresholdPx: Float): Float {
    return 1f - viewerDismissProgress(offset, dismissThresholdPx)
}

private fun renderedViewerDragOffset(offset: Offset): Offset {
    return Offset(offset.x * 0.96f, offset.y * 0.96f)
}

internal fun viewerDismissScale(offset: Offset, dismissThresholdPx: Float): Float {
    return 1f - viewerDismissProgress(offset, dismissThresholdPx) * 0.28f
}

internal fun viewerDismissProgress(offset: Offset, dismissThresholdPx: Float): Float {
    if (dismissThresholdPx <= 0f) return 0f
    val downwardOffset = offset.y.coerceAtLeast(0f)
    val distance = sqrt(offset.x * offset.x + downwardOffset * downwardOffset)
    return (distance / (dismissThresholdPx * 2.8f)).coerceIn(0f, 1f)
}

internal fun viewerDismissFraction(pullScale: Float): Float {
    return ((1f - pullScale) / (1f - 0.72f)).coerceIn(0f, 1f)
}

internal fun fittedMediaDimensions(
    containerWidth: Float,
    containerHeight: Float,
    mediaWidth: Int?,
    mediaHeight: Int?,
    fallbackAspect: Float = 16f / 9f
): Pair<Float, Float> {
    val aspect = if (mediaWidth != null && mediaHeight != null && mediaWidth > 0 && mediaHeight > 0) {
        mediaWidth.toFloat() / mediaHeight.toFloat()
    } else {
        fallbackAspect
    }
    val containerAspect = if (containerHeight > 0f) containerWidth / containerHeight else aspect
    return if (containerAspect > aspect) {
        val h = containerHeight
        val w = h * aspect
        Pair(w, h)
    } else {
        val w = containerWidth
        val h = if (aspect > 0f) w / aspect else containerHeight
        Pair(w, h)
    }
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
    return downwardOffset > dismissThresholdPx * 0.72f ||
        (downwardOffset > dismissThresholdPx * 0.28f && distance > dismissThresholdPx * 0.95f) ||
        (downwardOffset > dismissThresholdPx * 0.18f && velocity.y > 650f) ||
        (downwardOffset > dismissThresholdPx * 0.22f && velocityMagnitude > 950f)
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

internal fun shouldActivateVideoSideGesture(totalDrag: Offset, touchSlop: Float): Boolean {
    val minimumVerticalTravel = touchSlop.coerceAtLeast(1f) * 2.25f
    return abs(totalDrag.y) >= minimumVerticalTravel &&
        abs(totalDrag.y) > abs(totalDrag.x) * 1.35f
}

internal fun normalizedMediaVolume(currentVolume: Int, maximumVolume: Int): Float {
    if (maximumVolume <= 0) return 1f
    return currentVolume.toFloat().div(maximumVolume).coerceIn(0f, 1f)
}

internal fun mediaStreamVolumeLevel(targetVolume: Float, maximumVolume: Int): Int {
    if (maximumVolume <= 0) return 0
    return (targetVolume.coerceIn(0f, 1f) * maximumVolume)
        .roundToInt()
        .coerceIn(0, maximumVolume)
}

internal fun localPlayerGain(targetVolume: Float, systemVolumeApplied: Boolean): Float {
    val boundedVolume = targetVolume.coerceIn(0f, 1f)
    return when {
        boundedVolume <= 0.01f -> 0f
        systemVolumeApplied -> 1f
        else -> boundedVolume
    }
}

internal fun videoPreparedStateAfterPlaybackState(wasPrepared: Boolean, playbackState: Int): Boolean {
    return when (playbackState) {
        Player.STATE_READY, Player.STATE_ENDED -> true
        Player.STATE_BUFFERING -> wasPrepared
        else -> false
    }
}

internal fun shouldVideoPlaybackRun(
    isActive: Boolean,
    isPrepared: Boolean,
    foregroundPlaybackAllowed: Boolean,
    lifecycleResumed: Boolean
): Boolean = isActive &&
    isPrepared &&
    foregroundPlaybackAllowed &&
    lifecycleResumed

private fun applyVideoPlaybackVolume(
    audioManager: AudioManager,
    player: Player,
    targetVolume: Float
) {
    val boundedVolume = targetVolume.coerceIn(0f, 1f)
    val maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val requestedLevel = mediaStreamVolumeLevel(boundedVolume, maximumVolume)
    val updateSucceeded = if (audioManager.isVolumeFixed) {
        false
    } else {
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, requestedLevel, 0)
        }.isSuccess
    }
    val appliedLevel = runCatching {
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }.getOrDefault(-1)
    player.volume = localPlayerGain(
        targetVolume = boundedVolume,
        systemVolumeApplied = updateSucceeded && appliedLevel == requestedLevel
    )
}

internal fun isVideoSeekAcknowledged(
    requestedPositionMs: Int,
    reportedPositionMs: Int,
    durationMs: Int
): Boolean {
    val toleranceMs = minOf(
        (durationMs / 50).coerceIn(1_500, 6_000),
        if (durationMs > 0) (durationMs / 10).coerceAtLeast(30) else 1_500
    )
    return abs(requestedPositionMs - reportedPositionMs) <= toleranceMs
}

internal fun preferredVideoDurationMillis(runtime: Long, resolved: Long, library: Long): Long =
    runtime.takeIf { it > 0L } ?: resolved.takeIf { it > 0L } ?: library.coerceAtLeast(0L)

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
