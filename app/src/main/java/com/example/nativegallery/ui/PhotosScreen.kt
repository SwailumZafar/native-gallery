package com.example.nativegallery.ui

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SkeletonBlock

@Composable
fun PhotosScreen(
    mediaItems: List<MediaItem>,
    contentPadding: PaddingValues,
    mediaAccessNotice: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    onMediaClick: (MediaItem, Rect) -> Unit = { _, _ -> }
) {
    val sections = mediaItems
        .groupBy { it.dateLabel }
        .entries
        .toList()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = contentPadding.calculateBottomPadding() + 34.dp
        )
    ) {
        item(key = "pictures-header", contentType = "pictures-header") {
            PicturesHeader(mediaAccessNotice = mediaAccessNotice)
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

@Composable
private fun PicturesHeader(
    mediaAccessNotice: (@Composable () -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 92.dp, end = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pictures",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(78.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                GallerySearchBubble()
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchCircle()
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        if (mediaAccessNotice != null) {
            Spacer(Modifier.height(18.dp))
            mediaAccessNotice()
        }
        Spacer(Modifier.height(46.dp))
    }
}

@Composable
private fun GallerySearchBubble() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Color(0xFF26A8FF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(7.dp))
            Text(
                text = "Gallery Search",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SearchCircle() {
    Surface(
        modifier = Modifier
            .size(58.dp)
            .border(3.dp, Color(0xFF26A8FF), CircleShape),
        color = Color.White,
        shape = CircleShape,
        shadowElevation = 2.dp
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = Color.Black,
            modifier = Modifier.padding(13.dp)
        )
    }
}

private fun LazyListScope.photoSection(
    title: String,
    mediaItems: List<MediaItem>,
    columns: Int,
    onMediaClick: (MediaItem, Rect) -> Unit
) {
    if (mediaItems.isEmpty()) {
        return
    }

    item(key = "section-$title", contentType = "photo-section-title") {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 26.dp),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
    }
    items(
        items = mediaItems.chunked(columns),
        key = { rowItems -> "row-$title-${rowItems.first().id}" },
        contentType = { "photo-grid-row" }
    ) { rowItems ->
        PhotoGridRow(
            mediaItems = rowItems,
            columns = columns,
            spacing = 2.dp,
            onMediaClick = onMediaClick
        )
        Spacer(Modifier.height(2.dp))
    }
    item(key = "section-end-$title", contentType = "photo-section-end") {
        Spacer(Modifier.height(30.dp))
    }
}

private fun LazyListScope.loadingPhotoSections() {
    repeat(4) { sectionIndex ->
        item(key = "loading-section-$sectionIndex", contentType = "loading-section-title") {
            SkeletonBlock(
                modifier = Modifier
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth(0.28f)
                    .height(18.dp),
                cornerRadius = 9.dp
            )
            Spacer(Modifier.height(10.dp))
        }
        items(
            items = List(if (sectionIndex == 0) 3 else 2) { it },
            key = { rowIndex -> "loading-row-$sectionIndex-$rowIndex" },
            contentType = { "loading-photo-row" }
        ) {
            PhotoSkeletonRow(columns = 4, spacing = 2.dp)
            Spacer(Modifier.height(2.dp))
        }
        item(key = "loading-section-end-$sectionIndex", contentType = "loading-section-end") {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PhotoGridRow(
    mediaItems: List<MediaItem>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    onMediaClick: (MediaItem, Rect) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                var mediaBounds by remember(mediaItem.id) { mutableStateOf(Rect.Zero) }
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp,
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
private fun PhotoSkeletonRow(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
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