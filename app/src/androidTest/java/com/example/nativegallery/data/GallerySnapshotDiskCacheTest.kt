package com.example.nativegallery.data

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GallerySnapshotDiskCacheTest {
    @Test fun anOvernightCatalogueKeepsItsProviderRevisionAfterProcessLoss() {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "catalogue-test-${System.nanoTime()}")
        var now = 1_000_000L
        val access = MediaAccessState(setOf(MediaKind.Images), false)
        val snapshot = GallerySnapshot(emptyList(), emptyList())
        try {
            GallerySnapshotDiskCache(directory) { now }.save(access, snapshot, "external_primary:version:42")
            now += 30L * 60 * 60 * 1000
            val saved = GallerySnapshotDiskCache(directory) { now }.loadCatalogue(access)
            assertEquals(snapshot, saved?.snapshot)
            assertEquals("external_primary:version:42", saved?.providerRevision)
            assertNull(GallerySnapshotDiskCache(directory) { now }.load(access.copy(isLimited = true)))
        } finally { directory.deleteRecursively() }
    }

    @Test fun completeCatalogueSurvivesProcessCacheLossWithBurstAndMotionMetadata() {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "catalogue-test-${System.nanoTime()}")
        try {
            val access = MediaAccessState(setOf(MediaKind.Images, MediaKind.Videos), false)
            val child = MediaItem("child", "camera", MediaType.Photo, "burst.jpg", "Today", contentUri = Uri.parse("content://test/child"))
            val item = child.copy(id = "cover", title = "Cover", fileSizeBytes = 1234, width = 4000, height = 3000,
                relativePath = "DCIM/Camera/", dateModifiedMillis = 456, sortTimestampMillis = 789,
                isBurst = true, burstCount = 2, burstGroupId = "burst", burstItems = listOf(child),
                isMotionPhoto = true, motionVideoUri = Uri.parse("content://test/motion"), motionVideoOffset = 234)
            val snapshot = GallerySnapshot(listOf(item), listOf(Album("camera", "Camera", 1, listOf(item.id),
                coverUri = item.contentUri, coverMediaItem = item)))
            GallerySnapshotDiskCache(directory).save(access, snapshot)
            assertEquals(snapshot, GallerySnapshotDiskCache(directory).load(access))
            assertNull(GallerySnapshotDiskCache(directory).load(access.copy(isLimited = true)))
            assertNull(GallerySnapshotDiskCache(directory).load(access.copy(mediaKinds = setOf(MediaKind.Images))))
        } finally { directory.deleteRecursively() }
    }

    @Test fun corruptSavedCatalogueFallsBackToAProviderRefresh() {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "catalogue-test-${System.nanoTime()}")
        try {
            directory.mkdirs()
            File(directory, "catalogue-Images.gz").writeText("interrupted catalogue")
            assertNull(GallerySnapshotDiskCache(directory).load(MediaAccessState(setOf(MediaKind.Images), false)))
        } finally { directory.deleteRecursively() }
    }
}
