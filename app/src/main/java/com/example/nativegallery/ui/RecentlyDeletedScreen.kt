@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.MediaThumbnail

@Composable
fun RecentlyDeletedScreen(
    deletedItems: List<RecentlyDeletedMedia>,
    onBack: () -> Unit,
    onOpenMedia: (RecentlyDeletedMedia) -> Unit,
    onRestore: (RecentlyDeletedMedia) -> Unit,
    onRestoreAll: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 48.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 34.dp
        )
    ) {
        item(key = "recently-deleted-header", contentType = "recently-deleted-header") {
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
                TextButton(
                    enabled = deletedItems.isNotEmpty(),
                    onClick = onRestoreAll
                ) {
                    Text("Restore all")
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Deleted photos and videos stay here in the app so you can restore them.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }

        if (deletedItems.isEmpty()) {
            item(key = "recently-deleted-empty", contentType = "recently-deleted-empty") {
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
                contentType = { "recently-deleted-row" }
            ) { entry ->
                RecentlyDeletedRow(
                    entry = entry,
                    onOpenMedia = { onOpenMedia(entry) },
                    onRestore = { onRestore(entry) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun RecentlyDeletedRow(
    entry: RecentlyDeletedMedia,
    onOpenMedia: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaThumbnail(
            mediaItem = entry.mediaItem,
            modifier = Modifier.size(76.dp),
            cornerRadius = 18.dp,
            onClick = onOpenMedia
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 12.dp)
        ) {
            Text(
                text = entry.mediaItem.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.mediaItem.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Button(onClick = onRestore) {
            Text("Restore")
        }
    }
}
