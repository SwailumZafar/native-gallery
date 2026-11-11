package com.example.nativegallery.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision

@Composable
fun PhotoComparisonDialog(
    photoA: MediaItem,
    photoB: MediaItem,
    onDismiss: () -> Unit,
    onKeepBest: (keepItem: MediaItem, trashItem: MediaItem) -> Unit
) {
    var scaleA by remember { mutableFloatStateOf(1f) }
    var offsetA by remember { mutableStateOf(Offset.Zero) }
    var scaleB by remember { mutableFloatStateOf(1f) }
    var offsetB by remember { mutableStateOf(Offset.Zero) }

    var syncZoom by rememberSaveable { mutableStateOf(true) }
    var verticalSplit by rememberSaveable { mutableStateOf(true) }

    fun resetZoom() {
        scaleA = 1f; offsetA = Offset.Zero
        scaleB = 1f; offsetB = Offset.Zero
    }

    UsefulFeatureDialog(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("photo-comparison-dialog")
        ) {
            GalleryScreenHeader(
                title = "Compare Shots",
                onBack = onDismiss,
                subtitle = "Pinch to zoom & compare details side by side",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { verticalSplit = !verticalSplit }) {
                            Icon(
                                if (verticalSplit) Icons.Filled.ViewAgenda else Icons.Filled.ViewColumn,
                                contentDescription = "Toggle split orientation",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { syncZoom = !syncZoom }) {
                            Icon(
                                if (syncZoom) Icons.Filled.Link else Icons.Filled.LinkOff,
                                contentDescription = if (syncZoom) "Sync zoom on" else "Sync zoom off",
                                tint = if (syncZoom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (scaleA > 1.05f || scaleB > 1.05f) {
                            TextButton(onClick = ::resetZoom) {
                                Text("1x", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                val isLandscape = maxWidth > maxHeight * 1.25f
                val useVertical = verticalSplit && !isLandscape

                if (useVertical) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SingleComparisonPane(
                            photo = photoA,
                            label = "Shot A",
                            scale = scaleA,
                            offset = offsetA,
                            onTransform = { dScale, dPan ->
                                val nextScale = (scaleA * dScale).coerceIn(1f, 6f)
                                scaleA = nextScale
                                offsetA = if (nextScale > 1f) offsetA + dPan else Offset.Zero
                                if (syncZoom) {
                                    scaleB = nextScale
                                    offsetB = offsetA
                                }
                            },
                            onKeep = { onKeepBest(photoA, photoB) },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        SingleComparisonPane(
                            photo = photoB,
                            label = "Shot B",
                            scale = scaleB,
                            offset = offsetB,
                            onTransform = { dScale, dPan ->
                                val nextScale = (scaleB * dScale).coerceIn(1f, 6f)
                                scaleB = nextScale
                                offsetB = if (nextScale > 1f) offsetB + dPan else Offset.Zero
                                if (syncZoom) {
                                    scaleA = nextScale
                                    offsetA = offsetB
                                }
                            },
                            onKeep = { onKeepBest(photoB, photoA) },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SingleComparisonPane(
                            photo = photoA,
                            label = "Shot A",
                            scale = scaleA,
                            offset = offsetA,
                            onTransform = { dScale, dPan ->
                                val nextScale = (scaleA * dScale).coerceIn(1f, 6f)
                                scaleA = nextScale
                                offsetA = if (nextScale > 1f) offsetA + dPan else Offset.Zero
                                if (syncZoom) {
                                    scaleB = nextScale
                                    offsetB = offsetA
                                }
                            },
                            onKeep = { onKeepBest(photoA, photoB) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SingleComparisonPane(
                            photo = photoB,
                            label = "Shot B",
                            scale = scaleB,
                            offset = offsetB,
                            onTransform = { dScale, dPan ->
                                val nextScale = (scaleB * dScale).coerceIn(1f, 6f)
                                scaleB = nextScale
                                offsetB = if (nextScale > 1f) offsetB + dPan else Offset.Zero
                                if (syncZoom) {
                                    scaleA = nextScale
                                    offsetA = offsetB
                                }
                            },
                            onKeep = { onKeepBest(photoB, photoA) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onKeepBest(photoA, photoB) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Keep A", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onKeepBest(photoB, photoA) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Keep B", maxLines = 1)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Keep both")
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleComparisonPane(
    photo: MediaItem,
    label: String,
    scale: Float,
    offset: Offset,
    onTransform: (dScale: Float, dPan: Offset) -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F1014),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(photo.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onTransform(zoom, pan)
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                GalleryImage(
                    imageRes = photo.imageRes,
                    imageUri = photo.contentUri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    contentScale = ContentScale.Fit,
                    thumbnailSize = 2000,
                    loadQuality = ImageLoadQuality.HighQuality,
                    backgroundColor = Color.Transparent,
                    fallbackImageUri = photo.previewUri,
                    isVideo = photo.isVideo,
                    cacheRevision = mediaThumbnailCacheRevision(photo)
                )
            }

            // Top metadata tag
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    val resText = if (photo.width != null && photo.height != null) "${photo.width}×${photo.height}" else ""
                    val sizeText = photo.fileSizeBytes?.let { formatComparisonSize(it) } ?: ""
                    val info = listOf(resText, sizeText).filter { it.isNotBlank() }.joinToString(" · ")
                    if (info.isNotBlank()) {
                        Text(
                            info,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Quick Pick floating button
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = onKeep,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = "Pick best",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatComparisonSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(kb)
}
