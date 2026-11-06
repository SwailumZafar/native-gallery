package com.example.nativegallery.ui

import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.*
import com.example.nativegallery.model.*
import com.example.nativegallery.ui.components.GalleryImageWorkScheduler
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class GalleryResumeRevisionTest {
    @Test fun unchangedProviderSkipsBothPageQueriesAndFullScanOnResume(): Unit = runBlocking {
        val source = RevisionSource()
        val model = withContext(Dispatchers.Main) { GalleryMediaViewModel(source, access) }
        try {
            withTimeout(3_000) { while (source.revisions.get() == 0) delay(10) }
            delay(100)
            withContext(Dispatchers.Main) { model.onAppResumed() }
            withTimeout(3_000) { while (source.revisions.get() < 2) delay(10) }
            assertSame(source.snapshot, model.uiState.value.snapshot)
            assertEquals(0, source.pages.get())
            assertEquals(0, source.full.get())
        } finally { withContext(Dispatchers.Main) { model.viewModelScope.cancel() } }
    }

    @Test fun changedProviderWaitsForNavigationToSettleBeforeReconciling(): Unit = runBlocking {
        val source = RevisionSource()
        val model = withContext(Dispatchers.Main) { GalleryMediaViewModel(source, access) }
        try {
            withTimeout(3_000) { while (source.revisions.get() == 0) delay(10) }
            delay(100)
            source.current = false
            GalleryImageWorkScheduler.setNavigationMoving(true)
            withContext(Dispatchers.Main) { model.onAppResumed() }
            delay(800)
            assertEquals(0, source.pages.get())
            assertEquals(0, source.full.get())
            GalleryImageWorkScheduler.setNavigationMoving(false)
            withTimeout(3_000) { while (source.full.get() == 0) delay(10) }
            assertEquals(1, source.pages.get())
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
            withContext(Dispatchers.Main) { model.viewModelScope.cancel() }
        }
    }

    private val access = MediaAccessState(setOf(MediaKind.Images), false)
    private class RevisionSource : GalleryMediaSource {
        val snapshot = GallerySnapshot(listOf(MediaItem("cached", "camera", MediaType.Photo, "Cached", "Today")), emptyList())
        val revisions = AtomicInteger()
        val pages = AtomicInteger()
        val full = AtomicInteger()
        @Volatile var current = true
        override fun cachedGallery(mediaKinds: Set<MediaKind>) = snapshot
        override fun hasCompleteCachedGallery(mediaKinds: Set<MediaKind>) = true
        override fun isCachedGalleryCurrent(mediaKinds: Set<MediaKind>): Boolean { revisions.incrementAndGet(); return current }
        override fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot { full.incrementAndGet(); return snapshot }
        override fun loadGalleryPage(mediaKinds: Set<MediaKind>, limit: Int, offset: Int): GallerySnapshot { pages.incrementAndGet(); return snapshot }
        override fun mergeLatestPage(mediaKinds: Set<MediaKind>, baseSnapshot: GallerySnapshot?, latestPage: GallerySnapshot) = baseSnapshot ?: latestPage
        override fun loadTrashedMedia(mediaKinds: Set<MediaKind>) = emptyList<RecentlyDeletedMedia>()
    }
}
