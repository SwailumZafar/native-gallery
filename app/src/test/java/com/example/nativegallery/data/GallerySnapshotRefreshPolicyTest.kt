package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySnapshotRefreshPolicyTest {
    @Test
    fun unchangedLatestPageSkipsMergeWork() {
        val items = listOf(media("new", 20L), media("old", 10L))
        val base = GallerySnapshot(items, emptyList())
        val latest = GallerySnapshot(items, emptyList())

        assertFalse(
            GallerySnapshotRefreshPolicy.latestPageHasChanges(
                baseSnapshot = base,
                latestPage = latest,
                pageLimit = 120
            )
        )
    }

    @Test
    fun newOrUpdatedRowsRequireReconciliation() {
        val base = GallerySnapshot(listOf(media("old", 10L)), emptyList())
        val latest = GallerySnapshot(listOf(media("new", 20L)), emptyList())

        assertTrue(
            GallerySnapshotRefreshPolicy.latestPageHasChanges(
                baseSnapshot = base,
                latestPage = latest,
                pageLimit = 120
            )
        )
    }

    @Test
    fun deletionFromLatestPageRequiresReconciliation() {
        val base = GallerySnapshot(
            listOf(media("new", 30L), media("middle", 20L), media("old", 10L)),
            emptyList()
        )
        val latest = GallerySnapshot(
            listOf(media("middle", 20L), media("old", 10L)),
            emptyList()
        )

        assertTrue(
            GallerySnapshotRefreshPolicy.latestPageHasChanges(
                baseSnapshot = base,
                latestPage = latest,
                pageLimit = 2
            )
        )
    }

    @Test
    fun resumeFullRefreshRunsForChangesOrStaleState() {
        assertTrue(
            GallerySnapshotRefreshPolicy.shouldRunResumeFullRefresh(
                latestPageChanged = true,
                lastFullRefreshMillis = 9_000L,
                nowMillis = 10_000L
            )
        )
        assertTrue(
            GallerySnapshotRefreshPolicy.shouldRunResumeFullRefresh(
                latestPageChanged = false,
                lastFullRefreshMillis = 0L,
                nowMillis = GallerySnapshotRefreshPolicy.ResumeFullRefreshMaxAgeMillis
            )
        )
        assertFalse(
            GallerySnapshotRefreshPolicy.shouldRunResumeFullRefresh(
                latestPageChanged = false,
                lastFullRefreshMillis = 9_000L,
                nowMillis = 10_000L
            )
        )
    }

    @Test
    fun appOwnedWritesSuppressDuplicateObserverFullRefreshes() {
        assertTrue(
            GallerySnapshotRefreshPolicy.shouldSuppressObserverFullRefresh(
                appWriteInFlight = true,
                suppressionUntilMillis = -1L,
                nowMillis = 10_000L
            )
        )
        assertTrue(
            GallerySnapshotRefreshPolicy.shouldSuppressObserverFullRefresh(
                appWriteInFlight = false,
                suppressionUntilMillis = 12_000L,
                nowMillis = 10_000L
            )
        )
        assertFalse(
            GallerySnapshotRefreshPolicy.shouldSuppressObserverFullRefresh(
                appWriteInFlight = false,
                suppressionUntilMillis = 9_999L,
                nowMillis = 10_000L
            )
        )
    }
    private fun media(id: String, timestamp: Long) = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = id,
        dateLabel = "Today",
        sortTimestampMillis = timestamp
    )
}