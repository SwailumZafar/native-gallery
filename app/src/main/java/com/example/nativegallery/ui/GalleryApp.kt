@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@file:Suppress("SuspiciousIndentation")

package com.example.nativegallery.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.ContentObserver
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativegallery.data.DocumentPhotoRepository
import com.example.nativegallery.data.FavoritesRepository
import com.example.nativegallery.data.GalleryPrivacyFilter
import com.example.nativegallery.data.GalleryGridDensity
import com.example.nativegallery.data.GallerySettings
import com.example.nativegallery.data.GalleryThemeMode
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.data.HiddenMediaRepository
import com.example.nativegallery.data.HiddenSecurityRepository
import com.example.nativegallery.data.LockedMediaVaultProvider
import com.example.nativegallery.data.LockedMediaVaultRepository
import com.example.nativegallery.data.LockedMediaVaultSnapshot
import com.example.nativegallery.data.MediaPermissions
import com.example.nativegallery.data.PhotoEditorRepository
import com.example.nativegallery.data.RecentlyDeletedRepository
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.data.MediaStoreWriteRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaAccessNotice
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.ThumbnailMemoryCache
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import com.example.nativegallery.ui.components.bouncyClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt


private data class ViewerSessionSnapshot(
    val mediaItem: MediaItem,
    val mediaItems: List<MediaItem>,
    val sharedElementKey: Any?,
    val sharedElementKeyPrefix: String?,
    val actionMode: ViewerActionMode,
    val returnFallbackBounds: Rect,
    val sourceMediaId: String?,
    val sourceMediaIds: List<String>,
    val sourceBounds: Rect,
    val sourceGridColumns: Int
)

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
    val transitionMediaItem: MediaItem,
    val mediaItems: List<MediaItem>,
    val tileBounds: Rect,
    val sharedElementKey: Any?,
    val sharedElementKeyPrefix: String?,
    val actionMode: ViewerActionMode
)

private enum class MediaStoreWriteMode {
    Trash,
    RestoreFromTrash,
    DeleteForever,
    DeleteLockedOriginals,
    MoveToAlbum
}

private data class PendingMediaStoreWriteAction(
    val mode: MediaStoreWriteMode,
    val mediaItems: List<MediaItem>,
    val viewerDirection: Int = 1,
    val fromViewer: Boolean = false,
    val targetAlbumName: String? = null
)

