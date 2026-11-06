package com.example.nativegallery.data

import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Measures the actual granted library without logging names, URIs, or media contents. */
class GalleryCatalogueWarmupTest {
    @Test fun aSavedCompleteCatalogueRestoresWithoutAnotherMediaStoreScan(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val access = MediaPermissions.currentAccess(context)
        assumeTrue(access.hasAccess && !access.isLimited)
        withContext(Dispatchers.IO) {
            val repository = MediaStoreGalleryRepository(context)
            val start = SystemClock.elapsedRealtime()
            val scanned = repository.loadGallery(access.mediaKinds)
            val scanMillis = SystemClock.elapsedRealtime() - start
            repository.saveGallery(access, scanned)
            assertTrue("A stable MediaStore generation must make the saved catalogue reusable", repository.isCachedGalleryCurrent(access.mediaKinds))
            val restoreStart = SystemClock.elapsedRealtime()
            val reopened = MediaStoreGalleryRepository(context)
            val restored = reopened.loadSavedGallery(access)
            val restoreMillis = SystemClock.elapsedRealtime() - restoreStart
            assertNotNull(restored)
            assertEquals(scanned, restored)
            assertTrue("The provider revision must survive repository recreation", reopened.isCachedGalleryCurrent(access.mediaKinds))
            instrumentation.sendStatus(0, Bundle().apply {
                putString("catalogue_media_count", scanned.mediaItems.size.toString())
                putString("catalogue_album_count", scanned.albums.size.toString())
                putString("catalogue_full_scan_ms", scanMillis.toString())
                putString("catalogue_saved_restore_ms", restoreMillis.toString())
            })
        }
    }
}
