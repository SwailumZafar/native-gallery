package com.example.nativegallery.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.PrivacyMaskKind
import com.example.nativegallery.data.PrivacyRegion
import com.example.nativegallery.data.PrivateSharingRepository
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PrivateSharingUiState(val photo: MediaItem? = null, val preview: Bitmap? = null,
    val regions: List<PrivacyRegion> = emptyList(), val selected: Set<String> = emptySet(), val solid: Boolean = false,
    val loading: Boolean = false, val rendering: Boolean = false, val exporting: Boolean = false,
    val error: String? = null, val warning: String? = null, val shareUris: List<Uri> = emptyList(),
    val index: Int = 0, val total: Int = 0, val exportedCount: Int = 0)

private data class PrivatePhotoDraft(val regions: List<PrivacyRegion>, val selected: Set<String>, val solid: Boolean)

class PrivateSharingViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = PrivateSharingRepository(application)
    private val mutable = MutableStateFlow(PrivateSharingUiState())
    val uiState = mutable.asStateFlow()
    private var source: Bitmap? = null
    private var load: Job? = null
    private var render: Job? = null
    private var export: Job? = null
    private var session: String? = null
    private var photos: List<MediaItem> = emptyList()
    private val drafts = mutableMapOf<String, PrivatePhotoDraft>()
    private var selectionInvalid = false

    fun openSession(key: String, selectedIds: List<String>, media: List<MediaItem>) {
        if (session == key) return
        clear()
        session = key
        savedState["session"] = key
        val byId = media.associateBy { it.id }
        photos = selectedIds.distinct().mapNotNull(byId::get).filter { !it.isVideo && it.contentUri != null }
        if (photos.size != selectedIds.distinct().size || photos.isEmpty()) {
            selectionInvalid = true
            mutable.value = PrivateSharingUiState(error = "Some selected photos are no longer available. Select them again.")
        } else select(0)
    }
    fun updateAvailable(media: List<MediaItem>) {
        if (photos.isEmpty()) return
        val byId = media.associateBy { it.id }
        if (photos.any { item -> byId[item.id]?.let { it.contentUri == item.contentUri && it.dateModifiedMillis == item.dateModifiedMillis && it.fileSizeBytes == item.fileSizeBytes } != true }) {
            selectionInvalid = true
            load?.cancel(); render?.cancel()
            export?.cancel()
            mutable.update { it.copy(loading = false, rendering = false, exporting = false, shareUris = emptyList(), error = "A selected photo changed or is no longer available. Close this preview and select again.") }
        }
    }
    private fun saveDraft() {
        val state = mutable.value
        state.photo?.let { if (!state.loading && state.preview != null) drafts[it.id] = PrivatePhotoDraft(state.regions, state.selected, state.solid) }
    }
    fun move(delta: Int) {
        if (selectionInvalid) return
        val state = mutable.value
        if (state.loading || state.rendering || state.exporting) return
        val next = state.index + delta
        if (next !in photos.indices) return
        saveDraft(); select(next)
    }
    private fun select(index: Int) {
        val item = photos[index]
        load?.cancel(); render?.cancel(); export?.cancel(); source = null
        savedState["photo"] = item.id
        mutable.value = PrivateSharingUiState(photo = item, loading = true, index = index, total = photos.size)
        load = viewModelScope.launch {
            try {
                val draft = drafts[item.id]
                if (draft != null) {
                    source = repository.load(item)
                    mutable.value = mutable.value.copy(regions = draft.regions, selected = draft.selected, solid = draft.solid, loading = false)
                } else {
                    val prepared = repository.prepare(item)
                    source = prepared.bitmap
                    mutable.value = mutable.value.copy(regions = prepared.regions,
                        selected = prepared.regions.filter { it.kind == PrivacyMaskKind.Face || it.kind == PrivacyMaskKind.SensitiveText }.mapTo(hashSetOf()) { it.id },
                        loading = false, warning = prepared.warning)
                }
                renderPreview()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, error = "This photo could not be prepared. Try another photo.") } }
        }
    }
    fun clear() {
        load?.cancel(); render?.cancel(); export?.cancel(); source = null
        savedState["photo"] = null
        photos = emptyList(); drafts.clear()
        selectionInvalid = false
        mutable.value = PrivateSharingUiState()
        session = null
    }
    fun toggle(id: String) { mutable.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }; renderPreview() }
    fun toggleGroup(kind: PrivacyMaskKind) {
        val state = mutable.value
        val ids = state.regions.filter { it.kind == kind || (kind == PrivacyMaskKind.Text && it.kind == PrivacyMaskKind.SensitiveText) }.mapTo(hashSetOf()) { it.id }
        mutable.update { it.copy(selected = if (ids.isNotEmpty() && it.selected.containsAll(ids)) it.selected - ids else it.selected + ids) }
        renderPreview()
    }
    fun setSolid(value: Boolean) { mutable.update { it.copy(solid = value) }; renderPreview() }
    fun addRegion(left: Float, top: Float, right: Float, bottom: Float) {
        val region = PrivacyRegion(UUID.randomUUID().toString(), left, top, right, bottom).sanitized() ?: return
        mutable.update { it.copy(regions = it.regions + region, selected = it.selected + region.id) }
        renderPreview()
    }
    fun undo() {
        val last = mutable.value.regions.lastOrNull { it.kind == PrivacyMaskKind.Manual } ?: return
        mutable.update { it.copy(regions = it.regions - last, selected = it.selected - last.id) }
        renderPreview()
    }
    private fun renderPreview() {
        if (selectionInvalid) return
        val bitmap = source ?: return
        render?.cancel()
        val snapshot = mutable.value
        mutable.update { it.copy(rendering = true, shareUris = emptyList(), error = null) }
        render = viewModelScope.launch {
            try {
                val next = repository.preview(bitmap, snapshot.regions.filter { it.id in snapshot.selected }, snapshot.solid)
                mutable.update { it.copy(preview = next, rendering = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(rendering = false, error = "The preview could not be updated. Try again.") } }
        }
    }
    fun share() {
        if (selectionInvalid) return
        if (source == null) return
        val snapshot = mutable.value
        if (snapshot.loading || snapshot.rendering || snapshot.exporting || snapshot.error != null) return
        saveDraft()
        val unreviewed = photos.indexOfFirst { it.id !in drafts }
        if (unreviewed >= 0) { select(unreviewed); return }
        val batch = photos.map { it to drafts.getValue(it.id) }
        mutable.update { it.copy(exporting = true, error = null) }
        export = viewModelScope.launch {
            val created = mutableListOf<Uri>()
            try {
                for ((index, entry) in batch.withIndex()) {
                    val (photo, draft) = entry
                    val bitmap = repository.load(photo)
                    try { created += repository.export(bitmap, draft.regions.filter { it.id in draft.selected }, draft.solid) }
                    finally { bitmap.recycle() }
                    mutable.update { it.copy(exportedCount = index + 1) }
                }
                mutable.update { it.copy(exporting = false, shareUris = created.toList()) }
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) { repository.discard(created) }; throw cancelled
            } catch (_: Exception) {
                repository.discard(created)
                mutable.update { it.copy(exporting = false, error = "Could not prepare all copies. Your originals have not changed. Try again.") }
            }
        }
    }
    fun shared() { mutable.update { it.copy(shareUris = emptyList()) } }
    fun retry() {
        if (selectionInvalid) return
        if (source != null) renderPreview() else if (photos.isNotEmpty()) select(mutable.value.index)
    }
    fun shareFailed() { mutable.update { it.copy(shareUris = emptyList(), error = "No sharing app could be opened.") } }
}
