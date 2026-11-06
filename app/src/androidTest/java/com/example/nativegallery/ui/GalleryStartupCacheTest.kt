package com.example.nativegallery.ui

import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.*
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.model.RecentlyDeletedMedia
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GalleryStartupCacheTest {
    @Test fun savedAlbumsBecomeUsableBeforeASlowFullScanCompletes(): Unit = runBlocking {
        val source = SlowSource()
        val access = MediaAccessState(setOf(MediaKind.Images), false)
        val model = withContext(Dispatchers.Main) { GalleryMediaViewModel(source, access) }
        try {
            withTimeout(3000) { while (!model.uiState.value.initialSyncComplete) delay(10) }
            assertSame(source.saved, model.uiState.value.snapshot)
            assertEquals(0, source.pages.get())
            assertTrue(source.started.await(2, TimeUnit.SECONDS))
            // The complete saved catalogue is already visible while the provider is blocked.
            assertEquals(1L, source.release.count)
            source.release.countDown()
            withTimeout(3000) { while (model.uiState.value.snapshot !== source.fresh) delay(10) }
        } finally {
            source.release.countDown()
            withContext(Dispatchers.Main) { model.viewModelScope.cancel() }
        }
    }

    private class SlowSource : GalleryMediaSource {
        private val media = MediaItem("saved", "camera", MediaType.Photo, "Saved", "Today")
        val saved = GallerySnapshot(listOf(media), listOf(Album("camera", "Camera", 1, listOf(media.id), coverMediaItem = media)))
        val fresh = saved.copy(mediaItems = listOf(media.copy(title = "Fresh")))
        val pages = AtomicInteger()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        override fun cachedGallery(mediaKinds: Set<MediaKind>): GallerySnapshot? = null
        override fun loadSavedGallery(access: MediaAccessState) = saved
        override fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot {
            started.countDown(); check(release.await(5, TimeUnit.SECONDS)); return fresh
        }
        override fun loadGalleryPage(mediaKinds: Set<MediaKind>, limit: Int, offset: Int): GallerySnapshot {
            pages.incrementAndGet(); return fresh
        }
        override fun mergeLatestPage(mediaKinds: Set<MediaKind>, baseSnapshot: GallerySnapshot?, latestPage: GallerySnapshot) = latestPage
        override fun loadTrashedMedia(mediaKinds: Set<MediaKind>) = emptyList<RecentlyDeletedMedia>()
    }
}
