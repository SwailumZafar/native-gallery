@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.data.FakeGalleryRepository
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.data.MediaPermissions
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.MediaAccessNotice
import com.example.nativegallery.ui.components.ResourceImage
import com.example.nativegallery.ui.components.bouncyClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
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


@OptIn(ExperimentalSharedTransitionApi::class)
private val GalleryMediaBoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val mediaStoreRepository = remember(context) { MediaStoreGalleryRepository(context) }
    val hiddenRepository = remember(context) { HiddenAlbumsRepository(context) }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }
    val recentlyDeletedMedia = remember { mutableStateMapOf<String, RecentlyDeletedMedia>() }
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
    var albumTransition by remember { mutableStateOf<AlbumTransitionSpec?>(null) }
    var albumTransitionKey by remember { mutableIntStateOf(0) }
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
                mediaStoreRepository.loadGallery(mediaAccess.mediaKinds)
            }
        } else {
            mediaStoreSnapshot = null
        }
    }

    LaunchedEffect(viewerVisible) {
        if (!viewerVisible && viewerMediaItem != null) {
            delay(340)
            if (!viewerVisible) {
                viewerMediaItem = null
                viewerMediaItems = emptyList()
                viewerSharedElementKey = null
                viewerSharedElementKeyPrefix = null
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
    val hideableAlbums = hiddenManageableAlbums(
        albums = activeSnapshot.albums,
        mediaItems = activeSnapshot.mediaItems.filterNot { recentlyDeletedMedia.containsKey(it.id) }
    )

    LaunchedEffect(hideableAlbums.map { it.id }) {
        hideableAlbums.forEach { album ->
            if (!hiddenStates.containsKey(album.id)) {
                hiddenStates[album.id] = hiddenRepository.initialHiddenAlbumIds().contains(album.id)
            }
        }
    }

    val hiddenAlbumIds = hiddenStates.filterValues { it }.keys.toSet()
    val recentlyDeletedItems = recentlyDeletedMedia.values.sortedByDescending { it.deletedAtMillis }
    val albumNameById = activeSnapshot.albums.associate { it.id to it.name }
    val visibleMedia = visibleMedia(activeSnapshot.mediaItems, hiddenAlbumIds)
        .filterNot { recentlyDeletedMedia.containsKey(it.id) }
    val visibleAlbums = visibleAlbums(
        albums = activeSnapshot.albums,
        allMedia = activeSnapshot.mediaItems,
        visibleMedia = visibleMedia,
        hiddenAlbumIds = hiddenAlbumIds
    )
    val searchedVisibleMedia = searchMedia(visibleMedia, albumNameById, gallerySearchQuery)
    val searchedVisibleAlbums = searchAlbums(visibleAlbums, visibleMedia, gallerySearchQuery)
    val selectedAlbum = visibleAlbums.firstOrNull { it.id == selectedAlbumId }
    val selectedAlbumMedia = selectedAlbum?.let { mediaForAlbum(it, visibleMedia) }.orEmpty()
    val selectedMediaItems = activeSnapshot.mediaItems.filter { mediaItem ->
        selectedMediaIds.contains(mediaItem.id) && !recentlyDeletedMedia.containsKey(mediaItem.id)
    }


    LaunchedEffect(visibleMedia.map { it.id }) {
        val visibleIds = visibleMedia.map { it.id }.toSet()
        if (selectedMediaIds.any { it !in visibleIds }) {
            selectedMediaIds = selectedMediaIds.intersect(visibleIds)
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

    fun startAlbumOpen(album: Album, bounds: Rect) {
        val tileBounds = bounds.takeIf { it != Rect.Zero } ?: albumTileBounds[album.id]
        selectedAlbumId = album.id
        selectedTab = GalleryTab.Albums
        if (tileBounds != null && tileBounds != Rect.Zero) {
            albumTileBounds[album.id] = tileBounds
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = album,
                tileBounds = tileBounds,
                mode = AlbumTransitionMode.Opening
            )
        }
        destination = GalleryDestination.AlbumDetail
    }

    fun closeAlbumDetail() {
        val closingAlbum = selectedAlbum
        val closingBounds = selectedAlbumId?.let { albumTileBounds[it] }
        selectedMediaIds = emptySet()
        if (closingAlbum != null && closingBounds != null && closingBounds != Rect.Zero) {
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

    fun startViewerOpen(
        mediaItem: MediaItem,
        mediaItems: List<MediaItem>,
        bounds: Rect,
        sharedElementKey: Any? = null,
        sharedElementKeyPrefix: String? = null
    ) {
        viewerMediaItems = mediaItems
        viewerMediaItem = mediaItem
        viewerSharedElementKey = sharedElementKey
        viewerSharedElementKeyPrefix = sharedElementKeyPrefix
        if (bounds != Rect.Zero) {
            mediaTileBounds[mediaItem.id] = bounds
        }
        viewerVisible = true
    }

    fun closeViewer() {
        val currentItem = viewerMediaItem
        val keyPrefix = viewerSharedElementKeyPrefix
        if (currentItem != null && keyPrefix != null) {
            viewerSharedElementKey = "$keyPrefix-media-${currentItem.id}"
        }
        viewerVisible = false
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

    fun completeMediaDelete(mediaItem: MediaItem, direction: Int) {
        recentlyDeletedMedia[mediaItem.id] = RecentlyDeletedMedia(
            mediaItem = mediaItem,
            deletedAtMillis = System.currentTimeMillis()
        )
        selectedMediaIds = selectedMediaIds - mediaItem.id
        advanceViewerAfterRemoval(mediaItem, direction)
    }

    fun permanentlyDeleteMedia(mediaItem: MediaItem, direction: Int) {
        recentlyDeletedMedia.remove(mediaItem.id)
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
        val deletedAt = System.currentTimeMillis()
        selectedMediaItems.forEachIndexed { index, mediaItem ->
            recentlyDeletedMedia[mediaItem.id] = RecentlyDeletedMedia(
                mediaItem = mediaItem,
                deletedAtMillis = deletedAt + index
            )
        }
        selectedMediaIds = emptySet()
    }

    fun restoreDeletedMedia(entry: RecentlyDeletedMedia) {
        recentlyDeletedMedia.remove(entry.mediaItem.id)
    }

    fun restoreAllDeletedMedia() {
        recentlyDeletedMedia.clear()
    }

    fun deleteDeletedMedia(entry: RecentlyDeletedMedia) {
        recentlyDeletedMedia.remove(entry.mediaItem.id)
    }

    fun deleteAllDeletedMedia() {
        recentlyDeletedMedia.clear()
        if (destination == GalleryDestination.RecentlyDeleted && viewerVisible) {
            viewerVisible = false
        }
    }

    BackHandler(
        enabled = viewerVisible && viewerMediaItem != null,
        onBack = ::closeViewer
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
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = galleryVisibilityScope,
                                                sharedBoundsTransform = GalleryMediaBoundsTransform,
                                                activeSharedElementKey = viewerSharedElementKey,
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
                                                onOpenHiddenItems = { destination = GalleryDestination.HiddenItems },
                                                onOpenRecentlyDeleted = ::openRecentlyDeleted,
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
                                                onOpenHiddenItems = { destination = GalleryDestination.HiddenItems },
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
                                        activeSharedElementKey = viewerSharedElementKey,
                                        gridMode = albumDetailGridModes[selectedAlbum.id] ?: AlbumDetailGridMode.Compact,
                                        onGridModeChange = { gridMode -> albumDetailGridModes[selectedAlbum.id] = gridMode },
                                        selectedMediaIds = selectedMediaIds,
                                        onMediaLongClick = ::toggleMediaSelection,
                                        onMediaSelectionToggle = ::toggleMediaSelection,
                                        onSelectionClear = ::clearMediaSelection,
                                        onSelectAllVisible = { selectMedia(selectedAlbumMedia) },
                                        onDeleteSelected = ::deleteSelectedMedia,
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
                                onBack = ::openAlbums,
                                onHiddenChange = { album, hidden ->
                                    hiddenStates[album.id] = hidden
                                    hiddenRepository.setAlbumHidden(album.id, hidden)
                                },
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
                    onFinished = { finishedTransition ->
                        if (albumTransition?.key == finishedTransition.key) {
                            albumTransition = null
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
                    label = "Hidden items",
                    description = "Manage your private albums",
                    onClick = onOpenHiddenItems,
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
private fun visibleMedia(
    mediaItems: List<MediaItem>,
    hiddenAlbumIds: Set<String>
): List<MediaItem> {
    return mediaItems.filterNot { hiddenAlbumIds.contains(it.albumId) }
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
    onFinished: (AlbumTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val density = LocalDensity.current
    val progress = remember(transition.key) {
        Animatable(if (transition.mode == AlbumTransitionMode.Closing) 1f else 0f)
    }

    LaunchedEffect(transition.key) {
        progress.animateTo(
            targetValue = if (transition.mode == AlbumTransitionMode.Closing) 0f else 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        onFinished(transition)
    }

    val easedProgress = progress.value.coerceIn(0f, 1f)
    val startScaleX = ((transition.tileBounds.right - transition.tileBounds.left) / rootWidthPx)
        .coerceAtLeast(0.01f)
    val startScaleY = ((transition.tileBounds.bottom - transition.tileBounds.top) / rootHeightPx)
        .coerceAtLeast(0.01f)
    val scaleX = lerp(startScaleX, 1f, easedProgress)
    val scaleY = lerp(startScaleY, 1f, easedProgress)
    val translationX = lerp(transition.tileBounds.left, 0f, easedProgress)
    val translationY = lerp(transition.tileBounds.top, 0f, easedProgress)
    val radius = lerp(24f, 0f, easedProgress)
    val coverAlpha = (1f - ((easedProgress - 0.72f) / 0.22f)).coerceIn(0f, 1f)
    val labelAlpha = (1f - easedProgress * 1.35f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.translationX = translationX
                    this.translationY = translationY
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    clip = true
                    shape = RoundedCornerShape(with(density) { radius.toDp() })
                }        ) {
            ResourceImage(
                imageRes = transition.album.coverRes,
                imageUri = transition.album.coverUri,
                contentDescription = transition.album.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha },
                cornerRadius = 0.dp,
                thumbnailSize = 512
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.64f)),
                            startY = rootHeightPx * 0.45f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp)
                    .graphicsLayer { alpha = labelAlpha }
            ) {
                Text(
                    text = transition.album.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "%1$,d".format(transition.album.itemCount),
                    color = Color.White.copy(alpha = 0.9f),
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
                        animationSpec = spring(dampingRatio = 0.77f, stiffness = 380f),
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
                pressedScale = 0.9f,
                pressDampingRatio = 0.67f,
                pressStiffness = 500f,
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
