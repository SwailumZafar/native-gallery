package com.example.nativegallery.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max

internal fun gridThumbnailDimensionBucket(pixels: Int): Int =
    ((pixels.coerceIn(1, 1536) + 127) / 128 * 128).coerceAtMost(1536)

internal fun gridVideoThumbnailSize(width: Int, height: Int, previewSize: Int): Int =
    max(previewSize, gridThumbnailDimensionBucket(ceil(max(width, height) * 16.0 / 9.0).toInt())).coerceAtMost(1536)

internal fun gridThumbnailDetailKey(uri: String, revision: String?, width: Int, height: Int): String =
    "$uri#grid-detail-v1:${revision.orEmpty()}:${width}x$height@${max(width, height)}"

/** Resolve enough original pixels for Crop, rather than stretching the short edge of a preview. */
internal fun gridThumbnailDecodeDimensions(
    sourceWidth: Int, sourceHeight: Int, tileWidth: Int, tileHeight: Int, pixelBudget: Long
): Pair<Int, Int> {
    val width = sourceWidth.coerceAtLeast(1)
    val height = sourceHeight.coerceAtLeast(1)
    val scale = minOf(1.0, max(tileWidth.coerceAtLeast(1).toDouble() / width,
        tileHeight.coerceAtLeast(1).toDouble() / height))
    val edge = ceil(max(width, height) * scale).toInt().coerceIn(1, 4096)
    return photoDecodeDimensions(width, height, edge, pixelBudget)
}

@Composable
internal fun rememberGridThumbnailDetail(
    uri: Uri?, revision: String?, width: Int, height: Int, enabled: Boolean
): Bitmap? {
    if (!enabled || uri == null || width <= 0 || height <= 0) return null
    val context = LocalContext.current.applicationContext
    val targetWidth = gridThumbnailDimensionBucket(width)
    val targetHeight = gridThumbnailDimensionBucket(height)
    val key = gridThumbnailDetailKey(uri.toString(), revision, targetWidth, targetHeight)
    var bitmap by remember(key) { mutableStateOf(ThumbnailMemoryCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap != null) return@LaunchedEffect
        GalleryImageWorkScheduler.awaitIdle()
        val loaded = ThumbnailLoadCoordinator.load(key, validator = { !it.isRecycled }) {
            ThumbnailDiskCache.get(context, key)?.takeUnless { it.isRecycled }
                ?: decodeGridThumbnail(context, uri, targetWidth, targetHeight)?.also {
                    ThumbnailDiskCache.put(context, key, it)
                }
        }
        GalleryImageWorkScheduler.awaitIdle()
        bitmap = loaded
    }
    return bitmap
}

private suspend fun decodeGridThumbnail(context: Context, uri: Uri, width: Int, height: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        val budget = (Runtime.getRuntime().maxMemory() / 128L).coerceIn(750_000L, 2_000_000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            for (allocator in intArrayOf(ImageDecoder.ALLOCATOR_SOFTWARE, ImageDecoder.ALLOCATOR_HARDWARE)) {
                val decoded = runCatching {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                        decoder.allocator = allocator
                        val target = gridThumbnailDecodeDimensions(info.size.width, info.size.height, width, height, budget)
                        decoder.setTargetSize(target.first, target.second)
                    }
                }.getOrNull()
                if (decoded != null) return@withContext decoded
            }
        }
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val target = gridThumbnailDecodeDimensions(bounds.outWidth, bounds.outHeight, width, height, budget)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 1
                while (bounds.outWidth / (inSampleSize * 2) >= target.first &&
                    bounds.outHeight / (inSampleSize * 2) >= target.second) inSampleSize *= 2
                while ((bounds.outWidth.toLong() / inSampleSize) * (bounds.outHeight / inSampleSize) > budget)
                    inSampleSize *= 2
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }
