@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.MediaThumbnail

private class RecentlyDeletedBoundsRef(var value: Rect = Rect.Zero)

private enum class RecentlyDeletedDragSelectMode {
    Add,
    Remove
}

private enum class RecentlyDeletedConfirmationMode {
    Restore,
    DeleteForever
}

private data class RecentlyDeletedConfirmation(
    val mode: RecentlyDeletedConfirmationMode,
    val items: List<RecentlyDeletedMedia>
)

@Composable
fun RecentlyDeletedScreen(
    deletedItems: List<RecentlyDeletedMedia>,
    onBack: () -> Unit,
    onOpenMedia: (RecentlyDeletedMedia, Rect) -> Unit,
    onRestoreSelected: (List<RecentlyDeletedMedia>) -> Unit,
    onDeleteForeverSelected: (List<RecentlyDeletedMedia>) -> Unit,
    contentPadding: PaddingValues,
    gridColumns: Int = 4
) {
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmation by remember { mutableStateOf<RecentlyDeletedConfirmation?>(null) }
    val tileBounds = remember { mutableMapOf<String, Rect>() }
    val rootBounds = remember { RecentlyDeletedBoundsRef() }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val availableIds = remember(deletedItems) { deletedItems.map { it.mediaItem.id }.toSet() }

    BackHandler(enabled = isSelectionMode) {
        selectedMediaIds = emptySet()
    }

    LaunchedEffect(availableIds) {
        selectedMediaIds = selectedMediaIds.intersect(availableIds)
        tileBounds.keys.retainAll(availableIds)
    }

    fun toggleSelection(entry: RecentlyDeletedMedia) {
        val id = entry.mediaItem.id
        selectedMediaIds = if (id in selectedMediaIds) selectedMediaIds - id else selectedMediaIds + id
    }

    fun hitEntry(localPoint: Offset): RecentlyDeletedMedia? {
        val point = Offset(
            rootBounds.value.left + localPoint.x,
            rootBounds.value.top + localPoint.y
        )
        return deletedItems.firstOrNull { entry ->
            tileBounds[entry.mediaItem.id]?.contains(point) == true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds.value = it.boundsInWindow() }
            .pointerInput(isSelectionMode, deletedItems) {
                var dragMode: RecentlyDeletedDragSelectMode? = null
                var baseSelectedIds = emptySet<String>()
                val visitedIds = mutableSetOf<String>()

                fun applyDragSelectionAt(localPoint: Offset) {
                    val entry = hitEntry(localPoint) ?: return
                    val id = entry.mediaItem.id
                    val mode = dragMode ?: return
                    if (!visitedIds.add(id)) return
                    when (mode) {
                        RecentlyDeletedDragSelectMode.Add -> if (id !in baseSelectedIds) toggleSelection(entry)
                        RecentlyDeletedDragSelectMode.Remove -> if (id in baseSelectedIds) toggleSelection(entry)
                    }
                }

                if (isSelectionMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            baseSelectedIds = latestSelectedMediaIds
                            visitedIds.clear()
                            val hit = hitEntry(startOffset)
                            dragMode = hit?.let {
                                if (it.mediaItem.id in baseSelectedIds) {
                                    RecentlyDeletedDragSelectMode.Remove
                                } else {
                                    RecentlyDeletedDragSelectMode.Add
                                }
                            }
                            applyDragSelectionAt(startOffset)
                        },
                        onDrag = { change, _ ->
                            applyDragSelectionAt(change.position)
                            change.consume()
                        },
                        onDragEnd = {
                            dragMode = null
                            visitedIds.clear()
                        },
                        onDragCancel = {
                            dragMode = null
                            visitedIds.clear()
                        }
                    )
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
            contentPadding = PaddingValues(
                start = 4.dp,
                top = 48.dp,
                end = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + if (isSelectionMode) 132.dp else 34.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(
                key = "recently-deleted-header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "recently-deleted-header"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = if (isSelectionMode) ({ selectedMediaIds = emptySet() }) else onBack) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isSelectionMode) "Cancel selection" else "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = if (isSelectionMode) "%1$,d selected".format(selectedMediaIds.size) else "Recently deleted",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        IconButton(
                            enabled = deletedItems.isNotEmpty() && selectedMediaIds.size < deletedItems.size,
                            onClick = { selectedMediaIds = availableIds }
                        ) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                        }
                    }
                    if (!isSelectionMode) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Photos and videos are permanently removed after 30 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "%1$,d items".format(deletedItems.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (deletedItems.isEmpty()) {
                item(
                    key = "recently-deleted-empty",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "recently-deleted-empty"
                ) {
                    Text(
                        text = "Nothing deleted yet",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 28.dp),
                        style = MaterialTheme.typography.bodyLarge,
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
                        selected = entry.mediaItem.id in selectedMediaIds,
                        onBoundsChanged = { bounds -> tileBounds[entry.mediaItem.id] = bounds },
                        onLongClick = {
                            if (entry.mediaItem.id !in selectedMediaIds) toggleSelection(entry)
                        },
                        onClick = { bounds ->
                            if (isSelectionMode) toggleSelection(entry) else onOpenMedia(entry, bounds)
                        }
                    )
                }
            }
        }

        if (isSelectionMode) {
            RecentlyDeletedSelectionToolbar(
                selectedCount = selectedMediaIds.size,
                totalCount = deletedItems.size,
                onClear = { selectedMediaIds = emptySet() },
                onSelectAll = { selectedMediaIds = availableIds },
                onRestore = {
                    confirmation = RecentlyDeletedConfirmation(
                        mode = RecentlyDeletedConfirmationMode.Restore,
                        items = deletedItems.filter { it.mediaItem.id in selectedMediaIds }
                    )
                },
                onDeleteForever = {
                    confirmation = RecentlyDeletedConfirmation(
                        mode = RecentlyDeletedConfirmationMode.DeleteForever,
                        items = deletedItems.filter { it.mediaItem.id in selectedMediaIds }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    confirmation?.let { pending ->
        val itemCount = pending.items.size
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = {
                Text(
                    when (pending.mode) {
                        RecentlyDeletedConfirmationMode.Restore -> if (itemCount == 1) "Restore this item?" else "Restore %1$,d items?".format(itemCount)
                        RecentlyDeletedConfirmationMode.DeleteForever -> if (itemCount == 1) "Delete this item forever?" else "Delete %1$,d items forever?".format(itemCount)
                    }
                )
            },
            text = {
                Text(
                    when (pending.mode) {
                        RecentlyDeletedConfirmationMode.Restore -> "The selected media will return to your gallery."
                        RecentlyDeletedConfirmationMode.DeleteForever -> "This cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        selectedMediaIds = emptySet()
                        when (pending.mode) {
                            RecentlyDeletedConfirmationMode.Restore -> onRestoreSelected(pending.items)
                            RecentlyDeletedConfirmationMode.DeleteForever -> onDeleteForeverSelected(pending.items)
                        }
                    }
                ) {
                    Text(if (pending.mode == RecentlyDeletedConfirmationMode.Restore) "Restore" else "Delete forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun RecentlyDeletedTile(
    entry: RecentlyDeletedMedia,
    selected: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onClick: (Rect) -> Unit,
    onLongClick: () -> Unit
) {
    val itemBounds = remember(entry.mediaItem.id) { RecentlyDeletedBoundsRef() }
    MediaThumbnail(
        mediaItem = entry.mediaItem,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        cornerRadius = 0.dp,
        selected = selected,
        onBoundsChanged = { bounds ->
            itemBounds.value = bounds
            onBoundsChanged(bounds)
        },
        onClick = { onClick(itemBounds.value) },
        onLongClick = onLongClick
    )
}

@Composable
private fun RecentlyDeletedSelectionToolbar(
    selectedCount: Int,
    totalCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 14.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                }
                Text(
                    text = "%1$,d selected".format(selectedCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                IconButton(enabled = selectedCount < totalCount, onClick = onSelectAll) {
                    Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore")
                }
                TextButton(onClick = onDeleteForever, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete forever")
                }
            }
        }
    }
}