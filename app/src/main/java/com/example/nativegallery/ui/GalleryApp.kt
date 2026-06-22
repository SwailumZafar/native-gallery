package com.example.nativegallery.ui

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.FakeGalleryRepository
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.data.MediaPermissions
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.GalleryImage
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

private enum class MediaTransitionMode {
    Opening,
    Closing
}

private data class MediaTransitionSpec(
    val key: Int,
    val mediaItem: MediaItem,
    val tileBounds: Rect,
    val mode: MediaTransitionMode
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val mediaStoreRepository = remember(context) { MediaStoreGalleryRepository(context) }
    val hiddenRepository = remember(context) { HiddenAlbumsRepository(context) }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }
    val recentlyDeletedMedia = remember { mutableStateMapOf<String, RecentlyDeletedMedia>() }
    val albumTileBounds = remember { mutableStateMapOf<String, Rect>() }
    val mediaTileBounds = remember { mutableStateMapOf<String, Rect>() }
    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.Photos) }
    var destination by rememberSaveable { mutableStateOf(GalleryDestination.Main) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }
    var mediaAccess by remember { mutableStateOf(MediaPermissions.currentAccess(context)) }
    var mediaStoreSnapshot by remember { mutableStateOf<GallerySnapshot?>(null) }
    var viewerMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var viewerVisible by remember { mutableStateOf(false) }
    var albumTransition by remember { mutableStateOf<AlbumTransitionSpec?>(null) }
    var albumTransitionKey by remember { mutableIntStateOf(0) }
    var mediaTransition by remember { mutableStateOf<MediaTransitionSpec?>(null) }
    var mediaTransitionKey by remember { mutableIntStateOf(0) }
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

    LaunchedEffect(viewerVisible, mediaTransition?.key) {
        if (!viewerVisible && viewerMediaItem != null && mediaTransition?.mode != MediaTransitionMode.Opening) {
            delay(320)
            if (!viewerVisible && mediaTransition?.mode != MediaTransitionMode.Opening) {
                viewerMediaItem = null
                viewerMediaItems = emptyList()
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
    val hideableAlbums = activeSnapshot.albums.filterNot { it.isAllPhotos }

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
    val visibleAlbums = visibleAlbums(activeSnapshot.albums, visibleMedia, hiddenAlbumIds)
    val selectedAlbum = visibleAlbums.firstOrNull { it.id == selectedAlbumId }
    val selectedAlbumMedia = selectedAlbum?.let { mediaForAlbum(it, visibleMedia) }.orEmpty()

    fun openPhotos() {
        selectedTab = GalleryTab.Photos
        destination = GalleryDestination.Main
    }

    fun openAlbums() {
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.Main
    }

    fun openRecentlyDeleted() {
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.RecentlyDeleted
    }

    fun startAlbumOpen(album: Album, bounds: Rect) {
        selectedAlbumId = album.id
        selectedTab = GalleryTab.Albums
        if (bounds != Rect.Zero) {
            albumTileBounds[album.id] = bounds
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = album,
                tileBounds = bounds,
                mode = AlbumTransitionMode.Opening
            )
        } else {
            destination = GalleryDestination.AlbumDetail
        }
    }

    fun closeAlbumDetail() {
        val closingAlbum = selectedAlbum
        val closingBounds = selectedAlbumId?.let { albumTileBounds[it] }
        selectedTab = GalleryTab.Albums
        destination = GalleryDestination.Main
        if (closingAlbum != null && closingBounds != null && closingBounds != Rect.Zero) {
            albumTransitionKey += 1
            albumTransition = AlbumTransitionSpec(
                key = albumTransitionKey,
                album = closingAlbum,
                tileBounds = closingBounds,
                mode = AlbumTransitionMode.Closing
            )
        }
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

    fun startViewerOpen(mediaItem: MediaItem, mediaItems: List<MediaItem>, bounds: Rect) {
        viewerMediaItems = mediaItems
        viewerMediaItem = mediaItem
        if (bounds != Rect.Zero) {
            mediaTileBounds[mediaItem.id] = bounds
        }
        mediaTransition = null
        viewerVisible = true
    }

    fun closeViewer() {
        mediaTransition = null
        viewerVisible = false
    }

    fun completeMediaDelete(mediaItem: MediaItem, direction: Int) {
        val currentItems = if (viewerMediaItems.isNotEmpty()) viewerMediaItems else visibleMedia
        val currentIndex = currentItems.indexOfFirst { it.id == mediaItem.id }
        val remainingItems = currentItems.filterNot { it.id == mediaItem.id }
        recentlyDeletedMedia[mediaItem.id] = RecentlyDeletedMedia(
            mediaItem = mediaItem,
            deletedAtMillis = System.currentTimeMillis()
        )
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

    fun requestMediaDelete(mediaItem: MediaItem, direction: Int) {
        completeMediaDelete(mediaItem, direction)
    }

    fun restoreDeletedMedia(entry: RecentlyDeletedMedia) {
        recentlyDeletedMedia.remove(entry.mediaItem.id)
    }

    fun restoreAllDeletedMedia() {
        recentlyDeletedMedia.clear()
    }

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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (destination == GalleryDestination.Main && !viewerVisible) {
                    GalleryBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
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
                                        mediaItems = visibleMedia,
                                        contentPadding = innerPadding,
                                        mediaAccessNotice = mediaAccessNotice,
                                        isLoading = isLoadingMedia,
                                        onMediaClick = { mediaItem, bounds ->
                                            startViewerOpen(mediaItem, visibleMedia, bounds)
                                        }
                                    )
                                    GalleryTab.Albums -> AlbumsScreen(
                                        albums = visibleAlbums,
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
                                        mediaAccessNotice = mediaAccessNotice,
                                        isLoading = isLoadingMedia
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
                                onMediaClick = { mediaItem, bounds ->
                                    startViewerOpen(mediaItem, selectedAlbumMedia, bounds)
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
                    if (finishedTransition.mode == AlbumTransitionMode.Opening) {
                        destination = GalleryDestination.AlbumDetail
                    }
                    albumTransition = null
                }
            }
        )

        MediaTouchOriginTransitionOverlay(
            transition = mediaTransition,
            rootWidthPx = rootWidthPx,
            rootHeightPx = rootHeightPx,
            onFinished = { finishedTransition ->
                if (mediaTransition?.key == finishedTransition.key) {
                    if (finishedTransition.mode == MediaTransitionMode.Opening) {
                        viewerVisible = true
                    }
                    mediaTransition = null
                }
            }
        )

        PhotoViewerOverlay(
            mediaItems = viewerMediaItems,
            mediaItem = viewerMediaItem,
            visible = viewerVisible,
            onClose = ::closeViewer,
            onDelete = ::requestMediaDelete,
            onCurrentMediaChanged = { currentItem -> viewerMediaItem = currentItem },
            albumNameForMedia = { item -> albumNameById[item.albumId] }
        )
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
                start = 24.dp,
                top = 96.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            )
    ) {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(28.dp))
        GalleryMenuRow(
            icon = Icons.Filled.Delete,
            label = "Recently deleted",
            onClick = onOpenRecentlyDeleted
        )
        Spacer(Modifier.height(12.dp))
        GalleryMenuRow(
            icon = Icons.Filled.Folder,
            label = "Hidden items",
            onClick = onOpenHiddenItems
        )
        Spacer(Modifier.height(12.dp))
        GalleryMenuRow(
            icon = Icons.Filled.Settings,
            label = "Settings",
            onClick = {}
        )
    }
}

