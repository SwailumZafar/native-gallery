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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.example.nativegallery.data.ViewerTransitionStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.example.nativegallery.data.DocumentPhotoRepository
import com.example.nativegallery.data.FavoritesRepository
import com.example.nativegallery.R
import com.example.nativegallery.data.FolderTransitionStyle
import com.example.nativegallery.data.GalleryPrivacyFilter
import com.example.nativegallery.data.GalleryGridDensity
import com.example.nativegallery.data.GallerySettings
import com.example.nativegallery.data.GalleryThemeMode
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.data.HiddenMediaRepository
import com.example.nativegallery.data.HiddenSecurityRepository
import com.example.nativegallery.data.LockedMediaOperations
import com.example.nativegallery.data.LockedMediaOriginalRemovalResult
import com.example.nativegallery.data.LockedMediaVaultPolicy
import com.example.nativegallery.data.LockedMediaVaultProvider
import com.example.nativegallery.data.LockedMediaVaultRepository
import com.example.nativegallery.data.LockedMediaVaultSnapshot
import com.example.nativegallery.data.MediaManagementAccessRepository
import com.example.nativegallery.data.MediaManagementAccessState
import com.example.nativegallery.data.MediaManagementAccessStatus
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
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.MediaAccessNotice
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.ThumbnailMemoryCache
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import com.example.nativegallery.ui.components.MotionSpecs
import com.example.nativegallery.ui.components.solarTactile
import com.example.nativegallery.ui.components.bouncyClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val sourceCornerRadiusDp: Float,
    val mode: AlbumTransitionMode,
    val sortMode: AlbumDetailSortMode = AlbumDetailSortMode.Newest,
    val firstVisibleRow: Int = 0,
    val firstVisibleRowOffset: Int = 0,
    val initialProgress: Float = if (mode == AlbumTransitionMode.Closing) 1f else 0f
)

private const val FavoritesAlbumId = "favorites"
private const val AlbumOpeningWarmRowCount = 3
private const val InitialPresentationThumbnailCount = 6
private const val InitialPresentationPrefetchCount = 12

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

internal enum class MediaStoreWriteMode {
    Trash,
    RestoreFromTrash,
    DeleteForever,
    DeleteLockedOriginals,
    MoveToAlbum
}

internal fun shouldApplyOptimisticMediaRemoval(hasMediaManagementAccess: Boolean, mode: MediaStoreWriteMode): Boolean {
    return hasMediaManagementAccess && (mode == MediaStoreWriteMode.Trash || mode == MediaStoreWriteMode.DeleteForever)
}

internal data class PendingMediaStoreWriteAction(
    val mode: MediaStoreWriteMode,
    val mediaItems: List<MediaItem>,
    val viewerDirection: Int = 1,
    val fromViewer: Boolean = false,
    val uiRemovalApplied: Boolean = false,
    val viewerIndexBeforeRemoval: Int = -1,
    val selectedMediaIdsBeforeRemoval: Set<String> = emptySet(),
    val targetAlbumName: String? = null,
    val targetAlbumRelativePath: String? = null,
    val returnToAlbumsAfterMove: Boolean = false
)

private sealed interface PendingDeleteConfirmation {
    val mediaItems: List<MediaItem>

    data class MediaStore(val action: PendingMediaStoreWriteAction) : PendingDeleteConfirmation {
        override val mediaItems: List<MediaItem> = action.mediaItems
    }

    data class Locked(
        override val mediaItems: List<MediaItem>,
        val viewerDirection: Int? = null
    ) : PendingDeleteConfirmation

    data class VaultForever(
        override val mediaItems: List<MediaItem>,
        val viewerDirection: Int? = null
    ) : PendingDeleteConfirmation
}

private data class PendingLockConfirmation(
    val mediaItems: List<MediaItem>,
    val viewerMediaId: String? = null,
    val viewerDirection: Int = 1
)
private data class PendingLockedRestoreConfirmation(
    val mediaItems: List<MediaItem>,
    val viewerDirection: Int? = null
)

