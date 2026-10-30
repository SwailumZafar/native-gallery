@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui.components

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.data.VideoFrameDecoder
import com.example.nativegallery.data.VideoDurationCache
import com.example.nativegallery.data.VideoDurationResolver
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.util.GalleryPerformanceMonitor
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


enum class ImageLoadQuality {
    Thumbnail,
    HighQuality
}

private class LayoutCoordinatesRef(var value: LayoutCoordinates? = null)

internal object ThumbnailLoadCoordinator {
    // A grid must not start sixteen simultaneous bitmap decodes beside an animation.
    private val loadSemaphore = Semaphore(permits = 4)
    val videoDecodeSemaphore = Semaphore(permits = 2)
    private val keyLocks = ConcurrentHashMap<String, KeyLock>()

    private class KeyLock {
        val mutex = Mutex()
        val users = AtomicInteger(0)
    }

    suspend fun load(
        cacheKey: String,
        validator: (Bitmap) -> Boolean = { true },
        loader: suspend () -> Bitmap?
    ): Bitmap? {
        validatedCachedBitmap(cacheKey, validator)?.let { return it }
        val keyLock = keyLocks.compute(cacheKey) { _, existing ->
            (existing ?: KeyLock()).also { it.users.incrementAndGet() }
        } ?: return null
        return try {
            keyLock.mutex.withLock {
                validatedCachedBitmap(cacheKey, validator) ?: loadSemaphore.withPermit {
                    withContext(Dispatchers.IO) {
                        currentCoroutineContext().ensureActive()
                        val decoded = try { loader() }
                        catch (_: java.io.IOException) { null }
                        catch (_: SecurityException) { null }
                        decoded?.takeIf(validator)?.also { bitmap ->
                            bitmap.prepareToDraw()
                            ThumbnailMemoryCache.put(cacheKey, bitmap)
                        }
                    }
                }
            }
        } finally {
            keyLocks.computeIfPresent(cacheKey) { _, current ->
                if (current !== keyLock) {
                    current
                } else if (current.users.decrementAndGet() == 0) {
                    null
                } else {
                    current
                }
            }
        }
    }

    private fun validatedCachedBitmap(
        cacheKey: String,
        validator: (Bitmap) -> Boolean
    ): Bitmap? {
        val cached = ThumbnailMemoryCache.get(cacheKey) ?: return null
        if (validator(cached)) return cached
        ThumbnailMemoryCache.remove(cacheKey)
        return null
    }
}

private val thumbnailPrefetchSemaphore = Semaphore(permits = 2)
private val videoPrefetchSemaphore = Semaphore(permits = 1)
private val videoMetadataSemaphore = Semaphore(permits = 2)
private const val VideoPosterCacheVersion = "native-gallery-video-poster-v2"

private data class ThumbnailPrefetchRequest(
    val uri: Uri,
    val size: Int,
    val isVideo: Boolean,
    val cacheRevision: String
)

internal fun mediaThumbnailCacheRevision(mediaItem: MediaItem): String {
    return "${mediaItem.fileSizeBytes ?: -1L}:${mediaItem.dateModifiedMillis}"
}

internal fun mediaThumbnailIsVideo(mediaItem: MediaItem): Boolean =
    mediaItem.isVideo && (mediaItem.previewUri == null || mediaItem.contentUri != mediaItem.previewUri)

internal fun shouldShowVideoThumbnailFallback(isVideo: Boolean, loadComplete: Boolean, cachedOnly: Boolean): Boolean {
    return isVideo && loadComplete && !cachedOnly
}

@Composable
internal fun rememberVideoDurationMillis(mediaItem: MediaItem): Long? {
    if (mediaItem.previewUri != null && mediaItem.contentUri == mediaItem.previewUri) return mediaItem.durationMillis
    val context = LocalContext.current.applicationContext
    val uri = mediaItem.contentUri
    val revision = mediaThumbnailCacheRevision(mediaItem)
    var duration by remember(uri, revision) { mutableStateOf(mediaItem.durationMillis) }
    LaunchedEffect(uri, revision, mediaItem.isVideo) {
        if (!mediaItem.isVideo || uri == null) return@LaunchedEffect
        duration = withContext(Dispatchers.IO) {
            videoMetadataSemaphore.withPermit {
                val key = VideoDurationCache.identity(uri, mediaItem.id, mediaItem.fileSizeBytes, mediaItem.dateModifiedMillis)
                VideoDurationCache.get(context, key).takeIf { it > 0L } ?: run {
                    // Grid badges use known metadata; the full viewer resolves container timing.
                    val needsRecovery = duration == null || duration!! <= 0L
                    if (needsRecovery) {
                        VideoDurationResolver.resolve(context, uri, mediaItem.title, mediaItem.mimeType)
                            .takeIf { it > 0L }
                            ?.also { VideoDurationCache.put(context, key, it) }
                            ?: mediaItem.durationMillis
                    } else mediaItem.durationMillis
                }
            }
        }
    }
    return duration
}

