@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nativegallery.model.RecentlyDeletedMedia
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.MediaSelectionTopBar
import com.example.nativegallery.ui.components.mediaSelectionAccentColor

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
    val hapticFeedback = LocalHapticFeedback.current
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
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
                    GalleryScreenHeader(
                        title = "Recently deleted",
                        onBack = onBack,
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        leadingContentDescription = "Back",
                        trailingContent = {
                            if (!isSelectionMode) {
                                IconButton(
                                    enabled = deletedItems.isNotEmpty(),
                                    onClick = { selectedMediaIds = availableIds }
                                ) {
                                    Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                                }
                            }
                        }
                    )
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

        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f),
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(180),
                initialOffsetY = { -it / 2 }
            ),
            exit = fadeOut(tween(100)) + slideOutVertically(targetOffsetY = { -it / 2 })
        ) {
            MediaSelectionTopBar(
                selectedCount = selectedMediaIds.size,
                canSelectAll = selectedMediaIds.size < deletedItems.size,
                onClear = { selectedMediaIds = emptySet() },
                onSelectAll = { selectedMediaIds = availableIds },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
        }
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f),
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(190),
                initialOffsetY = { it / 2 + 24 }
            ),
            exit = fadeOut(tween(100)) + slideOutVertically(targetOffsetY = { it / 2 + 24 })
        ) {
            RecentlyDeletedSelectionToolbar(
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
            )
        }
    }

    confirmation?.let { pending ->
        val itemCount = pending.items.size
        AlertDialog(
            onDismissRequest = { confirmation = null },
            modifier = Modifier
                .widthIn(min = 340.dp, max = 460.dp)
                .fillMaxWidth(0.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
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
            shape = RoundedCornerShape(24.dp)
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
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = mediaSelectionAccentColor()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 2.dp),
        color = Color(0xFF171717),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            TextButton(
                onClick = onRestore,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore")
            }
            TextButton(
                onClick = onDeleteForever,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete forever")
            }
        }
    }
}
