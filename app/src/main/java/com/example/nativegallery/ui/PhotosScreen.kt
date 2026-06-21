package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.example.nativegallery.ui.components.SkeletonBlock

@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    onMediaClick: (MediaItem) -> Unit = {}
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

        if (isLoading) {
            loadingPhotoSections()
        } else {
            sections.forEach { section ->
                photoSection(
                    title = section.key,
                    mediaItems = section.value,
                    columns = 4,
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

private fun LazyListScope.photoSection(
    title: String,
    mediaItems: List<MediaItem>,
    columns: Int,
    onMediaClick: (MediaItem) -> Unit
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
            spacing = 5.dp,
            onMediaClick = onMediaClick
        )
        Spacer(Modifier.height(5.dp))
    }
    item(key = "section-end-$title") {
        Spacer(Modifier.height(16.dp))
    }
}

private fun LazyListScope.loadingPhotoSections() {
    repeat(4) { sectionIndex ->
        item(key = "loading-section-$sectionIndex") {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(14.dp),
                cornerRadius = 7.dp
            )
            Spacer(Modifier.height(8.dp))
        }
        items(
            items = List(if (sectionIndex == 0) 3 else 2) { it },
            key = { rowIndex -> "loading-row-$sectionIndex-$rowIndex" }
        ) {
            PhotoSkeletonRow(columns = 4, spacing = 5.dp)
            Spacer(Modifier.height(5.dp))
        }
        item(key = "loading-section-end-$sectionIndex") {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoGridRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    onMediaClick: (MediaItem) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    onClick = { onMediaClick(mediaItem) }
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}

@Composable
private fun PhotoSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(columns) {
                SkeletonBlock(
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 10.dp
                )
            }
        }
    }
}