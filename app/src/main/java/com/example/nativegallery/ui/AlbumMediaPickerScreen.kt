@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.rememberGalleryFlingBehavior

@Composable
fun AlbumMediaPickerScreen(
    albumName: String,
    mediaItems: List<MediaItem>,
    selectedMediaIds: Set<String>,
    onToggleMedia: (MediaItem) -> Unit,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
    onMoveSelected: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            flingBehavior = rememberGalleryFlingBehavior(),
            contentPadding = PaddingValues(start = 4.dp, top = 132.dp, end = 4.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(mediaItems, key = { it.id }, contentType = { "album-picker-media" }) { mediaItem ->
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = 4.dp,
                    selected = selectedMediaIds.contains(mediaItem.id),
                    onClick = { onToggleMedia(mediaItem) },
                    onLongClick = { onToggleMedia(mediaItem) }
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
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 44.dp, end = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel album creation")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Text(
                        text = if (selectedMediaIds.isEmpty()) {
                            "Choose photos and videos"
                        } else {
                            "%1$,d selected".format(selectedMediaIds.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    enabled = selectedMediaIds.size < mediaItems.size,
                    onClick = onSelectAll
                ) {
                    Text("Select all")
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Move to album",
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    enabled = selectedMediaIds.isNotEmpty(),
                    onClick = onMoveSelected
                ) {
                    Text("Move")
                }
            }
        }
    }
}
