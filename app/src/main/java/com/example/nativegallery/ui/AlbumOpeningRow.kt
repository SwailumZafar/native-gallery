package com.example.nativegallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.ThumbnailMemoryCache
import com.example.nativegallery.ui.components.ThumbnailDiskCache
import com.example.nativegallery.ui.components.ThumbnailLoadCoordinator
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import com.example.nativegallery.ui.components.mediaThumbnailIsVideo
import com.example.nativegallery.ui.components.thumbnailCacheIdentityUri
import com.example.nativegallery.ui.components.VideoBadge
import com.example.nativegallery.ui.components.rememberVideoDurationMillis
import com.example.nativegallery.ui.components.videoDurationLabel
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keep mounted rows alive; install their handlers/decoders one row per frame after motion. */
@Composable
internal fun rememberAlbumRowInteractive(deferred: Boolean, row: Int, firstVisibleRow: () -> Int): State<Boolean> {
    val interactive = remember { mutableStateOf(!deferred) }
    LaunchedEffect(deferred) {
        if (!deferred && !interactive.value) {
            // Sample only when activating. Reading the index during composition subscribes
            // every mounted row to every row crossed during a fast fling.
            repeat((row - firstVisibleRow()).coerceIn(0, 15) + 1) { withFrameNanos { } }
            interactive.value = true
        }
    }
    return interactive
}

/** Batched poster reads and the same badges as the live grid, without per-tile image loaders. */
@Composable
internal fun AlbumOpeningRow(mediaItems: List<MediaItem>, columns: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val cacheUris = remember(mediaItems) {
        mediaItems.map { item ->
            item.contentUri?.let { uri ->
                thumbnailCacheIdentityUri(uri, mediaThumbnailIsVideo(item), mediaThumbnailCacheRevision(item))
            }
        }
    }
    var images by remember(cacheUris) {
        mutableStateOf(cacheUris.map { uri ->
            uri?.let { ThumbnailMemoryCache.getNearest(it, 384) }?.takeUnless { it.isRecycled }?.asImageBitmap()
        })
    }
    LaunchedEffect(cacheUris) {
        if (images.all { it != null }) return@LaunchedEffect
        val initial = images
        val cached = withContext(Dispatchers.IO) {
            cacheUris.mapIndexed { index, uri ->
                initial[index] ?: uri?.let {
                    val key = ThumbnailMemoryCache.key(it, 384)
                    (ThumbnailMemoryCache.getNearest(it, 384) ?: ThumbnailLoadCoordinator.load(key, { bitmap -> !bitmap.isRecycled }) {
                        ThumbnailDiskCache.get(context, key)?.takeUnless { bitmap -> bitmap.isRecycled }
                    })?.takeUnless { bitmap -> bitmap.isRecycled }?.asImageBitmap()
                }
            }
        }
        if (cached != initial) images = cached
    }
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.layout { measurable, constraints ->
        val spacing = 1.dp.roundToPx()
        val available = constraints.maxWidth - spacing * (columns - 1)
        val cell = ((available + columns - 1) / columns).coerceAtLeast(1)
        val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, cell))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }) {
        Canvas(Modifier.matchParentSize()) {
            // Match Row.weight's pixel distribution, including its tallest square.
            // Otherwise upgrading a cached row shifts every following row by a pixel.
            val spacing = 1.dp.roundToPx()
            val available = size.width.roundToInt() - spacing * (columns - 1)
            val base = (available.toFloat() / columns).roundToInt()
            val remainder = available - base * columns
            images.forEachIndexed { index, image ->
                val left = (base + spacing) * index + minOf(index, abs(remainder)) * remainder.sign
                val cell = base + if (index < abs(remainder)) remainder.sign else 0
                if (image == null) {
                    drawRect(placeholder, Offset(left.toFloat(), 0f), Size(cell.toFloat(), cell.toFloat()))
                } else {
                    val side = minOf(image.width, image.height)
                    drawImage(
                        image = image,
                        srcOffset = IntOffset((image.width - side) / 2, (image.height - side) / 2),
                        srcSize = IntSize(side, side),
                        dstOffset = IntOffset(left, 0),
                        dstSize = IntSize(cell, cell),
                        filterQuality = FilterQuality.Low
                    )
                }
            }
        }
        if (mediaItems.any { it.isVideo && it.contentUri != null }) Row(
            Modifier.matchParentSize(), horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            repeat(columns) { index ->
                Box(Modifier.weight(1f).aspectRatio(1f)) {
                    val item = mediaItems.getOrNull(index)
                    if (item?.isVideo == true && item.contentUri != null) {
                        VideoBadge(
                            duration = videoDurationLabel(item, rememberVideoDurationMillis(item)),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
