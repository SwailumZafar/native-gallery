package com.example.nativegallery.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.activity.ComponentActivity
import com.example.nativegallery.ui.TestScreenAwake
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Exact generated workload used on both builds; every repository is redirected into a UUID sandbox. */
@RunWith(AndroidJUnit4::class)
class ScanThroughputTest {
    @get:Rule val host = ActivityScenarioRule(ComponentActivity::class.java)
    @Before fun keepAwake() = TestScreenAwake.enable()
    @After fun cleanAbandonedGeneratedWorkloads() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = app.cacheDir.canonicalFile
        val pattern = Regex("scan-throughput-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        cache.listFiles()?.filter { pattern.matches(it.name) && it.canonicalFile.parentFile == cache }?.forEach { directory ->
            val prefix = "scan-throughput-${directory.name}-"
            File(app.applicationInfo.dataDir, "shared_prefs").listFiles()?.filter { it.name.startsWith(prefix) && it.extension == "xml" }
                ?.forEach { app.deleteSharedPreferences(it.name.removeSuffix(".xml")) }
            check(directory.deleteRecursively())
        }
    }
    @Test fun allScannersPublishEveryCompletionAndReuseTheirCaches(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(app.cacheDir, "scan-throughput-${UUID.randomUUID()}").apply { mkdirs() }
        val namespace = "scan-throughput-${root.name}"
        val prefs = mutableSetOf<String>()
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir() = root
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val isolated = "$namespace-$name"; prefs += isolated
                return app.getSharedPreferences(isolated, mode)
            }
            override fun getDatabasePath(name: String): File {
                val path = if (File(name).isAbsolute) File(name) else File(root, name)
                check(path.canonicalPath.startsWith(root.canonicalPath + File.separator))
                path.parentFile?.mkdirs(); return path
            }
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?) =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
        }
        val photos = (0 until 16).map { index ->
            val bitmap = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap); canvas.drawColor(Color.rgb(245, 245, 235 + index))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 35f }
            listOf("CONCERT TICKET", "INVOICE RECEIPT", "Seat A${index + 1}", "12 NOV 2030", "Subtotal 1250", "Tax 150", "Total paid 1400", "Thank you for your order")
                .forEachIndexed { row, line -> canvas.drawText(line, 22f, 85f + row * 75f, paint) }
            val file = File(root, "fixture-$index.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }; bitmap.recycle()
            MediaItem("scan-$index", "fixture", MediaType.Photo, file.name, "", contentUri = Uri.fromFile(file), relativePath = "DCIM/Camera/",
                sortTimestampMillis = 1_793_000_000_000L + index * 3_600_000L, width = 640, height = 960, fileSizeBytes = file.length())
        }
        PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false); PhotoAnalysisWork.setPeopleAllowed(true)
        val measurements = mutableListOf<String>()
        val report = File(app.getExternalFilesDir(null), "feature-scan-benchmark.txt")
        fun record(value: String) { measurements += value; report.writeText(measurements.joinToString("\n")) }
        try {
            withTimeout(300_000) {
                val documents = DocumentPhotoRepository(context)
                var count = 0
                var start = SystemClock.elapsedRealtime()
                record("START documents")
                documents.scan(photos) { count = it.scannedCount }
                record("SCAN_BENCH documents_ms=${SystemClock.elapsedRealtime() - start} count=$count")
                assertEquals(16, count)
                val screenshots = ScreenshotRepository(context)
                val captures = photos.map { it.copy(title = "Screenshot-${it.id}.jpg", relativePath = "Pictures/Screenshots/") }
                start = SystemClock.elapsedRealtime()
                record("START screenshots")
                screenshots.scan(captures) { _, checked, _, _ -> count = checked }
                record("SCAN_BENCH screenshots_ms=${SystemClock.elapsedRealtime() - start} count=$count")
                assertEquals(16, count); assertTrue(screenshots.cached(captures).all { it.indexed })
                screenshots.close()
                val memories = MomentAnalysisRepository(context)
                start = SystemClock.elapsedRealtime()
                record("START moments")
                memories.scan(photos) { count = it.scanned }
                record("SCAN_BENCH moments_ms=${SystemClock.elapsedRealtime() - start} count=$count")
                assertEquals(16, count)
                val people = PeopleRepository(context)
                start = SystemClock.elapsedRealtime()
                record("START people")
                PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false); PhotoAnalysisWork.setPeopleAllowed(true)
                people.scan(photos) { _, checked, _ -> count = checked }
                record("SCAN_BENCH people_ms=${SystemClock.elapsedRealtime() - start} count=$count")
                assertEquals(16, count)
                var cachedCallbacks = 0
                start = SystemClock.elapsedRealtime()
                record("START people")
                people.scan(photos) { _, checked, _ -> count = checked; cachedCallbacks++ }
                record("SCAN_BENCH people_cached_ms=${SystemClock.elapsedRealtime() - start}")
                assertEquals(1, cachedCallbacks); assertEquals(16, count)
                File(app.getExternalFilesDir(null), "feature-scan-benchmark.txt").writeText(measurements.joinToString("\n"))
            }
        } finally {
            val gates = listOf("idle", "touching", "documentsScrolling", "peopleAllowed").joinToString { name ->
                val field = PhotoAnalysisWork::class.java.getDeclaredField(name).apply { isAccessible = true }
                "$name=${(field.get(PhotoAnalysisWork) as kotlinx.coroutines.flow.StateFlow<*>).value}"
            }
            record("GATES $gates")
            PhotoAnalysisWork.setPeopleAllowed(false)
            prefs.forEach { app.deleteSharedPreferences(it) }
            root.deleteRecursively()
        }
    }
}
