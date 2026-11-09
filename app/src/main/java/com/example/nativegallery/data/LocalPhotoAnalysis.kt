package com.example.nativegallery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal suspend fun decodeUsefulPhoto(context: Context, uri: Uri, maxEdge: Int): Bitmap = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = minOf(1f, maxEdge.toFloat() / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
        }
    } else {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        check(options.outWidth > 0 && options.outHeight > 0) { "This photo could not be opened" }
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > maxEdge) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("Photo unavailable")
        val exif = resolver.openInputStream(uri).use { it?.let(::ExifInterface) }
        val matrix = Matrix()
        if (exif?.isFlipped == true) matrix.postScale(-1f, 1f)
        matrix.postRotate((exif?.rotationDegrees ?: 0).toFloat())
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    }
}

internal data class LocalTextLine(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)
internal data class LocalTextResult(val text: String, val lines: List<LocalTextLine>)

internal suspend fun recognizeUsefulPhoto(bitmap: Bitmap): LocalTextResult {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        recognizeUsefulPhoto(bitmap, recognizer)
    } finally { recognizer.close() }
}

internal suspend fun recognizeUsefulPhoto(bitmap: Bitmap, recognizer: TextRecognizer): LocalTextResult {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask()
        return LocalTextResult(result.text.take(12_000), result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { box -> LocalTextLine(line.text, box.left.toFloat() / bitmap.width, box.top.toFloat() / bitmap.height,
                box.right.toFloat() / bitmap.width, box.bottom.toFloat() / bitmap.height) }
        })
}

// ML Kit may still read the bitmap after coroutine cancellation. Resume only when
// its task finishes, so the caller's finally block cannot recycle a live input.
internal suspend fun <T> Task<T>.awaitPhotoTask(): T = suspendCoroutine { continuation ->
    val executor = Executor { it.run() }
    addOnSuccessListener(executor) { continuation.resume(it) }
    addOnFailureListener(executor) { continuation.resumeWithException(it) }
    addOnCanceledListener(executor) { continuation.resumeWithException(CancellationException("Photo analysis cancelled")) }
}
