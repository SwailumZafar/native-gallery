@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.data.PhotoAnalysisWork
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail

@Composable
internal fun MomentsScreen(media: List<MediaItem>, favorites: Set<String>, onBack: () -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit, model: MomentsViewModel = viewModel(), viewerOpen: Boolean = false) {
    val state by model.uiState.collectAsStateWithLifecycle()
    var storyVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(media, favorites) { model.updateLibrary(media, favorites) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(lifecycle, viewerOpen, storyVisible) { model.setActive(!viewerOpen && !storyVisible && lifecycle.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(Unit) { onDispose { model.setActive(false); PhotoAnalysisWork.setDocumentsScrolling(false) } }
    val positions = rememberSaveableStateHolder()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MomentCard?>(null) }
    var removal by remember { mutableStateOf<MomentCard?>(null) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var highlights by rememberSaveable { mutableStateOf(true) }
    val card = state.cards.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selectedId != null && !viewerOpen && !storyVisible) { selectedId = null }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            GalleryScreenHeader(card?.name ?: "Moments", { if (selectedId != null) selectedId = null else onBack() },
                subtitle = if (card == null) "Trips, days and the photos worth keeping together" else "${card.media.size} photos and videos",
                trailingContent = { TextButton(onClick = { editing = card; editorVisible = true }) { Text(if (card == null) "Create" else "Edit") } })
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            AnimatedContent(card, Modifier.weight(1f), contentKey = { it?.id }, label = "moment-navigation",
                transitionSpec = { (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 24 }) togetherWith fadeOut(tween(110)) }) { shown ->
            positions.SaveableStateProvider(shown?.id ?: "moment-list") {
            Column(Modifier.fillMaxSize()) {
            if (shown != null) {
                val photos = if (highlights && shown.highlights.isNotEmpty()) shown.highlights else shown.media
                val gridState = rememberLazyGridState()
                LaunchedEffect(gridState) { snapshotFlow { gridState.isScrollInProgress }.collect { PhotoAnalysisWork.setDocumentsScrolling(it) } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(highlights, { highlights = true }, { Text("Highlights") })
                    FilterChip(!highlights, { highlights = false }, { Text("All photos") })
                    TextButton({ storyVisible = true }) { Text("Play story") }
                }
                LazyVerticalGrid(GridCells.Adaptive(112.dp), Modifier.weight(1f).testTag("moment-photos"), state = gridState, contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (shown.note.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { Text(shown.note, Modifier.padding(16.dp)) }
                    items(photos, key = { it.id }) { photo -> MediaThumbnail(photo, Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 8.dp,
                        onClickWithBounds = { bounds -> onOpenMedia(photo, photos, bounds) }) }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in listOf("All", "Saved", "Suggested")) FilterChip(filter == option, { filter = option }, { Text(option) })
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("People, outings and celebrations", style = MaterialTheme.typography.labelLarge)
                        Text("On-device checks skip screenshots, documents and similar frames.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(model::toggleAnalysis) { Text(if (state.scanning) "Pause" else "Find memories") }
                }
                if (state.scanning || state.total > 0) ScanProgressCard(if (state.scanning) "Finding memories" else "Memory scan complete",
                    state.scanning, state.scanned, state.total, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), state.baseline)
                val cards = state.cards.filter { filter == "All" || (filter == "Saved") == it.saved }
                val listState = rememberLazyListState()
                LaunchedEffect(listState) { snapshotFlow { listState.isScrollInProgress }.collect { PhotoAnalysisWork.setDocumentsScrolling(it) } }
                LazyColumn(Modifier.weight(1f).testTag("moment-list"), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (!state.loading && cards.isEmpty()) item { FeatureEmptyState("Your trips, together",
                        "Create a moment from your favorite photos, or let on-device checks find meaningful groups. Suggestions appear only when enough distinct photos belong together.") }
                    items(cards, key = { it.id }) { moment ->
                        Surface(Modifier.animateItem(fadeInSpec = tween(180), placementSpec = null, fadeOutSpec = null), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column {
                                MediaThumbnail(moment.cover, Modifier.fillMaxWidth().aspectRatio(1.8f), cornerRadius = 24.dp,
                                    onClick = { selectedId = moment.id; highlights = true; storyVisible = true })
                                Column(Modifier.padding(16.dp)) {
                                    Text(moment.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${moment.media.size} photos and videos · ${if (moment.saved) "Saved moment" else "Suggested"}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton({ selectedId = moment.id; highlights = true; storyVisible = true }) { Text("Open moment") }
                                        TextButton({ editing = moment; editorVisible = true }) { Text(if (moment.saved) "Edit" else "Save moment") }
                                        TextButton({ if (moment.saved) removal = moment else model.remove(moment) }) { Text(if (moment.saved) "Remove" else "Hide") }
                                    }
                                }
                            }
                        }
                    }
                    item { TextButton(model::restoreSuggestions) { Text("Restore hidden suggestions") } }
                }
            }
            }
            }
            }
        }
    }
    if (editorVisible) MomentEditorDialog(editing, media, { editorVisible = false }, { name, ids, cover, note ->
        model.saveMoment(editing?.id, name, ids, cover, note)
    })
    if (storyVisible && card != null && !viewerOpen) {
        val allowed = remember(media) { media.associateBy { it.id } }
        MomentStory(card.copy(media = card.media.mapNotNull { allowed[it.id] }, highlights = card.highlights.mapNotNull { allowed[it.id] })) {
            storyVisible = false; highlights = false
        }
    }
    removal?.let { moment -> AlertDialog(onDismissRequest = { removal = null }, title = { Text("Remove this moment?") },
        text = { Text("Your original photos and videos will stay in the gallery.") },
        confirmButton = { TextButton({ model.remove(moment); removal = null; selectedId = null }) { Text("Remove moment") } },
        dismissButton = { TextButton({ removal = null }) { Text("Cancel") } }) }
}
