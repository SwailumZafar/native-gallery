@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.data.FakeGalleryRepository
import com.example.nativegallery.data.FavoritesRepository
import com.example.nativegallery.data.GalleryPrivacyFilter
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.data.HiddenMediaRepository
import com.example.nativegallery.data.HiddenSecurityRepository
import com.example.nativegallery.data.MediaPermissions
import com.example.nativegallery.data.RecentlyDeletedRepository
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaAccessNotice
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import com.example.nativegallery.ui.components.bouncyClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class GalleryTab {
    Photos,
    Albums,
    Menu
}

private fun GalleryTab.pageIndex(): Int = when (this) {
    GalleryTab.Photos -> 0
    GalleryTab.Albums -> 1
    GalleryTab.Menu -> 2
}

private fun pageToGalleryTab(page: Int): GalleryTab {
    return when (page) {
        0 -> GalleryTab.Photos
        1 -> GalleryTab.Albums
        else -> GalleryTab.Menu
    }
}

private enum class GalleryDestination {
    Main,
    AlbumDetail,
    HiddenItems,
    LockedMedia,
    RecentlyDeleted
}

private enum class AlbumTransitionMode {
    Opening,
    Closing
}

private data class AlbumTransitionSpec(
    val key: Int,
    val album: Album,
    val tileBounds: Rect,
    val mode: AlbumTransitionMode
)

private const val FavoritesAlbumId = "favorites"

