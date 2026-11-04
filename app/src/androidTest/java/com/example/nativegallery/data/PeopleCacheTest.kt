package com.example.nativegallery.data

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PeopleCacheTest {
    @Test fun browsingUsesSavedMembershipWhileAnalysisIsPausedAndFiltersChangedMedia(): Unit = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "people-cache-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = root
        }
        try {
            val repository = PeopleRepository(context)
            val photo = MediaItem("one", "test", MediaType.Photo, "Fixture", "", contentUri = Uri.parse("content://fixture/one"),
                dateModifiedMillis = 3, fileSizeBytes = 12)
            val directory = File(root, "people-v1")
            File(directory, "cover.jpg").writeBytes(byteArrayOf(1))
            val entry = JSONObject().put("id", photo.id).put("revision", "${photo.contentUri}:3:12")
            val group = JSONObject().put("id", "person:fixture").put("cover", "cover.jpg").put("photos", JSONArray().put(entry))
            File(directory, "groups.json").writeText(JSONArray().put(group).toString())
            PhotoAnalysisWork.setPeopleAllowed(false)
            val cached = withTimeout(2000) { repository.cachedGroups(listOf(photo)) }
            assertEquals(listOf(photo), cached.single().photos)
            assertFalse(File(directory, "index.db").exists())
            assertTrue(repository.cachedGroups(emptyList()).isEmpty())
            assertTrue(repository.cachedGroups(listOf(photo.copy(dateModifiedMillis = 4))).isEmpty())
            repository.clear()
            assertFalse(File(directory, "groups.json").exists())
        } finally { root.deleteRecursively() }
    }
}
