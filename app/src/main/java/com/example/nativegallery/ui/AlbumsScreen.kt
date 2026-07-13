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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.HeaderActionButton
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.bouncyClickable
import com.example.nativegallery.ui.components.rememberGalleryFlingBehavior
import com.example.nativegallery.ui.components.ResourceImage
import com.example.nativegallery.ui.components.ScreenHeader
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SkeletonBlock

enum class AlbumDetailGridMode {
    Compact,
    Comfortable
}

enum class AlbumDetailSortMode {
    Newest,
    Oldest,
    Name
}

private class AlbumBoundsRef(var value: Rect = Rect.Zero)

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    layoutMode: AlbumLayoutMode,
    onLayoutModeChange: (AlbumLayoutMode) -> Unit,
    onOpenHiddenItems: () -> Unit,
    onOpenLockedMedia: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onCreateAlbum: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    hiddenAlbumCount: Int = 0,
    hiddenItemCount: Int = 0,
    lockedItemCount: Int = 0,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyListState,
    activeTransitionAlbumId: String? = null,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    var layoutExpanded by rememberSaveable { mutableStateOf(false) }
    var sortAlphabetically by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createAlbumName by rememberSaveable { mutableStateOf("") }

    val sortedAlbums = if (sortAlphabetically) {
        albums.sortedWith(compareBy<Album> { !it.isAllPhotos }.thenBy { it.name })
    } else {
        albums
    }
    val allPhotos = sortedAlbums.firstOrNull { it.isAllPhotos }
    val regularAlbums = sortedAlbums.filterNot { it.isAllPhotos }

    LazyColumn(
        state = listState,
        flingBehavior = rememberGalleryFlingBehavior(),
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
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                overflowExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SearchPill(
                placeholder = "Search albums",
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
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
        } else if (sortedAlbums.isEmpty()) {
            item(key = "albums-empty", contentType = "albums-empty") {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching albums." else "No albums yet.",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 22.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (layoutMode == AlbumLayoutMode.BigTiles) {
            if (allPhotos != null) {
                item(key = "album-hero-${allPhotos.id}", contentType = "album-hero") {
                    AlbumHeroCard(
                        album = allPhotos,
                        activeTransitionAlbumId = activeTransitionAlbumId,
                        onAlbumClick = onAlbumClick,
                        onAlbumBoundsChanged = onAlbumBoundsChanged
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            bigAlbumRows(
                albums = regularAlbums,
                activeTransitionAlbumId = activeTransitionAlbumId,
                onAlbumClick = onAlbumClick,
                onAlbumBoundsChanged = onAlbumBoundsChanged
            )
        } else {
            basicAlbumRows(
                albums = sortedAlbums,
                activeTransitionAlbumId = activeTransitionAlbumId,
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
            onDismissRequest = {
                showCreateDialog = false
                createAlbumName = ""
            },
            title = { Text("Create album") },
            text = {
                OutlinedTextField(
                    value = createAlbumName,
                    onValueChange = { createAlbumName = it },
                    singleLine = true,
                    label = { Text("Album name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = createAlbumName.isNotBlank(),
                    onClick = {
                        val albumName = createAlbumName.trim()
                        showCreateDialog = false
                        createAlbumName = ""
                        onCreateAlbum(albumName)
                    }
                ) {
                    Text("Choose media")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    createAlbumName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

}


@Composable
private fun HiddenItemsPill(
    hiddenAlbumCount: Int,
    hiddenItemCount: Int,
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
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hidden albums",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hiddenItemsPillLabel(hiddenAlbumCount, hiddenItemCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Manage",
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
private fun LockedMediaPill(
    lockedItemCount: Int,
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
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Locked media",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lockedMediaPillLabel(lockedItemCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Open",
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
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
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
            onDismissRequest = { onExpandedChange(false) },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier.width(220.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Big tiles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Large album covers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (layoutMode == AlbumLayoutMode.BigTiles) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onLayoutModeChange(AlbumLayoutMode.BigTiles)
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Basic", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("More albums per row", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Filled.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
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
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    val columns = 2
    val rowCount = (albums.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "big-album-row-${albums[rowIndex * columns].id}" },
        contentType = { "big-album-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowAlbums = albums.subList(startIndex, minOf(startIndex + columns, albums.size))
        BigAlbumRow(
            albums = rowAlbums,
            activeTransitionAlbumId = activeTransitionAlbumId,
            onAlbumClick = onAlbumClick,
            onAlbumBoundsChanged = onAlbumBoundsChanged
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun LazyListScope.basicAlbumRows(
    albums: List<Album>,
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    val columns = 3
    val rowCount = (albums.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "basic-album-row-${albums[rowIndex * columns].id}" },
        contentType = { "basic-album-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowAlbums = albums.subList(startIndex, minOf(startIndex + columns, albums.size))
        BasicAlbumRow(
            albums = rowAlbums,
            activeTransitionAlbumId = activeTransitionAlbumId,
            onAlbumClick = onAlbumClick,
            onAlbumBoundsChanged = onAlbumBoundsChanged
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AlbumHeroCard(
    album: Album,
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    AlbumImageCard(
        album = album,
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        cornerRadius = 24.dp,
        activeTransitionAlbumId = activeTransitionAlbumId,
        onAlbumClick = onAlbumClick,
        onAlbumBoundsChanged = onAlbumBoundsChanged
    )
}

@Composable
private fun BigAlbumRow(
    albums: List<Album>,
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
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
                    activeTransitionAlbumId = activeTransitionAlbumId,
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
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 8.dp
        val cellWidth = (maxWidth - spacing * 2) / 3
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            albums.forEach { album ->
                val albumBounds = remember(album.id) { AlbumBoundsRef() }
                Column(
                    modifier = Modifier
                        .width(cellWidth)
                        .graphicsLayer { alpha = if (album.id == activeTransitionAlbumId) 0f else 1f }
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            albumBounds.value = bounds
                            onAlbumBoundsChanged(album, bounds)
                        }
                        .bouncyClickable { onAlbumClick(album, albumBounds.value) }
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
                            fontSize = 13.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = album.itemCount.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
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
    activeSharedElementKey: Any? = null,
    albumEnterProgress: Float = 1f,
    gridMode: AlbumDetailGridMode,
    onGridModeChange: (AlbumDetailGridMode) -> Unit,
    selectedMediaIds: Set<String> = emptySet(),
    onMediaLongClick: (MediaItem) -> Unit = {},
    onMediaSelectionToggle: (MediaItem) -> Unit = {},
    onSelectionClear: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onHideSelected: () -> Unit = {},
    onHideAlbum: (() -> Unit)? = null,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    var sortMode by rememberSaveable(album.id) { mutableStateOf(AlbumDetailSortMode.Newest) }
    val sortedMediaItems = remember(mediaItems, sortMode) {
        when (sortMode) {
            AlbumDetailSortMode.Newest -> mediaItems
            AlbumDetailSortMode.Oldest -> mediaItems.asReversed()
            AlbumDetailSortMode.Name -> mediaItems.sortedBy { it.title.lowercase() }
        }
    }
    val columns = when (gridMode) {
        AlbumDetailGridMode.Compact -> 4
        AlbumDetailGridMode.Comfortable -> 3
    }
    val revealProgress = albumEnterProgress.coerceIn(0f, 1f)
    val gridTopPadding = 150.dp
    val interactiveGridReady = revealProgress >= 0.95f
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.graphicsLayer {
                alpha = GalleryMotion.smoothstep(0.48f, 0.78f, revealProgress)
            },
            flingBehavior = rememberGalleryFlingBehavior(),
            contentPadding = PaddingValues(
                start = 10.dp,
                top = gridTopPadding,
                end = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + if (selectedMediaIds.isNotEmpty()) 142.dp else 34.dp
            )
        ) {
            if (!interactiveGridReady && sortedMediaItems.isNotEmpty()) {
                albumDetailPreviewRows(mediaItems = sortedMediaItems, columns = columns)
            } else if (sortedMediaItems.isEmpty() && album.itemCount > 0) {
                albumDetailSkeletonRows(columns = columns)
            } else if (sortedMediaItems.isEmpty()) {
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
                    sharedElementPrefix = "album-${album.id}",
                    activeSharedElementKey = activeSharedElementKey,
                    selectedMediaIds = selectedMediaIds,
                    onMediaLongClick = onMediaLongClick,
                    onMediaSelectionToggle = onMediaSelectionToggle,
                    onMediaClick = onMediaClick
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = GalleryMotion.smoothstep(0.40f, 0.70f, revealProgress) },
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 3.dp
        ) {
            AlbumDetailHeader(
                album = album,
                itemCount = sortedMediaItems.size,
                sortMode = sortMode,
                gridMode = gridMode,
                onSortModeChange = { sortMode = it },
                onGridModeChange = onGridModeChange,
                selectedCount = selectedMediaIds.size,
                totalVisibleCount = sortedMediaItems.size,
                onSelectionClear = onSelectionClear,
                onSelectAllVisible = onSelectAllVisible,
                onDeleteSelected = onDeleteSelected,
                onHideSelected = onHideSelected,
                onHideAlbum = onHideAlbum,
                onBack = onBack,
                modifier = Modifier.padding(start = 10.dp, top = 42.dp, end = 10.dp, bottom = 14.dp)
            )
        }
        if (selectedMediaIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 12.dp
                    )
            ) {
                AlbumSelectionToolbar(
                    selectedCount = selectedMediaIds.size,
                    totalVisibleCount = sortedMediaItems.size,
                    onClear = onSelectionClear,
                    onSelectAll = onSelectAllVisible,
                    onDelete = onDeleteSelected,
                    onHide = onHideSelected
                )
            }
        }
    }
}

@Composable
fun AlbumDetailTransitionPreview(
    album: Album,
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    gridMode: AlbumDetailGridMode,
    transitionProgress: Float = 1f
) {
    val columns = when (gridMode) {
        AlbumDetailGridMode.Compact -> 4
        AlbumDetailGridMode.Comfortable -> 3
    }
    val spacing = if (columns == 3) 5.dp else 3.dp
    val previewItems = remember(mediaItems, columns) { mediaItems.take(columns * 6) }
    val boundedProgress = transitionProgress.coerceIn(0f, 1f)
    val gridAlpha = GalleryMotion.smoothstep(0.48f, 0.78f, boundedProgress)
    val headerAlpha = GalleryMotion.smoothstep(0.40f, 0.70f, boundedProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = gridAlpha }
                .padding(
                    start = 10.dp,
                    top = 150.dp,
                    end = 10.dp,
                    bottom = contentPadding.calculateBottomPadding() + 34.dp
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cellSize = (maxWidth - spacing * (columns - 1)) / columns
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(6) { rowIndex ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            repeat(columns) { columnIndex ->
                                val mediaItem = previewItems.getOrNull(rowIndex * columns + columnIndex)
                                if (mediaItem != null) {
                                    ResourceImage(
                                        imageRes = mediaItem.imageRes,
                                        imageUri = mediaItem.contentUri,
                                        contentDescription = mediaItem.title,
                                        modifier = Modifier.size(cellSize),
                                        cornerRadius = 0.dp,
                                    thumbnailSize = 384
                                    )
                                } else {
                                    Spacer(Modifier.size(cellSize))
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = headerAlpha },
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, top = 42.dp, end = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = album.name,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "%1$,d items".format(mediaItems.size),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                }
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
            }
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
    selectedCount: Int,
    totalVisibleCount: Int,
    onSelectionClear: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onDeleteSelected: () -> Unit,
    onHideSelected: () -> Unit,
    onHideAlbum: (() -> Unit)?,
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
                    if (onHideAlbum != null && !album.isAllPhotos) {
                        DropdownMenuItem(
                            text = { Text("Hide album") },
                            leadingIcon = { Icon(Icons.Filled.Security, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onHideAlbum()
                            }
                        )
                    }
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


@Composable
private fun AlbumSelectionToolbar(
    selectedCount: Int,
    totalVisibleCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "%1$,d selected".format(selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                enabled = selectedCount < totalVisibleCount,
                onClick = onSelectAll
            ) {
                Text("Select all")
            }
            IconButton(onClick = onHide) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Hide selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
private fun LazyListScope.albumDetailRows(
    mediaItems: List<MediaItem>,
    columns: Int,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedBoundsTransform: BoundsTransform? = null,
    sharedElementPrefix: String,
    activeSharedElementKey: Any? = null,
    selectedMediaIds: Set<String>,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    val spacing = if (columns == 3) 5.dp else 3.dp
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "album-media-row-$columns-${mediaItems[rowIndex * columns].id}" },
        contentType = { "album-media-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        AlbumDetailRow(
            mediaItems = rowItems,
            columns = columns,
            spacing = spacing,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedBoundsTransform = sharedBoundsTransform,
            sharedElementPrefix = sharedElementPrefix,
            activeSharedElementKey = activeSharedElementKey,
            selectedMediaIds = selectedMediaIds,
            onMediaLongClick = onMediaLongClick,
            onMediaSelectionToggle = onMediaSelectionToggle,
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
    sharedElementPrefix: String,
    activeSharedElementKey: Any? = null,
    selectedMediaIds: Set<String>,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaSelectionToggle: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect, String, String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                val sharedElementKey = "$sharedElementPrefix-media-${mediaItem.id}"
                val mediaBounds = remember(mediaItem.id) { AlbumBoundsRef() }
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementKey = sharedElementKey,
                    sharedBoundsTransform = sharedBoundsTransform,
                    isSharedElementSourceHidden = activeSharedElementKey == sharedElementKey,
                    selected = selectedMediaIds.contains(mediaItem.id),
                    onBoundsChanged = { bounds -> mediaBounds.value = bounds },
                    onLongClick = { onMediaLongClick(mediaItem) },
                    onClick = {
                        if (selectedMediaIds.isNotEmpty()) {
                            onMediaSelectionToggle(mediaItem)
                        } else {
                            onMediaClick(mediaItem, mediaBounds.value, sharedElementKey, sharedElementPrefix)
                        }
                    }
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}

private fun LazyListScope.albumDetailPreviewRows(
    mediaItems: List<MediaItem>,
    columns: Int
) {
    val spacing = if (columns == 3) 5.dp else 3.dp
    val rowCount = (mediaItems.size + columns - 1) / columns
    items(
        count = rowCount,
        key = { rowIndex -> "album-detail-preview-row-$columns-$rowIndex" },
        contentType = { "album-detail-preview-row" }
    ) { rowIndex ->
        val startIndex = rowIndex * columns
        val rowItems = mediaItems.subList(startIndex, minOf(startIndex + columns, mediaItems.size))
        AlbumDetailPreviewRow(mediaItems = rowItems, columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailPreviewRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                ResourceImage(
                    imageRes = mediaItem.imageRes,
                    imageUri = mediaItem.contentUri,
                    contentDescription = mediaItem.title,
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp,
                    thumbnailSize = 384
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}
private fun LazyListScope.albumDetailOpeningRows(columns: Int) {
    val spacing = if (columns == 3) 5.dp else 3.dp
    items(
        items = List(4) { it },
        key = { rowIndex -> "album-detail-opening-row-$columns-$rowIndex" },
        contentType = { "album-detail-opening-row" }
    ) {
        AlbumDetailOpeningRow(columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailOpeningRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(columns) {
                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                )
            }
        }
    }
}

private fun LazyListScope.albumDetailSkeletonRows(columns: Int) {
    val spacing = if (columns == 3) 5.dp else 3.dp
    items(
        items = List(5) { it },
        key = { rowIndex -> "album-detail-loading-row-$columns-$rowIndex" },
        contentType = { "album-detail-loading-row" }
    ) {
        AlbumDetailSkeletonRow(columns = columns, spacing = spacing)
        Spacer(Modifier.height(spacing))
    }
}

@Composable
private fun AlbumDetailSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(columns) {
                SkeletonBlock(
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp
                )
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
    activeTransitionAlbumId: String?,
    onAlbumClick: (Album, Rect) -> Unit,
    onAlbumBoundsChanged: (Album, Rect) -> Unit) {
    val albumBounds = remember(album.id) { AlbumBoundsRef() }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                albumBounds.value = bounds
                onAlbumBoundsChanged(album, bounds)
            }
            .graphicsLayer { alpha = if (album.id == activeTransitionAlbumId) 0f else 1f }
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bouncyClickable { onAlbumClick(album, albumBounds.value) }
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
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = album.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = "%1$,d".format(album.itemCount),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
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

private fun hiddenItemsPillLabel(hiddenAlbumCount: Int, hiddenItemCount: Int): String {
    return if (hiddenAlbumCount > 0) {
        "%1$,d albums, %2$,d items hidden".format(hiddenAlbumCount, hiddenItemCount)
    } else {
        "Choose albums to hide"
    }
}

private fun lockedMediaPillLabel(lockedItemCount: Int): String {
    return if (lockedItemCount > 0) {
        "%1$,d locked photos and videos".format(lockedItemCount)
    } else {
        "PIN, face, or fingerprint protected"
    }
}
