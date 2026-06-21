package com.example.nativegallery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.nativegallery.ui.components.MediaAccessNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class GalleryTab {
    Photos,
    Albums
}

private enum class GalleryDestination {
    Main,
    HiddenItems
}

@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val mediaStoreRepository = remember(context) { MediaStoreGalleryRepository(context) }
    val hiddenRepository = remember { HiddenAlbumsRepository() }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }
    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.Photos) }
    var destination by rememberSaveable { mutableStateOf(GalleryDestination.Main) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }
    var mediaAccess by remember { mutableStateOf(MediaPermissions.currentAccess(context)) }
    var mediaStoreSnapshot by remember { mutableStateOf<GallerySnapshot?>(null) }
    var viewerMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerVisible by remember { mutableStateOf(false) }

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
            delay(180)
            viewerMediaItem = null
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
    val visibleMedia = visibleMedia(activeSnapshot.mediaItems, hiddenAlbumIds)
    val visibleAlbums = visibleAlbums(activeSnapshot.albums, visibleMedia, hiddenAlbumIds)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (destination == GalleryDestination.Main && !viewerVisible) {
                    GalleryBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }
        ) { innerPadding ->
            when (destination) {
                GalleryDestination.Main -> {
                    when (selectedTab) {
                        GalleryTab.Photos -> PhotosScreen(
                            mediaItems = visibleMedia,
                            contentPadding = innerPadding,
                            mediaAccessNotice = mediaAccessNotice,
                            isLoading = isLoadingMedia,
                            onMediaClick = { mediaItem ->
                                viewerMediaItem = mediaItem
                                viewerVisible = true
                            }
                        )
                        GalleryTab.Albums -> AlbumsScreen(
                            albums = visibleAlbums,
                            layoutMode = albumLayoutMode,
                            onLayoutModeChange = { albumLayoutMode = it },
                            onOpenHiddenItems = { destination = GalleryDestination.HiddenItems },
                            contentPadding = innerPadding,
                            mediaAccessNotice = mediaAccessNotice,
                            isLoading = isLoadingMedia
                        )
                    }
                }
                GalleryDestination.HiddenItems -> HiddenItemsScreen(
                    albums = hideableAlbums,
                    hiddenStates = hiddenStates,
                    onBack = { destination = GalleryDestination.Main },
                    contentPadding = PaddingValues()
                )
            }
        }

        PhotoViewerOverlay(
            mediaItem = viewerMediaItem,
            visible = viewerVisible,
            onClose = { viewerVisible = false }
        )
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

@Composable
private fun GalleryBottomBar(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryNavigationItem(
                selected = selectedTab == GalleryTab.Photos,
                icon = Icons.Filled.PhotoLibrary,
                label = "Photos",
                onClick = { onTabSelected(GalleryTab.Photos) }
            )
            GalleryNavigationItem(
                selected = selectedTab == GalleryTab.Albums,
                icon = Icons.Filled.Folder,
                label = "Albums",
                onClick = { onTabSelected(GalleryTab.Albums) }
            )
        }
    }
}

@Composable
private fun GalleryNavigationItem(
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

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 8.dp),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}