private data class MediaCloseTransitionSpec(
    val key: Int,
    val mediaItem: MediaItem,
    val tileBounds: Rect,
    val startOffset: Offset,
    val startScale: Float,
    val startBackdropAlpha: Float,
    val startRotationZ: Float = 0f,
    val startRotationX: Float = 0f
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val GalleryMediaBoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = GalleryMotion.SharedBoundsMillis, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryApp(
    settings: GallerySettings,
    onInitialPresentationReady: (Boolean) -> Unit = {},
    onSettingsChange: (GallerySettings) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val separatingFoldBounds = rememberSeparatingFoldBounds(context)
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
    val viewerSessionViewModel: GalleryViewerSessionViewModel = viewModel()
    val retainedViewerSession = remember(viewerSessionViewModel) { viewerSessionViewModel.session }
    val selectedTab = navigationUiState.selectedTab
    val destination = navigationUiState.destination
    var mainNavigationChromeReady by remember { mutableStateOf(destination == GalleryDestination.Main) }
    LaunchedEffect(destination) {
        if (destination == GalleryDestination.Main) {
            delay(GalleryMotion.SecondaryCloseMillis.toLong())
            mainNavigationChromeReady = true
        } else {
            mainNavigationChromeReady = false
        }
    }
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
    val lockedMediaOperations = remember(lockedVaultRepository, hiddenMediaRepository) {
        LockedMediaOperations(lockedVaultRepository, hiddenMediaRepository)
    }
    val mediaManagementAccessRepository = remember(context) {
        MediaManagementAccessRepository(context)
    }
    var mediaManagementAccessStatus by remember {
        mutableStateOf(mediaManagementAccessRepository.status())
    }
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
    var optimisticRemovedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var permanentlyDeletedMediaIds by remember { mutableStateOf(recentlyDeletedRepository.initialPermanentlyDeletedMediaIds()) }
    val albumTileBounds = remember { mutableMapOf<String, Rect>() }
    val mediaTileBounds = remember { mutableMapOf<String, Rect>() }
    val albumDetailGridModes = remember { mutableStateMapOf<String, AlbumDetailGridMode>() }
    val albumDetailSortModes = remember { mutableStateMapOf<String, AlbumDetailSortMode>() }
    val defaultAlbumGridMode = remember(settings.gridDensity) { settings.gridDensity.defaultAlbumGridMode() }
    val launchAdaptivePolicy = remember(
        viewerConfiguration.screenWidthDp,
        viewerConfiguration.screenHeightDp
    ) {
        galleryAdaptivePolicy(
            widthDp = viewerConfiguration.screenWidthDp.toFloat(),
            heightDp = viewerConfiguration.screenHeightDp.toFloat()
        )
    }

    var editorViewerSession by remember { mutableStateOf<ViewerSessionSnapshot?>(null) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }
    var gallerySearchQuery by rememberSaveable { mutableStateOf("") }
    var vaultRefreshKey by remember { mutableIntStateOf(0) }
    var pendingMediaStoreWriteAction by viewerSessionViewModel.pendingMediaStoreWriteActionState
    var pendingLockConfirmation by remember { mutableStateOf<PendingLockConfirmation?>(null) }
    var pendingDeleteConfirmation by remember { mutableStateOf<PendingDeleteConfirmation?>(null) }
    var pendingLockedRestoreConfirmation by remember { mutableStateOf<PendingLockedRestoreConfirmation?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showMediaManagementExplanation by rememberSaveable { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMoveMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var pendingAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var albumCreationSelectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val mediaAccess = mediaUiState.mediaAccess
    val mediaStoreSnapshot = mediaUiState.snapshot
    val mediaStoreDeletedItems = mediaUiState.trashedMedia
    var viewerMediaItem by remember { mutableStateOf(retainedViewerSession?.mediaItem) }
    var viewerMediaItems by remember { mutableStateOf(retainedViewerSession?.mediaItems.orEmpty()) }
    var viewerVisible by remember { mutableStateOf(retainedViewerSession != null) }
    var viewerSharedElementKey by remember { mutableStateOf<Any?>(null) }
    var viewerSharedElementKeyPrefix by remember { mutableStateOf<String?>(null) }
    var viewerActionMode by remember { mutableStateOf(retainedViewerSession?.actionMode ?: ViewerActionMode.Normal) }
    var viewerReturnFallbackBounds by remember { mutableStateOf(Rect.Zero) }
    var viewerSourceMediaId by remember { mutableStateOf<String?>(null) }
    var viewerSourceMediaIds by remember { mutableStateOf(retainedViewerSession?.mediaItems?.map { it.id }.orEmpty()) }
    var viewerSourceBounds by remember { mutableStateOf(Rect.Zero) }
    var viewerSourceGridColumns by remember { mutableIntStateOf(4) }
    var albumTransition by remember { mutableStateOf<AlbumTransitionSpec?>(null) }
    var albumClosePreparing by remember { mutableStateOf(false) }
    var albumTransitionKey by remember { mutableIntStateOf(0) }
    var albumTransitionCommittedKey by remember { mutableIntStateOf(-1) }
    var albumTransitionAwaitingDestinationKey by remember { mutableIntStateOf(-1) }
    val activeTransitionAlbumIdState = remember {
        derivedStateOf {
            albumTransition
                ?.takeIf {
                    it.mode == AlbumTransitionMode.Closing ||
                        (
                            it.mode == AlbumTransitionMode.Opening &&
                                albumTransitionCommittedKey == it.key
                        )
                }
                ?.album?.id
        }
    }
    var mediaOpenTransition by remember { mutableStateOf<MediaOpenTransitionSpec?>(null) }
    var mediaOpenTransitionKey by remember { mutableIntStateOf(0) }
    var mediaCloseTransition by remember { mutableStateOf<MediaCloseTransitionSpec?>(null) }
    var mediaCloseTransitionKey by remember { mutableIntStateOf(0) }
    var viewerCloseInProgress by remember { mutableStateOf(false) }
    var viewerRevealMediaId by remember { mutableStateOf<String?>(null) }
    var transitionRootBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    val anyTransitionActive = albumTransition != null ||
        albumClosePreparing ||
        mediaOpenTransition != null ||
        mediaCloseTransition != null

    SideEffect {
        viewerSessionViewModel.retain(
            visible = viewerVisible,
            mediaItem = viewerMediaItem,
            mediaItems = viewerMediaItems,
            actionMode = viewerActionMode
        )
    }
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

    val mediaManagementAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        mediaManagementAccessStatus = mediaManagementAccessRepository.status()
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
                        mediaManagementAccessStatus = mediaManagementAccessRepository.status()
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
            pendingLockedRestoreConfirmation = null
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
    val isLoadingMedia = mediaAccess.hasAccess && mediaStoreSnapshot == null
    val areAlbumsLoading = isLoadingMedia || (mediaAccess.hasAccess && !mediaUiState.initialSyncComplete)
    var initialPresentationReady by remember { mutableStateOf(!initialMediaAccess.hasAccess) }
    var navigationPagesWarmed by remember { mutableStateOf(false) }
    SideEffect { onInitialPresentationReady(initialPresentationReady) }
    val activeSnapshot = when {
        isLoadingMedia -> emptySnapshot
        mediaAccess.hasAccess -> mediaStoreSnapshot ?: emptySnapshot
        else -> emptySnapshot
    }
    val activeSnapshotMediaIds = remember(activeSnapshot.mediaItems) {
        activeSnapshot.mediaItems.mapTo(hashSetOf()) { it.id }
    }
    LaunchedEffect(activeSnapshotMediaIds) {
        optimisticRemovedMediaIds = optimisticRemovedMediaIds.intersect(activeSnapshotMediaIds)
    }
    val removedMediaIds = remember(recentlyDeletedMedia, permanentlyDeletedMediaIds, optimisticRemovedMediaIds) {
        recentlyDeletedMedia.keys + permanentlyDeletedMediaIds + optimisticRemovedMediaIds
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
        visibleMedia.take(InitialPresentationThumbnailCount).joinToString(separator = "|") { it.id }
    }
    LaunchedEffect(mediaAccess.hasAccess, isLoadingMedia) {
        when {
            !mediaAccess.hasAccess -> initialPresentationReady = true
            isLoadingMedia -> initialPresentationReady = false
            else -> initialPresentationReady = true
        }
    }
    LaunchedEffect(initialPresentationReady) {
        if (initialPresentationReady && !navigationPagesWarmed) {
            // Keep cold-start composition lean, then precompose all three primary tabs so a direct
            // Photos-to-Menu tap does not pay the full Menu composition cost.
            delay(80L)
            navigationPagesWarmed = true
        }
    }
    LaunchedEffect(galleryFrontWarmupKey, initialPresentationReady) {
        if (initialPresentationReady && visibleMedia.isNotEmpty()) {
            // Let the first composed frame and navigation precomposition win scheduling priority.
            delay(180L)
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = visibleMedia.take(InitialPresentationPrefetchCount),
                thumbnailSizes = listOf(384),
                maxItems = InitialPresentationPrefetchCount,
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
    val moveTargetAlbums = remember(visibleAlbums, visibleMediaByAlbum) {
        visibleAlbums
            .asSequence()
            .filterNot { it.isAllPhotos || it.id == FavoritesAlbumId }
            .mapNotNull { album ->
                visibleMediaByAlbum[album.id]
                    ?.firstNotNullOfOrNull { it.relativePath?.takeIf(String::isNotBlank) }
                    ?.let { relativePath -> album to relativePath }
            }
            .toList()
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

    LaunchedEffect(destination, selectedTab, albumPreviewWarmupKey, visibleMedia.size, favoriteMediaIds, albumTransition) {
        if (destination == GalleryDestination.Main && selectedTab == GalleryTab.Albums && visibleMedia.isNotEmpty() && albumTransition == null) {
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

    val visibleMediaMembershipAuthoritative = mediaUiState.initialSyncComplete && mediaStoreSnapshot != null

    LaunchedEffect(visibleMediaIds, destination, viewerMediaItem?.id, viewerActionMode, visibleMediaMembershipAuthoritative) {
        if (!visibleMediaMembershipAuthoritative) return@LaunchedEffect
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
    val lockedFullQualityWarmupSize = remember(viewerPhotoDecodeSize) {
        LockedMediaVaultPolicy.fullQualityWarmupSize(viewerPhotoDecodeSize)
    }
    val lockedFullQualityWarmupPhotos = remember(lockedDisplayMedia) {
        val photos = lockedDisplayMedia.filterNot(MediaItem::isVideo)
        photos.take(LockedMediaVaultPolicy.fullQualityWarmupLimit(photos.size))
    }
    val lockedFullQualityWarmupKey = remember(lockedFullQualityWarmupPhotos) {
        lockedFullQualityWarmupPhotos.joinToString(separator = "|") { it.id }
    }
    LaunchedEffect(
        destination,
        hiddenVaultUnlocked,
        lockedFullQualityWarmupKey,
        lockedFullQualityWarmupSize
    ) {
        if (destination != GalleryDestination.LockedMedia || !hiddenVaultUnlocked) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            lockedFullQualityWarmupPhotos.forEach(lockedVaultRepository::prepareFullQualityRead)
        }
        prefetchMediaThumbnails(
            context = context.applicationContext,
            mediaItems = lockedFullQualityWarmupPhotos,
            thumbnailSizes = listOf(lockedFullQualityWarmupSize),
            maxItems = lockedFullQualityWarmupPhotos.size,
            pinInMemory = true
        )
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
        // Publish each recovered preview as soon as it is ready. Older installs may need to
        // create these previews from the encrypted original; waiting for the entire vault made
        // the grid look empty for many seconds after an update.
        missingLockedPreviewIds.mapNotNull(storedLockedMediaById::get).forEach { mediaItem ->
            val migrated = withContext(Dispatchers.IO) {
                lockedVaultRepository.ensureEncryptedPreviewFromVault(mediaItem)
            }
            if (migrated) {
                lockedVaultSnapshot = withContext(Dispatchers.IO) {
                    lockedVaultRepository.loadSnapshot()
                }
            }
        }
    }
    lateinit var launchSystemMediaStoreWrite: (PendingMediaStoreWriteAction) -> Unit
    lateinit var launchMediaStoreWrite: (PendingMediaStoreWriteAction) -> Unit
    fun lockMediaItems(mediaItems: List<MediaItem>, onLocked: () -> Unit = {}) {
        if (mediaItems.isEmpty()) return
        lockedSecurityViewModel.clearMessage()
        prefetchScope.launch {
            val importExecution = withContext(Dispatchers.IO) {
                lockedMediaOperations.importIntoVault(mediaItems)
            }
            val importedMediaIds = importExecution.outcome.importedIds
            if (importedMediaIds.isEmpty()) return@launch
            importExecution.updatedHiddenMediaIds?.let { hiddenMediaIds = it }
            vaultRefreshKey += 1

            val importedItems = mediaItems.filter { importedMediaIds.contains(it.id) }
            if (importExecution.outcome.failedIds.isNotEmpty()) {
                lockedSecurityViewModel.showMessage(
                    "%1$,d item(s) could not be copied into Locked Media."
                        .format(importExecution.outcome.failedIds.size)
                )
            }
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
    fun requestLockedMediaRestore(mediaItems: List<MediaItem>, viewerDirection: Int? = null) {
        if (mediaItems.isEmpty()) return
        pendingLockedRestoreConfirmation = PendingLockedRestoreConfirmation(
            mediaItems = mediaItems,
            viewerDirection = viewerDirection
        )
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
        val currentTransition = albumTransition
        if (currentTransition != null) {
            if (currentTransition.mode == AlbumTransitionMode.Closing) {
                if (currentTransition.album.id == album.id) {
                    // Seamless reversal from in-flight closing back to opening for the same album
                    albumTransitionKey += 1
                    albumTransitionCommittedKey = albumTransitionKey
                    albumTransition = currentTransition.copy(
                        key = albumTransitionKey,
                        mode = AlbumTransitionMode.Opening
                    )
                    navigationViewModel.selectAlbumForOpening(album.id)
                    return
                } else {
                    albumTransition = null
                    albumTransitionCommittedKey = -1
                    albumTransitionAwaitingDestinationKey = -1
                }
            } else {
                return
            }
        }
        val tileBounds = bounds.takeIf { it != Rect.Zero } ?: albumTileBounds[album.id]
        val openingMedia = mediaForAlbumFast(album, visibleMedia, visibleMediaByAlbum, favoriteMedia)
        val openingColumns = (
            (albumDetailGridModes[album.id] ?: defaultAlbumGridMode).columns + launchAdaptivePolicy.albumDetailColumnBoost
        ).coerceIn(1, 10)
        val openingWarmItemCount = (openingColumns * AlbumOpeningWarmRowCount).coerceAtMost(24)
        val firstViewportMedia = openingMedia
            .take(openingWarmItemCount)
            .filterNot(MediaItem::usesDeferredVideoThumbnail)
        navigationViewModel.selectAlbumForOpening(album.id)
        selectedMediaIds = emptySet()

        val hasTileTransition = tileBounds?.isUsableTransitionBounds() == true
        if (hasTileTransition) {
            albumTileBounds[album.id] = tileBounds
            albumTransitionKey += 1
            val openingKey = albumTransitionKey
            albumTransitionCommittedKey = -1
            albumTransitionAwaitingDestinationKey = -1
            albumTransition = AlbumTransitionSpec(
                key = openingKey,
                album = album,
                tileBounds = tileBounds,
                sourceCornerRadiusDp = when {
                    albumLayoutMode == AlbumLayoutMode.Basic || albumLayoutMode == AlbumLayoutMode.Compact -> 18f
                    albumLayoutMode == AlbumLayoutMode.List -> 14f
                    tileBounds.width > tileBounds.height * 1.35f -> 24f
                    else -> 22f
                },
                mode = AlbumTransitionMode.Opening,
                sortMode = albumDetailSortModes[album.id] ?: AlbumDetailSortMode.Newest
            )
            return
        }
        navigationViewModel.showAlbumDetail()
        prefetchScope.launch {
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = firstViewportMedia,
                thumbnailSizes = listOf(384),
                maxItems = firstViewportMedia.size,
                pinInMemory = true
            )
        }
    }

    fun closeAlbumDetail() {
        val currentTransition = albumTransition
        if (currentTransition != null && currentTransition.mode == AlbumTransitionMode.Closing) {
            return
        }
        if (currentTransition != null && currentTransition.mode == AlbumTransitionMode.Opening) {
            // Seamless reversal from in-flight opening back to closing
            albumTransitionKey += 1
            albumTransitionCommittedKey = albumTransitionKey
            albumTransition = currentTransition.copy(
                key = albumTransitionKey,
                mode = AlbumTransitionMode.Closing
            )
            navigationViewModel.closeAlbumDetail()
            return
        }
        selectedMediaIds = emptySet()
        val closingAlbum = selectedAlbum ?: currentTransition?.album
        val rawBounds = (selectedAlbumId ?: closingAlbum?.id)?.let(albumTileBounds::get)
        val closingBounds = if (rawBounds != null && rawBounds.isUsableTransitionBounds()) {
            rawBounds
        } else {
            val rootW = transitionRootBoundsInWindow.width.takeIf { it > 0f } ?: 1080f
            val rootH = transitionRootBoundsInWindow.height.takeIf { it > 0f } ?: 1920f
            val cardW = rootW * 0.60f
            val cardH = cardW * 0.72f
            val left = (rootW - cardW) / 2f
            val top = (rootH - cardH) / 2f
            Rect(left, top, left + cardW, top + cardH)
        }

        if (closingAlbum != null && closingBounds.isUsableTransitionBounds()) {
            albumTransitionKey += 1
            albumTransitionCommittedKey = -1
            albumTransitionAwaitingDestinationKey = -1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = closingAlbum,
                tileBounds = closingBounds,
                sourceCornerRadiusDp = when {
                    albumLayoutMode == AlbumLayoutMode.Basic || albumLayoutMode == AlbumLayoutMode.Compact -> 18f
                    albumLayoutMode == AlbumLayoutMode.List -> 14f
                    closingBounds.width > closingBounds.height * 1.35f -> 24f
                    else -> 22f
                },
                mode = AlbumTransitionMode.Closing,
                sortMode = albumDetailSortModes[closingAlbum.id] ?: AlbumDetailSortMode.Newest,
                firstVisibleRow = albumDetailListState.firstVisibleItemIndex,
                firstVisibleRowOffset = albumDetailListState.firstVisibleItemScrollOffset
            )
        } else {
            albumTransition = null
            albumTransitionCommittedKey = -1
            albumTransitionAwaitingDestinationKey = -1
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
            withFrameNanos { }
            if (albumTransition?.key == awaitingKey) {
                albumTransition = null
                albumTransitionCommittedKey = -1
            }
            albumTransitionAwaitingDestinationKey = -1
        }
    }
    LaunchedEffect(albumTransition?.key) {
        val watchedTransition = albumTransition ?: return@LaunchedEffect
        delay(900L)
        if (albumTransition?.key != watchedTransition.key) return@LaunchedEffect

        // A lifecycle interruption must not leave the source album invisible or block every
        // subsequent album tap. Normal transitions finish long before this safety deadline.
        albumTransition = null
        albumTransitionCommittedKey = -1
        albumTransitionAwaitingDestinationKey = -1
        if (watchedTransition.mode == AlbumTransitionMode.Opening) {
            navigationViewModel.showAlbumDetail()
        } else {
            navigationViewModel.closeAlbumDetail()
        }
    }

    LaunchedEffect(selectedTab, destination) {
        if (destination == GalleryDestination.Main) {
            val targetPage = selectedTab.pageIndex()
            if (mainPagerState.currentPage != targetPage) {
                mainPagerState.scrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(mainPagerState) {
        snapshotFlow { mainPagerState.settledPage }.collect { page ->
            navigationViewModel.syncPagerTab(pageToGalleryTab(page))
        }
    }

    fun selectMainTab(tab: GalleryTab) {
        selectedMediaIds = emptySet()
        bottomNavigationVisible = true
        val targetPage = tab.pageIndex()
        if (mainPagerState.currentPage != targetPage || mainPagerState.targetPage != targetPage) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            prefetchScope.launch {
                mainPagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = MotionSpecs.AppleSpringPager
                )
            }
        }
        navigationViewModel.openTab(tab)
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
        } else {
            // The viewer can paint its preview immediately and promote to full quality in the
            // background. Do not hold its controls behind thumbnail or vault IO.
            finishViewerOpen(
                mediaItem = mediaItem,
                mediaItems = mediaItems,
                sharedElementKey = sharedElementKey,
                sharedElementKeyPrefix = sharedElementKeyPrefix,
                actionMode = actionMode
            )
        }

        if (actionMode == ViewerActionMode.Locked && mediaItem.contentUri != null) {
            val fullQualitySize = LockedMediaVaultPolicy.fullQualityWarmupSize(viewerPhotoDecodeSize)
            prefetchScope.launch {
                withContext(Dispatchers.IO) {
                    lockedVaultRepository.prepareFullQualityRead(mediaItem)
                }
                if (!mediaItem.isVideo) {
                    prefetchMediaThumbnails(
                        context = context.applicationContext,
                        mediaItems = listOf(mediaItem),
                        thumbnailSizes = listOf(fullQualitySize),
                        maxItems = 1,
                        pinInMemory = true
                    )
                }
            }
        }
        val warmupItem = (if (actionMode == ViewerActionMode.Locked) {
            transitionMediaItem.copy(isVideo = false)
        } else {
            mediaItem
        }).takeIf { it.contentUri != null }
        if (warmupItem != null && !warmupItem.isVideo) {
            prefetchScope.launch {
                prefetchMediaThumbnails(
                    context = context.applicationContext,
                    mediaItems = listOf(warmupItem),
                    thumbnailSizes = listOf(
                        if (actionMode == ViewerActionMode.Locked) 512
                        else viewerPhotoDecodeSize
                    ),
                    maxItems = 1,
                    pinInMemory = true
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

    fun closeViewer(
        startOffset: Offset = Offset.Zero,
        startScale: Float = 1f,
        startBackdropAlpha: Float = 1f,
        startRotationZ: Float = 0f,
        startRotationX: Float = 0f
    ) {
        if (mediaCloseTransition != null || viewerCloseInProgress) return
        val openSpec = mediaOpenTransition
        if (openSpec != null) {
            mediaOpenTransition = null
            mediaCloseTransitionKey += 1
            val closeBounds = if (openSpec.tileBounds.isUsableTransitionBounds()) {
                openSpec.tileBounds
            } else {
                viewerReturnFallbackBounds.takeIf { it.isUsableTransitionBounds() } ?: returnBoundsForMedia(openSpec.mediaItem)
            }
            if (closeBounds.isUsableTransitionBounds()) {
                mediaCloseTransition = MediaCloseTransitionSpec(
                    key = mediaCloseTransitionKey,
                    mediaItem = openSpec.mediaItem,
                    tileBounds = closeBounds,
                    startOffset = startOffset,
                    startScale = startScale.coerceIn(0.68f, 1f),
                    startBackdropAlpha = startBackdropAlpha.coerceIn(0f, 1f),
                    startRotationZ = startRotationZ,
                    startRotationX = startRotationX
                )
                viewerCloseInProgress = false
            } else {
                clearViewerAfterClose()
            }
            return
        }
        val currentItem = viewerMediaItem
        if (currentItem == null) {
            clearViewerAfterClose()
            return
        }

        val supportsGridReveal =
            (destination == GalleryDestination.Main && selectedTab == GalleryTab.Photos) ||
                destination == GalleryDestination.AlbumDetail
        val knownBounds = mediaTileBounds[currentItem.id] ?: Rect.Zero
        val isDragDismiss = startOffset != Offset.Zero
        val isKnownVisible = knownBounds.isVisibleWithin(transitionRootBoundsInWindow)
        val needsGridReveal = supportsGridReveal && !isKnownVisible && !isDragDismiss

        val performClose = {
            viewerSharedElementKeyPrefix?.let { keyPrefix ->
                viewerSharedElementKey = "$keyPrefix-media-${currentItem.id}"
            }
            viewerVisible = false

            val calculatedReturnBounds = returnBoundsForMedia(currentItem)
            val targetBounds = when {
                needsGridReveal -> {
                    mediaTileBounds[currentItem.id]
                        ?.takeIf { it.isVisibleWithin(transitionRootBoundsInWindow) }
                        ?: calculatedReturnBounds.takeIf { it.isUsableTransitionBounds() }
                        ?: Rect.Zero
                }
                isKnownVisible -> knownBounds
                calculatedReturnBounds.isUsableTransitionBounds() -> calculatedReturnBounds
                isDragDismiss -> {
                    val originX = if (transitionRootBoundsInWindow.isUsableTransitionBounds()) transitionRootBoundsInWindow.left else 0f
                    val originY = if (transitionRootBoundsInWindow.isUsableTransitionBounds()) transitionRootBoundsInWindow.top else 0f
                    val rootWidth = transitionRootBoundsInWindow.width.takeIf { it > 0f } ?: 1080f
                    val rootHeight = transitionRootBoundsInWindow.height.takeIf { it > 0f } ?: 1920f
                    Rect(
                        left = originX + rootWidth * 0.15f,
                        top = originY + rootHeight + 120f,
                        right = originX + rootWidth * 0.85f,
                        bottom = originY + rootHeight + 120f + (rootHeight * 0.4f)
                    )
                }
                else -> viewerReturnFallbackBounds
            }
            if (targetBounds.isUsableTransitionBounds()) {
                mediaCloseTransitionKey += 1
                mediaCloseTransition = MediaCloseTransitionSpec(
                    key = mediaCloseTransitionKey,
                    mediaItem = currentItem,
                    tileBounds = targetBounds,
                    startOffset = startOffset,
                    startScale = startScale.coerceIn(0.68f, 1f),
                    startBackdropAlpha = startBackdropAlpha.coerceIn(0f, 1f),
                    startRotationZ = startRotationZ,
                    startRotationX = startRotationX
                )
                viewerCloseInProgress = false
            } else {
                clearViewerAfterClose()
            }
        }

        if (needsGridReveal) {
            viewerCloseInProgress = true
            prefetchScope.launch {
                mediaTileBounds.remove(currentItem.id)
                viewerRevealMediaId = currentItem.id
                val revealListState = when {
                    destination == GalleryDestination.Main && selectedTab == GalleryTab.Photos -> photosListState
                    destination == GalleryDestination.AlbumDetail -> albumDetailListState
                    else -> null
                }
                var previousBounds = Rect.Zero
                var stableFrames = 0
                for (attempt in 0 until 16) {
                    delay(16)
                    val revealedBounds = mediaTileBounds[currentItem.id] ?: Rect.Zero
                    val settled = revealListState?.isScrollInProgress != true
                    if (
                        settled &&
                        revealedBounds.isVisibleWithin(transitionRootBoundsInWindow) &&
                        revealedBounds == previousBounds
                    ) {
                        stableFrames += 1
                        if (stableFrames >= 2) break
                    } else {
                        stableFrames = 0
                    }
                    previousBounds = revealedBounds
                }
                viewerRevealMediaId = null
                // Allow the reveal-only tracking state to leave composition before freezing the
                // immutable close target. This prevents a one-frame correction under the hero.
                delay(16)
                performClose()
            }
        } else {
            performClose()
        }
    }

    val activeMediaCloseTransitionKey = mediaCloseTransition?.key
    LaunchedEffect(activeMediaCloseTransitionKey, destination, selectedTab, photosListState) {
        if (
            activeMediaCloseTransitionKey != null &&
            destination == GalleryDestination.Main &&
            selectedTab == GalleryTab.Photos
        ) {
            snapshotFlow { photosListState.isScrollInProgress }.first { it }
            if (mediaCloseTransition?.key == activeMediaCloseTransitionKey) {
                // A fling invalidates the immutable target bounds used by the closing hero.
                // Remove it immediately instead of drawing a stale tile above the moving grid.
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

    fun confirmLockedMediaRestore(pending: PendingLockedRestoreConfirmation) {
        pendingLockedRestoreConfirmation = null
        val viewerItem = pending.viewerDirection?.let { pending.mediaItems.firstOrNull() }
        unhideMediaItems(pending.mediaItems) { restoredIds ->
            val restoredViewerItem = viewerItem?.takeIf { it.id in restoredIds }
            if (restoredViewerItem != null) {
                advanceViewerAfterRemoval(restoredViewerItem, requireNotNull(pending.viewerDirection))
            }
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
                    mediaStoreWriteRepository.moveToAlbum(
                        mediaItems = action.mediaItems,
                        albumName = albumName,
                        targetRelativePath = action.targetAlbumRelativePath
                    )
                }
                mediaViewModel.finishAppMediaStoreWrite()
                if (moved) {
                    pendingAlbumName = null
                    albumCreationSelectedIds = emptySet()
                    selectedMediaIds = emptySet()
                    if (action.returnToAlbumsAfterMove) {
                        navigationViewModel.openTab(GalleryTab.Albums)
                    }
                }
                mediaViewModel.requestFullRefresh()
            }
            return
        }

        when (action.mode) {
            MediaStoreWriteMode.Trash -> {
                selectedMediaIds = selectedMediaIds - mediaIds
                if (action.fromViewer && !action.uiRemovalApplied) {
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
                if (action.fromViewer && !action.uiRemovalApplied) {
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

    fun rollbackOptimisticMediaRemoval(action: PendingMediaStoreWriteAction) {
        if (!action.uiRemovalApplied) return
        val mediaIds = action.mediaItems.mapTo(linkedSetOf()) { it.id }
        optimisticRemovedMediaIds = optimisticRemovedMediaIds - mediaIds
        selectedMediaIds = selectedMediaIds + action.selectedMediaIdsBeforeRemoval

        if (action.fromViewer) {
            val restoredItem = action.mediaItems.firstOrNull() ?: return
            if (viewerMediaItems.none { it.id == restoredItem.id }) {
                val restoredItems = viewerMediaItems.toMutableList()
                restoredItems.add(
                    index = action.viewerIndexBeforeRemoval.coerceIn(0, restoredItems.size),
                    element = restoredItem
                )
                viewerMediaItems = restoredItems
            }
            viewerMediaItem = restoredItem
            viewerVisible = true
        }
        mediaViewModel.requestQuickRefresh()
    }

    fun rollbackCancelledLockedMedia(action: PendingMediaStoreWriteAction) {
        if (action.mode != MediaStoreWriteMode.DeleteLockedOriginals) return
        prefetchScope.launch {
            val execution = withContext(Dispatchers.IO) {
                lockedMediaOperations.resolveOriginalRemoval(
                    importedIds = action.mediaItems.mapTo(linkedSetOf()) { it.id },
                    result = LockedMediaOriginalRemovalResult.CancelledOrFailed
                )
            }
            execution.updatedHiddenMediaIds?.let { hiddenMediaIds = it }
            if (execution.resolution.rollbackIds.isNotEmpty()) {
                vaultRefreshKey += 1
            }
            val rollbackCount = execution.resolution.rollbackIds.size
            val committedCount = execution.resolution.committedIds.size
            lockedSecurityViewModel.showMessage(
                when {
                    rollbackCount > 0 && committedCount > 0 ->
                        "%1$,d item(s) were not moved; %2$,d remain safely locked."
                            .format(rollbackCount, committedCount)
                    rollbackCount > 0 ->
                        "Original removal was cancelled, so those items were not moved to Locked Media."
                    else ->
                        "The public original was already gone, so the encrypted copy remains in Locked Media."
                }
            )
            mediaViewModel.requestQuickRefresh()
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
                    val deleted = withContext(Dispatchers.IO) {
                        mediaStoreWriteRepository.deleteDirectly(action.mediaItems)
                    }
                    mediaViewModel.finishAppMediaStoreWrite()
                    if (deleted) {
                        completeMediaStoreWrite(action)
                    } else {
                        rollbackOptimisticMediaRemoval(action)
                        lockedSecurityViewModel.showMessage(
                            "Android did not allow those items to be deleted. Nothing was removed."
                        )
                        mediaViewModel.requestFullRefresh()
                    }
                }
                MediaStoreWriteMode.DeleteLockedOriginals -> {
                    val originalsRemoved = withContext(Dispatchers.IO) {
                        mediaStoreWriteRepository.deleteDirectly(action.mediaItems)
                    }
                    mediaViewModel.finishAppMediaStoreWrite()
                    if (originalsRemoved) {
                        completeMediaStoreWrite(action)
                    } else {
                        rollbackCancelledLockedMedia(action)
                    }
                }
                MediaStoreWriteMode.MoveToAlbum -> {
                    completeMediaStoreWrite(action)
                }
            }
        }
    }

    fun completeOneTapMediaStoreWrite(action: PendingMediaStoreWriteAction) {
        if (action.mode == MediaStoreWriteMode.MoveToAlbum) {
            completeMediaStoreWrite(action)
            return
        }
        prefetchScope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                when (action.mode) {
                    MediaStoreWriteMode.Trash -> mediaStoreWriteRepository.setTrashedDirectly(action.mediaItems, true)
                    MediaStoreWriteMode.RestoreFromTrash -> mediaStoreWriteRepository.setTrashedDirectly(action.mediaItems, false)
                    MediaStoreWriteMode.DeleteForever,
                    MediaStoreWriteMode.DeleteLockedOriginals -> mediaStoreWriteRepository.deleteDirectly(action.mediaItems)
                    MediaStoreWriteMode.MoveToAlbum -> true
                }
            }
            if (succeeded) {
                mediaViewModel.finishAppMediaStoreWrite()
                completeMediaStoreWrite(action)
            } else {
                launchSystemMediaStoreWrite(action)
            }
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
            rollbackOptimisticMediaRemoval(action)
            rollbackCancelledLockedMedia(action)
        }
    }

    launchSystemMediaStoreWrite = { action ->
        val request = when (action.mode) {
            MediaStoreWriteMode.Trash -> mediaStoreWriteRepository.createTrashRequest(action.mediaItems, trashed = true)
            MediaStoreWriteMode.RestoreFromTrash -> mediaStoreWriteRepository.createTrashRequest(action.mediaItems, trashed = false)
            MediaStoreWriteMode.DeleteForever,
            MediaStoreWriteMode.DeleteLockedOriginals -> mediaStoreWriteRepository.createDeleteRequest(action.mediaItems)
            MediaStoreWriteMode.MoveToAlbum -> mediaStoreWriteRepository.createWriteRequest(action.mediaItems)
        }
        if (request != null) {
            pendingMediaStoreWriteAction = action
            mediaStoreWriteLauncher.launch(request)
        } else {
            completeMediaStoreFallback(action)
        }
    }

    launchMediaStoreWrite = { action ->
        mediaViewModel.beginAppMediaStoreWrite()
        if (mediaManagementAccessStatus.isGranted) {
            completeOneTapMediaStoreWrite(action)
        } else {
            launchSystemMediaStoreWrite(action)
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

    fun requestMediaStoreDeleteConfirmation(action: PendingMediaStoreWriteAction) {
        if (action.mediaItems.isEmpty()) return
        pendingDeleteConfirmation = PendingDeleteConfirmation.MediaStore(action)
    }

    fun confirmPendingDelete(pending: PendingDeleteConfirmation) {
        pendingDeleteConfirmation = null
        when (pending) {
            is PendingDeleteConfirmation.MediaStore -> {
                var action = pending.action
                val canApplyImmediately = shouldApplyOptimisticMediaRemoval(mediaManagementAccessStatus.isGranted, action.mode)
                if (canApplyImmediately) {
                    val mediaIds = action.mediaItems.mapTo(linkedSetOf()) { it.id }
                    val selectedIdsBeforeRemoval = selectedMediaIds.intersect(mediaIds)
                    val viewerIndex = if (action.fromViewer) {
                        viewerMediaItems.indexOfFirst { it.id in mediaIds }
                    } else {
                        -1
                    }
                    if (action.mode == MediaStoreWriteMode.Trash) {
                        optimisticRemovedMediaIds = optimisticRemovedMediaIds + mediaIds
                    }
                    selectedMediaIds = selectedMediaIds - mediaIds
                    if (action.fromViewer) {
                        action.mediaItems.firstOrNull()?.let { mediaItem ->
                            advanceViewerAfterRemoval(mediaItem, action.viewerDirection)
                        }
                    }
                    action = action.copy(
                        uiRemovalApplied = true,
                        viewerIndexBeforeRemoval = viewerIndex,
                        selectedMediaIdsBeforeRemoval = selectedIdsBeforeRemoval
                    )
                }
                launchMediaStoreWrite(action)
            }
            is PendingDeleteConfirmation.Locked -> {
                softDeleteLockedMediaItems(pending.mediaItems, pending.viewerDirection)
            }
            is PendingDeleteConfirmation.VaultForever -> {
                permanentlyDeleteVaultMediaItems(pending.mediaItems, pending.viewerDirection)
            }
        }
    }

    fun requestMediaDelete(mediaItem: MediaItem, direction: Int) {
        when {
            viewerActionMode == ViewerActionMode.RecentlyDeleted ||
                (destination == GalleryDestination.RecentlyDeleted && recentlyDeletedMedia.containsKey(mediaItem.id)) -> {
                pendingDeleteConfirmation = if (isVaultMedia(mediaItem)) {
                    PendingDeleteConfirmation.VaultForever(
                        mediaItems = listOf(mediaItem),
                        viewerDirection = direction
                    )
                } else {
                    PendingDeleteConfirmation.MediaStore(
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
                pendingDeleteConfirmation = PendingDeleteConfirmation.Locked(
                    mediaItems = listOf(mediaItem),
                    viewerDirection = direction
                )
            }
            else -> requestMediaStoreDeleteConfirmation(
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
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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

    fun chooseAlbumForSelectedMedia() {
        if (selectedMediaItems.isNotEmpty()) {
            pendingMoveMediaItems = selectedMediaItems
        }
    }

    fun deleteSelectedMedia() {
        if (selectedMediaItems.isEmpty()) return
        requestMediaStoreDeleteConfirmation(
            PendingMediaStoreWriteAction(MediaStoreWriteMode.Trash, selectedMediaItems)
        )
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
                requestLockedMediaRestore(listOf(mediaItem), viewerDirection = direction)
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
        !albumClosePreparing &&
        mediaOpenTransition == null &&
        mediaCloseTransition == null
    val backAction = resolveGalleryBackAction(
        destination = destination,
        selectedTab = selectedTab,
        viewerVisible = viewerVisible && viewerMediaItem != null,
        viewerClosing = viewerCloseInProgress,
        albumTransitionActive = albumTransition != null || albumClosePreparing,
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
                closeAlbumDetail()
            }
            GalleryBackAction.CancelMediaOpen -> {
                val openSpec = mediaOpenTransition
                if (openSpec != null && openSpec.tileBounds.isUsableTransitionBounds()) {
                    mediaOpenTransition = null
                    mediaCloseTransitionKey += 1
                    mediaCloseTransition = MediaCloseTransitionSpec(
                        key = mediaCloseTransitionKey,
                        mediaItem = openSpec.mediaItem,
                        tileBounds = openSpec.tileBounds,
                        startOffset = Offset.Zero,
                        startScale = 1f,
                        startBackdropAlpha = 1f
                    )
                    viewerCloseInProgress = false
                } else {
                    mediaOpenTransition = null
                    clearViewerAfterClose()
                }
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
        val foldSafeInsetsPx = remember(rootWidthPx, rootHeightPx, separatingFoldBounds) {
            separatingFoldBounds?.let { foldBounds ->
                gallerySafePaneInsets(
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    foldLeftPx = foldBounds.left.toFloat(),
                    foldTopPx = foldBounds.top.toFloat(),
                    foldRightPx = foldBounds.right.toFloat(),
                    foldBottomPx = foldBounds.bottom.toFloat()
                )
            } ?: GallerySafePaneInsetsPx()
        }
        val foldStartPadding = with(density) { foldSafeInsetsPx.start.toDp() }
        val foldTopPadding = with(density) { foldSafeInsetsPx.top.toDp() }
        val foldEndPadding = with(density) { foldSafeInsetsPx.end.toDp() }
        val foldBottomPadding = with(density) { foldSafeInsetsPx.bottom.toDp() }
        val safeContentWidth = (maxWidth - foldStartPadding - foldEndPadding).coerceAtLeast(1.dp)
        val safeContentHeight = (maxHeight - foldTopPadding - foldBottomPadding).coerceAtLeast(1.dp)
        val adaptivePolicy = remember(safeContentWidth, safeContentHeight) {
            galleryAdaptivePolicy(safeContentWidth.value, safeContentHeight.value)
        }
        val adaptivePhotoColumns = (
            settings.gridDensity.photoColumns + adaptivePolicy.photoColumnBoost
        ).coerceIn(1, 10)
        val adaptiveHeroHeight = when (adaptivePolicy.widthClass) {
            GalleryWindowWidthClass.Compact -> 176.dp
            GalleryWindowWidthClass.Medium -> 196.dp
            GalleryWindowWidthClass.Expanded -> 220.dp
        }

        SharedTransitionLayout(
            modifier = Modifier
                .padding(
                    start = foldStartPadding,
                    top = foldTopPadding,
                    end = foldEndPadding,
                    bottom = foldBottomPadding
                )
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
                            mainNavigationChromeReady &&
                            selectedMediaIds.isEmpty() &&
                            !viewerVisible &&
                            navigationTransitionIdle &&
                            !adaptivePolicy.useNavigationRail
                        ) {
                            GalleryBottomBar(
                                selectedTab = selectedTab,
                                visible = bottomNavigationVisible,
                                onTabSelected = ::selectMainTab
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (
                            destination == GalleryDestination.Main &&
                            mainNavigationChromeReady &&
                            selectedMediaIds.isEmpty() &&
                            !viewerVisible &&
                            navigationTransitionIdle &&
                            adaptivePolicy.useNavigationRail
                        ) {
                            GalleryNavigationRail(
                                selectedTab = selectedTab,
                                onTabSelected = ::selectMainTab
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
                                            if (albumTransition != null) {
                                                EnterTransition.None togetherWith ExitTransition.None
                                            } else {
                                                (
                                                    fadeIn(animationSpec = MotionSpecs.AppleSpringSnappy) +
                                                        slideInHorizontally(
                                                            animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                            initialOffsetX = { if (targetState == GalleryDestination.AlbumDetail) it / 8 else -it / 8 }
                                                        )
                                                ) togetherWith (
                                                    fadeOut(animationSpec = tween(120)) +
                                                        slideOutHorizontally(
                                                            animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                            targetOffsetX = { if (targetState == GalleryDestination.AlbumDetail) -it / 8 else it / 8 }
                                                        )
                                                )
                                            }
                                        }
                                        targetState == GalleryDestination.Main -> {
                                            (
                                                fadeIn(animationSpec = MotionSpecs.AppleSpringSnappy) +
                                                    slideInHorizontally(
                                                        animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                        initialOffsetX = { -it / 10 }
                                                    )
                                            ) togetherWith (
                                                fadeOut(animationSpec = tween(120)) +
                                                    slideOutHorizontally(
                                                        animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                        targetOffsetX = { it / 8 }
                                                    )
                                            )
                                        }
                                        else -> {
                                            (
                                                fadeIn(animationSpec = MotionSpecs.AppleSpringSnappy) +
                                                    slideInHorizontally(
                                                        animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                        initialOffsetX = { it / 8 }
                                                    )
                                            ) togetherWith (
                                                fadeOut(animationSpec = tween(120)) +
                                                    slideOutHorizontally(
                                                        animationSpec = MotionSpecs.AppleSpringSheetOffset,
                                                        targetOffsetX = { -it / 10 }
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
                                    beyondViewportPageCount = if (navigationPagesWarmed) 2 else 0,
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
                                                onGridColumnsChange = { requestedColumns ->
                                                    val requestedBaseColumns = (
                                                        requestedColumns - adaptivePolicy.photoColumnBoost
                                                        ).coerceIn(1, 6)
                                                    val nextDensity = GalleryGridDensity.entries.minBy { density ->
                                                        kotlin.math.abs(density.photoColumns - requestedBaseColumns)
                                                    }
                                                    if (nextDensity != settings.gridDensity) {
                                                        onSettingsChange(settings.copy(gridDensity = nextDensity))
                                                    }
                                                },
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
                                                onMoveSelected = ::chooseAlbumForSelectedMedia,
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
                                                onDeleteAlbum = { album ->
                                                    val albumMedia = mediaForAlbumFast(
                                                        album,
                                                        visibleMedia,
                                                        visibleMediaByAlbum,
                                                        favoriteMedia
                                                    )
                                                    if (albumMedia.isNotEmpty()) {
                                                        launchMediaStoreWrite(
                                                            PendingMediaStoreWriteAction(
                                                                mode = MediaStoreWriteMode.Trash,
                                                                mediaItems = albumMedia
                                                            )
                                                        )
                                                    }
                                                },
                                                canDeleteAlbum = { album ->
                                                    !album.isAllPhotos && album.id != FavoritesAlbumId
                                                },
                                                onOpenSettings = { showSettingsDialog = true },
                                                onRefresh = mediaViewModel::requestQuickRefresh,
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
                                                activeTransitionAlbumId = activeTransitionAlbumIdState,
                                                mediaAccessNotice = mediaAccessNotice,
                                                isLoading = areAlbumsLoading,
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
                                        sortMode = albumDetailSortModes[selectedAlbum.id] ?: AlbumDetailSortMode.Newest,
                                        onSortModeChange = { sortMode -> albumDetailSortModes[selectedAlbum.id] = sortMode },
                                        selectedMediaIds = selectedMediaIds,
                                        onMediaLongClick = ::toggleMediaSelection,
                                        onMediaSelectionToggle = ::toggleMediaSelection,
                                        onSelectionClear = ::clearMediaSelection,
                                        onSelectAllVisible = { selectMedia(selectedAlbumMedia) },
                                        onDeleteSelected = ::deleteSelectedMedia,
                                        onShareSelected = ::shareSelectedMedia,
                                        onHideSelected = ::hideSelectedMedia,
                                        onMoveSelected = ::chooseAlbumForSelectedMedia,
                                        onHideAlbum = { hideAlbumAndReturn(selectedAlbum) },
                                        onMediaBoundsChanged = { mediaItem, bounds ->
                                            if (bounds.isUsableTransitionBounds()) {
                                                mediaTileBounds[mediaItem.id] = bounds
                                            }
                                        },
                                        onMediaClick = { mediaItem, bounds, sharedElementKey, sharedElementKeyPrefix ->
                                            val selectedGridColumns = (
                                                (albumDetailGridModes[selectedAlbum.id] ?: defaultAlbumGridMode).columns + adaptivePolicy.albumDetailColumnBoost
                                            ).coerceIn(1, 10)
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
                                    requestLockedMediaRestore(selectedItems.map { fullItemsById[it.id] ?: it })
                                },
                                onDeleteSelected = { selectedItems ->
                                    val fullItemsById = lockedDisplayMedia.associateBy { it.id }
                                    pendingDeleteConfirmation = PendingDeleteConfirmation.Locked(
                                        selectedItems.map { fullItemsById[it.id] ?: it }
                                    )
                                },
                                onOpenMedia = { mediaItem, bounds, lockedGridColumns ->
                                    val lockedViewerItems = lockedDisplayMedia
                                    val lockedViewerItem = lockedViewerItems.firstOrNull { it.id == mediaItem.id }
                                        ?: mediaItem
                                    startViewerOpen(
                                        mediaItem = lockedViewerItem,
                                        mediaItems = lockedViewerItems,
                                        bounds = bounds,
                                        actionMode = ViewerActionMode.Locked,
                                        transitionMediaItem = mediaItem,
                                        sourceGridColumns = lockedGridColumns
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
                                                        targetAlbumName = albumName,
                                                        returnToAlbumsAfterMove = true
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
                                    requestMediaStoreDeleteConfirmation(
                                        PendingMediaStoreWriteAction(MediaStoreWriteMode.Trash, mediaItems)
                                    )
                                }
                            )
                        }
                            }
                    }
                }
                }

                PositionAwareAlbumTransitionOverlay(
                    transition = albumTransition,
                    committed = albumTransition?.let {
                        it.mode != AlbumTransitionMode.Opening ||
                            albumTransitionCommittedKey == it.key
                    } == true,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    rootBoundsInWindow = transitionRootBoundsInWindow,
                    transitionStyle = settings.folderTransitionStyle,
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
                                val warmColumns = (
                                    (albumDetailGridModes[finishedTransition.album.id] ?: defaultAlbumGridMode).columns + adaptivePolicy.albumDetailColumnBoost
                                ).coerceIn(1, 10)
                                val warmMedia = mediaForAlbumFast(finishedTransition.album, visibleMedia, visibleMediaByAlbum, favoriteMedia)
                                    .take((warmColumns * AlbumOpeningWarmRowCount).coerceAtMost(24))
                                    .filterNot(MediaItem::usesDeferredVideoThumbnail)
                                prefetchScope.launch {
                                    prefetchMediaThumbnails(
                                        context = context.applicationContext,
                                        mediaItems = warmMedia,
                                        thumbnailSizes = listOf(384),
                                        maxItems = warmMedia.size,
                                        pinInMemory = true
                                    )
                                }
                            } else {
                                albumTransition = null
                                albumTransitionCommittedKey = -1
                                navigationViewModel.clearSelectedAlbum()
                            }
                        }
                    }
                ) { overlayAlbum ->
                    AlbumDetailTransitionPreview(
                        album = overlayAlbum,
                        mediaItems = if (overlayAlbum.id == selectedAlbumId) selectedAlbumMedia else mediaForAlbumFast(overlayAlbum, visibleMedia, visibleMediaByAlbum, favoriteMedia),
                        contentPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues(),
                        gridMode = albumDetailGridModes[overlayAlbum.id] ?: defaultAlbumGridMode,
                        columnBoost = adaptivePolicy.albumDetailColumnBoost,
                        sortMode = albumTransition?.sortMode ?: AlbumDetailSortMode.Newest,
                        initialFirstVisibleRow = albumTransition?.firstVisibleRow ?: 0,
                        initialFirstVisibleRowOffset = albumTransition?.firstVisibleRowOffset ?: 0
                    )
                }
                ReferenceMediaOpenOverlay(
                    transition = mediaOpenTransition,
                    viewerTransitionStyle = settings.viewerTransitionStyle,
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
                    viewerTransitionStyle = settings.viewerTransitionStyle,
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
                    onClose = { off, sc, alp -> closeViewer(off, sc, alp) },
                    onCloseWithMotion = { off, sc, alp, rotZ, rotX -> closeViewer(off, sc, alp, rotZ, rotX) },
                    onDelete = ::requestMediaDelete,
                    onHide = ::hideMedia,
                    onRestore = ::restoreViewerMedia,
                    actionMode = viewerActionMode,
                    favoriteMediaIds = favoriteMediaIds,
                    onFavoriteChange = ::setMediaFavorite,
                    onEdit = ::editPhoto,
                    onMove = { mediaItem -> pendingMoveMediaItems = listOf(mediaItem) },
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

            pendingDeleteConfirmation?.let { pending ->
                val itemCount = pending.mediaItems.size
                val permanent = pending is PendingDeleteConfirmation.VaultForever ||
                    (pending is PendingDeleteConfirmation.MediaStore &&
                        pending.action.mode == MediaStoreWriteMode.DeleteForever)
                AlertDialog(
                    onDismissRequest = { pendingDeleteConfirmation = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = {
                        Text("Are you sure?")
                    },
                    text = {
                        Text(
                            when {
                                permanent && itemCount == 1 -> "This item will be deleted forever. This cannot be undone."
                                permanent -> "%1\$,d items will be deleted forever. This cannot be undone.".format(itemCount)
                                itemCount == 1 -> "This item will move to Recently deleted, where it can be restored."
                                else -> "%1\$,d items will move to Recently deleted, where they can be restored.".format(itemCount)
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { confirmPendingDelete(pending) }) {
                            Text(
                                text = if (permanent) "Delete forever" else "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteConfirmation = null }) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }

            pendingLockedRestoreConfirmation?.let { pending ->
                val itemCount = pending.mediaItems.size
                AlertDialog(
                    onDismissRequest = { pendingLockedRestoreConfirmation = null },
                    title = {
                        Text(lockedMediaRestoreTitle(itemCount))
                    },
                    text = {
                        Text(lockedMediaRestoreMessage(itemCount))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { confirmLockedMediaRestore(pending) }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingLockedRestoreConfirmation = null }) {
                            Text("No")
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
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
                    shape = RoundedCornerShape(24.dp)
                )
            }

            if (showSettingsDialog) {
                GallerySettingsDialog(
                    settings = settings,
                    mediaManagementAccessStatus = mediaManagementAccessStatus,
                    onRequestMediaManagementAccess = { showMediaManagementExplanation = true },
                    onSettingsChange = onSettingsChange,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (showMediaManagementExplanation) {
                AlertDialog(
                    onDismissRequest = { showMediaManagementExplanation = false },
                    title = { Text("Allow direct media changes?") },
                    text = {
                        Text(
                            "Android requires one one-time switch. On the next screen, turn on 'Allow app to manage media', then return. Native Gallery can then move, trash, restore, and delete media without the large confirmation block. This is optional and can be revoked in Android Settings."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showMediaManagementExplanation = false
                                mediaManagementAccessRepository.requestAccessIntent()?.let {
                                    mediaManagementAccessLauncher.launch(it)
                                }
                            }
                        ) {
                            Text("Continue to Android Settings", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMediaManagementExplanation = false }) {
                            Text("Not now")
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }

            if (pendingMoveMediaItems.isNotEmpty()) {
                MoveToAlbumDialog(
                    itemCount = pendingMoveMediaItems.size,
                    targetAlbums = moveTargetAlbums,
                    onDismiss = { pendingMoveMediaItems = emptyList() },
                    onTargetSelected = { album, targetRelativePath ->
                        val normalizedTarget = targetRelativePath.trimEnd('/', '\\')
                        val movingItems = pendingMoveMediaItems.filterNot { mediaItem ->
                            mediaItem.relativePath
                                ?.trimEnd('/', '\\')
                                ?.equals(normalizedTarget, ignoreCase = true) == true
                        }
                        pendingMoveMediaItems = emptyList()
                        if (movingItems.isEmpty()) {
                            selectedMediaIds = emptySet()
                        } else {
                            launchMediaStoreWrite(
                                PendingMediaStoreWriteAction(
                                    mode = MediaStoreWriteMode.MoveToAlbum,
                                    mediaItems = movingItems,
                                    targetAlbumName = album.name,
                                    targetAlbumRelativePath = targetRelativePath
                                )
                            )
                        }
                    }
                )
            }

        }
    }
}

private fun MediaItem.usesDeferredVideoThumbnail(): Boolean {
    return isVideo && (
        mimeType?.contains("matroska", ignoreCase = true) == true ||
            title.endsWith(".mkv", ignoreCase = true)
        )
}

@Composable
private fun MoveToAlbumDialog(
    itemCount: Int,
    targetAlbums: List<Pair<Album, String>>,
    onDismiss: () -> Unit,
    onTargetSelected: (Album, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Move to album",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "%1$,d selected".format(itemCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            if (targetAlbums.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Create another album before moving these items.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 390.dp)) {
                    items(targetAlbums, key = { (album, _) -> album.id }) { (album, relativePath) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .bouncyClickable { onTargetSelected(album, relativePath) },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GalleryImage(
                                    imageRes = album.coverRes,
                                    imageUri = album.coverUri,
                                    contentDescription = "",
                                    modifier = Modifier.size(52.dp),
                                    cornerRadius = 13.dp,
                                    thumbnailSize = 160
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "%1$,d items".format(album.itemCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun GallerySettingsDialog(
    settings: GallerySettings,
    mediaManagementAccessStatus: MediaManagementAccessStatus,
    onRequestMediaManagementAccess: () -> Unit,
    onSettingsChange: (GallerySettings) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scaleAnim = remember { Animatable(0.85f) }
    val offsetYAnim = remember { Animatable(48f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = MotionSpecs.AppleSpringModal
            )
        }
        launch {
            offsetYAnim.animateTo(
                targetValue = 0f,
                animationSpec = MotionSpecs.AppleSpringModal
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = MotionSpecs.AppleSpringSheet
            )
        }
    }

    fun dismissWithAnimation() {
        dialogScope.launch {
            val scaleJob = launch {
                scaleAnim.animateTo(0.88f, spring(dampingRatio = 0.85f, stiffness = 500f))
            }
            val offsetJob = launch {
                offsetYAnim.animateTo(32f, spring(dampingRatio = 0.85f, stiffness = 500f))
            }
            val alphaJob = launch {
                alphaAnim.animateTo(0f, tween(140))
            }
            scaleJob.join()
            offsetJob.join()
            alphaJob.join()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::dismissWithAnimation,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::dismissWithAnimation
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        translationY = offsetYAnim.value
                        alpha = alphaAnim.value
                        cameraDistance = 16f * density.density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 24.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                    description = String.format(Locale.getDefault(), "%1\$d columns across Photos & Albums", density.photoColumns),
                                    selected = settings.gridDensity == density,
                                    onClick = { onSettingsChange(settings.copy(gridDensity = density)) }
                                )
                            }
                        }

                        SettingsSectionTitle("Folder animation")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FolderTransitionStyle.entries.forEach { style ->
                                SettingsChoiceRow(
                                    label = style.label(),
                                    description = style.description(),
                                    selected = settings.folderTransitionStyle == style,
                                    onClick = { onSettingsChange(settings.copy(folderTransitionStyle = style)) }
                                )
                            }
                        }

                        SettingsSectionTitle("Photo animation")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ViewerTransitionStyle.entries.forEach { style ->
                                SettingsChoiceRow(
                                    label = style.label,
                                    description = style.description,
                                    selected = settings.viewerTransitionStyle == style,
                                    onClick = { onSettingsChange(settings.copy(viewerTransitionStyle = style)) }
                                )
                            }
                        }

                        if (
                            mediaManagementAccessStatus.state == MediaManagementAccessState.Granted ||
                            mediaManagementAccessStatus.state == MediaManagementAccessState.Requestable
                        ) {
                            SettingsSectionTitle("Media changes")
                            MediaManagementSettingsRow(
                                status = mediaManagementAccessStatus,
                                onRequestAccess = onRequestMediaManagementAccess
                            )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = ::dismissWithAnimation,
                            modifier = Modifier.solarTactile(scaleDown = 0.90f, onClick = ::dismissWithAnimation)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaManagementSettingsRow(
    status: MediaManagementAccessStatus,
    onRequestAccess: () -> Unit
) {
    val granted = status.isGranted
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (granted) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.68f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (granted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 11.dp, end = 8.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Direct media changes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (granted) {
                        "Enabled. Locking, moving, and deleting can skip repeated Android confirmations."
                    } else {
                        "Turn on Android's media-management switch once to remove repeated confirmation blocks."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Direct media changes enabled",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = onRequestAccess) {
                    Text("Set up", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
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
        modifier = modifier
            .semantics { this.selected = selected }
            .bouncyClickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
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
            .semantics(mergeDescendants = true) { this.selected = selected }
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
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
                tint = MaterialTheme.colorScheme.secondary
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
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
        GalleryGridDensity.Single -> "Single"
        GalleryGridDensity.Spacious -> "Spacious"
        GalleryGridDensity.Comfortable -> "Comfort"
        GalleryGridDensity.Compact -> "Compact"
        GalleryGridDensity.Dense -> "Dense"
        GalleryGridDensity.Micro -> "Micro"
    }
}

private fun GalleryGridDensity.defaultAlbumGridMode(): AlbumDetailGridMode {
    return when (this) {
        GalleryGridDensity.Single -> AlbumDetailGridMode.Single
        GalleryGridDensity.Spacious -> AlbumDetailGridMode.Spacious
        GalleryGridDensity.Comfortable -> AlbumDetailGridMode.Comfortable
        GalleryGridDensity.Compact -> AlbumDetailGridMode.Compact
        GalleryGridDensity.Dense -> AlbumDetailGridMode.Dense
        GalleryGridDensity.Micro -> AlbumDetailGridMode.Micro
    }
}

private fun FolderTransitionStyle.label(): String = this.label

private fun FolderTransitionStyle.description(): String = this.description
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
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }
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
            style = MaterialTheme.typography.headlineLarge,
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
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "v$versionName",
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
                top = 48.dp,
                end = horizontalPadding,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item(key = "cleanup_header") {
            Column {
                GalleryScreenHeader(
                    title = "Cleanup",
                    subtitle = "Review candidates before Android moves anything to trash.",
                    onBack = onBack
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
                modifier = Modifier.size(20.dp)
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
@Composable
private fun rememberSeparatingFoldBounds(context: Context): android.graphics.Rect? {
    val activity = remember(context) { context.findActivity() } ?: return null
    val foldBoundsFlow = remember(context, activity) {
        WindowInfoTracker.getOrCreate(context)
            .windowLayoutInfo(activity)
            .map { layoutInfo ->
                layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull { feature ->
                        feature.isSeparating ||
                            feature.occlusionType == FoldingFeature.OcclusionType.FULL
                    }
                    ?.bounds
                    ?.let { bounds -> android.graphics.Rect(bounds) }
            }
    }
    val foldBounds by foldBoundsFlow.collectAsStateWithLifecycle(initialValue = null)
    return foldBounds
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
    committed: Boolean,
    rootWidthPx: Float,
    rootHeightPx: Float,
    rootBoundsInWindow: Rect,
    transitionStyle: FolderTransitionStyle = FolderTransitionStyle.Normal,
    onCommitted: (AlbumTransitionSpec) -> Unit,
    onFinished: (AlbumTransitionSpec) -> Unit,
    content: @Composable (Album) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val sourceBounds = transition.tileBounds.relativeToRoot(
        rootBoundsInWindow = rootBoundsInWindow,
        fallbackRootWidth = rootWidthPx,
        fallbackRootHeight = rootHeightPx
    )
    if (!sourceBounds.isUsableTransitionBounds()) {
        LaunchedEffect(transition.key) {
            if (!committed) onCommitted(transition)
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

    LaunchedEffect(transition.key) {
        val targetValue = if (transition.mode == AlbumTransitionMode.Closing) 0f else 1f
        val isOpening = transition.mode == AlbumTransitionMode.Opening
        val folderSpring = when (transitionStyle) {
            FolderTransitionStyle.Normal -> if (isOpening) MotionSpecs.AppleSpringFolder else MotionSpecs.AppleSpringFolderClose
            FolderTransitionStyle.Fast -> if (isOpening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast
        }
        progress.animateTo(
            targetValue = targetValue,
            animationSpec = folderSpring
        )
        onFinished(transition)
    }

    val density = LocalDensity.current
    val rootBounds = Rect(0f, 0f, rootWidthPx, rootHeightPx)
    val rootWidthDp = with(density) { rootWidthPx.toDp() }
    val rootHeightDp = with(density) { rootHeightPx.toDp() }
    val reusableClippingPath = remember { Path() }
    val sourceWidthDp = with(density) { sourceBounds.width.toDp() }
    val sourceHeightDp = with(density) { sourceBounds.height.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(transition.key) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .drawBehind {
                val expansion = progress.value.coerceIn(0f, 1f)
                val scrimAlpha = GalleryMotion.smoothstep(0.04f, 0.35f, expansion) *
                    (1f - GalleryMotion.smoothstep(0.65f, 0.95f, expansion)) * 0.22f
                if (scrimAlpha > 0.001f) {
                    drawRect(Color.Black.copy(alpha = scrimAlpha))
                }
            }
            .testTag("AlbumTransitionOverlay")
    ) {
        Box(
            modifier = Modifier
                .requiredWidth(rootWidthDp)
                .requiredHeight(rootHeightDp)
                .graphicsLayer {
                    val expansion = progress.value.coerceIn(0f, 1f)
                    val heroBounds = lerpRect(sourceBounds, rootBounds, expansion)
                    val liftProgress = GalleryMotion.smoothstep(0.04f, 0.35f, expansion) *
                        (1f - GalleryMotion.smoothstep(0.70f, 1f, expansion))
                    val detailAlpha = GalleryMotion.smoothstep(0.08f, 0.70f, expansion)
                    val transitElevationPx = 18.dp.toPx() * liftProgress

                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    translationX = heroBounds.center.x - rootWidthPx / 2f
                    translationY = heroBounds.center.y - rootHeightPx / 2f
                    val contentScale = lerp(0.88f, 1f, expansion)
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = detailAlpha
                    shadowElevation = transitElevationPx
                    cameraDistance = 16f * density.density
                }
                .drawWithContent {
                    val expansion = progress.value.coerceIn(0f, 1f)
                    val heroBounds = lerpRect(sourceBounds, rootBounds, expansion)
                    val heroWidth = heroBounds.width.coerceAtLeast(1f)
                    val heroHeight = heroBounds.height.coerceAtLeast(1f)
                    val cornerRadius = lerp(transition.sourceCornerRadiusDp, 0f, expansion).dp
                    val cornerRadiusPx = cornerRadius.toPx()
                    val clipLeft = (rootWidthPx - heroWidth) / 2f
                    val clipTop = (rootHeightPx - heroHeight) / 2f
                    val clipRight = clipLeft + heroWidth
                    val clipBottom = clipTop + heroHeight

                    reusableClippingPath.reset()
                    reusableClippingPath.addRoundRect(
                        RoundRect(
                            rect = Rect(clipLeft, clipTop, clipRight, clipBottom),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                    clipPath(reusableClippingPath) { this@drawWithContent.drawContent() }
                }
                .background(MaterialTheme.colorScheme.background)
        ) {
            content(transition.album)
        }
        // Keep the cover as a lightweight layer over the full-screen preview. The preview itself
        // is laid out once and transformed by the GPU instead of remeasuring a LazyColumn on
        // every frame of both the open and close animations.
        Box(
            modifier = Modifier
                .requiredWidth(sourceWidthDp)
                .requiredHeight(sourceHeightDp)
                .graphicsLayer {
                    val expansion = progress.value.coerceIn(0f, 1f)
                    val heroBounds = lerpRect(sourceBounds, rootBounds, expansion)
                    val heroWidth = heroBounds.width.coerceAtLeast(1f)
                    val heroHeight = heroBounds.height.coerceAtLeast(1f)
                    val liftProgress = GalleryMotion.smoothstep(0.04f, 0.35f, expansion) *
                        (1f - GalleryMotion.smoothstep(0.70f, 1f, expansion))
                    val cornerRadius = lerp(transition.sourceCornerRadiusDp, 0f, expansion).dp
                    val transitElevationPx = 18.dp.toPx() * liftProgress

                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = heroBounds.left
                    translationY = heroBounds.top
                    scaleX = heroWidth / sourceBounds.width.coerceAtLeast(1f)
                    scaleY = heroHeight / sourceBounds.height.coerceAtLeast(1f)
                    shadowElevation = transitElevationPx
                    cameraDistance = 16f * density.density
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                }
        ) {
            GalleryImage(
                imageRes = transition.album.coverRes,
                imageUri = transition.album.coverUri,
                contentDescription = transition.album.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val expansion = progress.value.coerceIn(0f, 1f)
                        alpha = 1f - GalleryMotion.smoothstep(0.12f, 0.60f, expansion)
                    },
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = 512,
                loadQuality = ImageLoadQuality.Thumbnail,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                cachedOnly = true,
                animatedPlaceholder = false,
                isVideo = transition.album.hasVideoBadge
            )
        }
    }
}

private val ReferenceViewerOpenEasing = CubicBezierEasing(0.16f, 0.82f, 0.22f, 1f)
private val ReferenceViewerCloseEasing = CubicBezierEasing(0.30f, 0f, 0.58f, 1f)

@Composable
private fun ReferenceMediaOpenOverlay(
    transition: MediaOpenTransitionSpec?,
    viewerTransitionStyle: ViewerTransitionStyle = ViewerTransitionStyle.Normal,
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
    LaunchedEffect(transition.key) {
        val damping = if (viewerTransitionStyle == ViewerTransitionStyle.Fast) {
            GalleryMotion.MediaOpenDampingFast
        } else {
            GalleryMotion.MediaOpenDamping
        }
        val stiffness = if (viewerTransitionStyle == ViewerTransitionStyle.Fast) {
            GalleryMotion.MediaOpenStiffnessFast
        } else {
            GalleryMotion.MediaOpenStiffness
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = damping,
                stiffness = stiffness
            )
        )
        onFinished(transition)
    }

    val heroThumbnailSize = if (
        transition.actionMode == ViewerActionMode.Locked || transition.mediaItem.isVideo
    ) 512 else photoDecodeSize
    val decodedHeroBitmap = transition.transitionMediaItem.contentUri?.let { uri ->
        ThumbnailMemoryCache.get(ThumbnailMemoryCache.key(uri, heroThumbnailSize))
            ?: ThumbnailMemoryCache.getNearest(uri, 512, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 384, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 256, ImageLoadQuality.Thumbnail)
    }
    val geometryMediaItem = decodedHeroBitmap?.let { bitmap ->
        transition.mediaItem.copy(width = bitmap.width, height = bitmap.height)
    } ?: transition.mediaItem
    val expansion = progress.value.coerceIn(0f, 1f)
    val cornerRadius = 0.dp
    val liftProgress = GalleryMotion.smoothstep(0.04f, 0.35f, expansion) *
        (1f - GalleryMotion.smoothstep(0.70f, 1f, expansion))
    val shadowElevation = (18f * liftProgress).dp
    ReferenceMediaHeroFrame(
        mediaItem = transition.transitionMediaItem,
        startBounds = tileBounds,
        endBounds = fittedMediaRect(rootWidthPx, rootHeightPx, geometryMediaItem),
        progress = expansion,
        backdropAlpha = GalleryMotion.smoothstep(0f, 0.82f, expansion),
        cornerRadius = cornerRadius,
        shadowElevation = shadowElevation,
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
    viewerTransitionStyle: ViewerTransitionStyle = ViewerTransitionStyle.Normal,
    rootWidthPx: Float,
    rootHeightPx: Float,
    rootBoundsInWindow: Rect,
    onFinished: (MediaCloseTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val tileBounds = transition.tileBounds.relativeToRoot(
        rootBoundsInWindow = rootBoundsInWindow,
        fallbackRootWidth = rootWidthPx,
        fallbackRootHeight = rootHeightPx,
        clampToViewport = false
    )
    if (!tileBounds.isUsableTransitionBounds()) {
        LaunchedEffect(transition.key) {
            onFinished(transition)
        }
        return
    }
    val progress = remember(transition.key) { Animatable(0f) }

    LaunchedEffect(transition.key) {
        val springSpec = if (viewerTransitionStyle == ViewerTransitionStyle.Fast) {
            GalleryMotion.ViewerHeroCloseSpringFast
        } else {
            GalleryMotion.ViewerHeroCloseSpring
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = springSpec
        )
        onFinished(transition)
    }

    val decodedHeroBitmap = transition.mediaItem.contentUri?.let { uri ->
        ThumbnailMemoryCache.getNearest(uri, 512, ImageLoadQuality.HighQuality)
            ?: ThumbnailMemoryCache.getNearest(uri, 512, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 384, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 256, ImageLoadQuality.Thumbnail)
    }
    val effectiveWidth = transition.mediaItem.width?.takeIf { it > 0 }
        ?: decodedHeroBitmap?.width
    val effectiveHeight = transition.mediaItem.height?.takeIf { it > 0 }
        ?: decodedHeroBitmap?.height
    val geometryMediaItem = if (effectiveWidth != null && effectiveHeight != null) {
        transition.mediaItem.copy(width = effectiveWidth, height = effectiveHeight)
    } else if (transition.mediaItem.isVideo) {
        transition.mediaItem.copy(width = 16, height = 9)
    } else {
        transition.mediaItem
    }

    val collapse = progress.value.coerceIn(0f, 1f)
    val dismissFraction = ((1f - transition.startScale) / (1f - 0.72f)).coerceIn(0f, 1f)
    val startRadius = if (transition.startScale < 0.99f) {
        lerp(18f, 28f, dismissFraction)
    } else {
        24f
    }
    val cornerRadius = lerp(startRadius, 0f, GalleryMotion.smoothstep(0.20f, 1f, collapse)).dp
    val shadowElevation = (lerp(14f, 20f, dismissFraction) * (1f - GalleryMotion.smoothstep(0.80f, 1f, collapse))).dp
    val rotZ = lerp(transition.startRotationZ, 0f, GalleryMotion.smoothstep(0f, 0.85f, collapse))
    val rotX = lerp(transition.startRotationX, 0f, GalleryMotion.smoothstep(0f, 0.85f, collapse))

    val startBounds = scaledRectAroundCenter(
        rect = fittedMediaRect(rootWidthPx, rootHeightPx, geometryMediaItem),
        scale = transition.startScale.coerceIn(0.68f, 1f),
        offset = transition.startOffset
    )
    ReferenceMediaHeroFrame(
        mediaItem = geometryMediaItem,
        startBounds = startBounds,
        endBounds = tileBounds,
        progress = collapse,
        backdropAlpha = transition.startBackdropAlpha *
            (1f - GalleryMotion.smoothstep(0.12f, 0.92f, collapse)),
        cornerRadius = cornerRadius,
        shadowElevation = shadowElevation,
        rotationZ = rotZ,
        rotationX = rotX,
        cachedOnly = false
    )
}

@Composable
private fun ReferenceMediaHeroFrame(
    mediaItem: MediaItem,
    startBounds: Rect,
    endBounds: Rect,
    progress: Float,
    backdropAlpha: Float,
    cornerRadius: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    rotationZ: Float = 0f,
    rotationX: Float = 0f,
    thumbnailSize: Int = 512,
    loadQuality: ImageLoadQuality = ImageLoadQuality.Thumbnail,
    cachedOnly: Boolean = true
) {
    val density = LocalDensity.current
    val fraction = progress.coerceIn(0f, 1f)
    val heroBounds = lerpRect(startBounds, endBounds, fraction)
    val heroWidth = heroBounds.width.coerceAtLeast(1f)
    val heroHeight = heroBounds.height.coerceAtLeast(1f)
    val decodedHeroBitmap = mediaItem.contentUri?.let { uri ->
        ThumbnailMemoryCache.getNearest(uri, thumbnailSize, loadQuality)
            ?: ThumbnailMemoryCache.getNearest(uri, 512, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 384, ImageLoadQuality.Thumbnail)
            ?: ThumbnailMemoryCache.getNearest(uri, 256, ImageLoadQuality.Thumbnail)
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
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    translationX = heroBounds.left
                    translationY = heroBounds.top
                    this.rotationZ = rotationZ
                    this.rotationX = rotationX
                    this.cameraDistance = 16f * density.density
                    this.shadowElevation = with(density) { shadowElevation.toPx() }
                    clip = cornerRadius > 0.dp
                    shape = RoundedCornerShape(cornerRadius)
                }
                .background(Color.Black)
        ) {
            if (decodedHeroBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = decodedHeroBitmap.asImageBitmap(),
                    contentDescription = mediaItem.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
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
                    cachedOnly = cachedOnly,
                    fallbackImageUri = mediaItem.previewUri ?: mediaItem.contentUri,
                    isVideo = mediaItem.isVideo,
                    animatedPlaceholder = false
                )
            }
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
    fallbackRootHeight: Float,
    clampToViewport: Boolean = true
): Rect {
    if (!isUsableTransitionBounds()) return Rect.Zero

    val rootWidth = fallbackRootWidth.coerceAtLeast(1f)
    val rootHeight = fallbackRootHeight.coerceAtLeast(1f)
    val rootBoundsAreUsable = rootBoundsInWindow.isUsableTransitionBounds()
    val originX = if (rootBoundsAreUsable) rootBoundsInWindow.left else 0f
    val originY = if (rootBoundsAreUsable) rootBoundsInWindow.top else 0f
    val width = width.coerceIn(1f, rootWidth)
    val height = height.coerceIn(1f, rootHeight)
    val rawLeft = this.left - originX
    val rawTop = this.top - originY
    val left = if (clampToViewport) rawLeft.coerceIn(0f, (rootWidth - width).coerceAtLeast(0f)) else rawLeft
    val top = if (clampToViewport) rawTop.coerceIn(0f, (rootHeight - height).coerceAtLeast(0f)) else rawTop
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
    val tabWidth = 78.dp
    val tabHeight = 48.dp
    val tabGap = 2.dp
    val contentWidth = tabWidth * 3 + tabGap * 2
    val density = LocalDensity.current
    val hideDistancePx = with(density) { 88.dp.toPx() }
    val tabStepPx = with(density) { (tabWidth + tabGap).toPx() }

    val visibilityProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 360f),
        label = "bottom nav visibility"
    )

    val targetPage = selectedTab.pageIndex().toFloat()
    var isDragging by remember { mutableStateOf(false) }
    var dragPage by remember { mutableStateOf<Float?>(null) }
    val effectiveTarget = dragPage ?: targetPage

    val headState = animateFloatAsState(
        targetValue = effectiveTarget,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.90f, stiffness = 1200f)
        } else {
            spring(dampingRatio = 0.76f, stiffness = 650f)
        },
        label = "bottomNavHead"
    )
    val tailState = animateFloatAsState(
        targetValue = effectiveTarget,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.92f, stiffness = 950f)
        } else {
            spring(dampingRatio = 0.88f, stiffness = 320f)
        },
        label = "bottomNavTail"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 700f),
        label = "bottomNavPressScale"
    )

    val tabs = remember {
        listOf(
            Triple(GalleryTab.Photos, Icons.Filled.Image, "Photos"),
            Triple(GalleryTab.Albums, Icons.Filled.Collections, "Albums"),
            Triple(GalleryTab.Menu, Icons.Filled.Menu, "Menu")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 4.dp)
            .graphicsLayer {
                alpha = visibilityProgress
                translationY = hideDistancePx * (1f - visibilityProgress)
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 220.dp)
                .clip(containerShape),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = containerShape,
            tonalElevation = 0.dp,
            shadowElevation = 14.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .width(contentWidth)
                    .height(tabHeight)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isDragging = true
                            val startX = down.position.x
                            val page = (startX / tabStepPx).coerceIn(0f, 2f)
                            dragPage = page
                            val initialIdx = (page + 0.35f).toInt().coerceIn(0, 2)
                            val initialTab = tabs[initialIdx].first
                            if (initialTab != selectedTab) {
                                onTabSelected(initialTab)
                            }
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == pointerId } ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    break
                                }
                                val currentX = change.position.x
                                val curPage = (currentX / tabStepPx).coerceIn(0f, 2f)
                                dragPage = curPage
                                val curIdx = (curPage + 0.35f).toInt().coerceIn(0, 2)
                                val curTab = tabs[curIdx].first
                                if (curTab != selectedTab) {
                                    onTabSelected(curTab)
                                }
                            }
                            isDragging = false
                            dragPage = null
                        }
                    }
            ) {
                val baseTabWidthPx = with(density) { tabWidth.toPx() }
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(tabHeight)
                        .graphicsLayer {
                            val h = headState.value
                            val t = tailState.value
                            val stretchPx = kotlin.math.abs(h - t) * tabStepPx * 0.42f
                            val minP = kotlin.math.min(h, t)
                            translationX = minP * tabStepPx
                            scaleX = (1f + (stretchPx / baseTabWidthPx)) * pressScale
                            scaleY = pressScale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .clip(tabShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tabGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, (tab, icon, label) ->
                        GalleryNavigationItem(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(tabHeight)
                                .graphicsLayer {
                                    val h = headState.value
                                    val proximity = (1f - kotlin.math.abs(h - index)).coerceIn(0f, 1f)
                                    val mag = 1f + proximity * 0.08f
                                    scaleX = mag
                                    scaleY = mag
                                },
                            selected = selectedTab == tab,
                            icon = icon,
                            label = label
                        )
                    }
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
        modifier = Modifier.solarTactile(scaleDown = 0.90f, onClick = onClick),
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
    label: String
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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
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
