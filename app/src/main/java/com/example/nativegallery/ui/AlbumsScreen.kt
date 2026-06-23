@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.HeaderActionButton
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.bouncyClickable
import com.example.nativegallery.ui.components.ResourceImage
import com.example.nativegallery.ui.components.ScreenHeader
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock

private enum class AlbumDetailGridMode {
    Compact,
    Comfortable
}

private enum class AlbumDetailSortMode {
    Newest,
    Oldest,
    Name
}

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    layoutMode: AlbumLayoutMode,
    onLayoutModeChange: (AlbumLayoutMode) -> Unit,
    onOpenHiddenItems: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false
) {
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    var layoutExpanded by rememberSaveable { mutableStateOf(false) }
    var sortAlphabetically by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val sortedAlbums = if (sortAlphabetically) {
        albums.sortedWith(compareBy<Album> { !it.isAllPhotos }.thenBy { it.name })
    } else {
        albums
    }
    val allPhotos = sortedAlbums.firstOrNull { it.isAllPhotos }
    val regularAlbums = sortedAlbums.filterNot { it.isAllPhotos }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 58.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 26.dp
        )
    ) {
        item(key = "albums-header", contentType = "albums-header") {
            ScreenHeader(title = "Albums") {
                HeaderActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Create album",
                    onClick = { showCreateDialog = true }
                )
                HeaderActionButton(
                    icon = if (layoutMode == AlbumLayoutMode.BigTiles) Icons.Filled.GridView else Icons.Filled.Apps,
                    contentDescription = "Switch layout",
                    onClick = {
                        onLayoutModeChange(
                            if (layoutMode == AlbumLayoutMode.BigTiles) AlbumLayoutMode.Basic else AlbumLayoutMode.BigTiles
                        )
                    }
                )
                Box {
                    HeaderActionButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = "Album options",
                        onClick = { overflowExpanded = true }
                    )
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort albums") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                            trailingIcon = {
                                if (sortAlphabetically) Icon(Icons.Filled.Check, contentDescription = null)
                            },
                            onClick = {
                                sortAlphabetically = !sortAlphabetically
                                overflowExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hidden items") },
                            onClick = {
                                overflowExpanded = false
                                onOpenHiddenItems()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                showSettingsDialog = true
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SearchPill(placeholder = "Search albums")
            if (mediaAccessNotice != null) {
                Spacer(Modifier.height(18.dp))
                mediaAccessNotice()
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LayoutSelector(
                    layoutMode = layoutMode,
                    expanded = layoutExpanded,
                    onExpandedChange = { layoutExpanded = it },
                    onLayoutModeChange = onLayoutModeChange
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        if (isLoading) {
            item(key = "albums-loading", contentType = "albums-loading") {
                AlbumsLoadingState(layoutMode = layoutMode)
            }
        } else if (layoutMode == AlbumLayoutMode.BigTiles) {
            if (allPhotos != null) {
                item(key = "album-hero-${allPhotos.id}", contentType = "album-hero") {
                    AlbumHeroCard(
                        album = allPhotos,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = onAlbumBoundsChanged
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            bigAlbumRows(
                albums = regularAlbums,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        } else {
            basicAlbumRows(
                albums = sortedAlbums,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        }
        if (!isLoading) {
            item(key = "recently-deleted-pill", contentType = "album-list-action") {
                Spacer(Modifier.height(10.dp))
                RecentlyDeletedPill(onClick = onOpenRecentlyDeleted)
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create album") },
            text = { Text("New albums will appear here after media storage is connected.") },
            confirmButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings") },
            text = { Text("Theme follows your phone setting. Photo access can be changed in Android app settings.") },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun RecentlyDeletedPill(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bouncyClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(34.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Recently deleted",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LayoutSelector(
    layoutMode: AlbumLayoutMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLayoutModeChange: (AlbumLayoutMode) -> Unit
) {
    Box {
        Surface(
            modifier = Modifier.clickable { onExpandedChange(true) },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (layoutMode == AlbumLayoutMode.BigTiles) "Big tiles" else "Basic",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Big tiles") },
                trailingIcon = {
                    if (layoutMode == AlbumLayoutMode.BigTiles) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onLayoutModeChange(AlbumLayoutMode.BigTiles)
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text("Basic") },
                trailingIcon = {
                    if (layoutMode == AlbumLayoutMode.Basic) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onLayoutModeChange(AlbumLayoutMode.Basic)
                    onExpandedChange(false)
                }
            )
        }
    }
}

private fun LazyListScope.bigAlbumRows(
    albums: List<Album>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    items(
        items = albums.chunked(2),
        key = { rowAlbums -> "big-album-row-${rowAlbums.joinToString("-") { it.id }}" },
        contentType = { "big-album-row" }
    ) { rowAlbums ->
        BigAlbumRow(
            albums = rowAlbums,
            onAlbumClick = onAlbumClick,
            onAlbumBoundsChanged = onAlbumBoundsChanged
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun LazyListScope.basicAlbumRows(
    albums: List<Album>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    items(
        items = albums.chunked(3),
        key = { rowAlbums -> "basic-album-row-${rowAlbums.joinToString("-") { it.id }}" },
        contentType = { "basic-album-row" }
    ) { rowAlbums ->
        BasicAlbumRow(
            albums = rowAlbums,
            onAlbumClick = onAlbumClick,
            onAlbumBoundsChanged = onAlbumBoundsChanged
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AlbumHeroCard(
    album: Album,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    AlbumImageCard(
        album = album,
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        cornerRadius = 24.dp,
        onAlbumClick = onAlbumClick,
        onAlbumBoundsChanged = onAlbumBoundsChanged
    )
}

@Composable
private fun BigAlbumRow(
    albums: List<Album>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 12.dp
        val cellWidth = (maxWidth - spacing) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            albums.forEach { album ->
                AlbumImageCard(
                    album = album,
                    modifier = Modifier
                        .width(cellWidth)
                        .height(176.dp),
                    cornerRadius = 22.dp,
                    onAlbumClick = onAlbumClick,
                    onAlbumBoundsChanged = onAlbumBoundsChanged
                )
            }
            if (albums.size == 1) {
                Spacer(Modifier.width(cellWidth))
            }
        }
    }
}

@Composable
private fun BasicAlbumRow(
    albums: List<Album>,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 14.dp
        val cellWidth = (maxWidth - spacing * 2) / 3
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            albums.forEach { album ->
                var albumBounds by remember(album.id) { mutableStateOf<Rect?>(null) }
                Column(
                    modifier = Modifier
                        .width(cellWidth)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            albumBounds = bounds
                            onAlbumBoundsChanged(album, bounds)
                        }
                        .bouncyClickable { onAlbumClick(album, albumBounds ?: Rect.Zero) }
                ) {
                    ResourceImage(
                        imageRes = album.coverRes,
                        imageUri = album.coverUri,
                        contentDescription = album.name,
                        modifier = Modifier.size(cellWidth),
                        cornerRadius = 18.dp,
                        thumbnailSize = 384
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = album.itemCount.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            repeat(3 - albums.size) {
                Spacer(Modifier.width(cellWidth))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    onMediaClick: (MediaItem, Rect) -> Unit
) {
    var gridMode by rememberSaveable(album.id) { mutableStateOf(AlbumDetailGridMode.Compact) }
    var sortMode by rememberSaveable(album.id) { mutableStateOf(AlbumDetailSortMode.Newest) }
    val sortedMediaItems = when (sortMode) {
        AlbumDetailSortMode.Newest -> mediaItems
        AlbumDetailSortMode.Oldest -> mediaItems.asReversed()
        AlbumDetailSortMode.Name -> mediaItems.sortedBy { it.title.lowercase() }
    }
    val columns = when (gridMode) {
        AlbumDetailGridMode.Compact -> 4
        AlbumDetailGridMode.Comfortable -> 3
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 10.dp,
                top = 150.dp,
                end = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 34.dp
            )
        ) {
            if (sortedMediaItems.isEmpty()) {
                item(key = "album-detail-empty", contentType = "album-detail-empty") {
                    Text(
                        text = "No photos here yet.",
                        modifier = Modifier.padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                albumDetailRows(
                    mediaItems = sortedMediaItems,
                    columns = columns,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedBoundsTransform = sharedBoundsTransform,
                    onMediaClick = onMediaClick
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 3.dp
        ) {
            AlbumDetailHeader(
                album = album,
                itemCount = sortedMediaItems.size,
                sortMode = sortMode,
                gridMode = gridMode,
                onSortModeChange = { sortMode = it },
                onGridModeChange = { gridMode = it },
                onBack = onBack,
                modifier = Modifier.padding(start = 10.dp, top = 42.dp, end = 10.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun AlbumDetailHeader(
    album: Album,
    itemCount: Int,
    sortMode: AlbumDetailSortMode,
    gridMode: AlbumDetailGridMode,
    onSortModeChange: (AlbumDetailSortMode) -> Unit,
    onGridModeChange: (AlbumDetailGridMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = album.name,
                    modifier = Modifier.padding(start = 14.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Album options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Newest first") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                        trailingIcon = {
                            if (sortMode == AlbumDetailSortMode.Newest) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        onClick = {
                            onSortModeChange(AlbumDetailSortMode.Newest)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest first") },
                        trailingIcon = {
                            if (sortMode == AlbumDetailSortMode.Oldest) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        onClick = {
                            onSortModeChange(AlbumDetailSortMode.Oldest)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name") },
                        trailingIcon = {
                            if (sortMode == AlbumDetailSortMode.Name) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        onClick = {
                            onSortModeChange(AlbumDetailSortMode.Name)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compact grid") },
                        leadingIcon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                        trailingIcon = {
                            if (gridMode == AlbumDetailGridMode.Compact) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        onClick = {
                            onGridModeChange(AlbumDetailGridMode.Compact)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Comfortable grid") },
                        leadingIcon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                        trailingIcon = {
                            if (gridMode == AlbumDetailGridMode.Comfortable) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        onClick = {
                            onGridModeChange(AlbumDetailGridMode.Comfortable)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "%1$,d items, %2\$s, %3\$s".format(
                itemCount,
                sortMode.label(),
                gridMode.label()
            ),
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun LazyListScope.albumDetailRows(
    mediaItems: List<MediaItem>,
    columns: Int,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    onMediaClick: (MediaItem, Rect) -> Unit
) {
    val spacing = if (columns == 3) 5.dp else 3.dp
    items(
        items = mediaItems.chunked(columns),
        key = { rowItems -> "album-media-row-$columns-${rowItems.first().id}" },
        contentType = { "album-media-row" }
    ) { rowItems ->
        AlbumDetailRow(
            mediaItems = rowItems,
            columns = columns,
            spacing = spacing,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedBoundsTransform = sharedBoundsTransform,
            onMediaClick = onMediaClick
        )
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    onMediaClick: (MediaItem, Rect) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                var mediaBounds by remember(mediaItem.id) { mutableStateOf(Rect.Zero) }
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementKey = "media-${mediaItem.id}",
                    sharedBoundsTransform = sharedBoundsTransform,
                    onBoundsChanged = { bounds -> mediaBounds = bounds },
                    onClick = { onMediaClick(mediaItem, mediaBounds) }
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}

@Composable
private fun AlbumsLoadingState(layoutMode: AlbumLayoutMode) {
    if (layoutMode == AlbumLayoutMode.BigTiles) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            cornerRadius = 24.dp
        )
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 12.dp
            val cellWidth = (maxWidth - spacing) / 2
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        repeat(2) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(176.dp),
                                cornerRadius = 22.dp
                            )
                        }
                    }
                }
            }
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 14.dp
            val cellWidth = (maxWidth - spacing * 2) / 3
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        repeat(3) {
                            Column(modifier = Modifier.width(cellWidth)) {
                                SkeletonBlock(
                                    modifier = Modifier.size(cellWidth),
                                    cornerRadius = 18.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                SkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth(0.82f)
                                        .height(14.dp),
                                    cornerRadius = 7.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumImageCard(
    album: Album,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    var albumBounds by remember(album.id) { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                albumBounds = bounds
                onAlbumBoundsChanged(album, bounds)
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bouncyClickable { onAlbumClick(album, albumBounds ?: Rect.Zero) }
    ) {
        ResourceImage(
            imageRes = album.coverRes,
            imageUri = album.coverUri,
            contentDescription = album.name,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
            thumbnailSize = 512
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.66f)),
                        startY = 120f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(22.dp)
        ) {
            Text(
                text = album.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = "%1$,d".format(album.itemCount),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

private fun AlbumDetailSortMode.label(): String {
    return when (this) {
        AlbumDetailSortMode.Newest -> "Newest"
        AlbumDetailSortMode.Oldest -> "Oldest"
        AlbumDetailSortMode.Name -> "Name"
    }
}

private fun AlbumDetailGridMode.label(): String {
    return when (this) {
        AlbumDetailGridMode.Compact -> "Compact"
        AlbumDetailGridMode.Comfortable -> "Comfortable"
    }
}
