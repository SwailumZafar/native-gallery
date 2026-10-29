package com.example.nativegallery.ui

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.data.GalleryMediaSource
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.MediaAccessState
import com.example.nativegallery.data.MediaKind
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.model.RecentlyDeletedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Provider fixtures only: these tests never change the phone's real media. */
@RunWith(AndroidJUnit4::class)
class GalleryMutationRefreshTest {
    @Test fun aBatchKeepsItsStableSnapshotAndReconcilesOnceAfterTheOutermostWrite() = runBlocking {
        val original = snapshot("existing-video")
        val source = Source(original)
        val model = withContext(Dispatchers.Main) { GalleryMediaViewModel(source, access) }
        try {
            eventually { model.uiState.value.initialSyncComplete }
            assertEquals(1, source.fullQueries.get())
            withContext(Dispatchers.Main) {
                model.beginAppMediaStoreWrite()
                model.beginAppMediaStoreWrite()
                repeat(80) { model.onMediaStoreChanged() }
            }
            source.next = snapshot("existing-video", "restored-older-video")
            delay(300)
            assertSame(original, model.uiState.value.snapshot)
            assertEquals(1, source.fullQueries.get())
            withContext(Dispatchers.Main) { model.finishAppMediaStoreWrite() }
            delay(250)
            assertEquals(1, source.fullQueries.get())
            withContext(Dispatchers.Main) { model.finishAppMediaStoreWrite() }
            eventually { model.uiState.value.snapshot === source.next }
            assertEquals(2, source.fullQueries.get())
            assertEquals(0, source.pageQueries.get())
            assertEquals(2, model.uiState.value.snapshot?.albums?.single()?.itemCount)
        } finally { withContext(Dispatchers.Main) { model.viewModelScope.cancel() } }
    }

    @Test fun providerFailureRetainsTheLibraryUntilTheRetrySucceeds() = runBlocking {
        val original = snapshot("unchanged-video")
        val source = Source(original)
        val model = withContext(Dispatchers.Main) { GalleryMediaViewModel(source, access) }
        try {
            eventually { model.uiState.value.initialSyncComplete }
            source.failuresRemaining.set(1)
            source.next = snapshot("unchanged-video", "restored-photo")
            withContext(Dispatchers.Main) {
                model.beginAppMediaStoreWrite()
                model.finishAppMediaStoreWrite()
            }
            eventually { source.fullQueries.get() == 2 }
            assertSame(original, model.uiState.value.snapshot)
            eventually { model.uiState.value.snapshot === source.next }
            assertEquals(3, source.fullQueries.get())
            assertTrue(model.uiState.value.initialSyncComplete)
        } finally { withContext(Dispatchers.Main) { model.viewModelScope.cancel() } }
    }

    private suspend fun eventually(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

    private class Source(private val cached: GallerySnapshot) : GalleryMediaSource {
        @Volatile var next = cached
        val fullQueries = AtomicInteger()
        val pageQueries = AtomicInteger()
        val failuresRemaining = AtomicInteger()
        override fun cachedGallery(mediaKinds: Set<MediaKind>) = cached
        override fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot {
            fullQueries.incrementAndGet()
            if (failuresRemaining.getAndSet(0) > 0) error("Provider is committing a media batch")
            return next
        }
        override fun loadGalleryPage(mediaKinds: Set<MediaKind>, limit: Int, offset: Int): GallerySnapshot {
            pageQueries.incrementAndGet()
            return next
        }
        override fun mergeLatestPage(mediaKinds: Set<MediaKind>, baseSnapshot: GallerySnapshot?, latestPage: GallerySnapshot) = latestPage
        override fun loadTrashedMedia(mediaKinds: Set<MediaKind>) = emptyList<RecentlyDeletedMedia>()
    }

    private fun snapshot(vararg ids: String): GallerySnapshot {
        val media = ids.map { MediaItem(it, "camera", MediaType.Video, it, "Today", isVideo = true) }
        return GallerySnapshot(media, listOf(Album("camera", "Camera", media.size, ids.toList(),
            hasVideoBadge = true, coverMediaItem = media.first())))
    }

    private val access = MediaAccessState(setOf(MediaKind.Images, MediaKind.Videos), false)
}
