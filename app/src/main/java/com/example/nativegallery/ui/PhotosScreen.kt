package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
    val sections = mediaItems
        .groupBy { it.dateLabel }
        .entries
        .toList()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 56.dp,
            end = 12.dp,
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
                Spacer(Modifier.height(16.dp))
                mediaAccessNotice()
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(30.dp))
            }
        }

        sections.forEachIndexed { index, section ->
            photoSection(
                title = section.key,
                mediaItems = section.value,
                columns = 4
            )
        }
    }
}

private fun LazyListScope.photoSection(
    title: String,
    mediaItems: List<MediaItem>,
    columns: Int
) {
    if (mediaItems.isEmpty()) {
        return
    }

    item(key = "section-$title") {
        SectionTitle(title)
        Spacer(Modifier.height(8.dp))
    }
    items(
        items = mediaItems.chunked(columns),
        key = { rowItems -> "row-$title-${rowItems.first().id}" }
    ) { rowItems ->
        PhotoGridRow(
            mediaItems = rowItems,
            columns = columns,
            spacing = 5.dp
        )
        Spacer(Modifier.height(5.dp))
    }
    item(key = "section-end-$title") {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PhotoGridRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize)
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}