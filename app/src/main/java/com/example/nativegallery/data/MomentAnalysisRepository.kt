package com.example.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import com.example.nativegallery.model.MediaItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import android.os.SystemClock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.sqrt

internal data class MemoryScanProgress(val evidence: Map<String, MemoryEvidence>, val scanned: Int, val total: Int)

/** Only compact subject/quality evidence is cached; OCR text and decoded photos are never stored. */
internal class MomentAnalysisRepository(private val context: Context, private val databaseName: String = "photo_lane_memory_content.db") {
    private fun revision(item: MediaItem) = "2:${item.contentUri}:${item.dateModifiedMillis}:${item.fileSizeBytes}:${item.width}:${item.height}"

    suspend fun scan(photos: List<MediaItem>, onProgress: suspend (MemoryScanProgress) -> Unit) = withContext(PhotoAnalysisWork.dispatcher) {
        MemoryDatabase(context, databaseName).use { helper ->
            val db = helper.writableDatabase
            val byId = photos.associateBy { it.id }
            val evidence = mutableMapOf<String, MemoryEvidence>()
            db.query("evidence", null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val item = byId[id] ?: continue
                    if (cursor.getString(cursor.getColumnIndexOrThrow("revision")) != revision(item)) continue
                    val theme = cursor.getString(cursor.getColumnIndexOrThrow("theme"))
                    evidence[id] = MemoryEvidence(MemoryTheme.entries.firstOrNull { it.name == theme },
                        cursor.getFloat(cursor.getColumnIndexOrThrow("quality")), cursor.getLong(cursor.getColumnIndexOrThrow("fingerprint")))
                }
            }
            var count = evidence.size
            var snapshot = evidence.toMap()
            onProgress(MemoryScanProgress(snapshot, count, photos.size))
            val pending = photos.distinctBy { it.id }.filterNot { it.id in evidence }
            if (pending.isEmpty()) return@use
            var failed = 0
            var lastSnapshot = SystemClock.elapsedRealtime()
            var hasPublishedMemory = snapshot.values.any { it.theme != null }
            val writes = mutableListOf<Pair<MediaItem, MemoryEvidence>>()
            fun flush() {
                if (writes.isEmpty()) return
                db.beginTransaction()
                try {
                    writes.forEach { (photo, result) -> db.insertWithOnConflict("evidence", null, ContentValues().apply {
                        put("id", photo.id); put("revision", revision(photo)); put("theme", result.theme?.name ?: "")
                        put("quality", result.quality); put("fingerprint", result.fingerprint)
                    }, SQLiteDatabase.CONFLICT_REPLACE) }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
                writes.clear()
            }
            try {
                coroutineScope {
                    val parallelism = minOf(3, pending.size)
                    val inputs = Channel<MediaItem>(parallelism)
                    val results = Channel<Pair<MediaItem, Result<MemoryEvidence>>>(parallelism)
                    launch { try { pending.forEach { inputs.send(it) } } finally { inputs.close() } }
                    val workers = List(parallelism) { slot -> launch {
                        val executor = PhotoAnalysisWork.inferenceExecutor(slot)
                        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.60f).setExecutor(executor).build())
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().setExecutor(executor).build())
                        try { for (photo in inputs) {
                            PhotoAnalysisWork.awaitIdle()
                            val result = try {
                                val bitmap = decodeUsefulPhoto(context, requireNotNull(photo.contentUri), 640)
                                val evidence = try {
                                    PhotoAnalysisWork.awaitIdle()
                                    currentCoroutineContext().ensureActive()
                                    val metrics = memoryImageMetrics(bitmap)
                                    if (metrics.quality <= 0f) MemoryEvidence(null, 0f, metrics.fingerprint)
                                    else {
                                        val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask().associate { it.text to it.confidence }
                                        val subject = memoryTheme(labels, 0f, 0)
                                        if (subject == null) MemoryEvidence(null, metrics.quality, metrics.fingerprint)
                                        else {
                                            val text = recognizeUsefulPhoto(bitmap, textRecognizer)
                                            val coverage = text.lines.sumOf { ((it.right - it.left) * (it.bottom - it.top)).coerceAtLeast(0f).toDouble() }.toFloat()
                                            MemoryEvidence(memoryTheme(labels, coverage, text.text.length), metrics.quality, metrics.fingerprint)
                                        }
                                    }
                                } finally { bitmap.recycle() }
                                Result.success(evidence)
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) { Result.failure(error) }
                            results.send(photo to result)
                        } } finally { labeler.close(); textRecognizer.close() }
                    } }
                    launch { workers.joinAll(); results.close() }
                    for ((photo, analysis) in results) {
                        currentCoroutineContext().ensureActive()
                        val result = analysis.getOrNull()
                        if (result == null) failed++ else {
                            writes += photo to result
                            if (writes.size >= 16) flush()
                            evidence[photo.id] = result
                        }
                        count++
                        if (count == photos.size ||
                            (result?.theme != null && (!hasPublishedMemory || SystemClock.elapsedRealtime() - lastSnapshot >= 150))) {
                            snapshot = evidence.toMap(); lastSnapshot = SystemClock.elapsedRealtime()
                            hasPublishedMemory = hasPublishedMemory || result?.theme != null
                        }
                        onProgress(MemoryScanProgress(snapshot, count, photos.size))
                    }
                }
            } finally {
                withContext(NonCancellable) { flush() }
            }
            check(failed == 0) { "$failed photos were unavailable; retry to check them." }
        }
    }
}

private class MemoryDatabase(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE evidence(id TEXT PRIMARY KEY, revision TEXT NOT NULL, theme TEXT NOT NULL, quality REAL NOT NULL, fingerprint INTEGER NOT NULL)") }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

private data class MemoryImageMetrics(val quality: Float, val fingerprint: Long)
private fun memoryImageMetrics(bitmap: Bitmap): MemoryImageMetrics {
    val scaled = Bitmap.createScaledBitmap(bitmap, 160, 160, true)
    val pixels = IntArray(160 * 160)
    scaled.getPixels(pixels, 0, 160, 0, 0, 160, 160)
    if (scaled !== bitmap) scaled.recycle()
    val gray = IntArray(pixels.size) { index ->
        val c = pixels[index]; ((c shr 16 and 255) * 77 + (c shr 8 and 255) * 150 + (c and 255) * 29) shr 8
    }
    val mean = gray.average()
    val contrast = sqrt(gray.sumOf { (it - mean) * (it - mean) } / gray.size)
    var edgeEnergy = 0.0
    for (y in 1 until 159) for (x in 1 until 159) {
        val i = y * 160 + x
        val laplacian = gray[i - 1] + gray[i + 1] + gray[i - 160] + gray[i + 160] - 4 * gray[i]
        edgeEnergy += laplacian.toDouble() * laplacian
    }
    edgeEnergy /= 158 * 158
    var hash = 0L
    val small = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
    fun luminance(x: Int, y: Int): Int { val c = small.getPixel(x, y); return (c shr 16 and 255) * 77 + (c shr 8 and 255) * 150 + (c and 255) * 29 }
    for (y in 0..7) for (x in 0..7) if (luminance(x, y) > luminance(x + 1, y)) hash = hash or (1L shl (y * 8 + x))
    if (small !== bitmap) small.recycle()
    val quality = if (mean < 18 || mean > 244 || contrast < 15 || edgeEnergy < 16) 0f
        else (contrast / 70 + sqrt(edgeEnergy) / 100).toFloat().coerceIn(0f, 1f)
    return MemoryImageMetrics(quality, hash)
}
