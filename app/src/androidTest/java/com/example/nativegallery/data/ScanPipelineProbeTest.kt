package com.example.nativegallery.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.ui.TestScreenAwake
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Executor

/** Generated inputs only. Measures the native/ML stages separately from disk and UI. */
@RunWith(AndroidJUnit4::class)
class ScanPipelineProbeTest {
    @get:Rule val host = ActivityScenarioRule(ComponentActivity::class.java)
    @Before fun awake() = TestScreenAwake.enable()
    @Test fun fullResolutionBatchKeepsSmallPrintAndEachPhotosText(): Unit = runBlocking {
        fun fixtureText(index: Int) = listOf("INVOICE RECEIPT SOURCE ${index + 1}", "Order number 20301112", "Office supplies quantity 12", "Subtotal 1250.50",
            "Tax 150.00", "Total paid 1400.50", "Payment received November 12", "Thank you for your order")
        val bitmaps = listOf(16f, 22f, 32f, 44f).mapIndexed { index, size ->
            Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888).also { bitmap ->
                Canvas(bitmap).apply {
                    drawColor(Color.WHITE)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = size }
                    fixtureText(index)
                        .forEachIndexed { row, line -> drawText(line, 32f, 85f + row * 85f, paint) }
                }
            }
        }
        PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false)
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val originals = bitmaps.map { recognizeUsefulPhoto(it, client) }
            val batched = recognizePhotoTextBatch(bitmaps, client)
            fun normalized(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
            fun errors(expected: String, actual: String): Int {
                var previous = IntArray(actual.length + 1) { it }
                expected.forEachIndexed { row, char ->
                    val current = IntArray(actual.length + 1); current[0] = row + 1
                    actual.forEachIndexed { column, other ->
                        current[column + 1] = minOf(current[column] + 1, previous[column + 1] + 1,
                            previous[column] + if (char == other) 0 else 1)
                    }
                    previous = current
                }
                return previous.last()
            }
            originals.zip(batched).forEachIndexed { index, (original, batch) ->
                assertTrue("Generated text must be recognized", original.text.isNotBlank())
                val expected = normalized(fixtureText(index).joinToString(" "))
                val oldErrors = errors(expected, normalized(original.text))
                val newErrors = errors(expected, normalized(batch.text))
                assertTrue("Photo $index must preserve or improve accuracy against the known text: old=$oldErrors new=$newErrors", newErrors <= oldErrors)
                assertEquals(classifyDocumentPhoto(original.text, original.lines.size, original.blockCount)?.category,
                    classifyDocumentPhoto(batch.text, batch.lines.size, batch.blockCount)?.category)
            }
        } finally { client.close(); bitmaps.forEach { it.recycle() } }
    }
    @Test fun measureBoundedAnalysisPipelines(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val report = File(app.getExternalFilesDir(null), "scan-pipeline-probe.txt")
        val rows = mutableListOf<String>()
        fun record(line: String) { rows += line; report.writeText(rows.joinToString("\n")) }
        val bitmap = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(245, 245, 240))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 35f }
            listOf("CONCERT TICKET", "INVOICE RECEIPT", "Seat A12", "12 NOV 2030", "Subtotal 1250", "Tax 150", "Total paid 1400", "Thank you for your order")
                .forEachIndexed { i, text -> drawText(text, 22f, 85f + i * 75f, paint) }
        }
        try {
            withTimeout(120_000) {
                val text = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    val retries = mutableListOf<String>()
                    val exactBatch = recognizePhotoTextBatch(List(4) { bitmap }, text) { index, reason -> retries += "$index:$reason" }
                    assertTrue(exactBatch.all { it.text.contains("RECEIPT") })
                    record("production_batch_retries=$retries")
                    repeat(2) { pass ->
                        var start = SystemClock.elapsedRealtime()
                        repeat(16) { assertTrue(text.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask().text.contains("RECEIPT")) }
                        record("ocr_single_${pass}_ms=${SystemClock.elapsedRealtime() - start}")
                        for (batch in listOf(2, 4)) {
                            val columns = 2
                            val sheet = Bitmap.createBitmap(2 * 640 + 64, ((batch + 1) / 2) * 960 + 64, Bitmap.Config.ARGB_8888)
                            Canvas(sheet).apply {
                                drawColor(Color.WHITE)
                                repeat(batch) { drawBitmap(bitmap, (it % columns) * 704f, (it / columns) * 1024f, null) }
                            }
                            try {
                                start = SystemClock.elapsedRealtime()
                                repeat(16 / batch) {
                                    val result = text.process(InputImage.fromBitmap(sheet, 0)).awaitPhotoTask()
                                    val receipts = result.textBlocks.flatMap { it.lines }.count { it.text.contains("RECEIPT") }
                                    assertEquals("batch=$batch text=${result.text}", batch, receipts)
                                }
                                record("ocr_batch${batch}_${pass}_ms=${SystemClock.elapsedRealtime() - start}")
                            } finally { sheet.recycle() }
                        }
                    }
                } finally { text.close() }
                val executor = Executors.newFixedThreadPool(3)
                try {
                    val textClients = List(3) {
                        val lane = object : Executor { override fun execute(command: Runnable) = executor.execute(command) }
                        TextRecognition.getClient(TextRecognizerOptions.Builder().setExecutor(lane).build())
                    }
                    try {
                        repeat(2) { pass ->
                            val start = SystemClock.elapsedRealtime()
                            coroutineScope { textClients.mapIndexed { worker, client -> async(Dispatchers.Default) {
                                for (i in worker until 16 step textClients.size) {
                                    assertTrue(client.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask().text.contains("RECEIPT"))
                                }
                            } }.awaitAll() }
                            record("ocr_independent3_${pass}_ms=${SystemClock.elapsedRealtime() - start}")
                        }
                    } finally { textClients.forEach { it.close() } }
                    for (parallelism in listOf(1, 3)) {
                        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(.6f).setExecutor(executor).build())
                        try {
                            val start = SystemClock.elapsedRealtime()
                            coroutineScope { (0 until parallelism).map { worker -> async(Dispatchers.Default) {
                                for (i in worker until 16 step parallelism) labeler.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask()
                            } }.awaitAll() }
                            record("labeler_workers${parallelism}_ms=${SystemClock.elapsedRealtime() - start}")
                        } finally { labeler.close() }
                    }
                    val independent = List(3) {
                        val lane = object : Executor { override fun execute(command: Runnable) = executor.execute(command) }
                        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(.6f).setExecutor(lane).build())
                    }
                    try {
                        repeat(2) { pass ->
                            val start = SystemClock.elapsedRealtime()
                            coroutineScope { independent.mapIndexed { worker, labeler -> async(Dispatchers.Default) {
                                for (i in worker until 16 step independent.size) labeler.process(InputImage.fromBitmap(bitmap, 0)).awaitPhotoTask()
                            } }.awaitAll() }
                            record("labeler_independent3_${pass}_ms=${SystemClock.elapsedRealtime() - start}")
                        }
                    } finally { independent.forEach { it.close() } }
                } finally { executor.shutdown() }
                check(OpenCVLoader.initLocal())
                val model = File(app.cacheDir, "probe-yunet.onnx")
                app.assets.open("people/yunet.onnx").use { source -> model.outputStream().use { source.copyTo(it) } }
                val scaled = Bitmap.createScaledBitmap(bitmap, 426, 640, true)
                try {
                    for (workers in listOf(1, 2, 3)) {
                        Core.setNumThreads(2)
                        val start = SystemClock.elapsedRealtime()
                        coroutineScope { (0 until workers).map { worker -> async(Dispatchers.Default) {
                            val detector = FaceDetectorYN.create(model.absolutePath, "", Size(640.0, 640.0), .9f)
                            val rgba = Mat(); val bgr = Mat(); val faces = Mat()
                            try {
                                Utils.bitmapToMat(scaled, rgba); Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
                                detector.setInputSize(bgr.size())
                                for (i in worker until 16 step workers) detector.detect(bgr, faces)
                            } finally { rgba.release(); bgr.release(); faces.release() }
                        } }.awaitAll() }
                        record("yunet_workers${workers}_ms=${SystemClock.elapsedRealtime() - start}")
                    }
                } finally { scaled.recycle(); model.delete() }
            }
        } finally { bitmap.recycle() }
    }
}
