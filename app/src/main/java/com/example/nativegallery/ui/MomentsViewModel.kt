package com.example.nativegallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.MomentLibrary
import com.example.nativegallery.data.MomentRepository
import com.example.nativegallery.data.SavedMoment
import com.example.nativegallery.data.momentHighlights
import com.example.nativegallery.data.suggestMoments
import com.example.nativegallery.data.MemoryEvidence
import com.example.nativegallery.data.MomentAnalysisRepository
import com.example.nativegallery.data.isMemoryCandidate
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MomentCard(val id: String, val name: String, val media: List<MediaItem>, val highlights: List<MediaItem>,
    val cover: MediaItem, val saved: Boolean, val note: String = "")
data class MomentsUiState(val cards: List<MomentCard> = emptyList(), val loading: Boolean = true, val error: String? = null,
    val scanning: Boolean = false, val scanned: Int = 0, val total: Int = 0, val baseline: Int = 0)

class MomentsViewModel private constructor(application: Application, private val repository: MomentRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, MomentRepository(application))
    internal constructor(application: Application, preferenceName: String) : this(application, MomentRepository(application, preferenceName))
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(MomentsUiState())
    val uiState = mutable.asStateFlow()
    private var library = MomentLibrary()
    private var media: List<MediaItem> = emptyList()
    private var favorites: Set<String> = emptySet()
    private var refresh: Job? = null
    private var scan: Job? = null
    private var evidence: Map<String, MemoryEvidence> = emptyMap()
    private val analysis = MomentAnalysisRepository(application, "${repository.cacheNamespace}_content.db")
    private var scanEnabled = true
    private var resumed = false
    private var scanKey: List<String> = emptyList()
    private var scanGeneration = 0
    private val loaded = CompletableDeferred<Unit>()
    init {
        viewModelScope.launch {
            try { mutex.withLock { library = repository.load() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "Saved moments could not be loaded.") }
            finally { loaded.complete(Unit) }
            rebuild()
        }
    }

    fun updateLibrary(items: List<MediaItem>, favoriteIds: Set<String>) {
        media = items
        favorites = favoriteIds
        rebuild()
        resumeAnalysis()
    }
    fun setActive(active: Boolean) {
        resumed = active
        if (active) resumeAnalysis() else { scanGeneration++; scan?.cancel(); scanKey = emptyList(); mutable.value = mutable.value.copy(scanning = false) }
    }
    fun toggleAnalysis() {
        scanEnabled = !mutable.value.scanning
        if (scanEnabled) { scanKey = emptyList(); resumeAnalysis() }
        else { scanGeneration++; scan?.cancel(); mutable.value = mutable.value.copy(scanning = false) }
    }
    private fun resumeAnalysis() {
        if (!resumed || !scanEnabled) return
        val photos = media.filter { isMemoryCandidate(it, it.id in favorites) }.sortedByDescending { it.sortTimestampMillis }
        val key = photos.map { "${it.id}:${it.contentUri}:${it.dateModifiedMillis}:${it.fileSizeBytes}" }
        if (key == scanKey && scanKey.isNotEmpty()) return
        val previous = scan
        previous?.cancel(); scanKey = key
        val generation = ++scanGeneration
        scan = viewModelScope.launch {
            previous?.join()
            mutable.value = mutable.value.copy(scanning = true, error = null)
            try {
                var initial = true
                analysis.scan(photos) { progress ->
                    withContext(Dispatchers.Main.immediate) { if (generation == scanGeneration) {
                    val changed = evidence !== progress.evidence
                    evidence = progress.evidence
                    mutable.value = mutable.value.copy(scanned = progress.scanned, total = progress.total,
                        baseline = if (initial) progress.scanned else mutable.value.baseline)
                    initial = false
                    if (changed) rebuild()
                    } }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "Some photos could not be checked. Your saved moments are still available."); scanKey = emptyList() }
            finally { if (generation == scanGeneration) mutable.value = mutable.value.copy(scanning = false) }
        }
    }
    private fun rebuild() {
        refresh?.cancel()
        val photos = media.toList()
        val favoriteIds = favorites.toSet()
        val content = evidence
        refresh = viewModelScope.launch {
            loaded.await()
            val snapshot = mutex.withLock { library }
            val cards = withContext(Dispatchers.Default) {
                val byId = photos.associateBy { it.id }
                val saved = snapshot.moments.mapNotNull { moment ->
                    val members = moment.mediaIds.mapNotNull(byId::get).sortedBy { it.sortTimestampMillis }
                    val cover = members.firstOrNull { it.id == moment.coverId } ?: members.firstOrNull() ?: return@mapNotNull null
                    MomentCard(moment.id, moment.name, members, momentHighlights(members, favoriteIds), cover, true, moment.note)
                }
                val savedIds = saved.flatMap { it.media }.mapTo(hashSetOf()) { it.id }
                val formatter = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())
                val suggested = suggestMoments(photos, evidence = content).filter { it.id !in snapshot.dismissedSuggestions && !it.mediaIds.all(savedIds::contains) }
                    .mapNotNull { suggestion ->
                        val members = suggestion.mediaIds.mapNotNull(byId::get)
                        val highlights = momentHighlights(members, favoriteIds)
                        val cover = highlights.firstOrNull() ?: members.firstOrNull() ?: return@mapNotNull null
                        val start = formatter.format(Instant.ofEpochMilli(suggestion.startMillis))
                        val end = formatter.format(Instant.ofEpochMilli(suggestion.endMillis))
                        val dates = if (start == end) start else "$start – $end"
                        MomentCard(suggestion.id, "${suggestion.title} · $dates", members, highlights, cover, false)
                    }
                saved + suggested
            }
            mutable.value = mutable.value.copy(cards = cards, loading = false)
        }
    }

    fun saveMoment(id: String?, name: String, ids: List<String>, coverId: String?, note: String) = mutate { current ->
        require(name.isNotBlank()) { "Give your moment a name" }
        val allowed = media.mapTo(hashSetOf()) { it.id }
        val existing = current.moments.firstOrNull { it.id == id }
        val selected = ids.filter(allowed::contains).distinct()
        require(selected.isNotEmpty()) { "Choose at least one available photo" }
        // Hiding an album or granting limited access must not silently erase saved membership.
        val safeIds = (selected + existing?.mediaIds.orEmpty().filterNot(allowed::contains)).distinct()
        val next = existing?.copy(name = name.trim(), mediaIds = safeIds, coverId = coverId, note = note)
            ?: SavedMoment(name = name.trim(), mediaIds = safeIds, coverId = coverId, note = note)
        current.copy(moments = listOf(next) + current.moments.filterNot { it.id == next.id },
            dismissedSuggestions = if (id?.startsWith("suggested:") == true) current.dismissedSuggestions + id else current.dismissedSuggestions)
    }
    fun remove(card: MomentCard) = mutate { current ->
        if (card.saved) current.copy(moments = current.moments.filterNot { it.id == card.id })
        else current.copy(dismissedSuggestions = current.dismissedSuggestions + card.id)
    }
    fun restoreSuggestions() = mutate { it.copy(dismissedSuggestions = emptySet()) }
    private fun mutate(change: (MomentLibrary) -> MomentLibrary) {
        viewModelScope.launch {
            loaded.await()
            try {
                mutex.withLock { val next = change(library); repository.save(next); library = next }
                mutable.value = mutable.value.copy(error = null)
                rebuild()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutable.value = mutable.value.copy(error = error.message ?: "Could not save this moment") }
        }
    }
}
