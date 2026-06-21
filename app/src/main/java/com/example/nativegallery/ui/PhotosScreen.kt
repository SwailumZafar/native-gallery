package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.HeaderActionButton
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.ScreenHeader
import com.example.nativegallery.ui.components.SearchPill
import com.example.nativegallery.ui.components.SectionTitle

@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null
) {
    val todayItems = mediaItems.filter { it.dateLabel == "Today" }
    val otherSections = mediaItems
        .filterNot { it.dateLabel == "Today" }
        .groupBy { it.dateLabel }
        .entries
        .toList()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 58.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        )
    ) {
        item {
            ScreenHeader(title = "Photos") {
                HeaderActionButton(
                    icon = Icons.Filled.LocationOn,
                    contentDescription = "Map",
                    onClick = {}
                )
                HeaderActionButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    onClick = {}
                )
            }
            Spacer(Modifier.height(24.dp))
            SearchPill()
            if (mediaAccessNotice != null) {
                Spacer(Modifier.height(18.dp))
                mediaAccessNotice()
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(30.dp))
            }
        }

        if (todayItems.isNotEmpty()) {
            item {
                SectionTitle("Today")
                Spacer(Modifier.height(16.dp))
                PhotoGrid(
                    mediaItems = todayItems.take(12),
                    columns = 4,
                    spacing = 8.dp
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        otherSections.take(6).forEachIndexed { index, section ->
            item {
                SectionTitle(section.key)
                Spacer(Modifier.height(16.dp))
                PhotoGrid(
                    mediaItems = section.value.take(if (index == 0) 1 else 10),
                    columns = if (index == 0) 3 else 5,
                    spacing = 8.dp
                )
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    rowItems.forEach { mediaItem ->
                        MediaThumbnail(
                            mediaItem = mediaItem,
                            modifier = Modifier.size(cellSize)
                        )
                    }
                }
            }
        }
    }
}
