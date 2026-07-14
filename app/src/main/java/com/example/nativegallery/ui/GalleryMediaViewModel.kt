package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.GallerySnapshot
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
        if (!access.hasAccess) return
        quickRefresh?.cancel()
        quickRefresh = viewModelScope.launch {
            refreshMutex.withLock { refreshLatestPage(access) }
        }
    }

    fun requestFullRefresh() {
        scheduledRefresh?.cancel()
        quickRefresh?.cancel()
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

    fun onMediaStoreChanged() {
        if (!_uiState.value.mediaAccess.hasAccess) return
        scheduledRefresh?.cancel()
        quickRefresh?.cancel()
        scheduledRefresh = viewModelScope.launch {
            delay(120L)
            val access = _uiState.value.mediaAccess
            refreshMutex.withLock { refreshLatestPage(access) }
            delay(780L)
            refreshMutex.withLock { refreshFullLibrary(_uiState.value.mediaAccess) }
        }
    }

    private suspend fun refreshLatestPage(access: MediaAccessState) {
        if (!isCurrentAccess(access)) return
        val latestPage = withContext(Dispatchers.IO) {
            repository.loadGalleryPage(
                mediaKinds = access.mediaKinds,
                limit = MediaStoreGalleryRepository.InitialGalleryPageSize
            )
        }
        if (!isCurrentAccess(access)) return
        _uiState.update { current ->
            current.copy(
                snapshot = repository.mergeLatestPage(
                    mediaKinds = access.mediaKinds,
                    baseSnapshot = current.snapshot,
                    latestPage = latestPage
                ),
                initialSyncComplete = true
            )
        }
    }

    private suspend fun refreshFullLibrary(access: MediaAccessState) {
        if (!isCurrentAccess(access)) return
        val (snapshot, trashedMedia) = withContext(Dispatchers.IO) {
            repository.loadGallery(access.mediaKinds) to
                repository.loadTrashedMedia(access.mediaKinds)
        }
        if (!isCurrentAccess(access)) return
        _uiState.update {
            it.copy(
                snapshot = snapshot,
                trashedMedia = trashedMedia,
                initialSyncComplete = true
            )
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
