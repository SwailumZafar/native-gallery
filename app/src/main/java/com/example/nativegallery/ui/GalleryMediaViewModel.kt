package com.example.nativegallery.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.GallerySnapshotRefreshPolicy
import com.example.nativegallery.data.GalleryMediaSource
import com.example.nativegallery.data.GalleryMutationGate
import com.example.nativegallery.ui.components.GalleryImageWorkScheduler
import com.example.nativegallery.data.MediaAccessState
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.model.RecentlyDeletedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Immutable
internal data class GalleryMediaUiState(
    val mediaAccess: MediaAccessState,
    val snapshot: GallerySnapshot?,
    val trashedMedia: List<RecentlyDeletedMedia>,
    val initialSyncComplete: Boolean
)

internal class GalleryMediaViewModel(
    private val repository: GalleryMediaSource,
    initialAccess: MediaAccessState
) : ViewModel() {
    private val refreshMutex = Mutex()
    private var scheduledRefresh: Job? = null
    private var quickRefresh: Job? = null
    private var trashedRefresh: Job? = null
    private var lastFullRefreshElapsedMillis = -1L
    private var recentlyDeletedVisible = false
    private val mutationGate = GalleryMutationGate()
    private var observerRefresh: Job? = null
    private var observerRefreshPending = false
    private var suppressObserverFullRefreshUntilMillis = -1L

    private val _uiState = MutableStateFlow(
        GalleryMediaUiState(
            mediaAccess = initialAccess,
            snapshot = initialAccess.takeIf { it.hasAccess }
                ?.let { repository.cachedGallery(it.mediaKinds) },
            trashedMedia = emptyList(),
            initialSyncComplete = !initialAccess.hasAccess
        )
    )
    val uiState: StateFlow<GalleryMediaUiState> = _uiState.asStateFlow()

    init {
        requestFullRefresh()
    }

    fun updateAccess(access: MediaAccessState) {
        if (access == _uiState.value.mediaAccess) return
        scheduledRefresh?.cancel()
        quickRefresh?.cancel()
        trashedRefresh?.cancel()
        observerRefresh?.cancel()
        lastFullRefreshElapsedMillis = -1L
        _uiState.value = GalleryMediaUiState(
            mediaAccess = access,
            snapshot = access.takeIf { it.hasAccess }
                ?.let { repository.cachedGallery(it.mediaKinds) },
            trashedMedia = emptyList(),
            initialSyncComplete = !access.hasAccess
        )
        requestFullRefresh()
    }

    fun requestQuickRefresh() {
        if (mutationGate.isWriting) { observerRefreshPending = true; return }
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess || scheduledRefresh?.isActive == true) return
        quickRefresh?.cancel()
        quickRefresh = viewModelScope.launch {
            refreshSafely { refreshMutex.withLock { refreshLatestPage(access) } }
        }
    }

    fun requestFullRefresh() {
        if (mutationGate.isWriting) { observerRefreshPending = true; return }
        if (scheduledRefresh?.isActive == true) return
        quickRefresh?.cancel()
        trashedRefresh?.cancel()
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess) {
            _uiState.update {
                it.copy(snapshot = null, trashedMedia = emptyList(), initialSyncComplete = true)
            }
            return
        }
        scheduledRefresh = viewModelScope.launch {
            do {
                observerRefreshPending = false
                val presentingFromCache = _uiState.value.snapshot != null
                if (presentingFromCache) delay(160L)
                refreshSafely {
                    if (!presentingFromCache) {
                        refreshMutex.withLock { refreshLatestPage(access) }
                        delay(ColdStartFullRefreshYieldMillis)
                    }
                    // Warm-library writes need one complete reconciliation, not three newest-page
                    // queries followed by a full scan for each item in a batch.
                    refreshMutex.withLock { refreshFullLibrary(access) }
                }
            } while (observerRefreshPending && !mutationGate.isWriting && isCurrentAccess(access))
        }
    }

    fun onAppResumed() {
        if (mutationGate.isWriting) { observerRefreshPending = true; return }
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess) return
        if (
            scheduledRefresh?.isActive == true ||
            quickRefresh?.isActive == true ||
            trashedRefresh?.isActive == true
        ) return

        scheduledRefresh = viewModelScope.launch {
            refreshSafely { refreshMutex.withLock {
                val latestPageChanged = refreshLatestPage(access)
                if (
                    isCurrentAccess(access) &&
                    GallerySnapshotRefreshPolicy.shouldRunResumeFullRefresh(
                        latestPageChanged = latestPageChanged,
                        lastFullRefreshMillis = lastFullRefreshElapsedMillis,
                        nowMillis = SystemClock.elapsedRealtime()
                    )
                ) {
                    refreshFullLibrary(access)
                } else if (recentlyDeletedVisible) {
                    refreshTrashedMedia(access)
                }
            } }
        }
    }

    fun onMediaStoreChanged() {
        if (!_uiState.value.mediaAccess.hasAccess) return
        if (mutationGate.isWriting) { observerRefreshPending = true; return }
        val suppressFullRefresh = GallerySnapshotRefreshPolicy.shouldSuppressObserverFullRefresh(
            appWriteInFlight = mutationGate.isWriting,
            suppressionUntilMillis = suppressObserverFullRefreshUntilMillis,
            nowMillis = SystemClock.elapsedRealtime()
        )
        if (suppressFullRefresh && scheduledRefresh?.isActive == true) return
        observerRefreshPending = true
        observerRefresh?.cancel()
        observerRefresh = viewModelScope.launch { delay(180L); requestFullRefresh() }
    }

    fun beginAppMediaStoreWrite() {
        mutationGate.begin()
        observerRefreshPending = true
        scheduledRefresh?.cancel()
        quickRefresh?.cancel()
        trashedRefresh?.cancel()
        observerRefresh?.cancel()
    }

    fun finishAppMediaStoreWrite() {
        if (!mutationGate.finish()) return
        suppressObserverFullRefreshUntilMillis = SystemClock.elapsedRealtime() +
            GallerySnapshotRefreshPolicy.AppWriteObserverGraceMillis
        requestFullRefresh()
    }

    suspend inline fun <T> withAppMediaStoreWrite(block: () -> T): T {
        beginAppMediaStoreWrite()
        return try { block() } finally { finishAppMediaStoreWrite() }
    }

    fun setRecentlyDeletedVisible(visible: Boolean) {
        if (recentlyDeletedVisible == visible) return
        recentlyDeletedVisible = visible
        if (visible) {
            requestTrashedRefresh()
        } else {
            trashedRefresh?.cancel()
        }
    }

    fun applyMovedItems(ids: Set<String>, albumId: String?, relativePath: String?) {
        if (ids.isEmpty() || albumId == null) return
        _uiState.update { current -> current.copy(snapshot = current.snapshot?.let { snapshot ->
            snapshot.copy(mediaItems = snapshot.mediaItems.map { if (it.id in ids) it.copy(albumId = albumId, relativePath = relativePath) else it })
        }) }
    }

    private suspend fun refreshLatestPage(access: MediaAccessState): Boolean {
        if (!isCurrentAccess(access)) return false
        val queryRevision = mutationGate.revision
        val latestPage = withContext(Dispatchers.IO) {
            repository.loadGalleryPage(
                mediaKinds = access.mediaKinds,
                limit = MediaStoreGalleryRepository.InitialGalleryPageSize
            )
        }
        if (!isCurrentAccess(access) || !mutationGate.canPublish(queryRevision)) return false

        val baseSnapshot = _uiState.value.snapshot
        val mergedSnapshot = withContext(Dispatchers.Default) {
            repository.mergeLatestPage(
                mediaKinds = access.mediaKinds,
                baseSnapshot = baseSnapshot,
                latestPage = latestPage
            )
        }
        val changed = mergedSnapshot !== baseSnapshot
        if (!mutationGate.canPublish(queryRevision)) return false
        _uiState.update { current ->
            if (!changed && current.initialSyncComplete) {
                current
            } else {
                current.copy(
                    snapshot = mergedSnapshot,
                    initialSyncComplete = current.initialSyncComplete
                )
            }
        }
        if (changed) GalleryImageWorkScheduler.notifyLibrarySettled()
        return changed
    }

    private suspend fun refreshFullLibrary(access: MediaAccessState) {
        if (!isCurrentAccess(access)) return
        val queryRevision = mutationGate.revision
        val snapshot = withContext(Dispatchers.IO) {
            repository.loadGallery(access.mediaKinds)
        }
        if (!isCurrentAccess(access) || !mutationGate.canPublish(queryRevision)) return
        lastFullRefreshElapsedMillis = SystemClock.elapsedRealtime()
        _uiState.update { current ->
            val next = current.copy(
                snapshot = snapshot,
                initialSyncComplete = true
            )
            if (next == current) current else next
        }
        GalleryImageWorkScheduler.notifyLibrarySettled()
        if (recentlyDeletedVisible) refreshTrashedMedia(access)
    }

    private fun requestTrashedRefresh() {
        if (mutationGate.isWriting) { observerRefreshPending = true; return }
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess || scheduledRefresh?.isActive == true) return
        trashedRefresh?.cancel()
        trashedRefresh = viewModelScope.launch {
            refreshSafely { refreshMutex.withLock { refreshTrashedMedia(access) } }
        }
    }

    private suspend fun refreshTrashedMedia(access: MediaAccessState) {
        if (!recentlyDeletedVisible || !isCurrentAccess(access)) return
        val queryRevision = mutationGate.revision
        val trashedMedia = withContext(Dispatchers.IO) {
            repository.loadTrashedMedia(access.mediaKinds)
        }
        if (!recentlyDeletedVisible || !isCurrentAccess(access) || !mutationGate.canPublish(queryRevision)) return
        _uiState.update { current ->
            if (current.trashedMedia == trashedMedia) {
                current
            } else {
                current.copy(trashedMedia = trashedMedia)
            }
        }
    }

    private fun isCurrentAccess(access: MediaAccessState): Boolean {
        return !mutationGate.isWriting && access.hasAccess && access == _uiState.value.mediaAccess
    }

    private suspend fun refreshSafely(block: suspend () -> Unit) {
        repeat(2) { attempt ->
            try { block(); return }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                Log.w("GalleryRefresh", "Keeping the current library while MediaStore settles", failure)
                if (attempt == 0) delay(350L)
            }
        }
    }

    private companion object {
        const val ColdStartFullRefreshYieldMillis = 220L
    }
}

internal class GalleryMediaViewModelFactory(
    private val repository: MediaStoreGalleryRepository,
    private val initialAccess: MediaAccessState
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GalleryMediaViewModel::class.java))
        return GalleryMediaViewModel(repository, initialAccess) as T
    }
}
