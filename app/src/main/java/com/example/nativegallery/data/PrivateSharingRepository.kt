package com.example.nativegallery.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Core
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.floor

data class PreparedPrivatePhoto(val bitmap: Bitmap, val regions: List<PrivacyRegion>, val warning: String? = null)

class PrivateSharingRepository(context: Context, private val shareDirectory: File = File(context.cacheDir, "private_shares")) {
    private val context = context.applicationContext
    suspend fun load(item: MediaItem): Bitmap = decodeUsefulPhoto(context, requireNotNull(item.contentUri), 2048)
    suspend fun discard(uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.filter { it.authority == "${context.packageName}.private_shares" }.forEach { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    suspend fun prepare(item: MediaItem): PreparedPrivatePhoto = withContext(Dispatchers.Default) {
        require(!item.isVideo) { "Choose a photo for private sharing" }
        val bitmap = decodeUsefulPhoto(context, requireNotNull(item.contentUri), 2048)
        try {
            val regions = mutableListOf<PrivacyRegion>()
            var incomplete = false
            try { regions += faces(bitmap) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { incomplete = true }
            try {
                val text = recognizeUsefulPhoto(bitmap)
                regions += text.lines.mapIndexedNotNull { index, line ->
                    PrivacyRegion("text-$index", line.left - 0.006f, line.top - 0.004f, line.right + 0.006f, line.bottom + 0.004f,
                        if (isSensitiveShareText(line.text)) PrivacyMaskKind.SensitiveText else PrivacyMaskKind.Text).sanitized()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { incomplete = true }
            coroutineContext.ensureActive()
            PreparedPrivatePhoto(bitmap, regions, if (incomplete) "Some areas could not be detected. Add any missing areas yourself." else null)
        } catch (error: Throwable) { bitmap.recycle(); throw error }
    }

    private fun faces(bitmap: Bitmap): List<PrivacyRegion> {
        check(OpenCVLoader.initLocal()) { "Face detection unavailable" }
        Core.setNumThreads(1)
        val directory = File(context.cacheDir, "private_analysis").apply { mkdirs() }
        val model = File(directory, "yunet.onnx")
        if (!model.exists()) {
            val temp = File(directory, "yunet.tmp")
            context.assets.open("people/yunet.onnx").use { input -> temp.outputStream().use { input.copyTo(it) } }
            check(temp.renameTo(model)) { "Face detection could not start" }
        }
        val scale = minOf(1f, 640f / maxOf(bitmap.width, bitmap.height))
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        val rgba = Mat(); val image = Mat(); val found = Mat()
        return try {
            Utils.bitmapToMat(small, rgba)
            Imgproc.cvtColor(rgba, image, Imgproc.COLOR_RGBA2BGR)
            val detector = FaceDetectorYN.create(model.absolutePath, "", Size(small.width.toDouble(), small.height.toDouble()), 0.8f)
            detector.detect(image, found)
            (0 until found.rows()).mapNotNull { row ->
                val x = found.get(row, 0)[0].toFloat(); val y = found.get(row, 1)[0].toFloat()
                val width = found.get(row, 2)[0].toFloat(); val height = found.get(row, 3)[0].toFloat()
                PrivacyRegion("face-$row", (x - width * 0.12f) / small.width, (y - height * 0.15f) / small.height,
                    (x + width * 1.12f) / small.width, (y + height * 1.15f) / small.height, PrivacyMaskKind.Face).sanitized()
            }
        } finally {
            rgba.release(); image.release(); found.release()
            if (small !== bitmap) small.recycle()
        }
    }

    suspend fun preview(source: Bitmap, regions: List<PrivacyRegion>, solid: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val scale = minOf(1f, 1280f / maxOf(source.width, source.height))
        val small = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        try { renderPrivateCopy(small, regions, solid) } finally { if (small !== source) small.recycle() }
    }

    /** Re-encoding pixel data deliberately copies no EXIF, location, filename or motion-photo payload. */
    suspend fun export(source: Bitmap, regions: List<PrivacyRegion>, solid: Boolean): Uri = withContext(Dispatchers.IO) {
        val directory = shareDirectory.apply { check(mkdirs() || isDirectory) }
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.isFile && it.canonicalFile.parentFile == directory.canonicalFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
        val file = File.createTempFile("private-", ".jpg", directory)
        val rendered = renderPrivateCopy(source, regions, solid)
        try {
            file.outputStream().use { check(rendered.compress(Bitmap.CompressFormat.JPEG, 94, it)) { "Could not create share copy" } }
            coroutineContext.ensureActive()
            FileProvider.getUriForFile(context, "${context.packageName}.private_shares", file)
        } catch (error: Throwable) { file.delete(); throw error }
        finally { rendered.recycle() }
    }
}

internal fun renderPrivateCopy(source: Bitmap, regions: List<PrivacyRegion>, solid: Boolean): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(source, 0f, 0f, null)
    val cover = Paint().apply { color = Color.rgb(24, 24, 28); isAntiAlias = false }
    for (region in regions.mapNotNull { it.sanitized() }.sortedBy { it.kind != PrivacyMaskKind.Face }) {
        val box = Rect(floor(region.left * source.width).toInt().coerceIn(0, source.width - 1),
            floor(region.top * source.height).toInt().coerceIn(0, source.height - 1),
            ceil(region.right * source.width).toInt().coerceIn(1, source.width), ceil(region.bottom * source.height).toInt().coerceIn(1, source.height))
        if (solid || region.kind != PrivacyMaskKind.Face) canvas.drawRect(box, cover)
        else {
            val pixelated = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                Canvas(pixelated).drawBitmap(source, box, Rect(0, 0, 8, 8), Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawBitmap(pixelated, null, box, Paint().apply { isFilterBitmap = false })
            } finally { pixelated.recycle() }
        }
    }
    return output
}

internal fun privatePhotoShareIntent(uri: Uri): Intent = privatePhotosShareIntent(listOf(uri))

internal fun privatePhotosShareIntent(uris: List<Uri>): Intent {
    require(uris.isNotEmpty())
    return Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
    type = "image/jpeg"
    if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
    else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    clipData = ClipData.newRawUri("Private photos", uris.first()).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