private data class MediaOpenTransitionSpec(
    val key: Int,
    val mediaItem: MediaItem,
    val mediaItems: List<MediaItem>,
    val tileBounds: Rect,
    val sharedElementKey: Any?,
    val sharedElementKeyPrefix: String?
)
private data class MediaCloseTransitionSpec(
    val key: Int,
    val mediaItem: MediaItem,
    val tileBounds: Rect,
    val startOffset: Offset,
    val startScale: Float,
    val startBackdropAlpha: Float
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val GalleryMediaBoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = GalleryMotion.SharedBoundsMillis, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val prefetchScope = rememberCoroutineScope()
    val mediaStoreRepository = remember(context) { MediaStoreGalleryRepository(context) }
    val hiddenRepository = remember(context) { HiddenAlbumsRepository(context) }
    val hiddenMediaRepository = remember(context) { HiddenMediaRepository(context) }
    val favoritesRepository = remember(context) { FavoritesRepository(context) }
    val recentlyDeletedRepository = remember(context) { RecentlyDeletedRepository(context) }
    val hiddenSecurityRepository = remember(context) { HiddenSecurityRepository(context) }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }
    var hiddenMediaIds by remember { mutableStateOf(hiddenMediaRepository.initialHiddenMediaIds()) }
    var pendingLockedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hiddenVaultUnlocked by remember { mutableStateOf(false) }
    var hasHiddenPin by remember { mutableStateOf(hiddenSecurityRepository.hasPin()) }
    var hiddenAuthMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val biometricAvailable = remember(context) { context.supportsBiometricPrompt() }
    var favoriteMediaIds by remember { mutableStateOf(favoritesRepository.initialFavoriteIds()) }
    var recentlyDeletedMedia by remember { mutableStateOf(recentlyDeletedRepository.initialDeletedMedia()) }
    var permanentlyDeletedMediaIds by remember { mutableStateOf(recentlyDeletedRepository.initialPermanentlyDeletedMediaIds()) }
    val albumTileBounds = remember { mutableStateMapOf<String, Rect>() }
    val mediaTileBounds = remember { mutableStateMapOf<String, Rect>() }
    val albumDetailGridModes = remember { mutableStateMapOf<String, AlbumDetailGridMode>() }
    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.Photos) }
    var destination by rememberSaveable { mutableStateOf(GalleryDestination.Main) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }
    var gallerySearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mediaAccess by remember { mutableStateOf(MediaPermissions.currentAccess(context)) }
    var mediaStoreSnapshot by remember { mutableStateOf<GallerySnapshot?>(null) }
    var viewerMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var viewerVisible by remember { mutableStateOf(false) }
    var viewerSharedElementKey by remember { mutableStateOf<Any?>(null) }
    var viewerSharedElementKeyPrefix by remember { mutableStateOf<String?>(null) }
    var viewerReturnFallbackBounds by remember { mutableStateOf(Rect.Zero) }
    var albumTransition by remember { mutableStateOf<AlbumTransitionSpec?>(null) }
    var albumTransitionKey by remember { mutableIntStateOf(0) }
    var albumTransitionProgress by remember { mutableStateOf(1f) }
    var mediaOpenTransition by remember { mutableStateOf<MediaOpenTransitionSpec?>(null) }
    var mediaOpenTransitionKey by remember { mutableIntStateOf(0) }
    var mediaCloseTransition by remember { mutableStateOf<MediaCloseTransitionSpec?>(null) }
    var mediaCloseTransitionKey by remember { mutableIntStateOf(0) }
    val mainPagerState = rememberPagerState(
        initialPage = selectedTab.pageIndex(),
        pageCount = { 3 }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        mediaAccess = MediaPermissions.currentAccess(context)
    }

    LaunchedEffect(mediaAccess) {
        if (mediaAccess.hasAccess) {
            mediaStoreSnapshot = null
            mediaStoreSnapshot = withContext(Dispatchers.IO) {
                mediaStoreRepository.loadGalleryPage(
                    mediaKinds = mediaAccess.mediaKinds,
                    limit = MediaStoreGalleryRepository.InitialGalleryPageSize
                )
            }
            mediaStoreSnapshot = withContext(Dispatchers.IO) {
                mediaStoreRepository.loadGallery(mediaAccess.mediaKinds)
            }
        } else {
            mediaStoreSnapshot = null
        }
    }

    LaunchedEffect(viewerVisible, mediaCloseTransition) {
        if (!viewerVisible && viewerMediaItem != null && mediaCloseTransition == null) {
            delay(GalleryMotion.SharedBoundsMillis + GalleryMotion.ViewerChromeCloseDelayMillis + 90L)
            if (!viewerVisible && mediaCloseTransition == null) {
                viewerMediaItem = null
                viewerMediaItems = emptyList()
                viewerSharedElementKey = null
                viewerSharedElementKeyPrefix = null
            }
        }
    }

    LaunchedEffect(destination) {
        if (destination != GalleryDestination.LockedMedia) {
            hiddenVaultUnlocked = false
            hiddenAuthMessage = null
            if (!hasHiddenPin) {
                pendingLockedMediaIds = emptySet()
            }
        }
    }

    DisposableEffect(destination, hiddenVaultUnlocked, context) {
        val activity = context.findActivity()
        val secureWindow = destination == GalleryDestination.LockedMedia && hiddenVaultUnlocked
        if (secureWindow) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (secureWindow) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    val fakeSnapshot = remember { FakeGalleryRepository.snapshot() }
    val isLoadingMedia = mediaAccess.hasAccess && mediaStoreSnapshot == null
    val activeSnapshot = when {
        isLoadingMedia -> GallerySnapshot(emptyList(), emptyList())
        mediaAccess.hasAccess && !mediaStoreSnapshot?.mediaItems.isNullOrEmpty() -> mediaStoreSnapshot ?: fakeSnapshot
        else -> fakeSnapshot
    }
    val removedMediaIds = recentlyDeletedMedia.keys + permanentlyDeletedMediaIds
    val availableMedia = GalleryPrivacyFilter.availableMedia(
        mediaItems = activeSnapshot.mediaItems,
        removedMediaIds = removedMediaIds
    )
    val hideableAlbums = GalleryPrivacyFilter.hiddenManageableAlbums(
        albums = activeSnapshot.albums,
        mediaItems = availableMedia
    )

    LaunchedEffect(hideableAlbums.map { it.id }) {
        val savedHiddenAlbumIds = hiddenRepository.initialHiddenAlbumIds()
        hideableAlbums.forEach { album ->
            if (!hiddenStates.containsKey(album.id)) {
                hiddenStates[album.id] = savedHiddenAlbumIds.contains(album.id)
            }
        }
    }

    val hiddenAlbumIds = hiddenStates.filterValues { it }.keys.toSet()
    val mediaById = activeSnapshot.mediaItems.associateBy { it.id }
    val recentlyDeletedItems = recentlyDeletedMedia
        .mapNotNull { (mediaId, deletedAtMillis) ->
            mediaById[mediaId]?.let { mediaItem ->
                RecentlyDeletedMedia(mediaItem = mediaItem, deletedAtMillis = deletedAtMillis)
            }
        }
        .sortedByDescending { it.deletedAtMillis }
    val hiddenAlbumMedia = availableMedia.filter { hiddenAlbumIds.contains(it.albumId) }
    val privateHiddenMedia = availableMedia.filter { hiddenMediaIds.contains(it.id) }
    val visibleMedia = GalleryPrivacyFilter.visibleMedia(availableMedia, hiddenAlbumIds, hiddenMediaIds)
    val favoriteMedia = visibleMedia.filter { favoriteMediaIds.contains(it.id) }
    val hiddenAlbumCount = hideableAlbums.count { hiddenAlbumIds.contains(it.id) }
    val hiddenAlbumItemCount = hiddenAlbumMedia.size
    val lockedItemCount = privateHiddenMedia.size
    val baseVisibleAlbums = GalleryPrivacyFilter.visibleAlbums(
        albums = activeSnapshot.albums,
        allMedia = availableMedia,
        visibleMedia = visibleMedia,
        hiddenAlbumIds = hiddenAlbumIds
    )
    val visibleAlbums = albumsWithFavorites(
        albums = baseVisibleAlbums,
        favoriteAlbum = favoriteAlbum(favoriteMedia)
    )
    val albumNameById = visibleAlbums.associate { it.id to it.name }
    val searchedVisibleMedia = searchMedia(visibleMedia, albumNameById, gallerySearchQuery)
    val searchedVisibleAlbums = searchAlbums(visibleAlbums, visibleMedia, gallerySearchQuery)
    val selectedAlbum = visibleAlbums.firstOrNull { it.id == selectedAlbumId }
    val selectedAlbumMedia = selectedAlbum?.let { appMediaForAlbum(it, visibleMedia, favoriteMediaIds) }.orEmpty()
    val selectedMediaItems = visibleMedia.filter { mediaItem ->
        selectedMediaIds.contains(mediaItem.id)
    }

    LaunchedEffect(selectedAlbumId, selectedAlbumMedia.map { it.id }) {
        if (selectedAlbumId != null && selectedAlbumMedia.isNotEmpty()) {
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = selectedAlbumMedia,
                thumbnailSizes = listOf(384, 512),
                maxItems = 120
            )
        }
    }

    LaunchedEffect(visibleMedia.map { it.id }, destination, viewerMediaItem?.id) {
        val visibleIds = visibleMedia.map { it.id }.toSet()
        if (selectedMediaIds.any { it !in visibleIds }) {
            selectedMediaIds = selectedMediaIds.intersect(visibleIds)
        }
        if (destination != GalleryDestination.RecentlyDeleted) {
            if (viewerVisible && viewerMediaItem?.id !in visibleIds) {
                viewerVisible = false
            }
            if (viewerMediaItems.any { it.id !in visibleIds }) {
                viewerMediaItems = viewerMediaItems.filter { it.id in visibleIds }
            }
        }
    }

    fun openPhotos() {
        selectedMediaIds = emptySet()
        selectedTab = GalleryTab.Photos
        destination = GalleryDestination.Main
    }

    fun openAlbums() {
        selectedMediaIds = emptySet()
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.Main
    }

    fun openRecentlyDeleted() {
        selectedMediaIds = emptySet()
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.RecentlyDeleted
    }

    fun openHiddenItems() {
        selectedMediaIds = emptySet()
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.HiddenItems
    }

    fun openLockedMedia() {
        selectedMediaIds = emptySet()
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.LockedMedia
        hiddenAuthMessage = null
    }
    fun createHiddenPin(pin: String, confirmPin: String) {
        when {
            pin != confirmPin -> hiddenAuthMessage = "PINs do not match."
            !HiddenSecurityRepository.isValidPin(pin) -> hiddenAuthMessage = "Use 4 to 12 digits for the PIN."
            hiddenSecurityRepository.setPin(pin) -> {
                hasHiddenPin = true
                if (pendingLockedMediaIds.isNotEmpty()) {
                    hiddenMediaIds = hiddenMediaRepository.setMediaHidden(pendingLockedMediaIds, true)
                    pendingLockedMediaIds = emptySet()
                }
                hiddenSecurityRepository.clearFailedPinAttempts()
                hiddenVaultUnlocked = true
                hiddenAuthMessage = null
            }
            else -> hiddenAuthMessage = "Could not save this PIN."
        }
    }

    fun unlockHiddenWithPin(pin: String) {
        val remainingLockoutMillis = hiddenSecurityRepository.pinLockoutUntilMillis() - System.currentTimeMillis()
        if (remainingLockoutMillis > 0L) {
            hiddenAuthMessage = pinLockoutMessage(remainingLockoutMillis)
            return
        }

        if (hiddenSecurityRepository.verifyPin(pin)) {
            hiddenSecurityRepository.clearFailedPinAttempts()
            hiddenVaultUnlocked = true
            hiddenAuthMessage = null
        } else {
            val failureState = hiddenSecurityRepository.recordFailedPinAttempt()
            hiddenAuthMessage = if (failureState.remainingMillis() > 0L) {
                pinLockoutMessage(failureState.remainingMillis())
            } else {
                "Incorrect PIN."
            }
        }
    }

    fun requestHiddenBiometricUnlock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            hiddenAuthMessage = "Biometric unlock is not available on this Android version."
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            hiddenAuthMessage = "Biometric unlock needs an active screen."
            return
        }
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isDeviceSecure == false) {
            hiddenAuthMessage = "Set up screen lock or biometrics in Android settings first."
            return
        }
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Unlock locked media")
            .setSubtitle("Use face, fingerprint, or another available biometric")
            .setNegativeButton("Use PIN", activity.mainExecutor) { _, _ ->
                hiddenAuthMessage = null
            }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    hiddenSecurityRepository.clearFailedPinAttempts()
                    hiddenVaultUnlocked = true
                    hiddenAuthMessage = null
                }

                override fun onAuthenticationFailed() {
                    hiddenAuthMessage = "Biometric was not recognized."
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    hiddenAuthMessage = errString?.toString()?.takeIf { it.isNotBlank() }
                }
            }
        )
    }

    fun hideSelectedMedia() {
        val mediaIds = selectedMediaItems.map { it.id }.toSet()
        if (mediaIds.isEmpty()) return
        selectedMediaIds = emptySet()
        if (!hasHiddenPin) {
            pendingLockedMediaIds = pendingLockedMediaIds + mediaIds
            hiddenVaultUnlocked = false
            hiddenAuthMessage = "Set a PIN to lock %1$,d selected items.".format(mediaIds.size)
            selectedTab = GalleryTab.Albums
            destination = GalleryDestination.LockedMedia
            return
        }
        hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaIds, true)
    }
    fun unhideMedia(mediaItem: MediaItem) {
        hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaItem.id, false)
    }
    fun updateAlbumHidden(album: Album, hidden: Boolean) {
        if (album.isAllPhotos) return
        hiddenStates[album.id] = hidden
        hiddenRepository.setAlbumHidden(album.id, hidden)

        if (hidden) {
            val hiddenMediaIds = availableMedia
                .filter { it.albumId == album.id }
                .map { it.id }
                .toSet()
            selectedMediaIds = selectedMediaIds - hiddenMediaIds
            viewerMediaItems = viewerMediaItems.filterNot { it.albumId == album.id }
            if (destination != GalleryDestination.RecentlyDeleted && viewerMediaItem?.albumId == album.id) {
                viewerVisible = false
            }
            if (selectedAlbumId == album.id) {
                selectedAlbumId = null
            }
        }
    }

    fun hideAlbumAndReturn(album: Album) {
        updateAlbumHidden(album, true)
        openAlbums()
    }

    fun startAlbumOpen(album: Album, bounds: Rect) {
        val tileBounds = bounds.takeIf { it != Rect.Zero } ?: albumTileBounds[album.id]
        selectedAlbumId = album.id
        selectedTab = GalleryTab.Albums
        selectedMediaIds = emptySet()
        destination = GalleryDestination.AlbumDetail
        val openingAlbumMedia = appMediaForAlbum(album, visibleMedia, favoriteMediaIds)
        prefetchScope.launch {
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = openingAlbumMedia,
                thumbnailSizes = listOf(384, 512),
                maxItems = 120
            )
        }
        if (tileBounds != null && tileBounds != Rect.Zero) {
            albumTransitionProgress = 0f
            albumTileBounds[album.id] = tileBounds
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = album,
                tileBounds = tileBounds,
                mode = AlbumTransitionMode.Opening
            )
            return
        }
        albumTransitionProgress = 1f
    }
    fun closeAlbumDetail() {
        val closingAlbum = selectedAlbum
        val closingBounds = selectedAlbumId?.let { albumTileBounds[it] }
        selectedMediaIds = emptySet()
        if (closingAlbum != null && closingBounds != null && closingBounds != Rect.Zero) {
            albumTransitionProgress = 0f
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = closingAlbum,
                tileBounds = closingBounds,
                mode = AlbumTransitionMode.Closing
            )
        }
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.Main
    }

    LaunchedEffect(destination, selectedAlbumId, visibleAlbums.map { it.id }) {
        if (destination == GalleryDestination.AlbumDetail && selectedAlbumId != null && selectedAlbum == null) {
            selectedAlbumId = null
            openAlbums()
        }
    }
    LaunchedEffect(selectedTab, destination) {
        if (destination == GalleryDestination.Main) {
            val targetPage = selectedTab.pageIndex()
            if (mainPagerState.currentPage != targetPage) {
                mainPagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
    }

    LaunchedEffect(mainPagerState) {
        snapshotFlow { mainPagerState.settledPage }.collect { page ->
            val settledTab = pageToGalleryTab(page)
            if (selectedTab != settledTab) {
                selectedTab = settledTab
            }
        }
    }

    fun finishViewerOpen(
        mediaItem: MediaItem,
        mediaItems: List<MediaItem>,
        sharedElementKey: Any? = null,
        sharedElementKeyPrefix: String? = null
    ) {
        viewerMediaItems = mediaItems
        viewerMediaItem = mediaItem
        viewerSharedElementKey = sharedElementKey
        viewerSharedElementKeyPrefix = sharedElementKeyPrefix
        viewerVisible = true
    }

    fun startViewerOpen(
        mediaItem: MediaItem,
        mediaItems: List<MediaItem>,
        bounds: Rect,
        sharedElementKey: Any? = null,
        sharedElementKeyPrefix: String? = null
    ) {
        mediaOpenTransitionKey += 1
        val openKey = mediaOpenTransitionKey
        if (bounds != Rect.Zero) {
            viewerReturnFallbackBounds = bounds
        }

        fun openAfterBitmapReady() {
            prefetchScope.launch {
                if (mediaItem.contentUri != null) {
                    prefetchMediaThumbnails(
                        context = context.applicationContext,
                        mediaItems = listOf(mediaItem),
                        thumbnailSizes = listOf(1440, 512),
                        maxItems = 1
                    )
                }
                if (mediaOpenTransitionKey != openKey) return@launch

                if (bounds != Rect.Zero) {
                    mediaTileBounds[mediaItem.id] = bounds
                    mediaOpenTransition = MediaOpenTransitionSpec(
                        key = openKey,
                        mediaItem = mediaItem,
                        mediaItems = mediaItems,
                        tileBounds = bounds,
                        sharedElementKey = sharedElementKey,
                        sharedElementKeyPrefix = sharedElementKeyPrefix
                    )
                } else {
                    finishViewerOpen(
                        mediaItem = mediaItem,
                        mediaItems = mediaItems,
                        sharedElementKey = sharedElementKey,
                        sharedElementKeyPrefix = sharedElementKeyPrefix
                    )
                }
            }
        }

        openAfterBitmapReady()
    }
    fun clearViewerAfterClose() {
        mediaCloseTransition = null
        viewerMediaItem = null
        viewerMediaItems = emptyList()
        viewerSharedElementKey = null
        viewerSharedElementKeyPrefix = null
        viewerReturnFallbackBounds = Rect.Zero
        viewerVisible = false
    }

    fun closeViewer(startOffset: Offset = Offset.Zero, startScale: Float = 1f, startBackdropAlpha: Float = 1f) {
        if (mediaCloseTransition != null) return
        val currentItem = viewerMediaItem
        val keyPrefix = viewerSharedElementKeyPrefix
        if (currentItem != null && keyPrefix != null) {
            viewerSharedElementKey = "$keyPrefix-media-${currentItem.id}"
        }
        viewerVisible = false

        val directTargetBounds = currentItem?.let { mediaTileBounds[it.id] } ?: Rect.Zero
        val targetBounds = directTargetBounds.takeIf { it != Rect.Zero } ?: viewerReturnFallbackBounds
        if (currentItem != null && targetBounds != Rect.Zero) {
            mediaCloseTransitionKey += 1
            mediaCloseTransition = MediaCloseTransitionSpec(
                key = mediaCloseTransitionKey,
                mediaItem = currentItem,
                tileBounds = targetBounds,
                startOffset = startOffset,
                startScale = startScale.coerceIn(0.68f, 1f),
                startBackdropAlpha = startBackdropAlpha.coerceIn(0f, 1f)
            )
        }
    }

    fun advanceViewerAfterRemoval(mediaItem: MediaItem, direction: Int) {
        val currentItems = if (viewerMediaItems.isNotEmpty()) viewerMediaItems else visibleMedia
        val currentIndex = currentItems.indexOfFirst { it.id == mediaItem.id }
        val remainingItems = currentItems.filterNot { it.id == mediaItem.id }
        viewerMediaItems = remainingItems
        viewerMediaItem = nextMediaAfterDelete(
            remainingItems = remainingItems,
            deletedIndex = currentIndex,
            direction = direction
        )
        if (remainingItems.isEmpty()) {
            viewerVisible = false
        }
    }
    fun hideMedia(mediaItem: MediaItem, direction: Int) {
        selectedMediaIds = selectedMediaIds - mediaItem.id
        if (!hasHiddenPin) {
            pendingLockedMediaIds = pendingLockedMediaIds + mediaItem.id
            hiddenVaultUnlocked = false
            hiddenAuthMessage = "Set a PIN to lock this item."
            viewerVisible = false
            selectedTab = GalleryTab.Albums
            destination = GalleryDestination.LockedMedia
            return
        }
        hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaItem.id, true)
        advanceViewerAfterRemoval(mediaItem, direction)
    }
    fun completeMediaDelete(mediaItem: MediaItem, direction: Int) {
        recentlyDeletedMedia = recentlyDeletedRepository.markDeleted(mediaItem.id)
        selectedMediaIds = selectedMediaIds - mediaItem.id
        advanceViewerAfterRemoval(mediaItem, direction)
    }

    fun permanentlyDeleteMedia(mediaItem: MediaItem, direction: Int) {
        val deleteState = recentlyDeletedRepository.deleteForever(mediaItem.id)
        recentlyDeletedMedia = deleteState.deletedMedia
        permanentlyDeletedMediaIds = deleteState.permanentlyDeletedMediaIds
        favoriteMediaIds = favoritesRepository.removeFavorites(setOf(mediaItem.id))
        selectedMediaIds = selectedMediaIds - mediaItem.id
        advanceViewerAfterRemoval(mediaItem, direction)
    }

    fun requestMediaDelete(mediaItem: MediaItem, direction: Int) {
        if (destination == GalleryDestination.RecentlyDeleted && recentlyDeletedMedia.containsKey(mediaItem.id)) {
            permanentlyDeleteMedia(mediaItem, direction)
        } else {
            completeMediaDelete(mediaItem, direction)
        }
    }

    fun setMediaFavorite(mediaItem: MediaItem, favorite: Boolean) {
        favoriteMediaIds = favoritesRepository.setFavorite(mediaItem.id, favorite)
    }
    fun toggleMediaSelection(mediaItem: MediaItem) {
        selectedMediaIds = if (selectedMediaIds.contains(mediaItem.id)) {
            selectedMediaIds - mediaItem.id
        } else {
            selectedMediaIds + mediaItem.id
        }
    }

    fun selectMedia(mediaItems: List<MediaItem>) {
        selectedMediaIds = selectedMediaIds + mediaItems.map { it.id }
    }

    fun clearMediaSelection() {
        selectedMediaIds = emptySet()
    }

    fun deleteSelectedMedia() {
        val mediaIds = selectedMediaItems.map { it.id }
        if (mediaIds.isEmpty()) return
        recentlyDeletedMedia = recentlyDeletedRepository.markDeleted(mediaIds)
        selectedMediaIds = emptySet()
    }

    fun shareSelectedMedia() {
        val shareUris = selectedMediaItems.mapNotNull { it.contentUri }
        if (shareUris.isEmpty()) return

        val shareIntent = if (shareUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = selectedMediaItems.firstOrNull()?.let { if (it.isVideo) "video/*" else "image/*" } ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, shareUris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (selectedMediaItems.any { it.isVideo }) "*/*" else "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
            }
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    fun restoreDeletedMedia(entry: RecentlyDeletedMedia) {
        recentlyDeletedMedia = recentlyDeletedRepository.restore(entry.mediaItem.id)
    }

    fun restoreAllDeletedMedia() {
        recentlyDeletedMedia = recentlyDeletedRepository.restoreAll()
    }

    fun deleteDeletedMedia(entry: RecentlyDeletedMedia) {
        val deleteState = recentlyDeletedRepository.deleteForever(entry.mediaItem.id)
        recentlyDeletedMedia = deleteState.deletedMedia
        permanentlyDeletedMediaIds = deleteState.permanentlyDeletedMediaIds
        favoriteMediaIds = favoritesRepository.removeFavorites(setOf(entry.mediaItem.id))
    }

    fun deleteAllDeletedMedia() {
        val deletedIds = recentlyDeletedMedia.keys
        val deleteState = recentlyDeletedRepository.deleteAllForever()
        recentlyDeletedMedia = deleteState.deletedMedia
        permanentlyDeletedMediaIds = deleteState.permanentlyDeletedMediaIds
        favoriteMediaIds = favoritesRepository.removeFavorites(deletedIds)
        if (destination == GalleryDestination.RecentlyDeleted && viewerVisible) {
            viewerVisible = false
        }
    }

    BackHandler(
        enabled = viewerVisible && viewerMediaItem != null,
        onBack = { closeViewer() }
    )
    BackHandler(
        enabled = !viewerVisible && selectedMediaIds.isNotEmpty(),
        onBack = ::clearMediaSelection
    )
    BackHandler(
        enabled = !viewerVisible && destination == GalleryDestination.Main && selectedTab != GalleryTab.Photos,
        onBack = ::openPhotos
    )
    BackHandler(
        enabled = !viewerVisible && destination == GalleryDestination.AlbumDetail,
        onBack = ::closeAlbumDetail
    )
    BackHandler(
        enabled = !viewerVisible && destination == GalleryDestination.HiddenItems,
        onBack = ::openAlbums
    )
    BackHandler(
        enabled = !viewerVisible && destination == GalleryDestination.LockedMedia,
        onBack = ::openAlbums
    )
    BackHandler(
        enabled = !viewerVisible && destination == GalleryDestination.RecentlyDeleted,
        onBack = ::openAlbums
    )
    val mediaAccessNotice: (@Composable () -> Unit)? = when {
        !mediaAccess.hasAccess -> {
            {
                MediaAccessNotice(
                    message = "Allow photo access to show your device gallery.",
                    actionLabel = "Allow",
                    onRequestAccess = {
                        permissionLauncher.launch(MediaPermissions.requestPermissions())
                    }
                )
            }
        }
        mediaAccess.isLimited -> {
            {
                MediaAccessNotice(
                    message = "Showing selected photos only. Allow all photos to include your full gallery.",
                    actionLabel = "Allow all",
                    onRequestAccess = {
                        permissionLauncher.launch(MediaPermissions.requestPermissions())
                    }
                )
            }
        }
        else -> null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }

        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val sharedTransitionScope = this

            AnimatedVisibility(
                visible = true,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                label = "gallery content visibility"
            ) {
                val galleryVisibilityScope = this

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (destination == GalleryDestination.Main && !viewerVisible) {
                            GalleryBottomBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    selectedMediaIds = emptySet()
                                    selectedTab = tab
                                    destination = GalleryDestination.Main
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (destination) {
                            GalleryDestination.Main -> {
                                val tabFlingBehavior = PagerDefaults.flingBehavior(
                                    state = mainPagerState,
                                    snapAnimationSpec = spring(
                                        dampingRatio = 0.78f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                HorizontalPager(
                                    state = mainPagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1,
                                    flingBehavior = tabFlingBehavior,
                                    key = { page -> pageToGalleryTab(page).name }
                                ) { page ->
                                    val tab = pageToGalleryTab(page)
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        when (tab) {
                                            GalleryTab.Photos -> PhotosScreen(
                                                mediaItems = searchedVisibleMedia,
                                                contentPadding = innerPadding,
                                                mediaAccessNotice = mediaAccessNotice,
                                                isLoading = isLoadingMedia,
                                                searchQuery = gallerySearchQuery,
                                                onSearchQueryChange = { gallerySearchQuery = it },
                                                selectedMediaIds = selectedMediaIds,
                                                onMediaLongClick = ::toggleMediaSelection,
                                                onMediaSelectionToggle = ::toggleMediaSelection,
                                                onSelectionClear = ::clearMediaSelection,
                                                onSelectAllVisible = { selectMedia(searchedVisibleMedia) },
                                                onDeleteSelected = ::deleteSelectedMedia,
                                                onShareSelected = ::shareSelectedMedia,
                                                onHideSelected = ::hideSelectedMedia,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = galleryVisibilityScope,
                                                sharedBoundsTransform = GalleryMediaBoundsTransform,
                                                activeSharedElementKey = viewerSharedElementKey ?: mediaOpenTransition?.sharedElementKey,
                                                onMediaClick = { mediaItem, bounds, sharedElementKey, sharedElementKeyPrefix ->
                                                    startViewerOpen(
                                                        mediaItem = mediaItem,
                                                        mediaItems = searchedVisibleMedia,
                                                        bounds = bounds,
                                                        sharedElementKey = sharedElementKey,
                                                        sharedElementKeyPrefix = sharedElementKeyPrefix
                                                    )
                                                }
                                            )
                                            GalleryTab.Albums -> AlbumsScreen(
                                                albums = searchedVisibleAlbums,
                                                layoutMode = albumLayoutMode,
                                                onLayoutModeChange = { albumLayoutMode = it },
                                                onOpenHiddenItems = ::openHiddenItems,
                                                onOpenLockedMedia = ::openLockedMedia,
                                                onOpenRecentlyDeleted = ::openRecentlyDeleted,
                                                hiddenAlbumCount = hiddenAlbumCount,
                                                hiddenItemCount = hiddenAlbumItemCount,
                                                lockedItemCount = lockedItemCount,
                                                onAlbumClick = { album, bounds -> startAlbumOpen(album, bounds) },
                                                onAlbumBoundsChanged = { album, bounds ->
                                                    if (bounds != Rect.Zero) {
                                                        albumTileBounds[album.id] = bounds
                                                    }
                                                },
                                                contentPadding = innerPadding,
                                                activeTransitionAlbumId = albumTransition?.album?.id,
                                                mediaAccessNotice = mediaAccessNotice,
                                                isLoading = isLoadingMedia,
                                                searchQuery = gallerySearchQuery,
                                                onSearchQueryChange = { gallerySearchQuery = it }
                                            )
                                            GalleryTab.Menu -> GalleryMenuScreen(
                                                contentPadding = innerPadding,
                                                onOpenHiddenItems = ::openHiddenItems,
                                                onOpenLockedMedia = ::openLockedMedia,
                                                onOpenRecentlyDeleted = ::openRecentlyDeleted
                                            )
                                        }
                                    }
                                }
                            }
                            GalleryDestination.AlbumDetail -> {
                                if (selectedAlbum != null) {
                                    AlbumDetailScreen(
                                        album = selectedAlbum,
                                        mediaItems = selectedAlbumMedia,
                                        contentPadding = innerPadding,
                                        onBack = ::closeAlbumDetail,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = galleryVisibilityScope,
                                        sharedBoundsTransform = GalleryMediaBoundsTransform,
                                        activeSharedElementKey = viewerSharedElementKey ?: mediaOpenTransition?.sharedElementKey,
                                        albumEnterProgress = if (albumTransition?.mode == AlbumTransitionMode.Opening && albumTransition?.album?.id == selectedAlbum.id) albumTransitionProgress else 1f,
                                        gridMode = albumDetailGridModes[selectedAlbum.id] ?: AlbumDetailGridMode.Compact,
                                        onGridModeChange = { gridMode -> albumDetailGridModes[selectedAlbum.id] = gridMode },
                                        selectedMediaIds = selectedMediaIds,
                                        onMediaLongClick = ::toggleMediaSelection,
                                        onMediaSelectionToggle = ::toggleMediaSelection,
                                        onSelectionClear = ::clearMediaSelection,
                                        onSelectAllVisible = { selectMedia(selectedAlbumMedia) },
                                        onDeleteSelected = ::deleteSelectedMedia,
                                        onHideSelected = ::hideSelectedMedia,
                                        onHideAlbum = { hideAlbumAndReturn(selectedAlbum) },
                                        onMediaClick = { mediaItem, bounds, sharedElementKey, sharedElementKeyPrefix ->
                                            startViewerOpen(
                                                mediaItem = mediaItem,
                                                mediaItems = selectedAlbumMedia,
                                                bounds = bounds,
                                                sharedElementKey = sharedElementKey,
                                                sharedElementKeyPrefix = sharedElementKeyPrefix
                                            )
                                        }
                                    )
                                }
                            }
                            GalleryDestination.HiddenItems -> HiddenItemsScreen(
                                albums = hideableAlbums,
                                hiddenStates = hiddenStates,
                                hiddenAlbumCount = hiddenAlbumCount,
                                hiddenItemCount = hiddenAlbumItemCount,
                                onBack = ::openAlbums,
                                onHiddenChange = { album, hidden ->
                                    updateAlbumHidden(album, hidden)
                                },
                                contentPadding = PaddingValues()
                            )
                            GalleryDestination.LockedMedia -> LockedMediaScreen(
                                lockedMediaItems = privateHiddenMedia,
                                isUnlocked = hiddenVaultUnlocked,
                                hasPin = hasHiddenPin,
                                biometricAvailable = biometricAvailable,
                                authMessage = hiddenAuthMessage,
                                onBack = ::openAlbums,
                                onPinCreated = ::createHiddenPin,
                                onPinUnlock = ::unlockHiddenWithPin,
                                onBiometricUnlock = ::requestHiddenBiometricUnlock,
                                onUnhideMedia = ::unhideMedia,
                                contentPadding = PaddingValues()
                            )
                            GalleryDestination.RecentlyDeleted -> RecentlyDeletedScreen(
                                deletedItems = recentlyDeletedItems,
                                onBack = ::openAlbums,
                                onOpenMedia = { entry ->
                                    startViewerOpen(
                                        mediaItem = entry.mediaItem,
                                        mediaItems = recentlyDeletedItems.map { it.mediaItem },
                                        bounds = mediaTileBounds[entry.mediaItem.id] ?: Rect.Zero
                                    )
                                },
                                onRestore = ::restoreDeletedMedia,
                                onRestoreAll = ::restoreAllDeletedMedia,
                                onDeleteForever = ::deleteDeletedMedia,
                                onDeleteAllForever = ::deleteAllDeletedMedia,
                                contentPadding = PaddingValues()
                            )
                        }
                    }
                }

                AlbumTouchOriginTransitionOverlay(
                    transition = albumTransition,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    onProgressChanged = { runningTransition, progressValue ->
                        if (albumTransition?.key == runningTransition.key) {
                            albumTransitionProgress = progressValue
                        }
                    },
                    onFinished = { finishedTransition ->
                        if (albumTransition?.key == finishedTransition.key) {
                            albumTransitionProgress = if (finishedTransition.mode == AlbumTransitionMode.Opening) 1f else 0f
                            albumTransition = null
                        }
                    }
                )
                MediaOpenTransitionOverlay(
                    transition = mediaOpenTransition,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    onFinished = { finishedTransition ->
                        if (mediaOpenTransition?.key == finishedTransition.key) {
                            finishViewerOpen(
                                mediaItem = finishedTransition.mediaItem,
                                mediaItems = finishedTransition.mediaItems,
                                sharedElementKey = finishedTransition.sharedElementKey,
                                sharedElementKeyPrefix = finishedTransition.sharedElementKeyPrefix
                            )
                            mediaOpenTransition = null
                        }
                    }
                )
                MediaCloseTransitionOverlay(
                    transition = mediaCloseTransition,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    onFinished = { finishedTransition ->
                        if (mediaCloseTransition?.key == finishedTransition.key) {
                            clearViewerAfterClose()
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = viewerVisible && viewerMediaItem != null,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                label = "viewer visibility"
            ) {
                PhotoViewerOverlay(
                    mediaItems = viewerMediaItems,
                    mediaItem = viewerMediaItem,
                    visible = true,
                    onClose = ::closeViewer,
                    onDelete = ::requestMediaDelete,
                    onHide = ::hideMedia,
                    favoriteMediaIds = favoriteMediaIds,
                    onFavoriteChange = ::setMediaFavorite,
                    onCurrentMediaChanged = { currentItem ->
                        viewerMediaItem = currentItem
                        viewerSharedElementKey = viewerSharedElementKeyPrefix?.let { "$it-media-${currentItem.id}" }
                    },
                    albumNameForMedia = { item -> albumNameById[item.albumId] },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    sharedBoundsTransform = GalleryMediaBoundsTransform,
                    sharedElementKeyPrefix = viewerSharedElementKeyPrefix
                )
            }
        }
    }
}

@Composable
private fun GalleryMenuScreen(
    contentPadding: PaddingValues,
    onOpenHiddenItems: () -> Unit,
    onOpenLockedMedia: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 26.dp,
                top = 104.dp,
                end = 26.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            )
    ) {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Gallery settings and tools",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                GalleryMenuRow(
                    icon = Icons.Filled.Security,
                    label = "Hidden albums",
                    description = "Choose albums hidden from the gallery",
                    onClick = onOpenHiddenItems,
                    showDivider = true
                )
                GalleryMenuRow(
                    icon = Icons.Filled.Lock,
                    label = "Locked media",
                    description = "PIN, face, or fingerprint protected items",
                    onClick = onOpenLockedMedia,
                    showDivider = true
                )
                GalleryMenuRow(
                    icon = Icons.Filled.Delete,
                    label = "Recently deleted",
                    description = "Photos removed in the last 30 days",
                    onClick = onOpenRecentlyDeleted,
                    showDivider = true
                )
                GalleryMenuRow(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    description = "Theme, storage and permissions",
                    onClick = {},
                    showDivider = false
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = "NativeGallery",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "v0.1.0",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GalleryMenuRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(11.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                modifier = Modifier.size(22.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp, end = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
            )
        }
    }
}
private fun pinLockoutMessage(remainingMillis: Long): String {
    val seconds = ((remainingMillis.coerceAtLeast(1L) + 999L) / 1000L).coerceAtLeast(1L)
    return "Too many wrong PIN attempts. Try again in ${seconds}s."
}
private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.supportsBiometricPrompt(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    val keyguardManager = getSystemService(KeyguardManager::class.java)
    return keyguardManager?.isDeviceSecure != false
}
private fun visibleMedia(
    mediaItems: List<MediaItem>,
    hiddenAlbumIds: Set<String>,
    hiddenMediaIds: Set<String>
): List<MediaItem> {
    return mediaItems.filterNot { hiddenAlbumIds.contains(it.albumId) || hiddenMediaIds.contains(it.id) }
}