internal fun thumbnailCacheIdentityUri(
    uri: Uri,
    isVideo: Boolean,
    cacheRevision: String?
): Uri {
    if (!isVideo) return uri
    return uri.buildUpon()
        .fragment("$VideoPosterCacheVersion:${cacheRevision.orEmpty()}")
        .build()
}

internal fun mediaThumbnailCacheUri(mediaItem: MediaItem): Uri? = mediaItem.contentUri?.let {
    thumbnailCacheIdentityUri(it, mediaThumbnailIsVideo(mediaItem), mediaThumbnailCacheRevision(mediaItem))
}

@Composable
fun ScreenHeader(
    title: String,
    actions: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.solarTactile(scaleDown = 0.88f, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SearchPill(
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    query: String = "",
    focusRequester: FocusRequester? = null,
    onQueryChange: ((String) -> Unit)? = null
) {
    val editable = onQueryChange != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "searchPillPressScale"
    )
    val elevationDp by animateFloatAsState(
        targetValue = if (isFocused || query.isNotBlank() || isPressed) 5f else 1.5f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "searchPillElevation"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFocused || query.isNotBlank()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "searchIconTint"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.38f else 0f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "searchBorderAlpha"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(36.dp),
        shadowElevation = elevationDp.dp,
        border = if (borderAlpha > 0.01f) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = iconTint,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        val activeScale = if (isFocused || query.isNotBlank()) 1.08f else 1f
                        scaleX = activeScale
                        scaleY = activeScale
                    }
            )
            Spacer(Modifier.width(16.dp))
            if (editable) {
                BasicTextField(
                    value = query,
                    onValueChange = { value -> onQueryChange?.invoke(value) },
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = scaleIn(spring(dampingRatio = 0.72f, stiffness = 500f)) + fadeIn(tween(120)),
                    exit = scaleOut(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeOut(tween(100))
                ) {
                    IconButton(
                        onClick = { onQueryChange?.invoke("") },
                        modifier = Modifier.solarTactile(scaleDown = 0.85f) { onQueryChange?.invoke("") }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementKey: Any? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    isSharedElementSourceHidden: Boolean = false,
    selected: Boolean = false,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    deferUncachedLoad: State<Boolean>? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickWithBounds: ((Rect) -> Unit)? = null
) {
    val measuredModifier = if (onBoundsChanged == null) {
        modifier
    } else {
        modifier.onGloballyPositioned { coordinates ->
            onBoundsChanged(coordinates.boundsInWindow())
        }
    }
    val coordinatesRef = remember { LayoutCoordinatesRef() }
    val latestBoundsCallback = androidx.compose.runtime.rememberUpdatedState(onBoundsChanged)
    if (onBoundsChanged != null) {
        // A pager selection changes alpha without a layout pass. Republish the actual tile
        // coordinates so a newly selected, already visible photo still has a close target.
        LaunchedEffect(mediaItem.id, isSharedElementSourceHidden) {
            androidx.compose.runtime.withFrameNanos { }
            coordinatesRef.value?.takeIf { it.isAttached }?.boundsInWindow()?.let { bounds ->
                latestBoundsCallback.value?.invoke(bounds)
            }
        }
    }
    val positionedModifier = if (onClickWithBounds == null) {
        measuredModifier
    } else {
        measuredModifier.onPlaced { coordinates -> coordinatesRef.value = coordinates }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "thumbnailPressScale"
    )
    val containerModifier = if (onClick != null || onClickWithBounds != null || onLongClick != null) {
        positionedModifier
            .graphicsLayer {
                val scale = pressScaleState.value
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = {
                    val bounds = coordinatesRef.value
                        ?.takeIf { it.isAttached }
                        ?.boundsInWindow()
                        ?: Rect.Zero
                    onClickWithBounds?.invoke(bounds) ?: onClick?.invoke()
                }
            )
    } else {
        measuredModifier
    }
    val imageModifier = Modifier
        .fillMaxSize()

    val visibleContainerModifier = if (isSharedElementSourceHidden) {
        containerModifier.graphicsLayer { alpha = 0f }
    } else {
        containerModifier
    }
    val departing = mediaItem.id in com.example.nativegallery.ui.LocalDepartingMediaIds.current
    val removal = remember(mediaItem.id) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(departing) {
        if (departing) removal.animateTo(0f, tween(180)) else removal.snapTo(1f)
    }
    Box(modifier = if (departing) visibleContainerModifier.graphicsLayer {
        alpha = removal.value
        scaleX = 0.82f + removal.value * 0.18f
        scaleY = scaleX
    } else visibleContainerModifier) {
        GalleryImage(
            imageRes = mediaItem.imageRes,
            imageUri = mediaItem.contentUri,
            contentDescription = mediaItem.title,
            modifier = imageModifier,
            cornerRadius = cornerRadius,
            thumbnailSize = 384,
            sharpenThumbnail = true,
            displayedBitmapKey = sharedElementKey,
            animatedPlaceholder = false,
            deferUncachedLoad = deferUncachedLoad,
            isVideo = mediaThumbnailIsVideo(mediaItem),
            cacheRevision = mediaThumbnailCacheRevision(mediaItem)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = 0.72f, stiffness = 480f)) + fadeIn(tween(100)),
            exit = scaleOut(spring(dampingRatio = 0.84f, stiffness = 480f)) + fadeOut(tween(80)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(7.dp)
                    .size(24.dp)
            )
        }
        if (mediaItem.isVideo && mediaItem.contentUri != null) {
            val durationMillis = rememberVideoDurationMillis(mediaItem)
            VideoBadge(
                duration = durationMillis?.takeIf { it > 0L }?.let { duration ->
                    val seconds = duration / 1_000L
                    if (seconds >= 3_600L) {
                        String.format(java.util.Locale.US, "%d:%02d:%02d", seconds / 3_600L, seconds / 60L % 60L, seconds % 60L)
                    } else String.format(java.util.Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
                } ?: mediaItem.durationLabel.orEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        } else if (mediaItem.isBurst && mediaItem.burstCount > 1) {
            BurstBadge(
                count = mediaItem.burstCount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        } else if (mediaItem.isMotionPhoto) {
            MotionBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.mediaSharedElement(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedElementKey: Any?,
    sharedBoundsTransform: BoundsTransform?,
    callerManagedVisibility: Boolean? = null
): Modifier {
    val transitionScope = sharedTransitionScope ?: return this
    val key = sharedElementKey ?: return this

    return with(transitionScope) {
        val sharedContentState = rememberSharedContentState(key = key)
        if (callerManagedVisibility != null) {
            if (sharedBoundsTransform != null) {
                this@mediaSharedElement.sharedElementWithCallerManagedVisibility(
                    sharedContentState = sharedContentState,
                    visible = callerManagedVisibility,
                    boundsTransform = sharedBoundsTransform
                )
            } else {
                this@mediaSharedElement.sharedElementWithCallerManagedVisibility(
                    sharedContentState = sharedContentState,
                    visible = callerManagedVisibility
                )
            }
        } else {
            val visibilityScope = animatedVisibilityScope ?: return@with this@mediaSharedElement
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
}

@Composable
fun ResourceImage(
    imageRes: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    imageUri: Uri? = null,
    thumbnailSize: Int = 512
) {
    GalleryImage(
        imageRes = imageRes,
        imageUri = imageUri,
        contentDescription = contentDescription,
        modifier = modifier,
        cornerRadius = cornerRadius,
        thumbnailSize = thumbnailSize
    )
}

@Composable
fun GalleryImage(
    imageRes: Int?,
    imageUri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Int = 512,
    loadQuality: ImageLoadQuality = ImageLoadQuality.Thumbnail,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    colorFilter: ColorFilter? = null,
    cachedOnly: Boolean = false,
    allowApproximateCache: Boolean = true,
    fallbackImageUri: Uri? = null,
    isVideo: Boolean = false,
    cacheRevision: String? = null,
    animatedPlaceholder: Boolean = true,
    deferUncachedLoad: State<Boolean>? = null,
    drawBounds: (() -> Rect)? = null,
    initialBitmap: Bitmap? = null,
    previewContentScale: ContentScale? = null,
    sharpenThumbnail: Boolean = false,
    displayedBitmapKey: Any? = null
) {
    var renderedSize by remember { mutableStateOf(IntSize.Zero) }
    val requestSize = if (sharpenThumbnail && isVideo && renderedSize != IntSize.Zero) {
        gridVideoThumbnailSize(renderedSize.width, renderedSize.height, thumbnailSize)
    } else thumbnailSize
    val imageState = rememberContentUriBitmap(
        imageUri = imageUri,
        thumbnailSize = requestSize,
        loadQuality = loadQuality,
        cachedOnly = cachedOnly,
        allowApproximateCache = allowApproximateCache,
        deferUncachedLoad = deferUncachedLoad,
        isVideo = isVideo,
        cacheRevision = cacheRevision
    )
    val detailBitmap = rememberGridThumbnailDetail(imageUri, cacheRevision, renderedSize.width, renderedSize.height,
        enabled = sharpenThumbnail && !isVideo && !cachedOnly && contentScale == ContentScale.Crop)
    val bitmap = detailBitmap ?: imageState.bitmap ?: initialBitmap?.takeUnless { it.isRecycled }
    val fallbackBitmap = rememberContentUriBitmap(
        imageUri = fallbackImageUri?.takeUnless { it == imageUri },
        thumbnailSize = min(thumbnailSize, 512),
        loadQuality = ImageLoadQuality.Thumbnail,
        cachedOnly = cachedOnly,
        allowApproximateCache = true,
        deferUncachedLoad = deferUncachedLoad,
        isVideo = false,
        cacheRevision = null
    ).bitmap
    val displayedBitmap = bitmap ?: fallbackBitmap
    androidx.compose.runtime.DisposableEffect(imageUri, displayedBitmap, displayedBitmapKey) {
        val owner = Any()
        if (imageUri != null && displayedBitmap != null) DisplayedBitmapCache.register(owner, imageUri, displayedBitmap, displayedBitmapKey)
        onDispose { DisplayedBitmapCache.unregister(owner) }
    }
    val shapedModifier = if (cornerRadius > 0.dp) {
        modifier.clip(RoundedCornerShape(cornerRadius))
    } else {
        modifier
    }
    Box(
        modifier = shapedModifier.background(backgroundColor).then(
            if (sharpenThumbnail) Modifier.onSizeChanged { renderedSize = it } else Modifier)
    ) {
        when {
            bitmap != null || fallbackBitmap != null -> {
                val image = remember(bitmap, fallbackBitmap) { (bitmap ?: fallbackBitmap!!).asImageBitmap() }
                if (drawBounds != null) {
                    // Keep the image's layout fixed. Only its crop is redrawn as the hero moves.
                    Canvas(Modifier.fillMaxSize()) {
                        val bounds = drawBounds()
                        val scale = max(bounds.width / image.width, bounds.height / image.height)
                        clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                            withTransform({
                                translate(
                                    bounds.center.x - image.width * scale / 2f,
                                    bounds.center.y - image.height * scale / 2f
                                )
                                scale(scale, scale, pivot = Offset.Zero)
                            }) {
                                drawImage(image, colorFilter = colorFilter,
                                    filterQuality = if (sharpenThumbnail) androidx.compose.ui.graphics.FilterQuality.High
                                        else androidx.compose.ui.graphics.FilterQuality.Low)
                            }
                        }
                    }
                } else {
                    Image(
                        bitmap = image,
                        contentDescription = contentDescription,
                        contentScale = if (imageState.loadComplete) contentScale else previewContentScale ?: contentScale,
                        filterQuality = if (sharpenThumbnail) androidx.compose.ui.graphics.FilterQuality.High
                            else androidx.compose.ui.graphics.FilterQuality.Low,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = colorFilter
                    )
                }
            }
            imageRes != null -> {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = colorFilter
                )
            }
            shouldShowVideoThumbnailFallback(isVideo, imageState.loadComplete, cachedOnly) -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            animatedPlaceholder -> {
                SkeletonBlock(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp
                )
            }
            else -> Unit
        }
    }
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    val shimmerColors = remember(base, highlight) { listOf(base, highlight, base) }
    val transition = rememberInfiniteTransition(label = "skeleton shimmer")
    val offset = transition.animateFloat(
        initialValue = -420f,
        targetValue = 840f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GalleryMotion.SkeletonShimmerMillis, easing = LinearEasing)
        ),
        label = "skeleton shimmer offset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val currentOffset = offset.value
                drawRect(Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(currentOffset, 0f),
                    end = Offset(currentOffset + 360f, 360f)
                ))
            }
    )
}

@Composable
fun MediaAccessNotice(
    message: String,
    actionLabel: String,
    onRequestAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = onRequestAccess) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun VideoBadge(
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.42f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            if (duration.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = duration,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
fun BurstBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.54f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.BurstMode,
                contentDescription = "Burst photo",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            if (count > 1) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = count.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun MotionBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.54f),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MotionPhotosOn,
                contentDescription = "Motion photo",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

suspend fun prefetchMediaThumbnails(
    context: Context,
    mediaItems: List<MediaItem>,
    thumbnailSizes: List<Int> = listOf(384, 512),
    maxItems: Int = 96,
    pinInMemory: Boolean = false,
    videoCacheOnly: Boolean = false
) {
    if (mediaItems.isEmpty()) return
    val appContext = context.applicationContext
    val sizes = thumbnailSizes
        .filter { it > 0 }
        .distinct()
        .ifEmpty { listOf(384) }
    val requests = mediaItems.asSequence()
        .mapNotNull { mediaItem ->
            mediaItem.contentUri?.let { uri ->
                Triple(uri, mediaThumbnailIsVideo(mediaItem), mediaThumbnailCacheRevision(mediaItem))
            }
        }
        .distinctBy { it.first }
        .take(maxItems.coerceAtLeast(0))
        .flatMap { (uri, isVideo, cacheRevision) ->
            sizes.asSequence().map { size ->
                ThumbnailPrefetchRequest(uri, size, isVideo, cacheRevision)
            }
        }
        .distinct()
        .toList()
    if (requests.isEmpty()) return

    GalleryPerformanceMonitor.traceSuspend("Thumbnail prefetch ${mediaItems.size} items") {
        coroutineScope {
            requests.map { request ->
                async {
                    // A slow/paused video codec must never occupy both photo-prefetch slots.
                    val semaphore = if (request.isVideo) videoPrefetchSemaphore else thumbnailPrefetchSemaphore
                    semaphore.withPermit {
                        currentCoroutineContext().ensureActive()
                        val loadQuality = if (request.size >= 1024 && !request.isVideo) {
                            ImageLoadQuality.HighQuality
                        } else ImageLoadQuality.Thumbnail
                        val bitmap = if (request.isVideo && videoCacheOnly) {
                            loadPersistedThumbnail(
                                appContext, request.uri, request.size, loadQuality,
                                isVideo = true, cacheRevision = request.cacheRevision
                            )
                        } else loadCachedBitmap(
                            context = appContext,
                            imageUri = request.uri,
                            thumbnailSize = request.size,
                            loadQuality = loadQuality,
                            isVideo = request.isVideo,
                            cacheRevision = request.cacheRevision
                        )
                        if (pinInMemory && bitmap != null) {
                            val cacheUri = thumbnailCacheIdentityUri(
                                request.uri,
                                request.isVideo,
                                request.cacheRevision
                            )
                            ThumbnailMemoryCache.pin(
                                ThumbnailMemoryCache.key(cacheUri, request.size, loadQuality),
                                bitmap
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }
}
private val VideoFrameCacheScope = CoroutineScope(Dispatchers.IO)

fun cacheVideoFrameThumbnail(
    context: Context,
    uri: Uri,
    frame: Bitmap,
    cacheRevision: String? = null
) {
    if (frame.isRecycled || frame.width <= 0 || frame.height <= 0) return
    val appContext = context.applicationContext
    val cacheUri = thumbnailCacheIdentityUri(uri, isVideo = true, cacheRevision)
    // The caller may recycle its capture. Copy once, then scale and persist off the main thread.
    val snapshot = frame.copy(Bitmap.Config.ARGB_8888, false) ?: return
    VideoFrameCacheScope.launch {
        val highQualityKey = ThumbnailMemoryCache.key(cacheUri, 1024, ImageLoadQuality.HighQuality)
        val highQualityBitmap = if (max(snapshot.width, snapshot.height) <= 1024) {
            snapshot
        } else {
            val scale = 1024f / max(snapshot.width, snapshot.height)
            Bitmap.createScaledBitmap(snapshot, (snapshot.width * scale).roundToInt(), (snapshot.height * scale).roundToInt(), true)
        }
        highQualityBitmap?.let {
            ThumbnailMemoryCache.replace(highQualityKey, it)
        }

        listOf(384, 512).forEach { size ->
            val cacheKey = ThumbnailMemoryCache.key(cacheUri, size)
            // Keep the displayed poster stable across playback sessions. Captures fill missing posters.
            if (ThumbnailMemoryCache.get(cacheKey) != null) return@forEach
            val scale = (size.toFloat() / max(snapshot.width, snapshot.height)).coerceAtMost(1f)
            val width = (snapshot.width * scale).roundToInt().coerceAtLeast(1)
            val height = (snapshot.height * scale).roundToInt().coerceAtLeast(1)
            val cachedFrame = if (width == snapshot.width && height == snapshot.height) {
                snapshot.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                Bitmap.createScaledBitmap(snapshot, width, height, true)
            }
            ThumbnailMemoryCache.put(cacheKey, cachedFrame)
            ThumbnailMemoryCache.pin(cacheKey, cachedFrame)
            ThumbnailDiskCache.put(appContext, cacheKey, cachedFrame)
        }
        if (highQualityBitmap !== snapshot) snapshot.recycle()
    }
}

@Composable
private fun rememberContentUriBitmap(
    imageUri: Uri?,
    thumbnailSize: Int,
    loadQuality: ImageLoadQuality,
    cachedOnly: Boolean,
    allowApproximateCache: Boolean,
    isVideo: Boolean,
    cacheRevision: String?,
    deferUncachedLoad: State<Boolean>?
): GalleryBitmapResult {
    val context = LocalContext.current.applicationContext
    val cacheUri = imageUri?.let { thumbnailCacheIdentityUri(it, isVideo, cacheRevision) }
    val cacheKey = cacheUri?.let { ThumbnailMemoryCache.key(it, thumbnailSize, loadQuality) }
    var loadComplete by remember(imageUri, cacheKey, cachedOnly, allowApproximateCache) {
        mutableStateOf(imageUri == null)
    }

    var bitmap by remember(
        imageUri,
        thumbnailSize,
        loadQuality,
        cachedOnly,
        allowApproximateCache,
        isVideo,
        cacheRevision
    ) {
        mutableStateOf(
            cacheKey?.let { key ->
                ThumbnailMemoryCache.get(key)?.takeUnless { it.isRecycled }
            }
                ?: imageUri
                    ?.takeIf { allowApproximateCache }
                    ?.let {
                        ThumbnailMemoryCache.getNearest(
                            thumbnailCacheIdentityUri(it, isVideo, cacheRevision),
                            thumbnailSize,
                            loadQuality
                        ) ?: if (loadQuality == ImageLoadQuality.HighQuality) {
                            ThumbnailMemoryCache.getNearest(it, min(thumbnailSize, 512))
                        } else null
                    }
        )
    }

    LaunchedEffect(
        imageUri,
        thumbnailSize,
        loadQuality,
        cachedOnly,
        allowApproximateCache,
        isVideo,
        cacheRevision,
        deferUncachedLoad
    ) {
        if (imageUri == null || cacheKey == null) {
            bitmap = null
            return@LaunchedEffect
        }
        val resolvedCacheUri = cacheUri ?: return@LaunchedEffect

        suspend fun loadAndPublish(allowUncached: Boolean) {
            ThumbnailMemoryCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { cachedBitmap ->
                bitmap = cachedBitmap
                loadComplete = true
                return
            }
            if (allowApproximateCache) {
                ThumbnailMemoryCache.getNearest(resolvedCacheUri, thumbnailSize, loadQuality)?.let { cachedBitmap ->
                    if (bitmap == null) bitmap = cachedBitmap
                }
            }
            if (!allowUncached && !cachedOnly) return
            if (
                loadQuality == ImageLoadQuality.Thumbnail && thumbnailSize <= 512 &&
                (cachedOnly || !allowUncached)
            ) {
                val diskCached = withContext(Dispatchers.IO) {
                    ThumbnailDiskCache.get(context, cacheKey)?.takeUnless { it.isRecycled }
                }
                if (diskCached != null) {
                    diskCached.prepareToDraw()
                    ThumbnailMemoryCache.put(cacheKey, diskCached)
                    bitmap = diskCached
                    loadComplete = true
                    return
                }
            }
            if (cachedOnly) {
                loadComplete = true
                return
            }
            if (!allowUncached) return

            var observedRevision = GalleryImageWorkScheduler.mediaRevision()
            while (true) {
                val loadedBitmap = loadCachedBitmap(context, imageUri, thumbnailSize, loadQuality, isVideo, cacheRevision)
                if (loadedBitmap != null) {
                    GalleryImageWorkScheduler.awaitIdle()
                    bitmap = loadedBitmap
                    loadComplete = true
                    return
                }
                loadComplete = true
                if (bitmap != null) return
                // Failed visible requests retry after a completed library mutation. Successful
                // cache hits never subscribe or restart, and no decoder permit is held here.
                GalleryImageWorkScheduler.awaitLibraryChange(observedRevision)
                observedRevision = GalleryImageWorkScheduler.mediaRevision()
            }
        }

        val loadGate = deferUncachedLoad
        if (loadGate == null) {
            loadAndPublish(allowUncached = true)
        } else {
            snapshotFlow { loadGate.value }.collectLatest { deferred ->
                loadAndPublish(allowUncached = !deferred)
            }
        }
    }
    return GalleryBitmapResult(bitmap, loadComplete)
}

private data class GalleryBitmapResult(val bitmap: Bitmap?, val loadComplete: Boolean)

private suspend fun loadCachedBitmap(
    context: Context,
    imageUri: Uri,
    thumbnailSize: Int,
    loadQuality: ImageLoadQuality,
    isVideo: Boolean = false,
    cacheRevision: String? = null
): Bitmap? {
    val cacheUri = thumbnailCacheIdentityUri(imageUri, isVideo, cacheRevision)
    val cacheKey = ThumbnailMemoryCache.key(cacheUri, thumbnailSize, loadQuality)
    val diskCacheEligible = loadQuality == ImageLoadQuality.Thumbnail && thumbnailSize <= if (isVideo) 1536 else 512
    val validator: (Bitmap) -> Boolean = { bitmap ->
        !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0
    }
    // Prefetch uses this path too. Never decode a disk entry again for a live memory hit.
    ThumbnailMemoryCache.get(cacheKey)?.takeIf(validator)?.let { return it }
    GalleryImageWorkScheduler.awaitIdle()
    if (diskCacheEligible) {
        val diskHit = withContext(Dispatchers.IO) {
            ThumbnailDiskCache.get(context, cacheKey)?.let { cached ->
                if (validator(cached)) {
                    cached.prepareToDraw()
                    ThumbnailMemoryCache.put(cacheKey, cached)
                    cached
                } else {
                    ThumbnailDiskCache.remove(context, cacheKey)
                    null
                }
            }
        }
        if (diskHit != null) return diskHit
    }
    suspend fun decodePlatformThumbnail(): Bitmap? = ThumbnailLoadCoordinator.load(cacheKey, validator) {
        val loaded = when (loadQuality) {
            ImageLoadQuality.Thumbnail -> {
                loadThumbnail(context, imageUri, thumbnailSize, isVideo = isVideo)
            }
            ImageLoadQuality.HighQuality -> loadHighQualityBitmap(context, imageUri, thumbnailSize)
                ?: loadSampledBitmap(context, imageUri, thumbnailSize)
                ?: if (isVideo) loadThumbnail(context, imageUri, thumbnailSize, isVideo = true) else null
        }
        if (diskCacheEligible) {
            loaded?.let { ThumbnailDiskCache.put(context, cacheKey, it) }
        }
        loaded
    }
    // Disk hits are served above, independently of slow video codecs. Never occupy the
    // shared image-loader permits while queued behind another video's native decoder.
    val platformBitmap = if (isVideo) {
        ThumbnailLoadCoordinator.videoDecodeSemaphore.withPermit { decodePlatformThumbnail() }
    } else {
        decodePlatformThumbnail()
    }
    if (platformBitmap != null || !isVideo) return platformBitmap

    // A fallback may wait while the user views media. Do not hold the shared image-loader
    // permits or per-key lock during that wait: full-quality photos must still load immediately.
    ThumbnailMemoryCache.get(cacheKey)?.let { return it }
    val decodedFrame = VideoThumbnailDecoder.decode(context, imageUri, thumbnailSize) ?: return null
    currentCoroutineContext().ensureActive()
    decodedFrame.prepareToDraw()
    ThumbnailMemoryCache.replace(cacheKey, decodedFrame)
    if (diskCacheEligible) ThumbnailDiskCache.replace(context, cacheKey, decodedFrame)
    return decodedFrame
}

private suspend fun loadPersistedThumbnail(
    context: Context,
    imageUri: Uri,
    thumbnailSize: Int,
    loadQuality: ImageLoadQuality,
    isVideo: Boolean,
    cacheRevision: String?
): Bitmap? {
    if (loadQuality != ImageLoadQuality.Thumbnail || thumbnailSize > if (isVideo) 1536 else 512) return null
    val cacheUri = thumbnailCacheIdentityUri(imageUri, isVideo, cacheRevision)
    val cacheKey = ThumbnailMemoryCache.key(cacheUri, thumbnailSize)
    val validator: (Bitmap) -> Boolean = { bitmap ->
        !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0
    }
    ThumbnailMemoryCache.get(cacheKey)?.takeIf(validator)?.let { return it }
    return withContext(Dispatchers.IO) {
        ThumbnailDiskCache.get(context, cacheKey)?.also { cached ->
            if (!validator(cached)) ThumbnailDiskCache.remove(context, cacheKey)
        }?.takeIf(validator)?.also {
            it.prepareToDraw()
            ThumbnailMemoryCache.put(cacheKey, it)
        }
    }
}

private fun loadHighQualityBitmap(context: Context, imageUri: Uri, maxSide: Int): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return loadSampledBitmap(context, imageUri, maxSide)
    for (allocator in intArrayOf(ImageDecoder.ALLOCATOR_HARDWARE, ImageDecoder.ALLOCATOR_SOFTWARE)) {
        val decoded = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri)) { decoder, info, _ ->
                decoder.allocator = allocator
                val (width, height) = photoDecodeDimensions(info.size.width, info.size.height, maxSide, photoDecodePixelBudget())
                if (width != info.size.width || height != info.size.height) decoder.setTargetSize(width, height)
            }
        }.getOrNull()
        if (decoded != null) return decoded
    }
    return null
}

private fun loadSampledBitmap(context: Context, imageUri: Uri, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(imageUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
        val pixelBudget = photoDecodePixelBudget()
        while ((bounds.outWidth.toLong() / inSampleSize) * (bounds.outHeight / inSampleSize) > pixelBudget) {
            inSampleSize *= 2
        }
    }
    return context.contentResolver.openInputStream(imageUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
    }
}


private fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sampleSize = 1
    while (max(width / (sampleSize * 2), height / (sampleSize * 2)) >= maxSide) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun loadThumbnail(
    context: Context,
    imageUri: Uri,
    thumbnailSize: Int,
    isVideo: Boolean
): Bitmap? {
    val resolver = context.contentResolver
    val videoSource = isVideo
    val platformThumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            resolver.loadThumbnail(imageUri, Size(thumbnailSize, thumbnailSize), null)
        }.getOrNull()
    } else if (videoSource) {
        @Suppress("DEPRECATION")
        runCatching {
            MediaStore.Video.Thumbnails.getThumbnail(
                resolver,
                ContentUris.parseId(imageUri),
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
        }.getOrNull()
    } else {
        null
    }
    if (platformThumbnail != null) {
        return platformThumbnail
    }
    val fallback = runCatching {
        if (videoSource) {
            loadLegacyVideoThumbnail(context, imageUri, thumbnailSize)
        } else {
            loadSampledBitmap(context, imageUri, thumbnailSize)
        }
    }.getOrNull()
    return fallback
}

private fun loadLegacyVideoThumbnail(context: Context, uri: Uri, thumbnailSize: Int): Bitmap? {
    return VideoFrameDecoder.decode(context, uri, thumbnailSize)
}
