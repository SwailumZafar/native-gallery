package com.example.nativegallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.FakeGalleryRepository
import com.example.nativegallery.data.HiddenAlbumsRepository
import com.example.nativegallery.model.AlbumLayoutMode

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
    val hiddenRepository = remember { HiddenAlbumsRepository() }
    val hiddenStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            FakeGalleryRepository.hideableAlbums.forEach { album ->
                put(album.id, hiddenRepository.initialHiddenAlbumIds().contains(album.id))
            }
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.Photos) }
    var destination by rememberSaveable { mutableStateOf(GalleryDestination.Main) }
    var albumLayoutMode by rememberSaveable { mutableStateOf(AlbumLayoutMode.BigTiles) }

    val hiddenAlbumIds = hiddenStates.filterValues { it }.keys

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (destination == GalleryDestination.Main) {
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
                        mediaItems = FakeGalleryRepository.visibleMedia(hiddenAlbumIds),
                        contentPadding = innerPadding
                    )
                    GalleryTab.Albums -> AlbumsScreen(
                        albums = FakeGalleryRepository.visibleAlbums(hiddenAlbumIds),
                        layoutMode = albumLayoutMode,
                        onLayoutModeChange = { albumLayoutMode = it },
                        onOpenHiddenItems = { destination = GalleryDestination.HiddenItems },
                        contentPadding = innerPadding
                    )
                }
            }
            GalleryDestination.HiddenItems -> HiddenItemsScreen(
                albums = FakeGalleryRepository.hideableAlbums,
                hiddenStates = hiddenStates,
                onBack = { destination = GalleryDestination.Main },
                contentPadding = PaddingValues()
            )
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
