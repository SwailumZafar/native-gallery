@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.data.RecentlyDeletedRepository
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.MediaThumbnail
import java.util.concurrent.TimeUnit
private class RecentlyDeletedBoundsRef(var value: Rect = Rect.Zero)


@Composable
fun RecentlyDeletedScreen(
    deletedItems: List<RecentlyDeletedMedia>,
    onBack: () -> Unit,
    onOpenMedia: (RecentlyDeletedMedia, Rect) -> Unit,
    onRestore: (RecentlyDeletedMedia) -> Unit,
    onRestoreAll: () -> Unit,
    onDeleteForever: (RecentlyDeletedMedia) -> Unit,
    onDeleteAllForever: () -> Unit,
    contentPadding: PaddingValues,
    gridColumns: Int = 4
) {
    var actionEntry by remember { mutableStateOf<RecentlyDeletedMedia?>(null) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
        contentPadding = PaddingValues(
            start = 4.dp,
            top = 48.dp,
            end = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 34.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "recently-deleted-header", span = { GridItemSpan(maxLineSpan) }, contentType = "recently-deleted-header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Recently deleted",
                        modifier = Modifier.padding(start = 14.dp),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = deletedItems.isNotEmpty(),
                    onClick = onDeleteAllForever
                ) {
                    Text("Delete all")
                }
                TextButton(
                    enabled = deletedItems.isNotEmpty(),
                    onClick = onRestoreAll
                ) {
                    Text("Restore all")
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Deleted photos and videos stay here for 30 days inside this app before they are cleared from the bin.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            }
        }

        if (deletedItems.isEmpty()) {
            item(key = "recently-deleted-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "recently-deleted-empty") {
                Text(
                    text = "Nothing deleted yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = deletedItems,
                key = { entry -> entry.mediaItem.id },
                contentType = { "recently-deleted-tile" }
            ) { entry ->
                RecentlyDeletedTile(
                    entry = entry,
                    onOpenMedia = { bounds -> onOpenMedia(entry, bounds) },
                    onShowActions = { actionEntry = entry }
                )
            }
        }
    }

    actionEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { actionEntry = null },
            title = { Text(entry.mediaItem.title, maxLines = 1) },
            text = {
                Column {
                    MediaThumbnail(
                        mediaItem = entry.mediaItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        cornerRadius = 18.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(daysLeftLabel(entry.deletedAtMillis))
                }
            },
            confirmButton = {
                Button(onClick = {
                    actionEntry = null
                    onRestore(entry)
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    actionEntry = null
                    onDeleteForever(entry)
                }) {
                    Text("Delete forever")
                }
            }
        )
    }
}

@Composable
private fun RecentlyDeletedTile(
    entry: RecentlyDeletedMedia,
    onOpenMedia: (Rect) -> Unit,
    onShowActions: () -> Unit
) {
    val itemBounds = remember(entry.mediaItem.id) { RecentlyDeletedBoundsRef() }
    MediaThumbnail(
        mediaItem = entry.mediaItem,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        cornerRadius = 4.dp,
        onBoundsChanged = { itemBounds.value = it },
        onClick = { onOpenMedia(itemBounds.value) },
        onLongClick = onShowActions
    )
}

private fun daysLeftLabel(deletedAtMillis: Long): String {
    val ageMillis = (System.currentTimeMillis() - deletedAtMillis).coerceAtLeast(0L)
    val remainingMillis = (RecentlyDeletedRepository.RetentionMillis - ageMillis).coerceAtLeast(0L)
    val daysLeft = TimeUnit.MILLISECONDS.toDays(remainingMillis).coerceAtLeast(0L)
    return when {
        daysLeft > 1L -> "$daysLeft days left"
        daysLeft == 1L -> "1 day left"
        remainingMillis > 0L -> "less than a day left"
        else -> "expires today"
    }
}