private fun hiddenManageableAlbums(
    albums: List<Album>,
    mediaItems: List<MediaItem>
): List<Album> {
    val mediaByAlbum = mediaItems.groupBy { it.albumId }
    return albums
        .filterNot { it.isAllPhotos }
        .map { album ->
            val albumMedia = mediaByAlbum[album.id].orEmpty()
            if (albumMedia.isEmpty()) {
                album
            } else {
                val cover = albumMedia.first()
                album.copy(
                    itemCount = albumMedia.size,
                    coverMediaIds = albumMedia.take(4).map { it.id },
                    coverRes = cover.imageRes ?: album.coverRes,
                    coverUri = cover.contentUri ?: album.coverUri
                )
            }
        }
}

private fun visibleAlbums(
    albums: List<Album>,
    allMedia: List<MediaItem>,
    visibleMedia: List<MediaItem>,
    hiddenAlbumIds: Set<String>
): List<Album> {
    val allAlbumIds = allMedia.map { it.albumId }.toSet()
    val mediaByAlbum = visibleMedia.groupBy { it.albumId }
    return albums.mapNotNull { album ->
        when {
            album.isAllPhotos -> {
                val cover = visibleMedia.firstOrNull()
                album.copy(
                    itemCount = visibleMedia.size,
                    coverMediaIds = visibleMedia.take(4).map { it.id },
                    coverRes = cover?.imageRes ?: album.coverRes,
                    coverUri = cover?.contentUri ?: album.coverUri
                )
            }
            hiddenAlbumIds.contains(album.id) -> null
            allAlbumIds.contains(album.id) -> {
                val albumMedia = mediaByAlbum[album.id].orEmpty()
                if (albumMedia.isEmpty()) {
                    null
                } else {
                    val cover = albumMedia.first()
                    album.copy(
                        itemCount = albumMedia.size,
                        coverMediaIds = albumMedia.take(4).map { it.id },
                        coverRes = cover.imageRes ?: album.coverRes,
                        coverUri = cover.contentUri ?: album.coverUri
                    )
                }
            }
            else -> album
        }
    }
}

