package com.example.nativegallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.ScreenshotEntry
import com.example.nativegallery.data.ScreenshotRepository
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ScreenshotsUiState(val entries: List<ScreenshotEntry> = emptyList(), val scanning: Boolean = false,
    val checked: Int = 0, val total: Int = 0, val error: String? = null, val baseline: Int = 0)

class ScreenshotsViewModel private constructor(application: Application, private val repository: ScreenshotRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, ScreenshotRepository(application))
    internal constructor(application: Application, databaseName: String) : this(application, ScreenshotRepository(application, databaseName))
    private val mutable = MutableStateFlow(ScreenshotsUiState())
    val uiState = mutable.asStateFlow()
    private val edits = MutableStateFlow<Map<String, ScreenshotEntry>>(emptyMap())
    private val writes = Mutex()
    private var media: List<MediaItem> = emptyList()
    private var job: Job? = null
    @Volatile private var generation = 0L
    private var scanRequested = false

    fun updateLibrary(items: List<MediaItem>) {
        media = items
        val previous = job
        previous?.cancel()
        val current = ++generation
        job = viewModelScope.launch {
            previous?.join()
            try {
                val entries = repository.cached(items)
                if (current != generation) return@launch
                mutable.value = ScreenshotsUiState(entries = mergeEdits(entries), checked = entries.count { it.indexed }, total = entries.size, baseline = entries.count { it.indexed })
                if (scanRequested) scan(current, items)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (current == generation) mutable.update { it.copy(error = "Screenshots could not be loaded. Try opening this shelf again.") } }
        }
    }
    fun startScan() {
        scanRequested = true
        val previous = job
        previous?.cancel()
        val current = ++generation
        val snapshot = media.toList()
        mutable.update { it.copy(scanning = true, error = null) }
        job = viewModelScope.launch {
            previous?.join()
            scan(current, snapshot)
        }
    }
    private suspend fun scan(current: Long, snapshot: List<MediaItem>) {
        if (current != generation) return
        mutable.update { it.copy(scanning = true, error = null) }
        try {
            repository.scan(snapshot) { entries, checked, total, failed ->
                if (current == generation) mutable.update { it.copy(entries = mergeEdits(entries), checked = checked, total = total,
                    error = if (failed > 0) "$failed screenshots could not be read. You can retry after scanning." else null) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (current == generation) mutable.update { it.copy(error = "Some screenshots could not be read. You can try scanning again.") } }
        finally { if (current == generation) mutable.update { it.copy(scanning = false) } }
    }
    fun suspendWork() { generation++; job?.cancel(); mutable.update { it.copy(scanning = false) } }
    fun stopScan() { scanRequested = false; suspendWork() }
    fun change(id: String, transform: (ScreenshotEntry) -> ScreenshotEntry) {
        val current = mutable.value.entries.firstOrNull { it.media.id == id } ?: return
        val entry = transform(current)
        edits.update { it + (entry.media.id to entry) }
        mutable.update { state -> state.copy(entries = mergeEdits(state.entries)) }
        viewModelScope.launch {
            try { writes.withLock { repository.annotate(entry) } }
            catch (_: Exception) { mutable.update { it.copy(error = "That change could not be saved. Try again.") } }
        }
    }
    fun clearText() {
        stopScan()
        val previous = job
        val current = ++generation
        job = viewModelScope.launch {
            previous?.join()
            try {
                repository.clearIndex()
                val entries = repository.cached(media)
                if (current == generation) mutable.value = ScreenshotsUiState(entries = mergeEdits(entries), total = entries.size)
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(error = "The text index could not be cleared.") } }
        }
    }
    private fun mergeEdits(entries: List<ScreenshotEntry>): List<ScreenshotEntry> {
        val latest = edits.value
        return entries.map { entry -> latest[entry.media.id]?.let { entry.copy(pinned = it.pinned, done = it.done, followUp = it.followUp,
            manualCategory = it.manualCategory) } ?: entry }
    }
    override fun onCleared() { repository.close(); super.onCleared() }
}
