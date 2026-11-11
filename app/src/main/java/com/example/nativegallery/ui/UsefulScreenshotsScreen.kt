@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativegallery.data.PhotoAnalysisWork
import com.example.nativegallery.data.ScreenshotCategory
import com.example.nativegallery.data.ScreenshotEntry
import com.example.nativegallery.data.screenshotDate
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SearchPill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun UsefulScreenshotsScreen(media: List<MediaItem>, onBack: () -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit, model: ScreenshotsViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(media, lifecycle) {
        if (lifecycle.isAtLeast(Lifecycle.State.RESUMED)) model.updateLibrary(media) else model.suspendWork()
    }
    DisposableEffect(Unit) { onDispose { model.suspendWork(); PhotoAnalysisWork.setDocumentsScrolling(false) } }
    val list = rememberLazyListState()
    LaunchedEffect(list) { snapshotFlow { list.isScrollInProgress }.collect { PhotoAnalysisWork.setDocumentsScrolling(it) } }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<ScreenshotEntry>>(emptyList()) }
    LaunchedEffect(state.entries, query, filter) {
        delay(100)
        val snapshot = state.entries
        val term = query
        val category = filter
        entries = withContext(Dispatchers.Default) {
            snapshot.filter { entry ->
                (if (category == "Done") entry.done else !entry.done) &&
                    (category != "Pinned" || entry.pinned) &&
                    (category != "All" || (entry.indexed || entry.manualCategory != null) && entry.displayCategory != ScreenshotCategory.Other || entry.pinned || entry.followUp != null) &&
                    (category in listOf("All", "Pinned", "Done") || entry.displayCategory.label == category) &&
                    (term.isBlank() || entry.text.contains(term, true) || entry.media.title.contains(term, true) || entry.displayCategory.label.contains(term, true))
            }.sortedWith(compareByDescending<ScreenshotEntry> { it.pinned }.thenBy { it.followUp ?: LocalDate.MAX }.thenByDescending { it.media.sortTimestampMillis })
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            GalleryScreenHeader("Useful screenshots", onBack, subtitle = "Tickets, receipts and plans you saved")
            Text("Find what you’ll need again. Scan to sort your screen captures, pin the important ones, and mark them done when finished.",
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ScanProgressCard(if (state.scanning) "Sorting screenshots" else "Find useful screenshots", state.scanning,
                state.checked, state.total, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), state.baseline,
                if (state.scanning) "Pause" else "Scan screenshots", if (state.scanning) model::stopScan else model::startScan)
            SearchPill(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), "Search screenshots and scanned text", query, onQueryChange = { query = it })
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in listOf("All", "Pinned") + ScreenshotCategory.entries.map { it.label } + "Done") {
                    FilterChip(filter == option, { filter = option }, { Text(if (option == "All") "Useful" else option) })
                }
            }
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entries.isEmpty()) item { FeatureEmptyState(if (state.scanning) "Putting useful things in order…" else "Keep the useful ones close",
                    if (state.entries.none { it.indexed }) "Start with Scan screenshots. Tickets, receipts, orders and events will get their own sections. Everything else stays in Other."
                    else "No items in this section. Look in Other for unclassified captures, or pin a screenshot from its details.") }
                val sections = entries.groupBy { if (it.pinned && filter != "Done") "Pinned" else it.displayCategory.label }
                for ((name, members) in sections) {
                    item(key = "heading-$name") { Text("$name · ${members.size}", Modifier.padding(top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    items(members, key = { it.media.id }) { entry ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box {
                                    MediaThumbnail(entry.media, Modifier.width(102.dp).height(126.dp), cornerRadius = 12.dp,
                                        onClickWithBounds = { bounds -> onOpenMedia(entry.media, entries.map { it.media }, bounds) })
                                    if (entry.pinned && filter != "Done") {
                                        Surface(Modifier.padding(6.dp).align(Alignment.TopStart), shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)) {
                                            Icon(Icons.Filled.PushPin, "Pinned", Modifier.padding(4.dp).size(12.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(entry.text.lineSequence().firstOrNull { it.isNotBlank() } ?: "Saved screenshot",
                                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                                        Text(if (entry.indexed || entry.manualCategory != null) entry.displayCategory.label else "Ready to scan",
                                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                    if (entry.media.dateLabel.isNotBlank()) Text(entry.media.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    entry.followUp?.let { Text("Follow-up ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                                    TextButton({ detailId = entry.media.id }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) { Text("Details") }
                                }
                            }
                        }
                    }
                }
                if (state.entries.any { it.indexed }) item { TextButton(model::clearText) { Text("Clear scanned text") } }
            }
        }
    }
    state.entries.firstOrNull { it.media.id == detailId }?.let { entry -> ScreenshotDetails(entry, { detailId = null }, { transform -> model.change(entry.media.id, transform) },
        { item, bounds -> detailId = null; onOpenMedia(item, listOf(item), bounds) }) }
}

@Composable
private fun ScreenshotDetails(entry: ScreenshotEntry, onDismiss: () -> Unit, onChange: ((ScreenshotEntry) -> ScreenshotEntry) -> Unit,
    onOpen: (MediaItem, Rect) -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val suggested = remember(entry.text) { screenshotDate(entry.text) }
    fun chooseDate() {
        val initial = entry.followUp ?: suggested ?: LocalDate.now().plusDays(1)
        DatePickerDialog(context, { _, year, month, day -> onChange { it.copy(followUp = LocalDate.of(year, month + 1, day)) } },
            initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }
    UsefulFeatureDialog(onDismiss) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            GalleryScreenHeader(entry.displayCategory.label, onDismiss, subtitle = entry.media.title,
                trailingContent = { TextButton({ onChange { it.copy(pinned = !it.pinned) } }) { Text(if (entry.pinned) "Unpin" else "Pin") } })
            MediaThumbnail(entry.media, Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 300.dp).padding(horizontal = 16.dp), cornerRadius = 18.dp,
                onClickWithBounds = { onOpen(entry.media, it) })
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(entry.manualCategory == null, { onChange { it.copy(manualCategory = null) } }, { Text("Auto") })
                    for (category in ScreenshotCategory.entries) FilterChip(entry.manualCategory == category,
                        { onChange { it.copy(manualCategory = category) } }, { Text(category.label) })
                }
                Text(entry.followUp?.let { "Follow-up: ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" }
                    ?: suggested?.let { "Date found: ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" } ?: "Set a date to find this again when you need it.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(::chooseDate) { Text(if (entry.followUp == null) "Set follow-up" else "Change date") }
                    if (entry.followUp != null) TextButton({ onChange { it.copy(followUp = null) } }) { Text("Clear date") }
                }
                if (entry.followUp != null) TextButton({
                    val begin = entry.followUp.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                        putExtra(CalendarContract.Events.TITLE, "${entry.displayCategory.label} follow-up")
                        putExtra(CalendarContract.Events.DESCRIPTION, "Saved in Photo Lane: ${entry.media.title}")
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 30 * 60 * 1000L)
                    }
                    if (runCatching { context.startActivity(intent) }.isFailure) message = "No calendar app could be opened. The follow-up is still saved here."
                }) { Text("Add to calendar") }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            SelectionContainer(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(entry.text.ifBlank { "Scan this screenshot to read and search its text." }, style = MaterialTheme.typography.bodyLarge)
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = entry.text.isNotBlank(), onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Screenshot text", entry.text))
                    message = "Text copied"
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy text")
                }
                Button({ onChange { it.copy(done = !it.done) }; onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(if (entry.done) Icons.Filled.Refresh else Icons.Filled.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (entry.done) "Restore to shelf" else "Mark done")
                }
            }
        }
    }
}