private fun favoriteAlbum(favoriteMedia: List<MediaItem>): Album? {
    if (favoriteMedia.isEmpty()) return null
    val cover = favoriteMedia.first()
    return Album(
        id = FavoritesAlbumId,
        name = "Favorites",
        itemCount = favoriteMedia.size,
        coverMediaIds = favoriteMedia.take(4).map { it.id },
        coverRes = cover.imageRes,
        coverUri = cover.contentUri,
        hasVideoBadge = cover.isVideo
    )
}

private fun albumsWithFavorites(
    albums: List<Album>,
    favoriteAlbum: Album?
): List<Album> {
    val albumsWithoutFavorites = albums.filterNot { it.id == FavoritesAlbumId }
    if (favoriteAlbum == null) return albumsWithoutFavorites

    val allPhotosIndex = albumsWithoutFavorites.indexOfFirst { it.isAllPhotos }
    if (allPhotosIndex < 0) {
        return listOf(favoriteAlbum) + albumsWithoutFavorites
    }

    return buildList {
        addAll(albumsWithoutFavorites.take(allPhotosIndex + 1))
        add(favoriteAlbum)
        addAll(albumsWithoutFavorites.drop(allPhotosIndex + 1))
    }
}

private fun appMediaForAlbum(
    album: Album,
    mediaItems: List<MediaItem>,
    favoriteMediaIds: Set<String>
): List<MediaItem> {
    return if (album.id == FavoritesAlbumId) {
        mediaItems.filter { favoriteMediaIds.contains(it.id) }
    } else {
        GalleryPrivacyFilter.mediaForAlbum(album, mediaItems)
    }
}
private fun searchMedia(
    mediaItems: List<MediaItem>,
    albumNameById: Map<String, String>,
    query: String
): List<MediaItem> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return mediaItems
    return mediaItems.filter { mediaItem ->
        mediaMatchesQuery(mediaItem, albumNameById[mediaItem.albumId], normalizedQuery)
    }
}

