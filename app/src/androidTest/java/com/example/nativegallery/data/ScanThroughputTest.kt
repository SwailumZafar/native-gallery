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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.lifecycle.ViewModelStore
import com.example.nativegallery.ui.DocumentPhotosViewModel
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
        val pattern = Regex("(?:scan-throughput|scan-comparison)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        cache.listFiles()?.filter { pattern.matches(it.name) && it.canonicalFile.parentFile == cache }?.forEach { directory ->
            val prefix = if (directory.name.startsWith("scan-comparison-")) "${directory.name}-" else "scan-throughput-${directory.name}-"
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
        context.getSharedPreferences("native_gallery_document_photo_migrations", Context.MODE_PRIVATE).edit()
            .putBoolean("sqlite_index_migrated", true).putBoolean("file_browser_removed", true).commit()
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

                suspend fun assertIncremental(name: String, source: List<MediaItem>, createScan: suspend (List<MediaItem>, (Int) -> Unit) -> Unit) {
                    val cached = mutableListOf<Int>()
                    createScan(source) { cached += it }
                    assertEquals("$name must immediately reuse all saved results", listOf(16), cached.distinct())
                    val added = source + source.first().copy(id = "${source.first().id}-added")
                    val newCounts = mutableListOf<Int>()
                    createScan(added) { newCounts += it }
                    assertEquals("$name must only process the added photo", listOf(16, 17), newCounts.distinct())
                    val changed = source.mapIndexed { index, item -> if (index == 0) item.copy(dateModifiedMillis = 42) else item }
                    val editedCounts = mutableListOf<Int>()
                    createScan(changed) { editedCounts += it }
                    assertEquals("$name must only process the edited photo", listOf(15, 16), editedCounts.distinct())
                    val fresh = source.map { it.copy(id = "paused-${it.id}") }
                    val paused = runCatching {
                        createScan(fresh) { if (it == 4) throw CancellationException("Generated scan pause") }
                    }
                    assertTrue("$name pause should cancel this scan", paused.exceptionOrNull() is CancellationException)
                    val resumed = mutableListOf<Int>()
                    createScan(fresh) { resumed += it }
                    assertEquals("$name must retain completed work when paused", 4, resumed.first())
                    assertEquals(16, resumed.last())
                    record("INCREMENTAL $name cached=16 added=1 edited=1 resumed=${resumed.first()}")
                }
                assertIncremental("documents", photos) { items, progress ->
                    DocumentPhotoRepository(context).scan(items) { progress(it.scannedCount) }
                }
                assertIncremental("screenshots", captures) { items, progress ->
                    val reopened = ScreenshotRepository(context)
                    try { reopened.scan(items) { _, checked, _, _ -> progress(checked) } }
                    finally { reopened.close() }
                }
                assertIncremental("moments", photos) { items, progress ->
                    MomentAnalysisRepository(context).scan(items) { progress(it.scanned) }
                }
                assertIncremental("people", photos) { items, progress ->
                    PeopleRepository(context).scan(items) { _, checked, _ -> progress(checked) }
                }
                // Exercise the real refresh action, which previously invalidated every document.
                val completedPhotos = photos.map { it.copy(id = "paused-${it.id}") }
                val store = ViewModelStore()
                try {
                    val model = withContext(Dispatchers.Main) {
                        DocumentPhotosViewModel(DocumentPhotoRepository(context)).also {
                            store.put("documents", it); it.updateLibrary(completedPhotos)
                        }
                    }
                    model.uiState.first { !it.scanning && it.scannedCount == 16 }
                    withContext(Dispatchers.Main) { model.rescan() }
                    val refreshed = model.uiState.first { !it.scanning }
                    assertNull(refreshed.errorMessage)
                    assertEquals("The Scan button must retain the cached baseline", 16, refreshed.baselineCount)
                    record("INCREMENTAL documents_scan_button baseline=${refreshed.baselineCount}")
                } finally { withContext(Dispatchers.Main) { store.clear() } }
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