@Composable
private fun GalleryMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bouncyClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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

private fun visibleAlbums(
    albums: List<Album>,
    visibleMedia: List<MediaItem>,
    hiddenAlbumIds: Set<String>
): List<Album> {
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
            else -> album
        }
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
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMedium
            )
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
    val coverAlpha = (1f - ((easedProgress - 0.12f) / 0.46f)).coerceIn(0f, 1f)
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
                }
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = "%1$,d".format(transition.album.itemCount),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MediaTouchOriginTransitionOverlay(
    transition: MediaTransitionSpec?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onFinished: (MediaTransitionSpec) -> Unit
) {
    if (transition == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val density = LocalDensity.current
    val progress = remember(transition.key) {
        Animatable(if (transition.mode == MediaTransitionMode.Closing) 1f else 0f)
    }

    LaunchedEffect(transition.key) {
        progress.animateTo(
            targetValue = if (transition.mode == MediaTransitionMode.Closing) 0f else 1f,
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMedium
            )
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
    val radius = lerp(10f, 0f, easedProgress)

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
                }
                .background(Color.Black)
        ) {
            GalleryImage(
                imageRes = transition.mediaItem.imageRes,
                imageUri = transition.mediaItem.contentUri,
                contentDescription = transition.mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                thumbnailSize = 384
            )
        }
    }
}
@Composable
private fun GalleryBottomBar(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(326.dp)
                .height(68.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(38.dp),
            tonalElevation = 3.dp,
            shadowElevation = 12.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            ) {
                val itemWidth = maxWidth / 3
                val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
                val indicatorProgress by animateFloatAsState(
                    targetValue = selectedTab.pageIndex().toFloat(),
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "bottom nav selected progress"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer { translationX = itemWidthPx * indicatorProgress }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GalleryNavigationItem(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        selected = selectedTab == GalleryTab.Photos,
                        icon = Icons.Filled.PhotoLibrary,
                        label = "Photos",
                        onClick = { onTabSelected(GalleryTab.Photos) }
                    )
                    GalleryNavigationItem(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        selected = selectedTab == GalleryTab.Albums,
                        icon = Icons.Filled.Folder,
                        label = "Albums",
                        onClick = { onTabSelected(GalleryTab.Albums) }
                    )
                    GalleryNavigationItem(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
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
private fun GalleryNavigationItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(23.dp)
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 0.dp),
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
