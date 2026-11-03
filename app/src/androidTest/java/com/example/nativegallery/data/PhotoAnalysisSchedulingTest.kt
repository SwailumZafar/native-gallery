package com.example.nativegallery.data

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoAnalysisSchedulingTest {
    @Test fun touchAndDocumentFlingBlockBothScannersUntilSettled(): Unit = runBlocking {
        PhotoAnalysisWork.setPeopleAllowed(true)
        PhotoAnalysisWork.setInteractionBusy(false)
        PhotoAnalysisWork.setTouching(true)
        try {
            val people = async { PhotoAnalysisWork.awaitPeopleAllowed() }
            val documents = async { PhotoAnalysisWork.awaitIdle() }
            delay(400)
            assertFalse(people.isCompleted)
            assertFalse(documents.isCompleted)
            PhotoAnalysisWork.setDocumentsScrolling(true)
            PhotoAnalysisWork.setTouching(false)
            delay(400)
            assertFalse(people.isCompleted)
            assertFalse(documents.isCompleted)
            val release = SystemClock.elapsedRealtime()
            PhotoAnalysisWork.setDocumentsScrolling(false)
            withTimeout(3000) { people.await(); documents.await() }
            assertTrue(SystemClock.elapsedRealtime() - release >= 330)
            // The quiet period is paid once per gesture, not once per photograph.
            val start = SystemClock.elapsedRealtime()
            repeat(100) { PhotoAnalysisWork.awaitPeopleAllowed(); PhotoAnalysisWork.awaitIdle() }
            assertTrue(SystemClock.elapsedRealtime() - start < 1000)
        } finally {
            PhotoAnalysisWork.setTouching(false)
            PhotoAnalysisWork.setDocumentsScrolling(false)
            PhotoAnalysisWork.setPeopleAllowed(false)
        }
    }

    @Test fun cachedEighteenThousandPhotosNeedNoDecodeAndPublishOnce(): Unit = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "large-analysis-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = root
        }
        PhotoAnalysisWork.setPeopleAllowed(true)
        PhotoAnalysisWork.setInteractionBusy(false)
        try {
            val repository = PeopleRepository(isolated)
            repository.scan(emptyList()) { _, _, _ -> }
            val photos = (1..18000).map { i ->
                MediaItem("cached-$i", "fixture", MediaType.Photo, "cached", "Today",
                    contentUri = Uri.parse("file:///absent-$i.jpg"), dateModifiedMillis = 1, fileSizeBytes = 100)
            }
            android.database.sqlite.SQLiteDatabase.openDatabase(File(root, "people-v1/index.db").path, null, 0).use { db ->
                db.beginTransaction()
                try {
                    db.compileStatement("INSERT INTO photos VALUES (?, ?, '[]')").use { statement ->
                        photos.forEach {
                            statement.bindString(1, it.id)
                            statement.bindString(2, "${it.contentUri}:1:100")
                            statement.executeInsert()
                        }
                    }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            }
            var publications = 0
            repository.scan(photos) { groups, scanned, total ->
                publications++
                assertTrue(groups.isEmpty())
                assertEquals(18000, scanned)
                assertEquals(18000, total)
            }
            assertEquals(1, publications)
        } finally {
            PhotoAnalysisWork.setPeopleAllowed(false)
            root.deleteRecursively()
        }
    }
}