private fun searchAlbums(
    albums: List<Album>,
    mediaItems: List<MediaItem>,
    query: String
): List<Album> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return albums
    val mediaByAlbum = mediaItems.groupBy { it.albumId }
    return albums.filter { album ->
        album.name.lowercase().contains(normalizedQuery) ||
            if (album.isAllPhotos) {
                mediaItems.any { mediaMatchesQuery(it, album.name, normalizedQuery) }
            } else {
                mediaByAlbum[album.id].orEmpty().any { mediaMatchesQuery(it, album.name, normalizedQuery) }
            }
    }
}

private fun mediaMatchesQuery(
    mediaItem: MediaItem,
    albumName: String?,
    normalizedQuery: String
): Boolean {
    return mediaItem.title.lowercase().contains(normalizedQuery) ||
        mediaItem.dateLabel.lowercase().contains(normalizedQuery) ||
        mediaItem.type.name.lowercase().contains(normalizedQuery) ||
        mediaItem.mimeType?.lowercase()?.contains(normalizedQuery) == true ||
        albumName?.lowercase()?.contains(normalizedQuery) == true
}

private fun mediaForAlbum(
    album: Album,
    mediaItems: List<MediaItem>
): List<MediaItem> {
    return if (album.isAllPhotos) {
        mediaItems
    } else {
        mediaItems.filter { it.albumId == album.id }
    }
}