private data class PendingLockConfirmation(
    val mediaItems: List<MediaItem>,
    val viewerMediaId: String? = null,
    val viewerDirection: Int = 1
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
fun GalleryApp(
    settings: GallerySettings,
    onSettingsChange: (GallerySettings) -> Unit
) {
    val context = LocalContext.current
    val viewerConfiguration = LocalConfiguration.current
    val viewerDensity = LocalDensity.current
    val viewerPhotoDecodeSize = remember(
        viewerConfiguration.screenWidthDp,
        viewerConfiguration.screenHeightDp,
        viewerDensity.density
    ) {
        (max(viewerConfiguration.screenWidthDp, viewerConfiguration.screenHeightDp) * viewerDensity.density)
            .roundToInt()
            .coerceIn(1440, 3072)
    }
    val mediaStoreWriteRepository = remember(context) { MediaStoreWriteRepository(context) }
    val prefetchScope = rememberCoroutineScope()
    val mediaStoreRepository = remember(context) { MediaStoreGalleryRepository(context) }
    val initialMediaAccess = remember(context) { MediaPermissions.currentAccess(context) }
    var initialPermissionPromptHandled by rememberSaveable {
        mutableStateOf(!MediaPermissions.shouldRequestOnLaunch(context))
    }
    val mediaViewModel: GalleryMediaViewModel = viewModel(
        factory = remember(mediaStoreRepository, initialMediaAccess) {
            GalleryMediaViewModelFactory(mediaStoreRepository, initialMediaAccess)
        }
    )
    val mediaUiState by mediaViewModel.uiState.collectAsStateWithLifecycle()
    val navigationViewModel: GalleryNavigationViewModel = viewModel()
    val navigationUiState by navigationViewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab = navigationUiState.selectedTab
    val destination = navigationUiState.destination
    val selectedAlbumId = navigationUiState.selectedAlbumId
    val hiddenRepository = remember(context) { HiddenAlbumsRepository(context) }
    val hiddenMediaRepository = remember(context) { HiddenMediaRepository(context) }
    val favoritesRepository = remember(context) { FavoritesRepository(context) }
    val recentlyDeletedRepository = remember(context) { RecentlyDeletedRepository(context) }
    val hiddenSecurityRepository = remember(context) { HiddenSecurityRepository(context) }
    val lockedSecurityViewModel: LockedMediaSecurityViewModel = viewModel(
        factory = remember(hiddenSecurityRepository) {
            LockedMediaSecurityViewModelFactory(hiddenSecurityRepository)
        }
    )
    val lockedSecurityUiState by lockedSecurityViewModel.uiState.collectAsStateWithLifecycle()
    val hiddenVaultUnlocked = lockedSecurityUiState.isUnlocked
    val hasHiddenPin = lockedSecurityUiState.hasPin
    val hiddenAuthMessage = lockedSecurityUiState.authMessage
    val lockedVaultRepository = remember(context) { LockedMediaVaultRepository(context) }
    val documentPhotoRepository = remember(context) { DocumentPhotoRepository(context) }
    val documentPhotosViewModel: DocumentPhotosViewModel = viewModel(
        factory = remember(documentPhotoRepository) {
            DocumentPhotosViewModelFactory(documentPhotoRepository)
        }
    )
    val documentPhotosUiState by documentPhotosViewModel.uiState.collectAsStateWithLifecycle()
    val photoEditorRepository = remember(context) { PhotoEditorRepository(context) }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }
    var hiddenMediaIds by remember { mutableStateOf(hiddenMediaRepository.initialHiddenMediaIds()) }
    var pendingLockedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var hiddenBiometricCancellation by remember { mutableStateOf<CancellationSignal?>(null) }

    val latestDestination by rememberUpdatedState(destination)


    val biometricAvailable = remember(context) { context.supportsBiometricPrompt() }
    var favoriteMediaIds by remember { mutableStateOf(favoritesRepository.initialFavoriteIds()) }
    var recentlyDeletedMedia by remember { mutableStateOf(recentlyDeletedRepository.initialDeletedMedia()) }
    var permanentlyDeletedMediaIds by remember { mutableStateOf(recentlyDeletedRepository.initialPermanentlyDeletedMediaIds()) }
    val albumTileBounds = remember { mutableMapOf<String, Rect>() }
    val mediaTileBounds = remember { mutableMapOf<String, Rect>() }
    val albumDetailGridModes = remember { mutableStateMapOf<String, AlbumDetailGridMode>() }
    val defaultAlbumGridMode = remember(settings.gridDensity) { settings.gridDensity.defaultAlbumGridMode() }

    var editorViewerSession by remember { mutableStateOf<ViewerSessionSnapshot?>(null) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }
    var gallerySearchQuery by rememberSaveable { mutableStateOf("") }
    var vaultRefreshKey by remember { mutableIntStateOf(0) }
    var pendingMediaStoreWriteAction by remember { mutableStateOf<PendingMediaStoreWriteAction?>(null) }
    var pendingLockConfirmation by remember { mutableStateOf<PendingLockConfirmation?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var albumCreationSelectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val mediaAccess = mediaUiState.mediaAccess
    val mediaStoreSnapshot = mediaUiState.snapshot
    val initialMediaSyncComplete = mediaUiState.initialSyncComplete
    val mediaStoreDeletedItems = mediaUiState.trashedMedia
    var viewerMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var viewerVisible by remember { mutableStateOf(false) }
    var viewerSharedElementKey by remember { mutableStateOf<Any?>(null) }
    var viewerSharedElementKeyPrefix by remember { mutableStateOf<String?>(null) }
    var viewerActionMode by remember { mutableStateOf(ViewerActionMode.Normal) }
    var viewerReturnFallbackBounds by remember { mutableStateOf(Rect.Zero) }
    var viewerSourceMediaId by remember { mutableStateOf<String?>(null) }
    var viewerSourceMediaIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerSourceBounds by remember { mutableStateOf(Rect.Zero) }
    var viewerSourceGridColumns by remember { mutableIntStateOf(4) }
    var albumTransition by remember { mutableStateOf<AlbumTransitionSpec?>(null) }
    var albumTransitionKey by remember { mutableIntStateOf(0) }
    var albumWarmupReadyKey by remember { mutableIntStateOf(-1) }
    var albumTransitionCommittedKey by remember { mutableIntStateOf(-1) }
    var albumTransitionAwaitingDestinationKey by remember { mutableIntStateOf(-1) }
    var mediaOpenTransition by remember { mutableStateOf<MediaOpenTransitionSpec?>(null) }
    var mediaOpenTransitionKey by remember { mutableIntStateOf(0) }
    var mediaOpenWarmupReadyKey by remember { mutableIntStateOf(-1) }
    var mediaCloseTransition by remember { mutableStateOf<MediaCloseTransitionSpec?>(null) }
    var mediaCloseTransitionKey by remember { mutableIntStateOf(0) }
    var viewerCloseInProgress by remember { mutableStateOf(false) }
    var viewerRevealMediaId by remember { mutableStateOf<String?>(null) }
    var transitionRootBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    val mainPagerState = rememberPagerState(
        initialPage = selectedTab.pageIndex(),
        pageCount = { 3 }
    )
    val photosListState = rememberLazyListState()
    val albumsListState = rememberLazyListState()
    val albumDetailListState = rememberLazyListState()
    var bottomNavigationVisible by rememberSaveable { mutableStateOf(true) }
    val predictiveBackProgress = remember { Animatable(0f) }
    var predictiveBackSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var predictiveBackGestureAction by remember { mutableStateOf<GalleryBackAction?>(null) }

    LaunchedEffect(settings.gridDensity) {
        albumDetailGridModes.clear()
    }

    LaunchedEffect(selectedAlbumId) {
        if (selectedAlbumId != null) {
            albumDetailListState.scrollToItem(0)
        }
    }

    LaunchedEffect(destination, selectedTab) {
        if (destination != GalleryDestination.Main || selectedTab == GalleryTab.Menu) {
            bottomNavigationVisible = true
            return@LaunchedEffect
        }
        val listState = if (selectedTab == GalleryTab.Photos) photosListState else albumsListState
        bottomNavigationVisible = true
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                when {
                    index == 0 && offset < 24 -> bottomNavigationVisible = true
                    index > previousIndex -> bottomNavigationVisible = false
                    index < previousIndex -> bottomNavigationVisible = true
                    offset > previousOffset + 8 -> bottomNavigationVisible = false
                    offset < previousOffset - 4 -> bottomNavigationVisible = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        mediaViewModel.updateAccess(MediaPermissions.currentAccess(context))
    }

    LaunchedEffect(initialPermissionPromptHandled, mediaAccess.hasAccess) {
        if (!initialPermissionPromptHandled) {
            initialPermissionPromptHandled = true
            MediaPermissions.markInitialPromptHandled(context)
            if (!mediaAccess.hasAccess) {
                delay(180L)
                permissionLauncher.launch(MediaPermissions.requestPermissions())
            }
        }
    }


    DisposableEffect(mediaAccess.hasAccess, context) {
        if (!mediaAccess.hasAccess) {
            onDispose { }
        } else {
            val resolver = context.applicationContext.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    mediaViewModel.onMediaStoreChanged()
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    mediaViewModel.onMediaStoreChanged()
                }
            }
            resolver.registerContentObserver(collection, true, observer)
            onDispose {
                resolver.unregisterContentObserver(observer)
            }
        }
    }

    DisposableEffect(context) {
        val activity = context.findActivity() as? ComponentActivity
        if (activity == null) {
            onDispose { }
        } else {
            var initialResumeDelivered = false
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (!initialResumeDelivered) {
                        initialResumeDelivered = true
                    } else {
                        val currentAccess = MediaPermissions.currentAccess(context)
                        mediaViewModel.updateAccess(currentAccess)
                        if (currentAccess.hasAccess) {
                            mediaViewModel.onAppResumed()
                        }
                    }
                }
            }
            activity.lifecycle.addObserver(lifecycleObserver)
            onDispose { activity.lifecycle.removeObserver(lifecycleObserver) }
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
        mediaViewModel.setRecentlyDeletedVisible(destination == GalleryDestination.RecentlyDeleted)
        if (destination != GalleryDestination.LockedMedia) {
            lockedSecurityViewModel.lock()
            withContext(Dispatchers.IO) {
                LockedMediaVaultProvider.clearSessionCache(context.applicationContext)
            }
            val cancellation = hiddenBiometricCancellation
            hiddenBiometricCancellation = null
            cancellation?.cancel()
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
    val emptySnapshot = remember { GallerySnapshot(emptyList(), emptyList()) }
    val isLoadingMedia = mediaAccess.hasAccess && (!initialMediaSyncComplete || mediaStoreSnapshot == null)
    val activeSnapshot = when {
        isLoadingMedia -> emptySnapshot
        mediaAccess.hasAccess -> mediaStoreSnapshot ?: emptySnapshot
        else -> emptySnapshot
    }
    val removedMediaIds = remember(recentlyDeletedMedia, permanentlyDeletedMediaIds) {
        recentlyDeletedMedia.keys + permanentlyDeletedMediaIds
    }
    val availableMedia = remember(activeSnapshot.mediaItems, removedMediaIds) {
        GalleryPrivacyFilter.availableMedia(
            mediaItems = activeSnapshot.mediaItems,
            removedMediaIds = removedMediaIds
        )
    }
    val hideableAlbums = remember(activeSnapshot.albums, availableMedia) {
        GalleryPrivacyFilter.hiddenManageableAlbums(
            albums = activeSnapshot.albums,
            mediaItems = availableMedia
        )
    }
    val hideableAlbumIds = remember(hideableAlbums) { hideableAlbums.map { it.id } }

    LaunchedEffect(hideableAlbumIds) {
        val savedHiddenAlbumIds = hiddenRepository.initialHiddenAlbumIds()
        hideableAlbums.forEach { album ->
            if (!hiddenStates.containsKey(album.id)) {
                hiddenStates[album.id] = savedHiddenAlbumIds.contains(album.id)
            }
        }
    }

    val hiddenAlbumIds by remember {
        derivedStateOf { hiddenStates.filterValues { it }.keys.toSet() }
    }
    val mediaById = remember(activeSnapshot.mediaItems) { activeSnapshot.mediaItems.associateBy { it.id } }
    var lockedVaultSnapshot by remember { mutableStateOf(LockedMediaVaultSnapshot()) }
    LaunchedEffect(vaultRefreshKey) {
        lockedVaultSnapshot = withContext(Dispatchers.IO) {
            lockedVaultRepository.loadSnapshot()
        }
    }
    val storedLockedMedia = lockedVaultSnapshot.mediaItems
    val storedLockedMediaById = lockedVaultSnapshot.mediaById
    val recentlyDeletedItems = remember(
        recentlyDeletedMedia,
        mediaStoreDeletedItems,
        mediaById,
        storedLockedMediaById
    ) {
        val localDeletedItems = recentlyDeletedMedia.mapNotNull { (mediaId, deletedAtMillis) ->
            (mediaById[mediaId] ?: storedLockedMediaById[mediaId])?.let { mediaItem ->
                RecentlyDeletedMedia(mediaItem = mediaItem, deletedAtMillis = deletedAtMillis)
            }
        }
        (mediaStoreDeletedItems + localDeletedItems)
            .distinctBy { it.mediaItem.id }
            .sortedByDescending { it.deletedAtMillis }
    }
    val hiddenAlbumMedia = remember(availableMedia, hiddenAlbumIds) {
        availableMedia.filter { hiddenAlbumIds.contains(it.albumId) }
    }
    val privateHiddenMedia = remember(availableMedia, hiddenMediaIds, storedLockedMediaById) {
        val liveHiddenMedia = availableMedia
            .filter { hiddenMediaIds.contains(it.id) }
            .map { mediaItem -> storedLockedMediaById[mediaItem.id] ?: mediaItem }
        (liveHiddenMedia + storedLockedMediaById.values.filter { hiddenMediaIds.contains(it.id) })
            .distinctBy { it.id }
    }
    val visibleMedia = remember(availableMedia, hiddenAlbumIds, hiddenMediaIds) {
        GalleryPrivacyFilter.visibleMedia(availableMedia, hiddenAlbumIds, hiddenMediaIds)
    }
    val galleryFrontWarmupKey = remember(visibleMedia) {
        visibleMedia.take(48).joinToString(separator = "|") { it.id }
    }
    LaunchedEffect(galleryFrontWarmupKey) {
        if (visibleMedia.isNotEmpty()) {
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = visibleMedia,
                thumbnailSizes = listOf(384),
                maxItems = 48,
                pinInMemory = true
            )
        }
    }
    val visibleMediaByAlbum = remember(visibleMedia) { visibleMedia.groupBy { it.albumId } }
    val favoriteMedia = remember(visibleMedia, favoriteMediaIds) {
        visibleMedia.filter { favoriteMediaIds.contains(it.id) }
    }
    val hiddenAlbumCount = hideableAlbums.count { hiddenAlbumIds.contains(it.id) }
    val hiddenAlbumItemCount = hiddenAlbumMedia.size
    val lockedItemCount = privateHiddenMedia.size
    val baseVisibleAlbums = remember(activeSnapshot.albums, availableMedia, visibleMedia, hiddenAlbumIds) {
        GalleryPrivacyFilter.visibleAlbums(
            albums = activeSnapshot.albums,
            allMedia = availableMedia,
            visibleMedia = visibleMedia,
            hiddenAlbumIds = hiddenAlbumIds
        )
    }
    val visibleAlbums = remember(baseVisibleAlbums, favoriteMedia) {
        albumsWithFavorites(
            albums = baseVisibleAlbums,
            favoriteAlbum = favoriteAlbum(favoriteMedia)
        )
    }
    val visibleAlbumIds = remember(visibleAlbums) { visibleAlbums.map { it.id } }
    val albumNameById = remember(visibleAlbums) { visibleAlbums.associate { it.id to it.name } }
    var gallerySearchIndex by remember { mutableStateOf<GallerySearchIndex?>(null) }
    var gallerySearchResult by remember {
        mutableStateOf<Pair<GallerySearchIndex, GallerySearchResult>?>(null)
    }
    val normalizedSearchQuery = remember(gallerySearchQuery) {
        normalizeGallerySearchQuery(gallerySearchQuery)
    }

    LaunchedEffect(visibleMedia, visibleAlbums) {
        gallerySearchIndex = null
        gallerySearchResult = null
        val nextIndex = withContext(Dispatchers.Default) {
            GallerySearchIndex.build(
                mediaItems = visibleMedia,
                albums = visibleAlbums
            )
        }
        gallerySearchIndex = nextIndex
        gallerySearchResult = nextIndex to GallerySearchResult(
            normalizedQuery = "",
            mediaItems = visibleMedia,
            albums = visibleAlbums
        )
    }

    LaunchedEffect(gallerySearchIndex, normalizedSearchQuery) {
        val index = gallerySearchIndex ?: return@LaunchedEffect
        if (normalizedSearchQuery.isBlank()) {
            gallerySearchResult = null
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.Default) {
            index.search(normalizedSearchQuery)
        }
        gallerySearchResult = index to result
    }

    val currentSearchResult = gallerySearchResult
        ?.takeIf { (index, _) -> index === gallerySearchIndex }
        ?.second
    val searchedVisibleMedia = when {
        normalizedSearchQuery.isBlank() -> visibleMedia
        currentSearchResult != null -> currentSearchResult.mediaItems
        else -> emptyList()
    }
    val searchedVisibleAlbums = when {
        normalizedSearchQuery.isBlank() -> visibleAlbums
        currentSearchResult != null -> currentSearchResult.albums
        else -> emptyList()
    }
    val selectedAlbum = remember(visibleAlbums, selectedAlbumId) {
        visibleAlbums.firstOrNull { it.id == selectedAlbumId }
    }
    val selectedAlbumMedia = remember(selectedAlbum?.id, visibleMedia, visibleMediaByAlbum, favoriteMedia) {
        selectedAlbum?.let { album ->
            when {
                album.id == FavoritesAlbumId -> favoriteMedia
                album.isAllPhotos -> visibleMedia
                else -> visibleMediaByAlbum[album.id].orEmpty()
            }
        }.orEmpty()
    }
    val selectedMediaItems = remember(visibleMedia, selectedMediaIds) {
        if (selectedMediaIds.isEmpty()) {
            emptyList()
        } else {
            visibleMedia.filter { mediaItem -> selectedMediaIds.contains(mediaItem.id) }
        }
    }

    val albumPreviewWarmupKey = remember(visibleAlbums) {
        visibleAlbums.take(12).joinToString(separator = "|") { "${it.id}:${it.itemCount}" }
    }

    LaunchedEffect(destination, selectedTab, albumPreviewWarmupKey, visibleMedia.size, favoriteMediaIds) {
        if (destination == GalleryDestination.Main && selectedTab == GalleryTab.Albums && visibleMedia.isNotEmpty()) {
            visibleAlbums.take(12).forEach { album ->
                prefetchMediaThumbnails(
                    context = context.applicationContext,
                    mediaItems = mediaForAlbumFast(album, visibleMedia, visibleMediaByAlbum, favoriteMedia),
                    thumbnailSizes = listOf(160),
                    maxItems = 24
                )
            }
        }
    }

    val visibleMediaIds = remember(visibleMedia) { visibleMedia.map { it.id } }
    val visibleMediaIdSet = remember(visibleMediaIds) { visibleMediaIds.toSet() }

    LaunchedEffect(destination, visibleMediaIds, viewerVisible) {
        if (destination == GalleryDestination.Documents && !viewerVisible) {
            documentPhotosViewModel.updateLibrary(visibleMedia)
        } else {
            documentPhotosViewModel.stopScanning()
        }
    }

    LaunchedEffect(visibleMediaIds, destination, viewerMediaItem?.id, viewerActionMode) {
        if (selectedMediaIds.any { it !in visibleMediaIdSet }) {
            selectedMediaIds = selectedMediaIds.intersect(visibleMediaIdSet)
        }
        val viewerReadsHiddenMedia = destination == GalleryDestination.RecentlyDeleted ||
            destination == GalleryDestination.LockedMedia ||
            viewerActionMode != ViewerActionMode.Normal
        if (!viewerReadsHiddenMedia) {
            if (viewerVisible && viewerMediaItem?.id !in visibleMediaIdSet) {
                viewerVisible = false
            }
            if (viewerMediaItems.any { it.id !in visibleMediaIdSet }) {
                viewerMediaItems = viewerMediaItems.filter { it.id in visibleMediaIdSet }
            }
        }
    }

    fun returnFromOverlay(fallbackTab: GalleryTab) {
        selectedMediaIds = emptySet()
        navigationViewModel.returnFromOverlay(fallbackTab)
    }

    fun openPhotos() {
        selectedMediaIds = emptySet()
        navigationViewModel.openTab(GalleryTab.Photos)
    }

    fun openAlbums() {
        selectedMediaIds = emptySet()
        navigationViewModel.openTab(GalleryTab.Albums)
    }

    fun openRecentlyDeleted() {
        selectedMediaIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.RecentlyDeleted)
    }

    fun openCleanup() {
        selectedMediaIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.Cleanup)
    }

    fun openDocuments() {
        selectedMediaIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.Documents)
    }


    fun startAlbumCreator(albumName: String) {
        val trimmedName = albumName.trim()
        if (trimmedName.isBlank()) return
        pendingAlbumName = trimmedName
        albumCreationSelectedIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.AlbumCreator)
    }

    fun cancelAlbumCreator() {
        pendingAlbumName = null
        albumCreationSelectedIds = emptySet()
        returnFromOverlay(GalleryTab.Albums)
    }

    fun toggleAlbumCreationMedia(mediaItem: MediaItem) {
        albumCreationSelectedIds = if (mediaItem.id in albumCreationSelectedIds) {
            albumCreationSelectedIds - mediaItem.id
        } else {
            albumCreationSelectedIds + mediaItem.id
        }
    }

    fun closePhotoEditor() {
        editingMediaItem = null
        val viewerSession = editorViewerSession
        editorViewerSession = null
        navigationViewModel.closeEditor()
        if (viewerSession != null) {
            mediaOpenTransition = null
            mediaCloseTransition = null
            viewerMediaItem = viewerSession.mediaItem
            viewerMediaItems = viewerSession.mediaItems
            viewerSharedElementKey = viewerSession.sharedElementKey
            viewerSharedElementKeyPrefix = viewerSession.sharedElementKeyPrefix
            viewerActionMode = viewerSession.actionMode
            viewerReturnFallbackBounds = viewerSession.returnFallbackBounds
            viewerSourceMediaId = viewerSession.sourceMediaId
            viewerSourceMediaIds = viewerSession.sourceMediaIds
            viewerSourceBounds = viewerSession.sourceBounds
            viewerSourceGridColumns = viewerSession.sourceGridColumns
            viewerVisible = true
        }
    }

    fun openHiddenItems() {
        selectedMediaIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.HiddenItems)
    }

    fun openLockedMedia() {
        selectedMediaIds = emptySet()
        navigationViewModel.openOverlay(GalleryDestination.LockedMedia)
        lockedSecurityViewModel.clearMessage()
    }

    fun closeLockedMedia() {
        val cancellation = hiddenBiometricCancellation
        hiddenBiometricCancellation = null
        cancellation?.cancel()
        returnFromOverlay(GalleryTab.Albums)
    }

    val lockedDisplayMedia = remember(privateHiddenMedia, storedLockedMediaById) {
        privateHiddenMedia.map { mediaItem -> storedLockedMediaById[mediaItem.id] ?: mediaItem }
    }
    val lockedGridMedia = remember(lockedDisplayMedia) {
        lockedDisplayMedia.map { mediaItem ->
            mediaItem.previewUri?.let { previewUri ->
                mediaItem.copy(contentUri = previewUri)
            } ?: mediaItem.copy(contentUri = null)
        }
    }
    val missingLockedPreviewIds = remember(lockedVaultSnapshot, lockedDisplayMedia) {
        val displayedIds = lockedDisplayMedia.mapTo(mutableSetOf()) { it.id }
        lockedVaultSnapshot.missingPreviewIds.intersect(displayedIds)
    }
    val lockedPreviewMigrationKey = remember(missingLockedPreviewIds) {
        missingLockedPreviewIds.sorted().joinToString(separator = "|")
    }
    LaunchedEffect(destination, hiddenVaultUnlocked, lockedPreviewMigrationKey) {
        if (
            destination != GalleryDestination.LockedMedia ||
            !hiddenVaultUnlocked ||
            missingLockedPreviewIds.isEmpty()
        ) return@LaunchedEffect
        val migratedAny = withContext(Dispatchers.IO) {
            missingLockedPreviewIds.mapNotNull(storedLockedMediaById::get)
                .fold(false) { migrated, mediaItem ->
                    lockedVaultRepository.ensureEncryptedPreviewFromVault(mediaItem) || migrated
                }
        }
        if (migratedAny) vaultRefreshKey += 1
    }
    lateinit var launchMediaStoreWrite: (PendingMediaStoreWriteAction) -> Unit
    fun lockMediaItems(mediaItems: List<MediaItem>, onLocked: () -> Unit = {}) {
        if (mediaItems.isEmpty()) return
        lockedSecurityViewModel.clearMessage()
        prefetchScope.launch {
            val importedMediaIds = withContext(Dispatchers.IO) {
                mediaItems.mapNotNull { mediaItem ->
                    mediaItem.id.takeIf { lockedVaultRepository.importMedia(mediaItem) }
                }
            }.toSet()
            if (importedMediaIds.isEmpty()) return@launch
            vaultRefreshKey += 1

            val importedItems = mediaItems.filter { importedMediaIds.contains(it.id) }
            val updatedHiddenMediaIds = withContext(Dispatchers.IO) {
                hiddenMediaRepository.setMediaHidden(importedMediaIds, true)
            }
            hiddenMediaIds = updatedHiddenMediaIds
            onLocked()
            launchMediaStoreWrite(
                PendingMediaStoreWriteAction(
                    mode = MediaStoreWriteMode.DeleteLockedOriginals,
                    mediaItems = importedItems
                )
            )
        }
    }
    LaunchedEffect(lockedSecurityUiState.pinCreationEventId) {
        if (lockedSecurityUiState.pinCreationEventId <= 0 || pendingLockedMediaIds.isEmpty()) {
            return@LaunchedEffect
        }
        val pendingItems = availableMedia.filter { pendingLockedMediaIds.contains(it.id) }
        pendingLockedMediaIds = emptySet()
        lockMediaItems(pendingItems)
    }

    fun requestHiddenBiometricUnlock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            lockedSecurityViewModel.showMessage("Biometric unlock is not available on this Android version.")
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            lockedSecurityViewModel.showMessage("Biometric unlock needs an active screen.")
            return
        }
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isDeviceSecure == false) {
            lockedSecurityViewModel.showMessage("Set up screen lock or biometrics in Android settings first.")
            return
        }

        val cancellationSignal = CancellationSignal()
        hiddenBiometricCancellation?.cancel()
        hiddenBiometricCancellation = cancellationSignal
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Unlock locked media")
            .setSubtitle("Use face, fingerprint, or another available biometric")
            .setNegativeButton("Use PIN", activity.mainExecutor) { _, _ ->
                if (hiddenBiometricCancellation === cancellationSignal) {
                    hiddenBiometricCancellation = null
                    lockedSecurityViewModel.clearMessage()
                }
            }
            .build()
        prompt.authenticate(
            cancellationSignal,
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    if (
                        hiddenBiometricCancellation !== cancellationSignal ||
                        latestDestination != GalleryDestination.LockedMedia
                    ) {
                        return
                    }
                    hiddenBiometricCancellation = null
                    lockedSecurityViewModel.unlockWithBiometric()
                }

                override fun onAuthenticationFailed() {
                    if (
                        hiddenBiometricCancellation === cancellationSignal &&
                        latestDestination == GalleryDestination.LockedMedia
                    ) {
                        lockedSecurityViewModel.showMessage("Biometric was not recognized.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if (hiddenBiometricCancellation !== cancellationSignal) return
                    hiddenBiometricCancellation = null
                    if (latestDestination == GalleryDestination.LockedMedia) {
                        lockedSecurityViewModel.showMessage(
                            errString?.toString()?.takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
        )
    }

    fun hideSelectedMedia() {
        val mediaToLock = selectedMediaItems
        if (mediaToLock.isEmpty()) return
        pendingLockConfirmation = PendingLockConfirmation(mediaItems = mediaToLock)
    }
    fun unhideMediaItems(
        mediaItems: List<MediaItem>,
        onCompleted: (Set<String>) -> Unit = {}
    ) {
        if (mediaItems.isEmpty()) return
        prefetchScope.launch {
            val restoredIds = withContext(Dispatchers.IO) {
                mediaItems.mapNotNull { mediaItem ->
                    val restored = lockedVaultRepository.originalMediaExists(mediaItem.id) ||
                        lockedVaultRepository.restoreMedia(mediaItem) != null
                    mediaItem.id.takeIf { restored }?.also {
                        lockedVaultRepository.delete(mediaItem.id)
                    }
                }.toSet()
            }
            if (restoredIds.isEmpty()) {
                lockedSecurityViewModel.showMessage("Could not restore the selected media.")
                return@launch
            }
            hiddenMediaIds = hiddenMediaRepository.setMediaHidden(restoredIds, false)
            vaultRefreshKey += 1
            mediaViewModel.requestQuickRefresh()
            onCompleted(restoredIds)
        }
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
                navigationViewModel.clearSelectedAlbum()
            }
        }
    }

    fun hideAlbumAndReturn(album: Album) {
        updateAlbumHidden(album, true)
        openAlbums()
    }

    fun startAlbumOpen(album: Album, bounds: Rect) {
        if (albumTransition != null) return
        val tileBounds = bounds.takeIf { it != Rect.Zero } ?: albumTileBounds[album.id]
        val openingMedia = mediaForAlbumFast(album, visibleMedia, visibleMediaByAlbum, favoriteMedia)
        navigationViewModel.selectAlbumForOpening(album.id)
        selectedMediaIds = emptySet()

        val hasTileTransition = tileBounds?.isUsableTransitionBounds() == true
        if (hasTileTransition) {
            albumTileBounds[album.id] = tileBounds
            albumTransitionKey += 1
            val openingKey = albumTransitionKey
            albumWarmupReadyKey = -1
            albumTransitionCommittedKey = -1
            albumTransitionAwaitingDestinationKey = -1
            albumTransition = AlbumTransitionSpec(
                key = openingKey,
                album = album,
                tileBounds = tileBounds,
                mode = AlbumTransitionMode.Opening
            )
            prefetchScope.launch {
                prefetchMediaThumbnails(
                    context = context.applicationContext,
                    mediaItems = openingMedia,
                    thumbnailSizes = listOf(384),
                    maxItems = 24,
                    pinInMemory = true
                )
                if (albumTransitionKey == openingKey) {
                    albumWarmupReadyKey = openingKey
                }
            }
            return
        }
        navigationViewModel.showAlbumDetail()
    }

    fun closeAlbumDetail() {
        if (albumTransition != null) return
        val closingAlbum = selectedAlbum
        val closingBounds = selectedAlbumId?.let { albumTileBounds[it] }
        selectedMediaIds = emptySet()
        if (closingAlbum != null && closingBounds?.isUsableTransitionBounds() == true) {
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = closingAlbum,
                tileBounds = closingBounds,
                mode = AlbumTransitionMode.Closing
            )
        }
        navigationViewModel.closeAlbumDetail()
    }

    LaunchedEffect(destination, selectedAlbumId, visibleAlbumIds) {
        if (destination == GalleryDestination.AlbumDetail && selectedAlbumId != null && selectedAlbum == null) {
            navigationViewModel.clearSelectedAlbum()
            openAlbums()
        }
    }
    LaunchedEffect(destination, albumTransitionAwaitingDestinationKey) {
        val awaitingKey = albumTransitionAwaitingDestinationKey
        if (destination == GalleryDestination.AlbumDetail && awaitingKey >= 0) {
            if (albumTransition?.key == awaitingKey) {
                albumTransition = null
                albumTransitionCommittedKey = -1
            }
            albumTransitionAwaitingDestinationKey = -1
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
            navigationViewModel.syncPagerTab(pageToGalleryTab(page))
        }
    }

    fun finishViewerOpen(
        mediaItem: MediaItem,
        mediaItems: List<MediaItem>,
        sharedElementKey: Any? = null,
        sharedElementKeyPrefix: String? = null,
        actionMode: ViewerActionMode = ViewerActionMode.Normal
    ) {
        viewerMediaItems = mediaItems
        viewerMediaItem = mediaItem
        viewerSharedElementKey = sharedElementKey
        viewerSharedElementKeyPrefix = sharedElementKeyPrefix
        viewerActionMode = actionMode
        viewerVisible = true
    }

    fun startViewerOpen(
        mediaItem: MediaItem,
        mediaItems: List<MediaItem>,
        bounds: Rect,
        sharedElementKey: Any? = null,
        sharedElementKeyPrefix: String? = null,
        actionMode: ViewerActionMode = ViewerActionMode.Normal,
        transitionMediaItem: MediaItem = mediaItem,
        sourceGridColumns: Int = 4
    ) {
        mediaOpenTransitionKey += 1
        val openKey = mediaOpenTransitionKey
        mediaOpenWarmupReadyKey = -1
        mediaTileBounds.clear()
        if (bounds.isUsableTransitionBounds()) {
            viewerReturnFallbackBounds = bounds
            viewerSourceBounds = bounds
        }
        viewerSourceMediaId = mediaItem.id
        viewerSourceMediaIds = mediaItems.map { it.id }
        viewerSourceGridColumns = sourceGridColumns.coerceAtLeast(1)

        val hasTileTransition = bounds.isUsableTransitionBounds()
        if (hasTileTransition) {
            mediaTileBounds[mediaItem.id] = bounds
            mediaOpenTransition = MediaOpenTransitionSpec(
                key = openKey,
                mediaItem = mediaItem,
                transitionMediaItem = transitionMediaItem,
                mediaItems = mediaItems,
                tileBounds = bounds,
                sharedElementKey = sharedElementKey,
                sharedElementKeyPrefix = sharedElementKeyPrefix,
                actionMode = actionMode
            )
        }

        val warmupItem = (if (actionMode == ViewerActionMode.Locked) {
            transitionMediaItem.copy(isVideo = false)
        } else {
            mediaItem
        }).takeIf { it.contentUri != null }
        if (warmupItem != null) {
            prefetchScope.launch {
                prefetchMediaThumbnails(
                    context = context.applicationContext,
                    mediaItems = listOf(warmupItem),
                    thumbnailSizes = listOf(
                        if (actionMode == ViewerActionMode.Locked || warmupItem.isVideo) 512
                        else viewerPhotoDecodeSize
                    ),
                    maxItems = 1,
                    pinInMemory = true
                )
                if (mediaOpenTransitionKey == openKey) {
                    mediaOpenWarmupReadyKey = openKey
                    if (!hasTileTransition) {
                        finishViewerOpen(
                            mediaItem = mediaItem,
                            mediaItems = mediaItems,
                            sharedElementKey = sharedElementKey,
                            sharedElementKeyPrefix = sharedElementKeyPrefix,
                            actionMode = actionMode
                        )
                    }
                }
            }
        } else {
            mediaOpenWarmupReadyKey = openKey
            if (!hasTileTransition) {
                finishViewerOpen(
                    mediaItem = mediaItem,
                    mediaItems = mediaItems,
                    sharedElementKey = sharedElementKey,
                    sharedElementKeyPrefix = sharedElementKeyPrefix,
                    actionMode = actionMode
                )
            }
        }
    }
    fun clearViewerAfterClose() {
        mediaCloseTransition = null
        viewerMediaItem = null
        viewerMediaItems = emptyList()
        viewerSharedElementKey = null
        viewerSharedElementKeyPrefix = null
        viewerActionMode = ViewerActionMode.Normal
        viewerReturnFallbackBounds = Rect.Zero
        viewerSourceMediaId = null
        viewerSourceMediaIds = emptyList()
        viewerSourceBounds = Rect.Zero
        viewerSourceGridColumns = 4
        viewerRevealMediaId = null
        viewerCloseInProgress = false
        viewerVisible = false
    }

    fun returnBoundsForMedia(mediaItem: MediaItem): Rect {
        mediaTileBounds[mediaItem.id]?.takeIf { it != Rect.Zero }?.let { return it }

        val sourceId = viewerSourceMediaId ?: return viewerReturnFallbackBounds
        val sourceIndex = viewerSourceMediaIds.indexOf(sourceId)
        val targetIndex = viewerSourceMediaIds.indexOf(mediaItem.id)
        val sourceBounds = viewerSourceBounds.takeIf { it != Rect.Zero } ?: viewerReturnFallbackBounds
        if (sourceIndex < 0 || targetIndex < 0 || sourceBounds == Rect.Zero) {
            return viewerReturnFallbackBounds
        }

        val columns = viewerSourceGridColumns.coerceAtLeast(1)
        val sourceColumn = sourceIndex % columns
        val sourceRow = sourceIndex / columns
        val targetColumn = targetIndex % columns
        val targetRow = targetIndex / columns
        val stepX = sourceBounds.width + 1f
        val stepY = sourceBounds.height + 1f
        val left = sourceBounds.left + (targetColumn - sourceColumn) * stepX
        val top = sourceBounds.top + (targetRow - sourceRow) * stepY
        return Rect(
            left = left,
            top = top,
            right = left + sourceBounds.width,
            bottom = top + sourceBounds.height
        )
    }

    fun closeViewer(startOffset: Offset = Offset.Zero, startScale: Float = 1f, startBackdropAlpha: Float = 1f) {
        if (mediaCloseTransition != null || viewerCloseInProgress) return
        val currentItem = viewerMediaItem
        if (currentItem == null) {
            clearViewerAfterClose()
            return
        }

        viewerCloseInProgress = true
        prefetchScope.launch {
            val supportsGridReveal =
                (destination == GalleryDestination.Main && selectedTab == GalleryTab.Photos) ||
                    destination == GalleryDestination.AlbumDetail
            val knownBounds = mediaTileBounds[currentItem.id] ?: Rect.Zero
            val needsGridReveal = supportsGridReveal &&
                !knownBounds.isVisibleWithin(transitionRootBoundsInWindow)

            if (needsGridReveal) {
                mediaTileBounds.remove(currentItem.id)
                viewerRevealMediaId = currentItem.id
                for (attempt in 0 until 18) {
                    delay(16)
                    val revealedBounds = mediaTileBounds[currentItem.id] ?: Rect.Zero
                    if (revealedBounds.isVisibleWithin(transitionRootBoundsInWindow)) break
                }
                viewerRevealMediaId = null
            }

            viewerSharedElementKeyPrefix?.let { keyPrefix ->
                viewerSharedElementKey = "$keyPrefix-media-${currentItem.id}"
            }
            viewerVisible = false

            val targetBounds = if (needsGridReveal) {
                mediaTileBounds[currentItem.id]
                    ?.takeIf { it.isVisibleWithin(transitionRootBoundsInWindow) }
                    ?: Rect.Zero
            } else {
                returnBoundsForMedia(currentItem)
            }
            if (targetBounds.isUsableTransitionBounds()) {
                mediaCloseTransitionKey += 1
                mediaCloseTransition = MediaCloseTransitionSpec(
                    key = mediaCloseTransitionKey,
                    mediaItem = currentItem,
                    tileBounds = targetBounds,
                    startOffset = startOffset,
                    startScale = startScale.coerceIn(0.68f, 1f),
                    startBackdropAlpha = startBackdropAlpha.coerceIn(0f, 1f)
                )
                viewerCloseInProgress = false
            } else {
                clearViewerAfterClose()
            }
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

    fun confirmLockMedia(pending: PendingLockConfirmation) {
        pendingLockConfirmation = null
        val mediaIds = pending.mediaItems.map { it.id }.toSet()
        if (mediaIds.isEmpty()) return
        selectedMediaIds = selectedMediaIds - mediaIds

        if (!hasHiddenPin) {
            pendingLockedMediaIds = pendingLockedMediaIds + mediaIds
            lockedSecurityViewModel.lock(if (mediaIds.size == 1) {
                "Set a PIN to lock this item."
            } else {
                String.format(Locale.getDefault(), "Set a PIN to lock %1$,d selected items.", mediaIds.size)
            })
            if (pending.viewerMediaId != null) viewerVisible = false
            navigationViewModel.openOverlay(
                destination = GalleryDestination.LockedMedia,
                destinationTab = GalleryTab.Albums
            )
            return
        }

        lockMediaItems(pending.mediaItems) {
            pending.viewerMediaId?.let { viewerMediaId ->
                pending.mediaItems.firstOrNull { it.id == viewerMediaId }?.let { mediaItem ->
                    advanceViewerAfterRemoval(mediaItem, pending.viewerDirection)
                }
            }
        }
    }

    fun completeMediaStoreWrite(action: PendingMediaStoreWriteAction) {
        val mediaIds = action.mediaItems.map { it.id }.toSet()
        if (mediaIds.isEmpty()) return
        if (action.mode == MediaStoreWriteMode.MoveToAlbum) {
            val albumName = action.targetAlbumName.orEmpty()
            prefetchScope.launch {
                val moved = withContext(Dispatchers.IO) {
                    mediaStoreWriteRepository.moveToAlbum(action.mediaItems, albumName)
                }
                mediaViewModel.finishAppMediaStoreWrite()
                if (moved) {
                    pendingAlbumName = null
                    albumCreationSelectedIds = emptySet()
                    selectedMediaIds = emptySet()
                    navigationViewModel.openTab(GalleryTab.Albums)
                }
                mediaViewModel.requestFullRefresh()
            }
            return
        }

        when (action.mode) {
            MediaStoreWriteMode.Trash -> {
                selectedMediaIds = selectedMediaIds - mediaIds
                if (action.fromViewer) {
                    action.mediaItems.firstOrNull()?.let { mediaItem ->
                        advanceViewerAfterRemoval(mediaItem, action.viewerDirection)
                    }
                }
            }
            MediaStoreWriteMode.RestoreFromTrash -> {
                recentlyDeletedMedia = recentlyDeletedRepository.restore(mediaIds)
                if (action.fromViewer) {
                    action.mediaItems.firstOrNull()?.let { mediaItem ->
                        advanceViewerAfterRemoval(mediaItem, action.viewerDirection)
                    }
                }
            }
            MediaStoreWriteMode.DeleteForever -> {
                val deleteState = recentlyDeletedRepository.deleteForever(mediaIds)
                recentlyDeletedMedia = deleteState.deletedMedia
                permanentlyDeletedMediaIds = deleteState.permanentlyDeletedMediaIds
                favoriteMediaIds = favoritesRepository.removeFavorites(mediaIds)
                selectedMediaIds = selectedMediaIds - mediaIds
                if (action.fromViewer) {
                    action.mediaItems.firstOrNull()?.let { mediaItem ->
                        advanceViewerAfterRemoval(mediaItem, action.viewerDirection)
                    }
                }
            }
            MediaStoreWriteMode.DeleteLockedOriginals -> Unit
            MediaStoreWriteMode.MoveToAlbum -> Unit

        }
        if (action.mode == MediaStoreWriteMode.DeleteLockedOriginals) {
            mediaViewModel.requestQuickRefresh()
        } else {
            mediaViewModel.requestFullRefresh()
        }
    }

    fun completeMediaStoreFallback(action: PendingMediaStoreWriteAction) {
        prefetchScope.launch {
            when (action.mode) {
                MediaStoreWriteMode.Trash -> {
                    recentlyDeletedMedia = recentlyDeletedRepository.markDeleted(action.mediaItems.map { it.id })
                    mediaViewModel.finishAppMediaStoreWrite()
                    completeMediaStoreWrite(action)
                }
                MediaStoreWriteMode.RestoreFromTrash -> {
                    mediaViewModel.finishAppMediaStoreWrite()
                    completeMediaStoreWrite(action)
                }
                MediaStoreWriteMode.DeleteForever -> {
                    withContext(Dispatchers.IO) {
                        mediaStoreWriteRepository.deleteDirectly(action.mediaItems)
                    }
                    mediaViewModel.finishAppMediaStoreWrite()
                    completeMediaStoreWrite(action)
                }
                MediaStoreWriteMode.DeleteLockedOriginals -> {
                    withContext(Dispatchers.IO) {
                        mediaStoreWriteRepository.deleteDirectly(action.mediaItems)
                    }
                    mediaViewModel.finishAppMediaStoreWrite()
                    mediaViewModel.requestQuickRefresh()
                }
                MediaStoreWriteMode.MoveToAlbum -> {
                    completeMediaStoreWrite(action)
                }
            }
        }
    }

    fun rollbackCancelledLockedMedia(action: PendingMediaStoreWriteAction) {
        if (action.mode != MediaStoreWriteMode.DeleteLockedOriginals) return
        prefetchScope.launch {
            val rollbackIds = withContext(Dispatchers.IO) {
                action.mediaItems
                    .filter { lockedVaultRepository.originalMediaExists(it.id) }
                    .map { it.id }
                    .toSet()
            }
            if (rollbackIds.isEmpty()) return@launch
            hiddenMediaIds = hiddenMediaRepository.setMediaHidden(rollbackIds, false)
            withContext(Dispatchers.IO) {
                rollbackIds.forEach(lockedVaultRepository::delete)
            }
            vaultRefreshKey += 1
            lockedSecurityViewModel.showMessage(
                "Original removal was cancelled, so those items were not moved to Locked Media."
            )
            mediaViewModel.requestQuickRefresh()
        }
    }
    val mediaStoreWriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingMediaStoreWriteAction
        pendingMediaStoreWriteAction = null
        mediaViewModel.finishAppMediaStoreWrite()
        if (result.resultCode == Activity.RESULT_OK && action != null) {
            completeMediaStoreWrite(action)
        } else if (action != null) {
            rollbackCancelledLockedMedia(action)
        }
    }

    launchMediaStoreWrite = { action ->
        val request = when (action.mode) {
            MediaStoreWriteMode.Trash -> mediaStoreWriteRepository.createTrashRequest(action.mediaItems, trashed = true)
            MediaStoreWriteMode.RestoreFromTrash -> mediaStoreWriteRepository.createTrashRequest(action.mediaItems, trashed = false)
            MediaStoreWriteMode.DeleteForever,
            MediaStoreWriteMode.DeleteLockedOriginals -> mediaStoreWriteRepository.createDeleteRequest(action.mediaItems)
            MediaStoreWriteMode.MoveToAlbum -> mediaStoreWriteRepository.createWriteRequest(action.mediaItems)
        }

        mediaViewModel.beginAppMediaStoreWrite()
        if (request != null) {
            pendingMediaStoreWriteAction = action
            mediaStoreWriteLauncher.launch(request)
        } else {
            completeMediaStoreFallback(action)
        }
    }
    fun hideMedia(mediaItem: MediaItem, direction: Int) {
        pendingLockConfirmation = PendingLockConfirmation(
            mediaItems = listOf(mediaItem),
            viewerMediaId = mediaItem.id,
            viewerDirection = direction
        )
    }
    fun completeMediaDelete(mediaItem: MediaItem, direction: Int) {
        recentlyDeletedMedia = recentlyDeletedRepository.markDeleted(mediaItem.id)
        selectedMediaIds = selectedMediaIds - mediaItem.id
        advanceViewerAfterRemoval(mediaItem, direction)
    }

    fun isVaultMedia(mediaItem: MediaItem): Boolean {
        return storedLockedMediaById.containsKey(mediaItem.id) ||
            mediaItem.contentUri?.authority == LockedMediaVaultRepository.vaultAuthority(context)
    }

    fun softDeleteLockedMediaItems(
        mediaItems: List<MediaItem>,
        viewerDirection: Int? = null
    ) {
        if (mediaItems.isEmpty()) return
        val mediaIds = mediaItems.map { it.id }
        recentlyDeletedMedia = recentlyDeletedRepository.markDeleted(mediaIds)
        hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaIds.toSet(), false)
        selectedMediaIds = selectedMediaIds - mediaIds.toSet()
        viewerDirection?.let { direction ->
            mediaItems.firstOrNull()?.let { advanceViewerAfterRemoval(it, direction) }
        }
    }

    fun restoreVaultDeletedMediaItems(
        mediaItems: List<MediaItem>,
        viewerDirection: Int? = null
    ) {
        if (mediaItems.isEmpty()) return
        val mediaIds = mediaItems.map { it.id }.toSet()
        recentlyDeletedMedia = recentlyDeletedRepository.restore(mediaIds)
        hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaIds, true)
        selectedMediaIds = selectedMediaIds - mediaIds
        viewerDirection?.let { direction ->
            mediaItems.firstOrNull()?.let { advanceViewerAfterRemoval(it, direction) }
        }
    }

    fun permanentlyDeleteVaultMediaItems(
        mediaItems: List<MediaItem>,
        viewerDirection: Int? = null
    ) {
        if (mediaItems.isEmpty()) return
        prefetchScope.launch {
            val mediaIds = mediaItems.map { it.id }.toSet()
            withContext(Dispatchers.IO) {
                mediaIds.forEach(lockedVaultRepository::delete)
            }
            val deleteState = recentlyDeletedRepository.deleteForever(mediaIds)
            recentlyDeletedMedia = deleteState.deletedMedia
            permanentlyDeletedMediaIds = deleteState.permanentlyDeletedMediaIds
            favoriteMediaIds = favoritesRepository.removeFavorites(mediaIds)
            hiddenMediaIds = hiddenMediaRepository.setMediaHidden(mediaIds, false)
            selectedMediaIds = selectedMediaIds - mediaIds
            vaultRefreshKey += 1
            viewerDirection?.let { direction ->
                mediaItems.firstOrNull()?.let { advanceViewerAfterRemoval(it, direction) }
            }
        }
    }

    fun requestMediaDelete(mediaItem: MediaItem, direction: Int) {
        when {
            viewerActionMode == ViewerActionMode.RecentlyDeleted ||
                (destination == GalleryDestination.RecentlyDeleted && recentlyDeletedMedia.containsKey(mediaItem.id)) -> {
                if (isVaultMedia(mediaItem)) {
                    permanentlyDeleteVaultMediaItems(listOf(mediaItem), direction)
                } else {
                    launchMediaStoreWrite(
                        PendingMediaStoreWriteAction(
                            mode = MediaStoreWriteMode.DeleteForever,
                            mediaItems = listOf(mediaItem),
                            viewerDirection = direction,
                            fromViewer = true
                        )
                    )
                }
            }
            viewerActionMode == ViewerActionMode.Locked -> {
                softDeleteLockedMediaItems(listOf(mediaItem), direction)
            }
            else -> launchMediaStoreWrite(
                PendingMediaStoreWriteAction(
                    mode = MediaStoreWriteMode.Trash,
                    mediaItems = listOf(mediaItem),
                    viewerDirection = direction,
                    fromViewer = true
                )
            )
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
        if (selectedMediaItems.isEmpty()) return
        launchMediaStoreWrite(PendingMediaStoreWriteAction(MediaStoreWriteMode.Trash, selectedMediaItems))
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

    fun editPhoto(mediaItem: MediaItem) {
        if (mediaItem.isVideo || mediaItem.contentUri == null) return
        navigationViewModel.openEditor()
        editorViewerSession = viewerMediaItem?.let { currentViewerItem ->
            ViewerSessionSnapshot(
                mediaItem = currentViewerItem,
                mediaItems = viewerMediaItems,
                sharedElementKey = viewerSharedElementKey,
                sharedElementKeyPrefix = viewerSharedElementKeyPrefix,
                actionMode = viewerActionMode,
                returnFallbackBounds = viewerReturnFallbackBounds,
                sourceMediaId = viewerSourceMediaId,
                sourceMediaIds = viewerSourceMediaIds,
                sourceBounds = viewerSourceBounds,
                sourceGridColumns = viewerSourceGridColumns
            )
        }
        clearViewerAfterClose()
        editingMediaItem = mediaItem
    }



    fun restoreViewerMedia(mediaItem: MediaItem, direction: Int) {
        when (viewerActionMode) {
            ViewerActionMode.RecentlyDeleted -> {
                if (isVaultMedia(mediaItem)) {
                    restoreVaultDeletedMediaItems(listOf(mediaItem), direction)
                } else {
                    launchMediaStoreWrite(
                        PendingMediaStoreWriteAction(
                            mode = MediaStoreWriteMode.RestoreFromTrash,
                            mediaItems = listOf(mediaItem),
                            viewerDirection = direction,
                            fromViewer = true
                        )
                    )
                }
            }
            ViewerActionMode.Locked -> {
                unhideMediaItems(listOf(mediaItem)) { restoredIds ->
                    if (mediaItem.id in restoredIds) {
                        advanceViewerAfterRemoval(mediaItem, direction)
                    }
                }
            }
            ViewerActionMode.Normal -> Unit
        }
    }

    fun restoreDeletedMediaItems(entries: List<RecentlyDeletedMedia>) {
        if (entries.isEmpty()) return
        val mediaItems = entries.map { it.mediaItem }
        val (vaultItems, mediaStoreItems) = mediaItems.partition(::isVaultMedia)
        restoreVaultDeletedMediaItems(vaultItems)
        if (mediaStoreItems.isNotEmpty()) {
            launchMediaStoreWrite(
                PendingMediaStoreWriteAction(MediaStoreWriteMode.RestoreFromTrash, mediaStoreItems)
            )
        }
    }

    fun deleteDeletedMediaItems(entries: List<RecentlyDeletedMedia>) {
        if (entries.isEmpty()) return
        val mediaItems = entries.map { it.mediaItem }
        val (vaultItems, mediaStoreItems) = mediaItems.partition(::isVaultMedia)
        permanentlyDeleteVaultMediaItems(vaultItems)
        if (mediaStoreItems.isNotEmpty()) {
            launchMediaStoreWrite(
                PendingMediaStoreWriteAction(MediaStoreWriteMode.DeleteForever, mediaStoreItems)
            )
        }
    }

    val navigationTransitionIdle = albumTransition == null &&
        mediaOpenTransition == null &&
        mediaCloseTransition == null
    val backAction = resolveGalleryBackAction(
        destination = destination,
        selectedTab = selectedTab,
        viewerVisible = viewerVisible && viewerMediaItem != null,
        viewerClosing = viewerCloseInProgress,
        albumTransitionActive = albumTransition != null,
        albumTransitionCanCancel = albumTransition?.mode == AlbumTransitionMode.Opening,
        mediaTransitionActive = mediaOpenTransition != null || mediaCloseTransition != null,
        mediaTransitionCanCancel = mediaOpenTransition != null,
        hasSelection = selectedMediaIds.isNotEmpty()
    )
    val currentBackAction by rememberUpdatedState(backAction)

    fun performGalleryBack(action: GalleryBackAction) {
        when (action) {
            GalleryBackAction.System,
            GalleryBackAction.BlockTransition -> Unit
            GalleryBackAction.CloseViewer -> {
                val progress = predictiveBackProgress.value.coerceIn(0f, 1f)
                val direction = if (predictiveBackSwipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                closeViewer(
                    startOffset = Offset(
                        x = direction * context.resources.displayMetrics.density * 24f * progress,
                        y = 0f
                    ),
                    startScale = 1f - 0.04f * progress,
                    startBackdropAlpha = 1f - 0.22f * progress
                )
            }
            GalleryBackAction.CancelAlbumOpen -> {
                albumTransition = null
                albumTransitionCommittedKey = -1
                albumTransitionAwaitingDestinationKey = -1
                navigationViewModel.cancelAlbumOpen()
            }
            GalleryBackAction.CancelMediaOpen -> {
                mediaOpenTransition = null
                clearViewerAfterClose()
            }
            GalleryBackAction.ClearSelection -> clearMediaSelection()
            GalleryBackAction.OpenPhotos -> openPhotos()
            GalleryBackAction.CloseAlbumDetail -> closeAlbumDetail()
            GalleryBackAction.ReturnToAlbums -> {
                if (destination == GalleryDestination.LockedMedia) {
                    closeLockedMedia()
                } else {
                    returnFromOverlay(GalleryTab.Albums)
                }
            }
            GalleryBackAction.ReturnToMenu -> returnFromOverlay(GalleryTab.Menu)
            GalleryBackAction.CancelAlbumCreator -> cancelAlbumCreator()
            GalleryBackAction.ClosePhotoEditor -> closePhotoEditor()
        }
    }

    LaunchedEffect(backAction, viewerVisible, viewerCloseInProgress) {
        val gestureAction = predictiveBackGestureAction
        if (
            gestureAction == GalleryBackAction.CloseViewer &&
            viewerVisible &&
            viewerCloseInProgress
        ) {
            return@LaunchedEffect
        }
        if (gestureAction != null && gestureAction != backAction) {
            predictiveBackProgress.animateTo(0f, tween(90))
            predictiveBackGestureAction = null
        }
    }

    PredictiveBackHandler(enabled = backAction != GalleryBackAction.System) { progressFlow ->
        val actionAtStart = currentBackAction
        predictiveBackGestureAction = actionAtStart
        if (actionAtStart == GalleryBackAction.BlockTransition) {
            progressFlow.collect()
            predictiveBackProgress.snapTo(0f)
            predictiveBackGestureAction = null
            return@PredictiveBackHandler
        }
        try {
            progressFlow.collect { event ->
                predictiveBackSwipeEdge = event.swipeEdge
                predictiveBackProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            performGalleryBack(actionAtStart)
        } catch (cancellation: CancellationException) {
            predictiveBackProgress.animateTo(0f, tween(130))
            predictiveBackGestureAction = null
            throw cancellation
        }
    }
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val density = LocalDensity.current
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }
        val adaptivePolicy = remember(maxWidth, maxHeight) {
            galleryAdaptivePolicy(maxWidth.value, maxHeight.value)
        }
        val adaptivePhotoColumns = (
            settings.gridDensity.photoColumns + adaptivePolicy.photoColumnBoost
        ).coerceIn(3, 10)
        val adaptiveHeroHeight = when (adaptivePolicy.widthClass) {
            GalleryWindowWidthClass.Compact -> 176.dp
            GalleryWindowWidthClass.Medium -> 196.dp
            GalleryWindowWidthClass.Expanded -> 220.dp
        }

        SharedTransitionLayout(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val gestureAction = predictiveBackGestureAction
                    val progress = if (
                        gestureAction != null &&
                        gestureAction != GalleryBackAction.CloseViewer &&
                        gestureAction != GalleryBackAction.BlockTransition
                    ) {
                        predictiveBackProgress.value.coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val fromRight = predictiveBackSwipeEdge == BackEventCompat.EDGE_RIGHT
                    val direction = if (fromRight) -1f else 1f
                    val scale = 1f - 0.04f * progress
                    scaleX = scale
                    scaleY = scale
                    translationX = direction * 24.dp.toPx() * progress
                    transformOrigin = TransformOrigin(if (fromRight) 1f else 0f, 0.5f)
                    shadowElevation = 18.dp.toPx() * progress
                    shape = RoundedCornerShape((28f * progress).dp)
                    clip = progress > 0f
                }
                .onGloballyPositioned { transitionRootBoundsInWindow = it.boundsInWindow() }
        ) {
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
                        if (
                            destination == GalleryDestination.Main &&
                            selectedMediaIds.isEmpty() &&
                            !viewerVisible &&
                            navigationTransitionIdle &&
                            !adaptivePolicy.useNavigationRail
                        ) {
                            GalleryBottomBar(
                                selectedTab = selectedTab,
                                visible = bottomNavigationVisible,
                                onTabSelected = { tab ->
                                    selectedMediaIds = emptySet()
                                    bottomNavigationVisible = true
                                    navigationViewModel.openTab(tab)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (
                            destination == GalleryDestination.Main &&
                            selectedMediaIds.isEmpty() &&
                            !viewerVisible &&
                            navigationTransitionIdle &&
                            adaptivePolicy.useNavigationRail
                        ) {
                            GalleryNavigationRail(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    selectedMediaIds = emptySet()
                                    navigationViewModel.openTab(tab)
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AnimatedContent(
                                targetState = destination,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    val usesAlbumHero = initialState == GalleryDestination.AlbumDetail ||
                                        targetState == GalleryDestination.AlbumDetail
                                    when {
                                        usesAlbumHero -> {
                                            EnterTransition.None togetherWith ExitTransition.None
                                        }
                                        targetState == GalleryDestination.Main -> {
                                            (
                                                fadeIn(tween(GalleryMotion.SecondaryCloseMillis)) +
                                                    slideInHorizontally(
                                                        animationSpec = tween(GalleryMotion.SecondaryCloseMillis),
                                                        initialOffsetX = { -it / 14 }
                                                    )
                                                ) togetherWith (
                                                fadeOut(tween(150)) +
                                                    slideOutHorizontally(
                                                        animationSpec = tween(GalleryMotion.SecondaryCloseMillis),
                                                        targetOffsetX = { it / 10 }
                                                    )
                                                )
                                        }
                                        else -> {
                                            (
                                                fadeIn(tween(GalleryMotion.SecondaryOpenMillis)) +
                                                    slideInHorizontally(
                                                        animationSpec = tween(GalleryMotion.SecondaryOpenMillis),
                                                        initialOffsetX = { it / 10 }
                                                    )
                                                ) togetherWith (
                                                fadeOut(tween(150)) +
                                                    slideOutHorizontally(
                                                        animationSpec = tween(GalleryMotion.SecondaryOpenMillis),
                                                        targetOffsetX = { -it / 14 }
                                                    )
                                                )
                                        }
                                    }
                                },
                                contentKey = { it },
                                label = "gallery destination"
                            ) { animatedDestination ->
                                when (animatedDestination) {
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
                                    beyondViewportPageCount = 2,
                                    userScrollEnabled = navigationTransitionIdle,
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
                                                gridColumns = adaptivePhotoColumns,
                                                listState = photosListState,
                                                revealMediaId = viewerRevealMediaId,
                                                selectedMediaIds = selectedMediaIds,
                                                onMediaLongClick = ::toggleMediaSelection,
                                                onMediaSelectionToggle = ::toggleMediaSelection,
                                                onSelectionClear = ::clearMediaSelection,
                                                onSelectAllVisible = { selectMedia(searchedVisibleMedia) },
                                                onDeleteSelected = ::deleteSelectedMedia,
                                                onShareSelected = ::shareSelectedMedia,
                                                onHideSelected = ::hideSelectedMedia,
                                                onRefresh = mediaViewModel::requestQuickRefresh,
                                                onOpenSettings = { showSettingsDialog = true },
                                                onMediaBoundsChanged = { mediaItem, bounds ->
                                                    if (bounds.isUsableTransitionBounds()) {
                                                        mediaTileBounds[mediaItem.id] = bounds
                                                    }
                                                },
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
                                                        sharedElementKeyPrefix = sharedElementKeyPrefix,
                                                        sourceGridColumns = adaptivePhotoColumns
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
                                                onCreateAlbum = ::startAlbumCreator,
                                                onOpenSettings = { showSettingsDialog = true },
                                                hiddenAlbumCount = hiddenAlbumCount,
                                                hiddenItemCount = hiddenAlbumItemCount,
                                                lockedItemCount = lockedItemCount,
                                                onAlbumClick = { album, bounds -> startAlbumOpen(album, bounds) },
                                                onAlbumBoundsChanged = { album, bounds ->
                                                    if (bounds.isUsableTransitionBounds()) {
                                                        albumTileBounds[album.id] = bounds
                                                    }
                                                },
                                                contentPadding = innerPadding,
                                                listState = albumsListState,
                                                activeTransitionAlbumId = albumTransition
                                                    ?.takeIf {
                                                        it.mode == AlbumTransitionMode.Opening &&
                                                            albumTransitionCommittedKey == it.key
                                                    }
                                                    ?.album?.id,
                                                mediaAccessNotice = mediaAccessNotice,
                                                isLoading = isLoadingMedia,
                                                searchQuery = gallerySearchQuery,
                                                onSearchQueryChange = { gallerySearchQuery = it },
                                                bigTileColumns = adaptivePolicy.bigAlbumColumns,
                                                basicTileColumns = adaptivePolicy.basicAlbumColumns,
                                                heroHeight = adaptiveHeroHeight
                                            )
                                            GalleryTab.Menu -> GalleryMenuScreen(
                                                contentPadding = innerPadding,
                                                maxContentWidth = if (adaptivePolicy.useNavigationRail) 720.dp else androidx.compose.ui.unit.Dp.Unspecified,
                                                onOpenHiddenItems = ::openHiddenItems,
                                                onOpenLockedMedia = ::openLockedMedia,
                                                onOpenRecentlyDeleted = ::openRecentlyDeleted,
                                                onOpenDocuments = ::openDocuments,
                                                onOpenSettings = { showSettingsDialog = true },
                                                onOpenCleanup = ::openCleanup
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
                                        listState = albumDetailListState,
                                        revealMediaId = viewerRevealMediaId,
                                        columnBoost = adaptivePolicy.albumDetailColumnBoost,
                                        onBack = ::closeAlbumDetail,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = galleryVisibilityScope,
                                        sharedBoundsTransform = GalleryMediaBoundsTransform,
                                        activeSharedElementKey = viewerSharedElementKey ?: mediaOpenTransition?.sharedElementKey,
                                        albumEnterProgress = 1f,
                                        gridMode = albumDetailGridModes[selectedAlbum.id] ?: defaultAlbumGridMode,
                                        onGridModeChange = { gridMode -> albumDetailGridModes[selectedAlbum.id] = gridMode },
                                        selectedMediaIds = selectedMediaIds,
                                        onMediaLongClick = ::toggleMediaSelection,
                                        onMediaSelectionToggle = ::toggleMediaSelection,
                                        onSelectionClear = ::clearMediaSelection,
                                        onSelectAllVisible = { selectMedia(selectedAlbumMedia) },
                                        onDeleteSelected = ::deleteSelectedMedia,
                                        onShareSelected = ::shareSelectedMedia,
                                        onHideSelected = ::hideSelectedMedia,
                                        onHideAlbum = { hideAlbumAndReturn(selectedAlbum) },
                                        onMediaBoundsChanged = { mediaItem, bounds ->
                                            if (bounds.isUsableTransitionBounds()) {
                                                mediaTileBounds[mediaItem.id] = bounds
                                            }
                                        },
                                        onMediaClick = { mediaItem, bounds, sharedElementKey, sharedElementKeyPrefix ->
                                            val selectedGridColumns = when (albumDetailGridModes[selectedAlbum.id] ?: defaultAlbumGridMode) {
                                                AlbumDetailGridMode.Compact -> 4
                                                AlbumDetailGridMode.Comfortable -> 3
                                            } + adaptivePolicy.albumDetailColumnBoost
                                            startViewerOpen(
                                                mediaItem = mediaItem,
                                                mediaItems = selectedAlbumMedia,
                                                bounds = bounds,
                                                sharedElementKey = sharedElementKey,
                                                sharedElementKeyPrefix = sharedElementKeyPrefix,
                                                sourceGridColumns = selectedGridColumns
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
                                onBack = { returnFromOverlay(GalleryTab.Albums) },
                                onHiddenChange = { album, hidden ->
                                    updateAlbumHidden(album, hidden)
                                },
                                contentPadding = PaddingValues()
                            )
                            GalleryDestination.LockedMedia -> LockedMediaScreen(
                                lockedMediaItems = lockedGridMedia,
                                isUnlocked = hiddenVaultUnlocked,
                                hasPin = hasHiddenPin,
                                biometricAvailable = biometricAvailable,
                                authMessage = hiddenAuthMessage,
                                authInProgress = lockedSecurityUiState.operationInProgress,
                                onBack = ::closeLockedMedia,
                                onPinCreated = lockedSecurityViewModel::createPin,
                                onPinUnlock = lockedSecurityViewModel::unlockWithPin,
                                onBiometricUnlock = ::requestHiddenBiometricUnlock,
                                onUnhideSelected = { selectedItems ->
                                    val fullItemsById = lockedDisplayMedia.associateBy { it.id }
                                    unhideMediaItems(selectedItems.map { fullItemsById[it.id] ?: it })
                                },
                                onDeleteSelected = { selectedItems ->
                                    val fullItemsById = lockedDisplayMedia.associateBy { it.id }
                                    softDeleteLockedMediaItems(selectedItems.map { fullItemsById[it.id] ?: it })
                                },
                                onOpenMedia = { mediaItem, bounds ->
                                    val lockedViewerItems = lockedDisplayMedia
                                    val lockedViewerItem = lockedViewerItems.firstOrNull { it.id == mediaItem.id }
                                        ?: mediaItem
                                    startViewerOpen(
                                        mediaItem = lockedViewerItem,
                                        mediaItems = lockedViewerItems,
                                        bounds = bounds,
                                        actionMode = ViewerActionMode.Locked,
                                        transitionMediaItem = mediaItem,
                                        sourceGridColumns = adaptivePolicy.utilityGridColumns
                                    )
                                },
                                contentPadding = PaddingValues(),
                                gridColumns = adaptivePolicy.utilityGridColumns
                            )
                            GalleryDestination.Documents -> DocumentPhotosScreen(
                                matches = documentPhotosUiState.matches,
                                scanning = documentPhotosUiState.scanning,
                                scannedCount = documentPhotosUiState.scannedCount,
                                totalCount = documentPhotosUiState.totalCount,
                                errorMessage = documentPhotosUiState.errorMessage,
                                onBack = { returnFromOverlay(GalleryTab.Menu) },
                                onRescan = documentPhotosViewModel::rescan,
                                onOpenMedia = { mediaItem, mediaItems, bounds ->
                                    startViewerOpen(
                                        mediaItem = mediaItem,
                                        mediaItems = mediaItems,
                                        bounds = bounds,
                                        sourceGridColumns = adaptivePolicy.utilityGridColumns
                                    )
                                },
                                gridColumns = adaptivePolicy.utilityGridColumns,
                                maxContentWidth = if (adaptivePolicy.useNavigationRail) 760.dp else androidx.compose.ui.unit.Dp.Unspecified,
                                contentPadding = PaddingValues()
                            )
                            GalleryDestination.RecentlyDeleted -> RecentlyDeletedScreen(
                                deletedItems = recentlyDeletedItems,
                                onBack = { returnFromOverlay(GalleryTab.Albums) },
                                onOpenMedia = { entry, bounds ->
                                    startViewerOpen(
                                        mediaItem = entry.mediaItem,
                                        mediaItems = recentlyDeletedItems.map { it.mediaItem },
                                        bounds = bounds,
                                        actionMode = ViewerActionMode.RecentlyDeleted,
                                        sourceGridColumns = adaptivePolicy.utilityGridColumns
                                    )
                                },
                                onRestoreSelected = ::restoreDeletedMediaItems,
                                onDeleteForeverSelected = ::deleteDeletedMediaItems,
                                contentPadding = PaddingValues(),
                                gridColumns = adaptivePolicy.utilityGridColumns
                            )
                            GalleryDestination.AlbumCreator -> {
                                val albumName = pendingAlbumName
                                if (albumName != null) {
                                    AlbumMediaPickerScreen(
                                        albumName = albumName,
                                        mediaItems = visibleMedia,
                                        selectedMediaIds = albumCreationSelectedIds,
                                        onToggleMedia = ::toggleAlbumCreationMedia,
                                        onSelectAll = {
                                            albumCreationSelectedIds = visibleMedia.map { it.id }.toSet()
                                        },
                                        onCancel = ::cancelAlbumCreator,
                                        onMoveSelected = {
                                            val selectedItems = visibleMedia.filter {
                                                it.id in albumCreationSelectedIds
                                            }
                                            if (selectedItems.isNotEmpty()) {
                                                launchMediaStoreWrite(
                                                    PendingMediaStoreWriteAction(
                                                        mode = MediaStoreWriteMode.MoveToAlbum,
                                                        mediaItems = selectedItems,
                                                        targetAlbumName = albumName
                                                    )
                                                )
                                            }
                                        },
                                        gridColumns = adaptivePolicy.utilityGridColumns
                                    )
                                }
                            }
                            GalleryDestination.PhotoEditor -> {
                                editingMediaItem?.let { mediaItem ->
                                    PhotoEditorScreen(
                                        mediaItem = mediaItem,
                                        repository = photoEditorRepository,
                                        useCompactLayout = adaptivePolicy.useCompactEditorLayout,
                                        onBack = ::closePhotoEditor,
                                        onSaved = {
                                            mediaViewModel.requestFullRefresh()
                                            closePhotoEditor()
                                        }
                                    )
                                }
                            }
                            GalleryDestination.Cleanup -> GalleryCleanupScreen(
                                mediaItems = visibleMedia,
                                maxContentWidth = if (adaptivePolicy.useNavigationRail) 920.dp else androidx.compose.ui.unit.Dp.Unspecified,
                                onBack = { returnFromOverlay(GalleryTab.Menu) },
                                onOpenMedia = { mediaItem, mediaItems, bounds ->
                                    startViewerOpen(
                                        mediaItem = mediaItem,
                                        mediaItems = mediaItems,
                                        bounds = bounds,
                                        sourceGridColumns = 4
                                    )
                                },
                                onTrashMedia = { mediaItems ->
                                    launchMediaStoreWrite(PendingMediaStoreWriteAction(MediaStoreWriteMode.Trash, mediaItems))
                                }
                            )
                        }
                            }
                    }
                }
                }

                PositionAwareAlbumTransitionOverlay(
                    transition = albumTransition,
                    contentReady = albumTransition?.key == albumWarmupReadyKey,
                    committed = albumTransition?.let {
                        it.mode != AlbumTransitionMode.Opening ||
                            albumTransitionCommittedKey == it.key
                    } == true,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    rootBoundsInWindow = transitionRootBoundsInWindow,
                    onCommitted = { committedTransition ->
                        if (albumTransition?.key == committedTransition.key) {
                            albumTransitionCommittedKey = committedTransition.key
                        }
                    },
                    onFinished = { finishedTransition ->
                        if (albumTransition?.key == finishedTransition.key) {
                            if (finishedTransition.mode == AlbumTransitionMode.Opening) {
                                albumTransitionAwaitingDestinationKey = finishedTransition.key
                                navigationViewModel.showAlbumDetail()
                            } else {
                                albumTransition = null
                                albumTransitionCommittedKey = -1
                            }
                        }
                    }
                ) { overlayAlbum, _ ->
                    AlbumDetailTransitionPreview(
                        album = overlayAlbum,
                        mediaItems = if (overlayAlbum.id == selectedAlbumId) selectedAlbumMedia else mediaForAlbumFast(overlayAlbum, visibleMedia, visibleMediaByAlbum, favoriteMedia),
                        contentPadding = PaddingValues(),
                        gridMode = albumDetailGridModes[overlayAlbum.id] ?: defaultAlbumGridMode,
                        columnBoost = adaptivePolicy.albumDetailColumnBoost
                    )
                }
                ReferenceMediaOpenOverlay(
                    transition = mediaOpenTransition,
                    contentReady = mediaOpenTransition?.key == mediaOpenWarmupReadyKey,
                    photoDecodeSize = viewerPhotoDecodeSize,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    rootBoundsInWindow = transitionRootBoundsInWindow,
                    onFinished = { finishedTransition ->
                        if (mediaOpenTransition?.key == finishedTransition.key) {
                            finishViewerOpen(
                                mediaItem = finishedTransition.mediaItem,
                                mediaItems = finishedTransition.mediaItems,
                                sharedElementKey = finishedTransition.sharedElementKey,
                                sharedElementKeyPrefix = finishedTransition.sharedElementKeyPrefix,
                                actionMode = finishedTransition.actionMode
                            )
                            mediaOpenTransition = null
                        }
                    }
                )
                ReferenceMediaCloseOverlay(
                    transition = mediaCloseTransition,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    rootBoundsInWindow = transitionRootBoundsInWindow,
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
                    onRestore = ::restoreViewerMedia,
                    actionMode = viewerActionMode,
                    favoriteMediaIds = favoriteMediaIds,
                    onFavoriteChange = ::setMediaFavorite,
                    onEdit = ::editPhoto,
                    onCurrentMediaChanged = { currentItem ->
                        viewerMediaItem = currentItem
                        viewerSharedElementKey = viewerSharedElementKeyPrefix?.let { "$it-media-${currentItem.id}" }
                        mediaTileBounds[currentItem.id]?.takeIf { it != Rect.Zero }?.let { bounds ->
                            viewerReturnFallbackBounds = bounds
                        }
                    },
                    albumNameForMedia = { item -> albumNameById[item.albumId] },
                    autoplayVideos = settings.autoplayVideos,
                    startVideosMuted = settings.startVideosMuted,
                    preferredPhotoDecodeSize = viewerPhotoDecodeSize,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    sharedBoundsTransform = GalleryMediaBoundsTransform,
                    sharedElementKeyPrefix = viewerSharedElementKeyPrefix,
                    predictiveBackProgressProvider = {
                        if (predictiveBackGestureAction == GalleryBackAction.CloseViewer) {
                            predictiveBackProgress.value
                        } else {
                            0f
                        }
                    },
                    predictiveBackDirectionProvider = {
                        if (predictiveBackSwipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                    }
                )
            }

            pendingLockConfirmation?.let { pending ->
                val itemCount = pending.mediaItems.size
                AlertDialog(
                    onDismissRequest = { pendingLockConfirmation = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null
                        )
                    },
                    title = {
                        Text(if (itemCount == 1) "Lock this item?" else "Lock %1$,d items?".format(itemCount))
                    },
                    text = {
                        Text(
                            if (itemCount == 1) {
                                "This item will move to Locked Media and will no longer appear in your gallery. Android may ask you to confirm removing the original."
                            } else {
                                "These items will move to Locked Media and will no longer appear in your gallery. Android may ask you to confirm removing the originals."
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { confirmLockMedia(pending) }) {
                            Text("Lock")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingLockConfirmation = null }) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            if (showSettingsDialog) {
                GallerySettingsDialog(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
private fun GallerySettingsDialog(
    settings: GallerySettings,
    onSettingsChange: (GallerySettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsSectionTitle("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryThemeMode.entries.forEach { mode ->
                        SettingsChoiceChip(
                            label = mode.label(),
                            selected = settings.themeMode == mode,
                            onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SettingsSectionTitle("Grid")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GalleryGridDensity.entries.forEach { density ->
                        SettingsChoiceRow(
                            label = density.label(),
                            description = String.format(Locale.getDefault(), "%1\$d columns in Photos", density.photoColumns),
                            selected = settings.gridDensity == density,
                            onClick = { onSettingsChange(settings.copy(gridDensity = density)) }
                        )
                    }
                }

                SettingsSectionTitle("Video")
                SettingsSwitchRow(
                    label = "Autoplay videos",
                    description = "Start the active video when it opens",
                    checked = settings.autoplayVideos,
                    onCheckedChange = { checked -> onSettingsChange(settings.copy(autoplayVideos = checked)) }
                )
                SettingsSwitchRow(
                    label = "Start muted",
                    description = "Open videos with volume at zero",
                    checked = settings.startVideosMuted,
                    onCheckedChange = { checked -> onSettingsChange(settings.copy(startVideosMuted = checked)) }
                )

                SettingsSectionTitle("Performance")
                SettingsSwitchRow(
                    label = "Smooth mode",
                    description = "Ask Android for the highest available refresh rate",
                    checked = settings.performanceMode,
                    onCheckedChange = { checked -> onSettingsChange(settings.copy(performanceMode = checked)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.bouncyClickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun GalleryThemeMode.label(): String {
    return when (this) {
        GalleryThemeMode.System -> "System"
        GalleryThemeMode.Light -> "Light"
        GalleryThemeMode.Dark -> "Dark"
    }
}

private fun GalleryGridDensity.label(): String {
    return when (this) {
        GalleryGridDensity.Compact -> "Compact"
        GalleryGridDensity.Comfortable -> "Comfort"
        GalleryGridDensity.Spacious -> "Spacious"
    }
}

private fun GalleryGridDensity.defaultAlbumGridMode(): AlbumDetailGridMode {
    return when (this) {
        GalleryGridDensity.Compact -> AlbumDetailGridMode.Compact
        GalleryGridDensity.Comfortable,
        GalleryGridDensity.Spacious -> AlbumDetailGridMode.Comfortable
    }
}
@Composable
private fun GalleryMenuScreen(
    contentPadding: PaddingValues,
    maxContentWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    onOpenHiddenItems: () -> Unit,
    onOpenLockedMedia: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCleanup: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePadding = if (
            maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified && maxWidth > maxContentWidth
        ) {
            (maxWidth - maxContentWidth) / 2 + 26.dp
        } else {
            26.dp
        }
        val topPadding = if (maxWidth > maxHeight) 36.dp else 104.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = sidePadding,
                    top = topPadding,
                    end = sidePadding,
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
            color = MaterialTheme.colorScheme.surface,
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
                    icon = Icons.Filled.Description,
                    label = "Document photos",
                    description = "Find bills, menus, letters, forms and text-heavy pictures",
                    onClick = onOpenDocuments,
                    showDivider = true
                )
                GalleryMenuRow(
                    icon = Icons.Filled.CleaningServices,
                    label = "Cleanup",
                    description = "Review duplicate candidates and large files",
                    onClick = onOpenCleanup,
                    showDivider = true
                )
                GalleryMenuRow(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    description = "Theme, layout, playback and performance",
                    onClick = onOpenSettings,
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
}

@Composable
private fun GalleryCleanupScreen(
    mediaItems: List<MediaItem>,
    maxContentWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    onBack: () -> Unit,
    onTrashMedia: (List<MediaItem>) -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit
) {
    val duplicateGroups = remember(mediaItems) { potentialDuplicateGroups(mediaItems) }
    val largeMedia = remember(mediaItems) {
        mediaItems
            .filter { (it.fileSizeBytes ?: 0L) >= LargeMediaThresholdBytes }
            .sortedByDescending { it.fileSizeBytes ?: 0L }
            .take(40)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (
            maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified && maxWidth > maxContentWidth
        ) {
            (maxWidth - maxContentWidth) / 2 + 22.dp
        } else {
            22.dp
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 40.dp,
                end = horizontalPadding,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item(key = "cleanup_header") {
            Column {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Cleanup",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Review candidates before Android moves anything to trash.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        item(key = "duplicates_title") {
            CleanupSectionHeader(
                title = "Potential duplicates",
                detail = if (duplicateGroups.isEmpty()) "No exact metadata matches found" else "${duplicateGroups.size} groups"
            )
        }
        if (duplicateGroups.isEmpty()) {
            item(key = "duplicates_empty") { CleanupEmptyCard("Your library does not have obvious duplicate candidates.") }
        } else {
            items(items = duplicateGroups, key = { group -> "duplicate-${group.first().id}" }) { group ->
                DuplicateCleanupCard(
                    group = group,
                    onOpenMedia = { mediaItem, bounds -> onOpenMedia(mediaItem, group, bounds) },
                    onTrashMedia = onTrashMedia
                )
            }
        }
        item(key = "large_title") {
            Spacer(Modifier.height(8.dp))
            CleanupSectionHeader(
                title = "Large files",
                detail = if (largeMedia.isEmpty()) "Nothing over 25 MB" else "${largeMedia.size} items over 25 MB"
            )
        }
        if (largeMedia.isEmpty()) {
            item(key = "large_empty") { CleanupEmptyCard("No unusually large photos or videos were found.") }
        } else {
            items(items = largeMedia, key = { mediaItem -> "large-${mediaItem.id}" }) { mediaItem ->
                LargeCleanupCard(
                    mediaItem = mediaItem,
                    onOpenMedia = { bounds -> onOpenMedia(mediaItem, largeMedia, bounds) },
                    onTrash = { onTrashMedia(listOf(mediaItem)) }
                )
            }
        }
        }
    }
}

@Composable
private fun CleanupSectionHeader(title: String, detail: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CleanupEmptyCard(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CleanupActionCard(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun DuplicateCleanupCard(
    group: List<MediaItem>,
    onOpenMedia: (MediaItem, Rect) -> Unit,
    onTrashMedia: (List<MediaItem>) -> Unit
) {
    var keepMediaId by remember(group.map { it.id }) { mutableStateOf(group.first().id) }
    val extras = group.filterNot { it.id == keepMediaId }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${group.size} matching items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tap a thumbnail to inspect it, then choose the copy to keep.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(group, key = { "cleanup-preview-${it.id}" }) { mediaItem ->
                    Column(
                        modifier = Modifier.width(104.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MediaThumbnail(
                            mediaItem = mediaItem,
                            modifier = Modifier.size(96.dp),
                            cornerRadius = 14.dp,
                            selected = mediaItem.id == keepMediaId,
                            onClickWithBounds = { bounds -> onOpenMedia(mediaItem, bounds) }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mediaItem.id == keepMediaId,
                                onClick = { keepMediaId = mediaItem.id }
                            )
                            Text("Keep", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cleanupFileSize(group.sumOf { it.fileSizeBytes ?: 0L })} total",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    enabled = extras.isNotEmpty(),
                    onClick = { onTrashMedia(extras) }
                ) {
                    Text("Trash ${extras.size}")
                }
            }
        }
    }
}

@Composable
private fun LargeCleanupCard(
    mediaItem: MediaItem,
    onOpenMedia: (Rect) -> Unit,
    onTrash: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnail(
                mediaItem = mediaItem,
                modifier = Modifier.size(78.dp),
                cornerRadius = 15.dp,
                onClickWithBounds = onOpenMedia
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${cleanupFileSize(mediaItem.fileSizeBytes ?: 0L)} ? ${if (mediaItem.isVideo) "Video" else "Photo"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onTrash) { Text("Trash") }
        }
    }
}

private data class DuplicateMediaSignature(
    val size: Long,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long?,
    val mimeType: String?,
    val isVideo: Boolean
)

private fun potentialDuplicateGroups(mediaItems: List<MediaItem>): List<List<MediaItem>> {
    return mediaItems
        .filter { (it.fileSizeBytes ?: 0L) > 0L }
        .groupBy { mediaItem ->
            DuplicateMediaSignature(
                size = mediaItem.fileSizeBytes ?: 0L,
                width = mediaItem.width,
                height = mediaItem.height,
                durationMillis = mediaItem.durationMillis,
                mimeType = mediaItem.mimeType,
                isVideo = mediaItem.isVideo
            )
        }
        .values
        .filter { it.size > 1 }
        .sortedByDescending { group -> group.drop(1).sumOf { it.fileSizeBytes ?: 0L } }
}

private fun cleanupFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024.0) return String.format(Locale.getDefault(), "%.1f KB", kilobytes)
    val megabytes = kilobytes / 1024.0
    if (megabytes < 1024.0) return String.format(Locale.getDefault(), "%.1f MB", megabytes)
    return String.format(Locale.getDefault(), "%.1f GB", megabytes / 1024.0)
}

private const val LargeMediaThresholdBytes = 25L * 1024L * 1024L

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

private fun mediaForAlbumFast(
    album: Album,
    visibleMedia: List<MediaItem>,
    visibleMediaByAlbum: Map<String, List<MediaItem>>,
    favoriteMedia: List<MediaItem>
): List<MediaItem> {
    return when {
        album.id == FavoritesAlbumId -> favoriteMedia
        album.isAllPhotos -> visibleMedia
        else -> visibleMediaByAlbum[album.id].orEmpty()
    }
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

@Composable
private fun PositionAwareAlbumTransitionOverlay(
    transition: AlbumTransitionSpec?,
    contentReady: Boolean,
    committed: Boolean,
    rootWidthPx: Float,
    rootHeightPx: Float,
    rootBoundsInWindow: Rect,
    onCommitted: (AlbumTransitionSpec) -> Unit,
    onFinished: (AlbumTransitionSpec) -> Unit,
    content: @Composable (Album, Float) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val sourceBounds = transition.tileBounds.relativeToRoot(
        rootBoundsInWindow = rootBoundsInWindow,
        fallbackRootWidth = rootWidthPx,
        fallbackRootHeight = rootHeightPx
    )
    if (!sourceBounds.isUsableTransitionBounds()) {
        LaunchedEffect(transition.key) {
            onFinished(transition)
        }
        return
    }
    if (transition.mode == AlbumTransitionMode.Opening && !committed) {
        SideEffect { onCommitted(transition) }
        return
    }

    val progress = remember(transition.key) {
        Animatable(if (transition.mode == AlbumTransitionMode.Closing) 1f else 0f)
    }
    val latestContentReady by rememberUpdatedState(contentReady)

    LaunchedEffect(transition.key) {
        val targetValue = if (transition.mode == AlbumTransitionMode.Closing) 0f else 1f
        progress.animateTo(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = if (transition.mode == AlbumTransitionMode.Opening) {
                    GalleryMotion.AlbumOpenMillis
                } else {
                    GalleryMotion.AlbumCloseMillis
                },
                easing = FastOutSlowInEasing
            )
        )
        if (transition.mode == AlbumTransitionMode.Opening && !latestContentReady) {
            withTimeoutOrNull(180L) {
                snapshotFlow { latestContentReady }.first { it }
            }
        }
        onFinished(transition)
    }

    val density = LocalDensity.current
    val expansion = progress.value.coerceIn(0f, 1f)
    val approachProgress = GalleryMotion.smoothstep(0f, 0.22f, expansion)
    val revealProgress = GalleryMotion.smoothstep(0.18f, 1f, expansion)
    val rootBounds = Rect(0f, 0f, rootWidthPx, rootHeightPx)
    val towardCenter = Offset(
        x = (rootBounds.center.x - sourceBounds.center.x) * 0.12f * approachProgress,
        y = (rootBounds.center.y - sourceBounds.center.y) * 0.08f * approachProgress
    )
    val approachedBounds = scaledRectAroundCenter(
        rect = sourceBounds,
        scale = lerp(1f, 1.055f, approachProgress),
        offset = towardCenter
    )
    val heroBounds = lerpRect(approachedBounds, rootBounds, revealProgress)
    val heroWidth = heroBounds.width.coerceAtLeast(1f)
    val heroHeight = heroBounds.height.coerceAtLeast(1f)
    val detailAlpha = GalleryMotion.smoothstep(0.34f, 0.68f, expansion)
    val coverAlpha = 1f - GalleryMotion.smoothstep(0.22f, 0.58f, expansion)
    val cornerRadius = lerp(22f, 0f, revealProgress).dp
    val scrimAlpha = GalleryMotion.smoothstep(0.04f, 0.70f, expansion) * 0.18f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { heroWidth.toDp() })
                .height(with(density) { heroHeight.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = heroBounds.left
                    translationY = heroBounds.top
                    shadowElevation = with(density) {
                        22.dp.toPx() * approachProgress * (1f - revealProgress)
                    }
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                }
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(with(density) { rootWidthPx.toDp() })
                    .requiredHeight(with(density) { rootHeightPx.toDp() })
                    .graphicsLayer {
                        alpha = detailAlpha
                        translationX = (heroWidth - rootWidthPx) / 2f
                        translationY = (heroHeight - rootHeightPx) / 2f
                    }
            ) {
                content(transition.album, expansion)
            }
            GalleryImage(
                imageRes = transition.album.coverRes,
                imageUri = transition.album.coverUri,
                contentDescription = transition.album.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha },
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = 512,
                loadQuality = ImageLoadQuality.Thumbnail,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                cachedOnly = true
            )
        }
    }
}

private val ReferenceViewerOpenEasing = CubicBezierEasing(0.16f, 0.82f, 0.22f, 1f)
private val ReferenceViewerCloseEasing = CubicBezierEasing(0.30f, 0f, 0.58f, 1f)

@Composable
private fun ReferenceMediaOpenOverlay(
    transition: MediaOpenTransitionSpec?,
    contentReady: Boolean,
    photoDecodeSize: Int,
    rootWidthPx: Float,
    rootHeightPx: Float,
    rootBoundsInWindow: Rect,
    onFinished: (MediaOpenTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val tileBounds = transition.tileBounds.relativeToRoot(
        rootBoundsInWindow = rootBoundsInWindow,
        fallbackRootWidth = rootWidthPx,
        fallbackRootHeight = rootHeightPx
    )
    if (!tileBounds.isUsableTransitionBounds()) {
        LaunchedEffect(transition.key) {
            onFinished(transition)
        }
        return
    }
    val progress = remember(transition.key) { Animatable(0f) }
    val latestContentReady by rememberUpdatedState(contentReady)

    LaunchedEffect(transition.key) {
        if (!latestContentReady) {
            snapshotFlow { latestContentReady }.first { it }
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = GalleryMotion.ViewerHeroOpenMillis,
                easing = ReferenceViewerOpenEasing
            )
        )
        onFinished(transition)
    }

    val heroThumbnailSize = if (
        transition.actionMode == ViewerActionMode.Locked || transition.mediaItem.isVideo
    ) 512 else photoDecodeSize
    val decodedHeroBitmap = transition.transitionMediaItem.contentUri?.let { uri ->
        ThumbnailMemoryCache.get(ThumbnailMemoryCache.key(uri, heroThumbnailSize))
    }
    val geometryMediaItem = decodedHeroBitmap?.let { bitmap ->
        transition.mediaItem.copy(width = bitmap.width, height = bitmap.height)
    } ?: transition.mediaItem
    val expansion = progress.value.coerceIn(0f, 1f)
    ReferenceMediaHeroFrame(
        mediaItem = transition.transitionMediaItem,
        startBounds = tileBounds,
        endBounds = fittedMediaRect(rootWidthPx, rootHeightPx, geometryMediaItem),
        progress = expansion,
        backdropAlpha = GalleryMotion.smoothstep(0f, 0.82f, expansion),
        thumbnailSize = heroThumbnailSize,
        loadQuality = if (
            transition.actionMode == ViewerActionMode.Locked || transition.mediaItem.isVideo
        ) ImageLoadQuality.Thumbnail else ImageLoadQuality.HighQuality,
        cachedOnly = false
    )
}

@Composable
private fun ReferenceMediaCloseOverlay(
    transition: MediaCloseTransitionSpec?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    rootBoundsInWindow: Rect,
    onFinished: (MediaCloseTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val tileBounds = transition.tileBounds.relativeToRoot(
        rootBoundsInWindow = rootBoundsInWindow,
        fallbackRootWidth = rootWidthPx,
        fallbackRootHeight = rootHeightPx
    )
    if (!tileBounds.isUsableTransitionBounds()) {
        LaunchedEffect(transition.key) {
            onFinished(transition)
        }
        return
    }
    val progress = remember(transition.key) { Animatable(0f) }

    LaunchedEffect(transition.key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = GalleryMotion.ViewerHeroCloseMillis,
                easing = ReferenceViewerCloseEasing
            )
        )
        onFinished(transition)
    }

    val collapse = progress.value.coerceIn(0f, 1f)
    val startBounds = scaledRectAroundCenter(
        rect = fittedMediaRect(rootWidthPx, rootHeightPx, transition.mediaItem),
        scale = transition.startScale.coerceIn(0.68f, 1f),
        offset = transition.startOffset
    )
    ReferenceMediaHeroFrame(
        mediaItem = transition.mediaItem,
        startBounds = startBounds,
        endBounds = tileBounds,
        progress = collapse,
        backdropAlpha = transition.startBackdropAlpha *
            (1f - GalleryMotion.smoothstep(0.12f, 0.92f, collapse))
    )
}

@Composable
private fun ReferenceMediaHeroFrame(
    mediaItem: MediaItem,
    startBounds: Rect,
    endBounds: Rect,
    progress: Float,
    backdropAlpha: Float,
    thumbnailSize: Int = 512,
    loadQuality: ImageLoadQuality = ImageLoadQuality.Thumbnail,
    cachedOnly: Boolean = true
) {
    val density = LocalDensity.current
    val fraction = progress.coerceIn(0f, 1f)
    val heroBounds = lerpRect(startBounds, endBounds, fraction)
    val heroWidth = heroBounds.width.coerceAtLeast(1f)
    val heroHeight = heroBounds.height.coerceAtLeast(1f)
    val cornerRadius = with(density) {
        (3.dp.toPx() * (1f - GalleryMotion.smoothstep(0f, 0.65f, fraction))).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha.coerceIn(0f, 1f)))
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { heroWidth.toDp() })
                .height(with(density) { heroHeight.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = heroBounds.left
                    translationY = heroBounds.top
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                }
                .background(Color.Black)
        ) {
            GalleryImage(
                imageRes = mediaItem.imageRes,
                imageUri = mediaItem.contentUri,
                contentDescription = mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = thumbnailSize,
                loadQuality = loadQuality,
                backgroundColor = Color.Black,
                cachedOnly = cachedOnly
            )
        }
    }
}

private fun Rect.isUsableTransitionBounds(): Boolean {
    return left.isFinite() &&
        top.isFinite() &&
        right.isFinite() &&
        bottom.isFinite() &&
        width >= 4f &&
        height >= 4f
}

private fun Rect.isVisibleWithin(viewport: Rect): Boolean {
    if (!isUsableTransitionBounds()) return false
    if (!viewport.isUsableTransitionBounds()) return true
    return right > viewport.left &&
        left < viewport.right &&
        bottom > viewport.top &&
        top < viewport.bottom
}

private fun Rect.relativeToRoot(
    rootBoundsInWindow: Rect,
    fallbackRootWidth: Float,
    fallbackRootHeight: Float
): Rect {
    if (!isUsableTransitionBounds()) return Rect.Zero

    val rootWidth = fallbackRootWidth.coerceAtLeast(1f)
    val rootHeight = fallbackRootHeight.coerceAtLeast(1f)
    val rootBoundsAreUsable = rootBoundsInWindow.isUsableTransitionBounds()
    val originX = if (rootBoundsAreUsable) rootBoundsInWindow.left else 0f
    val originY = if (rootBoundsAreUsable) rootBoundsInWindow.top else 0f
    val width = width.coerceIn(1f, rootWidth)
    val height = height.coerceIn(1f, rootHeight)
    val left = (this.left - originX).coerceIn(0f, (rootWidth - width).coerceAtLeast(0f))
    val top = (this.top - originY).coerceIn(0f, (rootHeight - height).coerceAtLeast(0f))
    return Rect(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height
    )
}

private fun lerpRect(start: Rect, stop: Rect, fraction: Float): Rect {
    return Rect(
        left = lerp(start.left, stop.left, fraction),
        top = lerp(start.top, stop.top, fraction),
        right = lerp(start.right, stop.right, fraction),
        bottom = lerp(start.bottom, stop.bottom, fraction)
    )
}

private fun fittedMediaRect(rootWidthPx: Float, rootHeightPx: Float, mediaItem: MediaItem): Rect {
    val mediaWidth = mediaItem.width?.takeIf { it > 0 }?.toFloat()
    val mediaHeight = mediaItem.height?.takeIf { it > 0 }?.toFloat()
    if (mediaWidth == null || mediaHeight == null || rootWidthPx <= 0f || rootHeightPx <= 0f) {
        return Rect(0f, 0f, rootWidthPx, rootHeightPx)
    }

    val mediaAspect = mediaWidth / mediaHeight
    val rootAspect = rootWidthPx / rootHeightPx
    return if (rootAspect > mediaAspect) {
        val height = rootHeightPx
        val width = height * mediaAspect
        val left = (rootWidthPx - width) / 2f
        Rect(left, 0f, left + width, height)
    } else {
        val width = rootWidthPx
        val height = width / mediaAspect
        val top = (rootHeightPx - height) / 2f
        Rect(0f, top, width, top + height)
    }
}

private fun scaledRectAroundCenter(rect: Rect, scale: Float, offset: Offset): Rect {
    val width = rect.width * scale
    val height = rect.height * scale
    val centerX = rect.left + rect.width / 2f + offset.x
    val centerY = rect.top + rect.height / 2f + offset.y
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f
    )
}

@Composable
private fun GalleryBottomBar(
    selectedTab: GalleryTab,
    visible: Boolean,
    onTabSelected: (GalleryTab) -> Unit
) {
    val containerShape = RoundedCornerShape(50.dp)
    val tabShape = RoundedCornerShape(40.dp)
    val tabWidth = 86.dp
    val tabHeight = 56.dp
    val tabGap = 4.dp
    val contentWidth = tabWidth * 3 + tabGap * 2
    val density = LocalDensity.current
    val hideDistancePx = with(density) { 112.dp.toPx() }
    val visibilityProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "bottom nav visibility"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .graphicsLayer {
                alpha = visibilityProgress
                translationY = hideDistancePx * (1f - visibilityProgress)
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .clip(containerShape),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = containerShape,
                tonalElevation = 0.dp,
                shadowElevation = 14.dp
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

@Composable
private fun GalleryNavigationRail(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(88.dp),
        containerColor = MaterialTheme.colorScheme.background,
        header = { Spacer(Modifier.height(44.dp)) }
    ) {
        GalleryRailItem(
            selected = selectedTab == GalleryTab.Photos,
            icon = Icons.Filled.Image,
            label = "Photos",
            onClick = { onTabSelected(GalleryTab.Photos) }
        )
        GalleryRailItem(
            selected = selectedTab == GalleryTab.Albums,
            icon = Icons.Filled.Collections,
            label = "Albums",
            onClick = { onTabSelected(GalleryTab.Albums) }
        )
        GalleryRailItem(
            selected = selectedTab == GalleryTab.Menu,
            icon = Icons.Filled.Menu,
            label = "Menu",
            onClick = { onTabSelected(GalleryTab.Menu) }
        )
    }
}

@Composable
private fun GalleryRailItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, maxLines = 1) },
        alwaysShowLabel = true
    )
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
