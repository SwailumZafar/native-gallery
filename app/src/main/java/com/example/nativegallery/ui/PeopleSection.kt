package com.example.nativegallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.PeopleRepository
import com.example.nativegallery.data.PersonGroup
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PeopleState(val repository: PeopleRepository) {
    var enabled by mutableStateOf(repository.enabled)
    var groups by mutableStateOf<List<PersonGroup>>(emptyList())
    var scanning by mutableStateOf(false)
    var scanned by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var baseline by mutableIntStateOf(0)
    var revision by mutableIntStateOf(0)
    var completedRevision = 0
    var error by mutableStateOf<String?>(null)
    var loaded by mutableStateOf(false)
    fun scan() { repository.enabled = true; enabled = true; revision++ }
    fun disable() { repository.enabled = false; enabled = false; groups = emptyList(); error = null; loaded = false }
    fun changeEnabled(value: Boolean) { if (value) scan() else disable() }
    fun publish(results: List<PersonGroup>, checked: Int, count: Int) {
        if (!enabled) return
        groups = results; scanned = checked; total = count
        scanning = checked < count; loaded = true
    }
}

@Composable
internal fun rememberPeopleState(repository: PeopleRepository, media: List<MediaItem>, libraryReady: Boolean): PeopleState {
    val state = remember(repository) { PeopleState(repository) }
    LaunchedEffect(media, libraryReady, state.enabled, state.revision) {
        // Immediately remove revoked, hidden and locked photos from the presented groups.
        val allowed = media.associateBy { it.id }
        state.groups = state.groups.mapNotNull { group ->
            val photos = group.photos.mapNotNull { allowed[it.id] }
            if (photos.size == group.photos.size) group.copy(photos = photos) else null
        }
        if (!state.enabled) { repository.clear(); return@LaunchedEffect }
        if (!libraryReady) return@LaunchedEffect
        state.groups = repository.cachedGroups(media)
        state.loaded = true
        if (state.revision <= state.completedRevision) return@LaunchedEffect
        state.scanned = 0; state.baseline = 0; state.total = media.count { !it.isVideo }; state.scanning = true; state.error = null
        var initial = true
        try {
            repository.scan(media) { groups, scanned, total ->
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    if (initial) { state.baseline = scanned; initial = false }
                    state.publish(groups, scanned, total)
                }
            }
            state.completedRevision = state.revision
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { state.loaded = true; state.error = "Some photos could not be scanned. Tap Scan to retry." }
        finally { state.scanning = false }
    }
    return state
}

internal fun PersonGroup.album(index: Int): Album = Album(id, "Person ${index + 1}", photos.size,
    photos.take(4).map { it.id }, coverUri = thumbnail, isSmart = true)

@Composable
internal fun PeopleSection(
    state: PeopleState,
    groups: List<PersonGroup>,
    onOpen: (Album, Rect) -> Unit,
    activeTransitionAlbumId: State<String?>? = null,
    onBoundsChanged: (Album, Rect) -> Unit = { _, _ -> }
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("People", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = state::scan, enabled = !state.scanning) { Text(if (state.enabled) "Scan" else "Find people") }
        }
        Text(when {
            state.scanning -> "Finding faces on this device. You can browse results as they arrive."
            state.error != null -> state.error!!
            !state.enabled -> "Group photos by face, privately on this device. Tap Find people to start."
            groups.isEmpty() -> "Tap Scan to find people in your photos, privately on this device."
            else -> "Tap a face to see their photos. Scan checks for new photos."
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.scanning || state.total > 0) ScanProgressCard(if (state.scanning) "Finding people" else "Face scan complete",
            state.scanning, state.scanned, state.total, Modifier.fillMaxWidth(), state.baseline)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(groups, key = { _, person -> person.id }) { index, person ->
                val album = remember(person, index) { person.album(index) }
                var bounds by remember(person.id) { mutableStateOf(Rect.Zero) }
                // Face crops deliberately bypass the shared gallery memory/disk thumbnail cache.
                var thumbnail by remember(person.thumbnail) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(person.thumbnail) {
                    thumbnail = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(person.thumbnail.path)?.asImageBitmap()
                    }
                }
                Column(Modifier.width(84.dp)
                    .clickable { onOpen(album, bounds) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(76.dp)
                        .onGloballyPositioned {
                            bounds = it.boundsInWindow()
                            onBoundsChanged(album, bounds)
                        }
                        .albumTransitionSource(person.id, activeTransitionAlbumId)
                        .clip(CircleShape)) {
                        thumbnail?.let { Image(it, "Person ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    }
                    Text("Person ${index + 1}", style = MaterialTheme.typography.labelMedium)
                    Text("${person.photos.size} photos", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
