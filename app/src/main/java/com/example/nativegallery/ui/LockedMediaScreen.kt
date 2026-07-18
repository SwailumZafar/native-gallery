@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaThumbnail

private class LockedMediaBoundsRef(var value: Rect = Rect.Zero)

private enum class LockedMediaDragSelectMode {
    Add,
    Remove
}

@Composable
fun LockedMediaScreen(
    lockedMediaItems: List<MediaItem>,
    isUnlocked: Boolean,
    hasPin: Boolean,
    biometricAvailable: Boolean,
    authMessage: String?,
    authInProgress: Boolean,
    onBack: () -> Unit,
    onPinCreated: (String, String) -> Unit,
    onPinUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onUnhideSelected: (List<MediaItem>) -> Unit,
    onDeleteSelected: (List<MediaItem>) -> Unit,
    onOpenMedia: (MediaItem, Rect) -> Unit,
    contentPadding: PaddingValues,
    gridColumns: Int = 4
) {
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val tileBounds = remember { mutableMapOf<String, Rect>() }
    val rootBounds = remember { LockedMediaBoundsRef() }
    val latestSelectedMediaIds by rememberUpdatedState(selectedMediaIds)
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val mediaIds = remember(lockedMediaItems) { lockedMediaItems.map { it.id }.toSet() }
    val gridRows = remember(lockedMediaItems, gridColumns) {
        lockedMediaItems.chunked(gridColumns.coerceAtLeast(2))
    }

    BackHandler(enabled = isSelectionMode) {
        selectedMediaIds = emptySet()
    }

    LaunchedEffect(Unit) {
        if (shouldAutoLaunchLockedMediaBiometric(isUnlocked, hasPin, biometricAvailable)) {
            onBiometricUnlock()
        }
    }
    LaunchedEffect(mediaIds) {
        selectedMediaIds = selectedMediaIds.intersect(mediaIds)
        tileBounds.keys.retainAll(mediaIds)
    }

    fun toggleSelection(mediaItem: MediaItem) {
        selectedMediaIds = if (mediaItem.id in selectedMediaIds) {
            selectedMediaIds - mediaItem.id
        } else {
            selectedMediaIds + mediaItem.id
        }
    }

    fun hitMedia(localPoint: Offset): MediaItem? {
        val point = Offset(
            rootBounds.value.left + localPoint.x,
            rootBounds.value.top + localPoint.y
        )
        return lockedMediaItems.firstOrNull { mediaItem ->
            tileBounds[mediaItem.id]?.contains(point) == true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds.value = it.boundsInWindow() }
            .pointerInput(isSelectionMode, lockedMediaItems) {
                var dragMode: LockedMediaDragSelectMode? = null
                var baseSelectedIds = emptySet<String>()
                val visitedMediaIds = mutableSetOf<String>()

                fun applyDragSelectionAt(localPoint: Offset) {
                    val hit = hitMedia(localPoint) ?: return
                    val mode = dragMode ?: return
                    if (!visitedMediaIds.add(hit.id)) return
                    when (mode) {
                        LockedMediaDragSelectMode.Add -> if (hit.id !in baseSelectedIds) toggleSelection(hit)
                        LockedMediaDragSelectMode.Remove -> if (hit.id in baseSelectedIds) toggleSelection(hit)
                    }
                }

                if (isSelectionMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            baseSelectedIds = latestSelectedMediaIds
                            visitedMediaIds.clear()
                            val hit = hitMedia(startOffset)
                            dragMode = hit?.let {
                                if (it.id in baseSelectedIds) LockedMediaDragSelectMode.Remove else LockedMediaDragSelectMode.Add
                            }
                            applyDragSelectionAt(startOffset)
                        },
                        onDrag = { change, _ ->
                            applyDragSelectionAt(change.position)
                            change.consume()
                        },
                        onDragEnd = {
                            dragMode = null
                            visitedMediaIds.clear()
                        },
                        onDragCancel = {
                            dragMode = null
                            visitedMediaIds.clear()
                        }
                    )
                }
            }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 48.dp,
                end = 18.dp,
                bottom = contentPadding.calculateBottomPadding() + if (isSelectionMode) 132.dp else 34.dp
            )
        ) {
            item(key = "locked-header", contentType = "locked-header") {
                LockedMediaHeader(
                    lockedItemCount = lockedMediaItems.size,
                    isUnlocked = isUnlocked,
                    onBack = onBack
                )
                Spacer(Modifier.height(30.dp))
            }

            if (!isUnlocked) {
                item(key = "locked-auth", contentType = "locked-auth") {
                    LockedMediaAuthCard(
                        hasPin = hasPin,
                        biometricAvailable = biometricAvailable,
                        authMessage = authMessage,
                        authInProgress = authInProgress,
                        onPinCreated = onPinCreated,
                        onPinUnlock = onPinUnlock,
                        onBiometricUnlock = onBiometricUnlock
                    )
                }
            } else {
                item(key = "locked-summary", contentType = "locked-summary") {
                    LockedMediaSummary(lockedItemCount = lockedMediaItems.size)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Private photos and videos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (lockedMediaItems.isEmpty()) {
                    item(key = "locked-empty", contentType = "locked-empty") {
                        LockedMediaEmpty()
                    }
                } else {
                    items(
                        items = gridRows,
                        key = { rowItems -> "locked-grid-row-${rowItems.first().id}" },
                        contentType = { "locked-grid-row" }
                    ) { rowItems ->
                        LockedMediaGridRow(
                            mediaItems = rowItems,
                            columns = gridColumns.coerceAtLeast(2),
                            selectedMediaIds = selectedMediaIds,
                            onMediaBoundsChanged = { mediaItem, bounds -> tileBounds[mediaItem.id] = bounds },
                            onMediaLongClick = { mediaItem ->
                                if (mediaItem.id !in selectedMediaIds) toggleSelection(mediaItem)
                            },
                            onMediaClick = { mediaItem, bounds ->
                                if (isSelectionMode) toggleSelection(mediaItem) else onOpenMedia(mediaItem, bounds)
                            }
                        )
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        if (isSelectionMode) {
            LockedMediaSelectionToolbar(
                selectedCount = selectedMediaIds.size,
                totalCount = lockedMediaItems.size,
                onClear = { selectedMediaIds = emptySet() },
                onSelectAll = { selectedMediaIds = mediaIds },
                onUnhide = {
                    val selectedItems = lockedMediaItems.filter { it.id in selectedMediaIds }
                    selectedMediaIds = emptySet()
                    onUnhideSelected(selectedItems)
                },
                onDelete = {
                    val selectedItems = lockedMediaItems.filter { it.id in selectedMediaIds }
                    selectedMediaIds = emptySet()
                    onDeleteSelected(selectedItems)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

internal fun shouldAutoLaunchLockedMediaBiometric(
    isUnlocked: Boolean,
    hasPin: Boolean,
    biometricAvailable: Boolean
): Boolean = !isUnlocked && hasPin && biometricAvailable

@Composable
private fun LockedMediaHeader(
    lockedItemCount: Int,
    isUnlocked: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Locked media",
                modifier = Modifier.padding(start = 18.dp),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = lockedMediaBadgeLabel(lockedItemCount, isUnlocked),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LockedMediaAuthCard(
    hasPin: Boolean,
    biometricAvailable: Boolean,
    authMessage: String?,
    authInProgress: Boolean,
    onPinCreated: (String, String) -> Unit,
    onPinUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit
) {
    var pin by remember(hasPin) { mutableStateOf("") }
    var confirmPin by remember(hasPin) { mutableStateOf("") }
    val pinIsReady = pin.length >= 4
    val setupReady = pinIsReady && pin == confirmPin

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hasPin) "Unlock locked media" else "Set up locked media",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasPin) {
                    "Use your PIN, face unlock, fingerprint, or another Android biometric method."
                } else {
                    "Create a PIN before opening locked photos and videos. Face or fingerprint unlock can be used after setup when this phone supports it."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            LockedPinField(
                value = pin,
                label = if (hasPin) "PIN" else "New PIN",
                enabled = !authInProgress,
                onValueChange = { pin = it }
            )
            if (!hasPin) {
                Spacer(Modifier.height(12.dp))
                LockedPinField(
                    value = confirmPin,
                    label = "Confirm PIN",
                    enabled = !authInProgress,
                    onValueChange = { confirmPin = it }
                )
            }
            if (!authMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = authMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                enabled = !authInProgress && if (hasPin) pinIsReady else setupReady,
                onClick = {
                    if (hasPin) {
                        onPinUnlock(pin)
                    } else {
                        onPinCreated(pin, confirmPin)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (hasPin) Icons.Filled.LockOpen else Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (hasPin) "Unlock" else "Set PIN and unlock")
            }
            if (hasPin && biometricAvailable) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onBiometricUnlock,
                    enabled = !authInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Use face or fingerprint")
                }
            }
        }
    }
}

@Composable
private fun LockedPinField(
    value: String,
    label: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        enabled = enabled,
        onValueChange = { next -> onValueChange(next.filter { it.isDigit() }.take(12)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

@Composable
private fun LockedMediaSummary(
    lockedItemCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lockedMediaSummary(lockedItemCount),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Locked media stays hidden until this screen is unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LockedMediaEmpty() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp
    ) {
        Text(
            text = "No photos or videos are locked yet. Select media in Photos or an album, then use the lock action.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LockedMediaGridRow(
    mediaItems: List<MediaItem>,
    selectedMediaIds: Set<String>,
    onMediaBoundsChanged: (MediaItem, Rect) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onMediaClick: (MediaItem, Rect) -> Unit,
    columns: Int = 4,
    spacing: androidx.compose.ui.unit.Dp = 1.dp
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val cellSize = (maxWidth - spacing * (columns - 1)) / columns
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            mediaItems.forEach { mediaItem ->
                val itemBounds = remember(mediaItem.id) { LockedMediaBoundsRef() }
                MediaThumbnail(
                    mediaItem = mediaItem,
                    modifier = Modifier.size(cellSize),
                    cornerRadius = 0.dp,
                    selected = mediaItem.id in selectedMediaIds,
                    onBoundsChanged = { bounds ->
                        itemBounds.value = bounds
                        onMediaBoundsChanged(mediaItem, bounds)
                    },
                    onLongClick = { onMediaLongClick(mediaItem) },
                    onClick = { onMediaClick(mediaItem, itemBounds.value) }
                )
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}

@Composable
private fun LockedMediaSelectionToolbar(
    selectedCount: Int,
    totalCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
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
                TextButton(onClick = onUnhide, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unhide")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

private fun lockedMediaSummary(lockedItemCount: Int): String {
    return if (lockedItemCount > 0) {
        "%1$,d photos and videos are locked.".format(lockedItemCount)
    } else {
        "Locked photos and videos will appear here after you protect them."
    }
}

private fun lockedMediaBadgeLabel(lockedItemCount: Int, isUnlocked: Boolean): String {
    return when {
        !isUnlocked -> "Locked"
        lockedItemCount > 0 -> "%1$,d items".format(lockedItemCount)
        else -> "Empty"
    }
}