private fun nextMediaAfterDelete(
    remainingItems: List<MediaItem>,
    deletedIndex: Int,
    direction: Int
): MediaItem? {
    if (remainingItems.isEmpty()) return null
    val preferredIndex = if (direction < 0) deletedIndex - 1 else deletedIndex
    return remainingItems[preferredIndex.coerceIn(0, remainingItems.lastIndex)]
}

@Composable
private fun AlbumTouchOriginTransitionOverlay(
    transition: AlbumTransitionSpec?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onProgressChanged: (AlbumTransitionSpec, Float) -> Unit = { _, _ -> },
    onFinished: (AlbumTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val density = LocalDensity.current
    val progress = remember(transition.key) {
        Animatable(if (transition.mode == AlbumTransitionMode.Closing) 1f else 0f)
    }

    LaunchedEffect(transition.key) {
        val targetValue = if (transition.mode == AlbumTransitionMode.Closing) 0f else 1f
        progress.animateTo(
            targetValue = targetValue,
            animationSpec = spring(
                dampingRatio = GalleryMotion.AlbumHeroOpenDamping,
                stiffness = GalleryMotion.AlbumHeroOpenStiffness
            )
        )
        onFinished(transition)
    }

    val easedProgress = progress.value.coerceIn(0f, 1f)
    SideEffect { onProgressChanged(transition, easedProgress) }
    val startWidth = (transition.tileBounds.right - transition.tileBounds.left).coerceAtLeast(1f)
    val startHeight = (transition.tileBounds.bottom - transition.tileBounds.top).coerceAtLeast(1f)
    val width = lerp(startWidth, rootWidthPx, easedProgress)
    val height = lerp(startHeight, rootHeightPx, easedProgress)
    val translationX = lerp(transition.tileBounds.left, 0f, easedProgress)
    val translationY = lerp(transition.tileBounds.top, 0f, easedProgress)
    val radius = lerp(22f, 0f, easedProgress)
    val motionProgress = if (transition.mode == AlbumTransitionMode.Opening) {
        easedProgress
    } else {
        1f - easedProgress
    }
    val transitionPulse = (if (motionProgress < 0.5f) {
        motionProgress * 2f
    } else {
        (1f - motionProgress) * 2f
    }).coerceIn(0f, 1f)
    val coverFade = ((motionProgress - 0.68f) / 0.32f).coerceIn(0f, 1f)
    val labelFade = ((motionProgress - 0.48f) / 0.34f).coerceIn(0f, 1f)
    val backdropAlpha = transitionPulse * 0.22f
    val surfaceAlpha = ((1f - coverFade) * 0.10f + transitionPulse * 0.04f).coerceIn(0f, 0.14f)
    val coverAlpha = 1f - coverFade
    val labelAlpha = 1f - labelFade

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = backdropAlpha))
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.translationX = translationX
                    this.translationY = translationY
                    clip = true
                    shape = RoundedCornerShape(with(density) { radius.toDp() })
                }
                .background(MaterialTheme.colorScheme.background.copy(alpha = surfaceAlpha))
        ) {
            GalleryImage(
                imageRes = transition.album.coverRes,
                imageUri = transition.album.coverUri,
                contentDescription = transition.album.name,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = coverAlpha },
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = 512,
                loadQuality = ImageLoadQuality.Thumbnail,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = coverAlpha * 0.18f))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = lerp(0.06f, 0f, easedProgress)))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp)
                    .graphicsLayer { alpha = labelAlpha }
            ) {
                Text(
                    text = transition.album.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "%1$,d items".format(transition.album.itemCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun MediaOpenTransitionOverlay(
    transition: MediaOpenTransitionSpec?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onFinished: (MediaOpenTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val density = LocalDensity.current
    val progress = remember(transition.key) { Animatable(0f) }

    LaunchedEffect(transition.key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = GalleryMotion.MediaOpenDamping, stiffness = GalleryMotion.MediaOpenStiffness)
        )
        onFinished(transition)
    }

    val easedProgress = progress.value.coerceIn(0f, 1f)
    val startWidth = (transition.tileBounds.right - transition.tileBounds.left).coerceAtLeast(1f)
    val startHeight = (transition.tileBounds.bottom - transition.tileBounds.top).coerceAtLeast(1f)
    val width = lerp(startWidth, rootWidthPx, easedProgress)
    val height = lerp(startHeight, rootHeightPx, easedProgress)
    val translationX = lerp(transition.tileBounds.left, 0f, easedProgress)
    val translationY = lerp(transition.tileBounds.top, 0f, easedProgress)
    val radius = lerp(10f, 0f, easedProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = easedProgress))
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.translationX = translationX
                    this.translationY = translationY
                    clip = true
                    shape = RoundedCornerShape(with(density) { radius.toDp() })
                }
        ) {
            GalleryImage(
                imageRes = transition.mediaItem.imageRes,
                imageUri = transition.mediaItem.contentUri,
                contentDescription = transition.mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Fit,
                thumbnailSize = 1440,
                loadQuality = if (transition.mediaItem.isVideo) ImageLoadQuality.Thumbnail else ImageLoadQuality.HighQuality,
                backgroundColor = if (transition.mediaItem.isVideo) Color.Black else Color.Transparent
            )
        }
    }
}

