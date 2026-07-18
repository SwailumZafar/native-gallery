package com.example.nativegallery.ui

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.GallerySnapshot
import com.example.nativegallery.data.GallerySnapshotRefreshPolicy
import com.example.nativegallery.data.MediaAccessState
import com.example.nativegallery.data.MediaStoreGalleryRepository
import com.example.nativegallery.model.RecentlyDeletedMedia
import kotlinx.coroutines.Dispatchers
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
    private val repository: MediaStoreGalleryRepository,
    initialAccess: MediaAccessState
) : ViewModel() {
    private val refreshMutex = Mutex()
    private var scheduledRefresh: Job? = null
    private var quickRefresh: Job? = null
    private var trashedRefresh: Job? = null
    private var lastFullRefreshElapsedMillis = -1L
    private var recentlyDeletedVisible = false
    private var appWriteInFlight = false
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
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess || scheduledRefresh?.isActive == true) return
        quickRefresh?.cancel()
        quickRefresh = viewModelScope.launch {
            refreshMutex.withLock { refreshLatestPage(access) }
        }
    }

    fun requestFullRefresh() {
        scheduledRefresh?.cancel()
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
            refreshMutex.withLock {
                refreshLatestPage(access)
                refreshFullLibrary(access)
            }
        }
    }

    fun onAppResumed() {
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess) return
        if (
            scheduledRefresh?.isActive == true ||
            quickRefresh?.isActive == true ||
            trashedRefresh?.isActive == true
        ) return

        scheduledRefresh = viewModelScope.launch {
            refreshMutex.withLock {
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
            }
        }
    }

    fun onMediaStoreChanged() {
        if (!_uiState.value.mediaAccess.hasAccess) return
        val suppressFullRefresh = GallerySnapshotRefreshPolicy.shouldSuppressObserverFullRefresh(
            appWriteInFlight = appWriteInFlight,
            suppressionUntilMillis = suppressObserverFullRefreshUntilMillis,
            nowMillis = SystemClock.elapsedRealtime()
        )
        if (suppressFullRefresh) {
            requestQuickRefresh()
            return
        }

        scheduledRefresh?.cancel()
        quickRefresh?.cancel()
        trashedRefresh?.cancel()
        scheduledRefresh = viewModelScope.launch {
            delay(120L)
            val access = _uiState.value.mediaAccess
            refreshMutex.withLock { refreshLatestPage(access) }
            delay(780L)
            refreshMutex.withLock { refreshFullLibrary(_uiState.value.mediaAccess) }
        }
    }

    fun beginAppMediaStoreWrite() {
        appWriteInFlight = true
    }

    fun finishAppMediaStoreWrite() {
        appWriteInFlight = false
        suppressObserverFullRefreshUntilMillis = SystemClock.elapsedRealtime() +
            GallerySnapshotRefreshPolicy.AppWriteObserverGraceMillis
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

    private suspend fun refreshLatestPage(access: MediaAccessState): Boolean {
        if (!isCurrentAccess(access)) return false
        val latestPage = withContext(Dispatchers.IO) {
            repository.loadGalleryPage(
                mediaKinds = access.mediaKinds,
                limit = MediaStoreGalleryRepository.InitialGalleryPageSize
            )
        }
        if (!isCurrentAccess(access)) return false

        val baseSnapshot = _uiState.value.snapshot
        val mergedSnapshot = withContext(Dispatchers.Default) {
            repository.mergeLatestPage(
                mediaKinds = access.mediaKinds,
                baseSnapshot = baseSnapshot,
                latestPage = latestPage
            )
        }
        val changed = mergedSnapshot !== baseSnapshot
        _uiState.update { current ->
            if (!changed && current.initialSyncComplete) {
                current
            } else {
                current.copy(
                    snapshot = mergedSnapshot,
                    initialSyncComplete = true
                )
            }
        }
        return changed
    }

    private suspend fun refreshFullLibrary(access: MediaAccessState) {
        if (!isCurrentAccess(access)) return
        val snapshot = withContext(Dispatchers.IO) {
            repository.loadGallery(access.mediaKinds)
        }
        if (!isCurrentAccess(access)) return
        lastFullRefreshElapsedMillis = SystemClock.elapsedRealtime()
        _uiState.update { current ->
            val next = current.copy(
                snapshot = snapshot,
                initialSyncComplete = true
            )
            if (next == current) current else next
        }
        if (recentlyDeletedVisible) refreshTrashedMedia(access)
    }

    private fun requestTrashedRefresh() {
        val access = _uiState.value.mediaAccess
        if (!access.hasAccess || scheduledRefresh?.isActive == true) return
        trashedRefresh?.cancel()
        trashedRefresh = viewModelScope.launch {
            refreshMutex.withLock { refreshTrashedMedia(access) }
        }
    }

    private suspend fun refreshTrashedMedia(access: MediaAccessState) {
        if (!recentlyDeletedVisible || !isCurrentAccess(access)) return
        val trashedMedia = withContext(Dispatchers.IO) {
            repository.loadTrashedMedia(access.mediaKinds)
        }
        if (!recentlyDeletedVisible || !isCurrentAccess(access)) return
        _uiState.update { current ->
            if (current.trashedMedia == trashedMedia) {
                current
            } else {
                current.copy(trashedMedia = trashedMedia)
            }
        }
    }

    private fun isCurrentAccess(access: MediaAccessState): Boolean {
        return access.hasAccess && access == _uiState.value.mediaAccess
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