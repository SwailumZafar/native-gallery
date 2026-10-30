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
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Uses generated photos and a separate index. Never scans or changes the user's library. */
@RunWith(AndroidJUnit4::class)
class PhotoAnalysisPerformanceTest {
    @Test fun legacyFacesMigrateAndPrivateRemovalsAndClearPersist(): Unit = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "face-migration-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = root
        }
        val directory = File(root, "people-v1").apply { mkdirs() }
        val photos = (1..2).map {
            MediaItem("fixture-$it", "fixture", MediaType.Photo, "face", "Today",
                contentUri = Uri.parse("file:///fixture-$it.jpg"), dateModifiedMillis = 1, fileSizeBytes = 100)
        }
        val legacy = JSONObject()
        photos.forEachIndexed { i, photo ->
            File(directory, "crop-$i.jpg").writeBytes(byteArrayOf(1))
            val face = JSONObject().put("key", "crop-$i").put("vector", JSONArray(listOf(1f, 0f, 0f)))
            legacy.put(photo.id, JSONObject().put("fingerprint", "${photo.contentUri}:1:100").put("faces", JSONArray().put(face)))
        }
        File(directory, "index.json").writeText(legacy.toString())
        PhotoAnalysisWork.setPeopleAllowed(true)
        PhotoAnalysisWork.setInteractionBusy(false)
        try {
            PeopleRepository(isolated).scan(photos) { groups, scanned, total ->
                assertEquals(total, scanned); assertEquals(1, groups.size); assertEquals(2, groups.single().photos.size)
            }
            assertFalse(File(directory, "index.json").exists())
            assertTrue(File(directory, "index.db").exists())
            val reopened = PeopleRepository(isolated)
            reopened.scan(photos.take(1)) { groups, _, _ -> assertEquals(listOf(photos.first()), groups.single().photos) }
            assertFalse("Removed/hidden photo crop must be pruned", File(directory, "crop-1.jpg").exists())
            reopened.clear()
            assertFalse(File(directory, "crop-0.jpg").exists())
            SQLiteDatabase.openDatabase(File(directory, "index.db").absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT COUNT(*) FROM photos", null).use { cursor -> cursor.moveToFirst(); assertEquals(0, cursor.getInt(0)) }
            }
        } finally { PhotoAnalysisWork.setPeopleAllowed(false); root.deleteRecursively() }
    }

    @Test fun coldAndCachedScanning(): Unit = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "analysis-fixture-${UUID.randomUUID()}").apply { mkdirs() }
        val prefix = root.name
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = root
            override fun getDatabasePath(name: String): File = if (File(name).isAbsolute) File(name) else File(root, name)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = base.getSharedPreferences("$prefix-$name", mode)
        }
        isolated.getSharedPreferences("native_gallery_document_photo_migrations", Context.MODE_PRIVATE).edit()
            .putBoolean("sqlite_index_migrated", true).putBoolean("file_browser_removed", true).commit()
        // Also allows this same test APK to measure the previously installed v0.9.21 baseline.
        runCatching {
            val cls = Class.forName("com.example.nativegallery.data.PhotoAnalysisWork")
            cls.getMethod("setPeopleAllowed", Boolean::class.javaPrimitiveType).invoke(cls.getField("INSTANCE").get(null), true)
        }
        val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        listOf("INVOICE RECEIPT", "Company: Example Supplies", "Invoice number 20261030", "Date October 30 2026",
            "Description       Quantity       Price", "Office supplies       2       120.00", "Subtotal 240.00",
            "Tax 12.00", "Total amount due 252.00", "Payment terms: due on receipt").forEachIndexed { i, text ->
            canvas.drawText(text, 70f, 180f + i * 100, paint)
        }
        try {
            val photos = (1..24).map { number ->
                val file = File(root, "$number.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                MediaItem("fixture-$number", "fixture", MediaType.Photo, "invoice-$number.jpg", "Today",
                    contentUri = Uri.fromFile(file), fileSizeBytes = file.length(), width = 1200, height = 1600,
                    mimeType = "image/jpeg", sortTimestampMillis = number.toLong())
            }
            val people = PeopleRepository(isolated)
            val startPeople = SystemClock.elapsedRealtime()
            people.scan(photos) { _, _, _ -> }
            val peopleCold = SystemClock.elapsedRealtime() - startPeople
            val startPeopleWarm = SystemClock.elapsedRealtime()
            people.scan(photos) { groups, scanned, total -> assertTrue(groups.isEmpty()); assertEquals(total, scanned) }
            val peopleWarm = SystemClock.elapsedRealtime() - startPeopleWarm
            val documents = DocumentPhotoRepository(isolated)
            var documentMatches = 0
            val startDocs = SystemClock.elapsedRealtime()
            documents.scan(photos) { documentMatches = it.matches.size }
            val docsCold = SystemClock.elapsedRealtime() - startDocs
            assertTrue("Generated invoices should be recognized", documentMatches > 0)
            val startDocsWarm = SystemClock.elapsedRealtime()
            documents.scan(photos) { assertEquals(24, it.scannedCount); assertEquals(documentMatches, it.matches.size) }
            val docsWarm = SystemClock.elapsedRealtime() - startDocsWarm
            Log.i("AnalysisBenchmark", "peopleColdMs=$peopleCold peopleWarmMs=$peopleWarm docsColdMs=$docsCold docsWarmMs=$docsWarm photos=24")
            InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply {
                putString("stream", "\nAnalysisBenchmark: peopleColdMs=$peopleCold peopleWarmMs=$peopleWarm docsColdMs=$docsCold docsWarmMs=$docsWarm photos=24\n")
            })
        } finally {
            bitmap.recycle()
            // All paths are isolated beneath this exact fixture directory.
            root.deleteRecursively()
            base.getSharedPreferences("$prefix-people", Context.MODE_PRIVATE).edit().clear().commit()
            base.getSharedPreferences("$prefix-native_gallery_document_photo_migrations", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