@Composable
private fun MediaCloseTransitionOverlay(
    transition: MediaCloseTransitionSpec?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onFinished: (MediaCloseTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val density = LocalDensity.current
    val progress = remember(transition.key) { Animatable(0f) }

    LaunchedEffect(transition.key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = GalleryMotion.MediaOpenDamping, stiffness = GalleryMotion.MediaOpenStiffness)
        )
        onFinished(transition)
    }

    val easedProgress = progress.value.coerceIn(0f, 1f)
    val targetWidth = (transition.tileBounds.right - transition.tileBounds.left).coerceAtLeast(1f)
    val targetHeight = (transition.tileBounds.bottom - transition.tileBounds.top).coerceAtLeast(1f)
    val startScale = transition.startScale.coerceIn(0.68f, 1f)
    val startWidth = rootWidthPx * startScale
    val startHeight = rootHeightPx * startScale
    val startLeft = transition.startOffset.x + (rootWidthPx - startWidth) / 2f
    val startTop = transition.startOffset.y + (rootHeightPx - startHeight) / 2f
    val width = lerp(startWidth, targetWidth, easedProgress)
    val height = lerp(startHeight, targetHeight, easedProgress)
    val translationX = lerp(startLeft, transition.tileBounds.left, easedProgress)
    val translationY = lerp(startTop, transition.tileBounds.top, easedProgress)
    val radius = lerp(0f, 10f, easedProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = lerp(transition.startBackdropAlpha, 0f, easedProgress)))
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.translationX = translationX
                    this.translationY = translationY
                    clip = true
                    shape = RoundedCornerShape(with(density) { radius.toDp() })
                }
        ) {
            GalleryImage(
                imageRes = transition.mediaItem.imageRes,
                imageUri = transition.mediaItem.contentUri,
                contentDescription = transition.mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Fit,
                thumbnailSize = 1440,
                loadQuality = if (transition.mediaItem.isVideo) ImageLoadQuality.Thumbnail else ImageLoadQuality.HighQuality,
                backgroundColor = if (transition.mediaItem.isVideo) Color.Black else Color.Transparent
            )
        }
    }
}

