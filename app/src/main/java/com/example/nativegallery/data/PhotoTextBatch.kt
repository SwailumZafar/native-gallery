package com.example.nativegallery.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

/** Four full-resolution inputs share OCR setup; no source is resized to fit the sheet. */
internal suspend fun <T> scanPhotoText(
    items: List<T>,
    decode: suspend (T) -> Bitmap?,
    onResult: suspend (T, Result<LocalTextResult?>) -> Unit
) = coroutineScope {
    data class Input<T>(val item: T, val bitmap: Result<Bitmap?>)
    val slots = Semaphore(2)
    fun recycle(batch: List<Input<T>>) { batch.forEach { it.bitmap.getOrNull()?.recycle() }; slots.release() }
    val queue = Channel<List<Input<T>>>(1, onUndeliveredElement = ::recycle)
    val producer = launch {
        try {
            for (group in items.asSequence().chunked(4)) {
                slots.acquire()
                val batch = mutableListOf<Input<T>>()
                var transferred = false
                try {
                    for (item in group) {
                        PhotoAnalysisWork.awaitIdle()
                        val bitmap = try { Result.success(decode(item)) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { Result.failure(error) }
                        batch += Input(item, bitmap)
                    }
                    transferred = true
                    queue.send(batch)
                } finally { if (!transferred) recycle(batch) }
            }
        } finally { queue.close() }
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    var batching = true
    try {
        for (batch in queue) {
            try {
                PhotoAnalysisWork.awaitIdle()
                val available = batch.mapNotNull { input -> input.bitmap.getOrNull()?.let { input to it } }
                val results = if (available.isEmpty()) emptyList() else try {
                    val text = if (batching) recognizePhotoTextBatch(available.map { it.second }, recognizer)
                        else available.map { PhotoAnalysisWork.awaitIdle(); recognizeUsefulPhoto(it.second, recognizer) }
                    // Text-free camera rolls retain the original fast single-photo path after
                    // the first batch, rather than repeatedly paying for safety retries.
                    if (text.count { it.text.isBlank() } * 2 >= text.size) batching = false
                    text.map { Result.success<LocalTextResult?>(it) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    // A bad batch must not discard readable neighbours or permanently index a failure.
                    available.map { (_, bitmap) ->
                        try {
                            PhotoAnalysisWork.awaitIdle()
                            Result.success(recognizeUsefulPhoto(bitmap, recognizer))
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { Result.failure(error) }
                    }
                }
                val byInput = available.mapIndexed { index, (input, _) -> input to results[index] }.toMap()
                for (input in batch) {
                    currentCoroutineContext().ensureActive()
                    onResult(input.item, byInput[input] ?: input.bitmap.map { null })
                }
            } finally { recycle(batch) }
        }
    } finally { producer.cancel(); queue.cancel(); recognizer.close() }
}

internal suspend fun recognizePhotoTextBatch(bitmaps: List<Bitmap>, recognizer: TextRecognizer,
    onRetry: ((Int, String) -> Unit)? = null): List<LocalTextResult> {
    require(bitmaps.isNotEmpty() && bitmaps.size <= 4)
    if (bitmaps.size == 1) return listOf(recognizeUsefulPhoto(bitmaps.single(), recognizer))
    val cellWidth = bitmaps.maxOf { it.width }
    val cellHeight = bitmaps.maxOf { it.height }
    val columns = if (bitmaps.size == 2 && cellWidth > cellHeight) 1 else 2
    val rows = (bitmaps.size + columns - 1) / columns
    val gap = 64
    val width = columns * cellWidth + (columns - 1) * gap
    val height = rows * cellHeight + (rows - 1) * gap
    val runtime = Runtime.getRuntime()
    val freeHeap = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    // Bound the temporary bitmap independently of device/library size.
    if (width.toLong() * height > 8_000_000L || maxOf(width, height) > 4096 ||
        width.toLong() * height * 4 + 32L * 1024 * 1024 > freeHeap) {
        return bitmaps.map { PhotoAnalysisWork.awaitIdle(); recognizeUsefulPhoto(it, recognizer) }
    }
    val bounds = bitmaps.mapIndexed { index, bitmap ->
        val left = (index % columns) * (cellWidth + gap)
        val top = (index / columns) * (cellHeight + gap)
        Rect(left, top, left + bitmap.width, top + bitmap.height)
    }
    val sheet = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lines = List(bitmaps.size) { mutableListOf<LocalTextLine>() }
    val blocks = List(bitmaps.size) { mutableSetOf<Int>() }
    val ambiguous = mutableSetOf<Int>()
    try {
        val paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
        Canvas(sheet).apply {
            drawColor(Color.WHITE)
            bitmaps.forEachIndexed { index, bitmap -> drawBitmap(bitmap, bounds[index].left.toFloat(), bounds[index].top.toFloat(), paint) }
        }
        PhotoAnalysisWork.awaitIdle()
        val text = recognizer.process(InputImage.fromBitmap(sheet, 0)).awaitPhotoTask()
        text.textBlocks.forEachIndexed { blockIndex, block -> block.lines.forEach { line ->
            val box = line.boundingBox ?: return@forEach
            val owner = bounds.indexOfFirst { it.contains(box) }
            if (owner >= 0) {
                val tile = bounds[owner]
                lines[owner] += line.localLine(tile)
                blocks[owner] += blockIndex
            } else {
                // OCR sometimes joins neighbouring columns into one line. Split its words by
                // their actual source rectangle instead of retrying every photo in that row.
                val words = line.elements.groupBy { element ->
                    element.boundingBox?.let { word -> bounds.indexOfFirst { it.contains(word) } } ?: -1
                }
                if (words.isEmpty() || words.containsKey(-1)) {
                    bounds.forEachIndexed { index, tile -> if (Rect.intersects(tile, box)) ambiguous += index }
                } else words.forEach { (index, elements) ->
                    val tile = bounds[index]
                    val wordBounds = Rect(requireNotNull(elements.first().boundingBox))
                    elements.drop(1).forEach { wordBounds.union(requireNotNull(it.boundingBox)) }
                    lines[index] += LocalTextLine(elements.joinToString(" ") { it.text },
                        (wordBounds.left - tile.left).toFloat() / tile.width(), (wordBounds.top - tile.top).toFloat() / tile.height(),
                        (wordBounds.right - tile.left).toFloat() / tile.width(), (wordBounds.bottom - tile.top).toFloat() / tile.height())
                    blocks[index] += blockIndex
                }
            }
        } }
    } finally { sheet.recycle() }
    return bitmaps.mapIndexed { index, bitmap ->
        val result = LocalTextResult(lines[index].joinToString("\n") { it.text }.take(12_000), lines[index], blocks[index].size)
        // Small print, an empty result, or a boundary-spanning line receives the original
        // single-photo pass. Batching must not trade readable text for its speed gain.
        if (index in ambiguous || result.lines.isEmpty() || result.lines.any { (it.bottom - it.top) * bitmap.height < 20f }) {
            onRetry?.invoke(index, when { index in ambiguous -> "boundary"; result.lines.isEmpty() -> "empty"; else -> "small-print" })
            PhotoAnalysisWork.awaitIdle()
            recognizeUsefulPhoto(bitmap, recognizer)
        } else result
    }
}

private fun Text.Line.localLine(tile: Rect): LocalTextLine {
    val box = requireNotNull(boundingBox)
    return LocalTextLine(text, (box.left - tile.left).toFloat() / tile.width(), (box.top - tile.top).toFloat() / tile.height(),
        (box.right - tile.left).toFloat() / tile.width(), (box.bottom - tile.top).toFloat() / tile.height())
}
