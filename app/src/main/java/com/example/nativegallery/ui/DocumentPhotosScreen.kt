package com.example.nativegallery.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.data.DocumentPhotoCategory
import com.example.nativegallery.data.DocumentPhotoMatch
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SearchPill

private enum class DocumentPhotoFilter(
    val label: String,
    val category: DocumentPhotoCategory?
) {
    All("All", null),
    BillsReceipts("Bills & receipts", DocumentPhotoCategory.BillsReceipts),
    Menus("Menus", DocumentPhotoCategory.Menus),
    FormsLetters("Forms & letters", DocumentPhotoCategory.FormsLetters),
    NotesTranscripts("Notes & transcripts", DocumentPhotoCategory.NotesTranscripts),
    Other("Other", DocumentPhotoCategory.Other)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DocumentPhotosScreen(
    matches: List<DocumentPhotoMatch>,
    scanning: Boolean,
    scannedCount: Int,
    totalCount: Int,
    errorMessage: String?,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit,
    gridColumns: Int,
    maxContentWidth: Dp = Dp.Unspecified,
    contentPadding: PaddingValues = PaddingValues()
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DocumentPhotoFilter.All) }
    val visibleMatches = remember(matches, query, filter) {
        val normalizedQuery = query.trim()
        matches.filter { match ->
            (filter.category == null || match.category == filter.category) &&
                (
                    normalizedQuery.isBlank() ||
                        match.mediaItem.title.contains(normalizedQuery, ignoreCase = true) ||
                        match.recognizedText.contains(normalizedQuery, ignoreCase = true) ||
                        match.category.label.contains(normalizedQuery, ignoreCase = true)
                    )
        }
    }
    val visibleMedia = remember(visibleMatches) { visibleMatches.map { it.mediaItem } }
    val progress = if (totalCount == 0) 0f else {
        scannedCount.toFloat().div(totalCount).coerceIn(0f, 1f)
    }
    val gridWidthModifier = if (maxContentWidth == Dp.Unspecified) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .widthIn(max = maxContentWidth)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
            modifier = Modifier
                .fillMaxHeight()
                .then(gridWidthModifier),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 34.dp,
                end = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item(
                key = "document-photos-header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Document photos",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 31.sp,
                                    lineHeight = 37.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Bills, menus, letters, forms and text-heavy pictures",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onRescan,
                            enabled = !scanning
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Scan document photos again",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    SearchPill(
                        placeholder = "Search text inside document photos",
                        query = query,
                        onQueryChange = { query = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocumentPhotoFilter.entries.forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (scanning || scannedCount > 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (scanning) {
                                            "Checking photos on this phone"
                                        } else {
                                            "Document-photo index is up to date"
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = scannedCount.toString() + " / " + totalCount,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(9.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Text(
                        text = when {
                            query.isNotBlank() || filter != DocumentPhotoFilter.All ->
                                visibleMatches.size.toString() + " matching photos"
                            matches.size == 1 -> "1 document photo"
                            else -> matches.size.toString() + " document photos"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (visibleMatches.isEmpty()) {
                item(
                    key = "document-photos-empty",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = when {
                                errorMessage != null -> errorMessage
                                scanning -> "Scanning gallery photos for readable document text…"
                                matches.isNotEmpty() -> "No document photos match this search or filter."
                                else -> "No document photos were found. Photos need several readable lines of text to appear here."
                            },
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = visibleMatches,
                    key = { match -> match.mediaItem.id }
                ) { match ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        MediaThumbnail(
                            mediaItem = match.mediaItem,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 8.dp,
                            onClickWithBounds = { bounds ->
                                onOpenMedia(match.mediaItem, visibleMedia, bounds)
                            }
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.66f)
                        ) {
                            Text(
                                text = match.category.label,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