@Composable
private fun GalleryBottomBar(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    val containerShape = RoundedCornerShape(50.dp)
    val tabShape = RoundedCornerShape(40.dp)
    val tabWidth = 86.dp
    val tabHeight = 56.dp
    val tabGap = 4.dp
    val contentWidth = tabWidth * 3 + tabGap * 2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(20.dp)
                    .clip(containerShape)
                    .background(Color.White.copy(alpha = 0.52f))
            )
            Surface(
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .clip(containerShape),
                color = Color.White.copy(alpha = 0.92f),
                shape = containerShape,
                tonalElevation = 0.dp,
                shadowElevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .width(contentWidth)
                        .height(tabHeight)
                ) {
                    val density = LocalDensity.current
                    val tabStepPx = with(density) { (tabWidth + tabGap).toPx() }
                    val indicatorProgress by animateFloatAsState(
                        targetValue = selectedTab.pageIndex().toFloat(),
                        animationSpec = spring(dampingRatio = GalleryMotion.BottomNavIndicatorDamping, stiffness = GalleryMotion.BottomNavIndicatorStiffness),
                        label = "bottom nav pill progress"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationX = tabStepPx * indicatorProgress }
                            .width(tabWidth)
                            .height(tabHeight)
                            .clip(tabShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tabGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GalleryNavigationItem(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(tabHeight),
                            selected = selectedTab == GalleryTab.Photos,
                            icon = Icons.Filled.Image,
                            label = "Photos",
                            onClick = { onTabSelected(GalleryTab.Photos) }
                        )
                        GalleryNavigationItem(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(tabHeight),
                            selected = selectedTab == GalleryTab.Albums,
                            icon = Icons.Filled.Collections,
                            label = "Albums",
                            onClick = { onTabSelected(GalleryTab.Albums) }
                        )
                        GalleryNavigationItem(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(tabHeight),
                            selected = selectedTab == GalleryTab.Menu,
                            icon = Icons.Filled.Menu,
                            label = "Menu",
                            onClick = { onTabSelected(GalleryTab.Menu) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryNavigationItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    val color by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "bottom nav item color"
    )

    Column(
        modifier = modifier
            .bouncyClickable(
                pressedScale = GalleryMotion.BottomNavPressedScale,
                pressDampingRatio = GalleryMotion.BottomNavPressDamping,
                pressStiffness = GalleryMotion.BottomNavPressStiffness,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 10.5.sp,
                lineHeight = 13.sp
            ),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
