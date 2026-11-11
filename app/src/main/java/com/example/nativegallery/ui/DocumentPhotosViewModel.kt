package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.DocumentPhotoMatch
import com.example.nativegallery.data.DocumentPhotoRepository
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class DocumentPhotosUiState(
    val matches: List<DocumentPhotoMatch> = emptyList(),
    val scanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val errorMessage: String? = null,
    val baselineCount: Int = 0
)

class DocumentPhotosViewModel(
    private val repository: DocumentPhotoRepository
) : ViewModel() {
    private val mutableUiState = kotlinx.coroutines.flow.MutableStateFlow(DocumentPhotosUiState())
    val uiState = mutableUiState.asStateFlow()

    private var activeMedia: List<MediaItem> = emptyList()
    private var libraryKey: String = ""
    private var scanJob: Job? = null
    @Volatile private var scanGeneration: Long = 0L

    fun updateLibrary(mediaItems: List<MediaItem>) {
        val photos = mediaItems
            .filterNot { it.isVideo || com.example.nativegallery.data.isScreenshotMedia(it) }
            .distinctBy { it.id }
            .sortedByDescending { it.sortTimestampMillis }
        val nextKey = photos.joinToString(separator = "|") { item ->
            "${item.id}:${item.contentUri}:${item.dateModifiedMillis}:${item.sortTimestampMillis}:${item.fileSizeBytes}:${item.width}:${item.height}"
        }
        activeMedia = photos

        val scanIsActive = scanJob?.isActive == true
        val current = mutableUiState.value
        if (
            nextKey == libraryKey &&
            (scanIsActive || (current.errorMessage == null && current.scannedCount >= current.totalCount))
        ) {
            return
        }
        libraryKey = nextKey
        launchScan()
    }

    fun stopScanning() {
        scanGeneration += 1
        scanJob?.cancel()
        scanJob = null
        mutableUiState.value = mutableUiState.value.copy(scanning = false)
    }

    fun rescan() {
        launchScan()
    }

    private fun launchScan() {
        val previous = scanJob
        previous?.cancel()
        scanGeneration += 1
        val generation = scanGeneration
        val media = activeMedia.toList()
        mutableUiState.value = mutableUiState.value.copy(
            scanning = media.isNotEmpty(),
            totalCount = media.size,
            errorMessage = null
        )
        scanJob = viewModelScope.launch {
            try {
                previous?.join()
                repository.scan(media) { progress ->
                    if (generation != scanGeneration) return@scan
                    mutableUiState.value = DocumentPhotosUiState(
                        matches = progress.matches,
                        scanning = progress.scanning,
                        scannedCount = progress.scannedCount,
                        totalCount = progress.totalCount,
                        baselineCount = if (progress.initial) progress.scannedCount else mutableUiState.value.baselineCount
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (generation == scanGeneration) {
                    mutableUiState.value = mutableUiState.value.copy(
                        scanning = false,
                        errorMessage = "Some photos could not be checked. Try scanning again."
                    )
                }
            } finally {
                if (generation == scanGeneration) {
                    mutableUiState.value = mutableUiState.value.copy(scanning = false)
                    scanJob = null
                }
            }
        }
    }
}

class DocumentPhotosViewModelFactory(
    private val repository: DocumentPhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DocumentPhotosViewModel::class.java))
        return DocumentPhotosViewModel(repository) as T
    }
}
