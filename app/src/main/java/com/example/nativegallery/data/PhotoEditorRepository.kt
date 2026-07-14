package com.example.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class PhotoEditFilter {
    Original,
    Mono,
    Warm,
    Cool
}

data class NormalizedEditPoint(val x: Float, val y: Float)

data class PhotoEditStroke(
    val points: List<NormalizedEditPoint>,
    val color: Int = android.graphics.Color.RED,
    val widthFraction: Float = 0.008f
)

data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun sanitized(minSize: Float = 0.08f): NormalizedCropRect {
        val safeMinSize = minSize.coerceIn(0.01f, 1f)
        var safeLeft = min(left, right).coerceIn(0f, 1f)
        var safeRight = max(left, right).coerceIn(0f, 1f)
        var safeTop = min(top, bottom).coerceIn(0f, 1f)
        var safeBottom = max(top, bottom).coerceIn(0f, 1f)
        if (safeRight - safeLeft < safeMinSize) {
            safeRight = (safeLeft + safeMinSize).coerceAtMost(1f)
            safeLeft = (safeRight - safeMinSize).coerceAtLeast(0f)
        }
        if (safeBottom - safeTop < safeMinSize) {
            safeBottom = (safeTop + safeMinSize).coerceAtMost(1f)
            safeTop = (safeBottom - safeMinSize).coerceAtLeast(0f)
        }
        return NormalizedCropRect(safeLeft, safeTop, safeRight, safeBottom)
    }

    fun isFullFrame(tolerance: Float = 0.002f): Boolean {
        val safe = sanitized()
        return safe.left <= tolerance &&
            safe.top <= tolerance &&
            safe.right >= 1f - tolerance &&
            safe.bottom >= 1f - tolerance
    }

    companion object {
        val Full = NormalizedCropRect(0f, 0f, 1f, 1f)
    }
}

internal fun normalizedCropRectForAspect(
    sourceAspect: Float,
    targetAspect: Float
): NormalizedCropRect {
    val safeSourceAspect = sourceAspect.coerceAtLeast(0.001f)
    val safeTargetAspect = targetAspect.coerceAtLeast(0.001f)
    return if (safeSourceAspect > safeTargetAspect) {
        val width = (safeTargetAspect / safeSourceAspect).coerceIn(0.08f, 1f)
        NormalizedCropRect((1f - width) / 2f, 0f, (1f + width) / 2f, 1f)
    } else {
        val height = (safeSourceAspect / safeTargetAspect).coerceIn(0.08f, 1f)
        NormalizedCropRect(0f, (1f - height) / 2f, 1f, (1f + height) / 2f)
    }
}

data class PhotoEditRecipe(
    val rotationDegrees: Int = 0,
    val cropRect: NormalizedCropRect? = null,
    val filter: PhotoEditFilter = PhotoEditFilter.Original,
    val strokes: List<PhotoEditStroke> = emptyList()
)

class PhotoEditorRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun saveEditedCopy(
        mediaItem: MediaItem,
        recipe: PhotoEditRecipe
    ): Uri? = withContext(Dispatchers.IO) {
        val sourceUri = mediaItem.contentUri ?: return@withContext null
        val decoded = decodeBitmap(sourceUri) ?: return@withContext null
        var working = decoded

        fun replaceWorking(next: Bitmap) {
            if (next !== working && !working.isRecycled) working.recycle()
            working = next
        }

        return@withContext try {
            replaceWorking(rotateBitmap(working, recipe.rotationDegrees))
            recipe.cropRect?.let { cropRect ->
                replaceWorking(cropBitmap(working, cropRect))
            }
            replaceWorking(applyFilter(working, recipe.filter))
            replaceWorking(applyMarkup(working, recipe.strokes))
            insertEditedBitmap(working, mediaItem.title)
        } finally {
            if (!working.isRecycled) working.recycle()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(appContext.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val largestSide = max(info.size.width, info.size.height)
                    if (largestSide > MaxEditSide) {
                        val scale = MaxEditSide.toFloat() / largestSide
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                val bitmap = appContext.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                    ?: return null
                val largestSide = max(bitmap.width, bitmap.height)
                if (largestSide <= MaxEditSide) {
                    bitmap
                } else {
                    val scale = MaxEditSide.toFloat() / largestSide
                    val scaled = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                    if (scaled !== bitmap) bitmap.recycle()
                    scaled
                }
            }
        }.getOrNull()
    }

    private fun cropBitmap(bitmap: Bitmap, cropRect: NormalizedCropRect): Bitmap {
        val safe = cropRect.sanitized()
        if (safe.isFullFrame()) return bitmap
        val left = floor(safe.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = floor(safe.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = ceil(safe.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = ceil(safe.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun applyFilter(bitmap: Bitmap, filter: PhotoEditFilter): Bitmap {
        if (filter == PhotoEditFilter.Original) return bitmap
        val colorMatrix = when (filter) {
            PhotoEditFilter.Original -> ColorMatrix()
            PhotoEditFilter.Mono -> ColorMatrix().apply { setSaturation(0f) }
            PhotoEditFilter.Warm -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 8f,
                    0f, 1.02f, 0f, 0f, 2f,
                    0f, 0f, 0.90f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoEditFilter.Cool -> ColorMatrix(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, 0f,
                    0f, 1.00f, 0f, 0f, 0f,
                    0f, 0f, 1.10f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { output ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            Canvas(output).drawBitmap(bitmap, 0f, 0f, paint)
        }
    }

    private fun applyMarkup(bitmap: Bitmap, strokes: List<PhotoEditStroke>): Bitmap {
        if (strokes.isEmpty()) return bitmap
        return bitmap.copy(Bitmap.Config.ARGB_8888, true).also { output ->
            val canvas = Canvas(output)
            strokes.forEach { stroke ->
                if (stroke.points.size < 2) return@forEach
                val path = Path()
                val first = stroke.points.first()
                path.moveTo(first.x * output.width, first.y * output.height)
                stroke.points.drop(1).forEach { point ->
                    path.lineTo(point.x * output.width, point.y * output.height)
                }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.color
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = max(output.width, output.height) * stroke.widthFraction
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun insertEditedBitmap(bitmap: Bitmap, sourceTitle: String): Uri? {
        val displayName = "Edited_${System.currentTimeMillis()}_${sourceTitle.substringBeforeLast('.')}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Native Gallery Edits")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val saved = runCatching {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            } == true
        }.getOrDefault(false)
        if (!saved) {
            resolver.delete(uri, null, null)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
        }
        return uri
    }

    private companion object {
        const val MaxEditSide = 4096
    }
}
