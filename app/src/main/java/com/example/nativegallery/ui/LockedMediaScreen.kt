@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.prefetchMediaThumbnails

private class LockedMediaBoundsRef(var value: Rect = Rect.Zero)

@Composable
fun LockedMediaScreen(
    lockedMediaItems: List<MediaItem>,
    isUnlocked: Boolean,
    hasPin: Boolean,
    biometricAvailable: Boolean,
    authMessage: String?,
    onBack: () -> Unit,
    onPinCreated: (String, String) -> Unit,
    onPinUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onUnhideMedia: (MediaItem) -> Unit,
    onOpenMedia: (MediaItem, Rect) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var biometricPromptLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(isUnlocked, hasPin, biometricAvailable) {
        if (!isUnlocked && hasPin && biometricAvailable && !biometricPromptLaunched) {
            biometricPromptLaunched = true
            onBiometricUnlock()
        }
        if (isUnlocked) {
            biometricPromptLaunched = false
        }
    }

    LaunchedEffect(isUnlocked, lockedMediaItems.map { it.id }) {
        if (isUnlocked && lockedMediaItems.isNotEmpty()) {
            // Prefetch only after authentication, and only into the app's in-memory thumbnail cache.
            prefetchMediaThumbnails(
                context = context.applicationContext,
                mediaItems = lockedMediaItems,
                thumbnailSizes = listOf(384, 512),
                maxItems = 120
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 48.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 34.dp
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
                    items = lockedMediaItems.chunked(4),
                    key = { rowItems -> "locked-grid-row-${rowItems.first().id}" },
                    contentType = { "locked-grid-row" }
                ) { rowItems ->
                    LockedMediaGridRow(
                        mediaItems = rowItems,
                        onUnhideMedia = onUnhideMedia,
                        onOpenMedia = onOpenMedia
                    )
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

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
                onValueChange = { pin = it }
            )
            if (!hasPin) {
                Spacer(Modifier.height(12.dp))
                LockedPinField(
                    value = confirmPin,
                    label = "Confirm PIN",
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
                enabled = if (hasPin) pinIsReady else setupReady,
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
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
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
    onUnhideMedia: (MediaItem) -> Unit,
    onOpenMedia: (MediaItem, Rect) -> Unit,
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
                Box(modifier = Modifier.size(cellSize)) {
                    MediaThumbnail(
                        mediaItem = mediaItem,
                        modifier = Modifier.matchParentSize(),
                        cornerRadius = 0.dp,
                        onBoundsChanged = { itemBounds.value = it },
                        onClick = { onOpenMedia(mediaItem, itemBounds.value) }
                    )
                    TextButton(
                        onClick = { onUnhideMedia(mediaItem) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .height(28.dp)
                    ) {
                        Text(
                            text = "Show",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            repeat(columns - mediaItems.size) {
                Spacer(Modifier.size(cellSize))
            }
        }
    }
}
@Composable
private fun LockedMediaRow(
    mediaItem: MediaItem,
    onUnhide: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnail(
                mediaItem = mediaItem,
                modifier = Modifier.size(68.dp),
                cornerRadius = 14.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (mediaItem.isVideo) "Video - ${mediaItem.dateLabel}" else "Photo - ${mediaItem.dateLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            TextButton(onClick = onUnhide) {
                Text("Show")
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
