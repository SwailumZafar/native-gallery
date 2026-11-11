@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        ?: state.flashbacks.firstOrNull { it.card.id == selectedId }?.card
    BackHandler(enabled = selectedId != null && !viewerOpen && !storyVisible) { selectedId = null }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            GalleryScreenHeader(card?.name ?: "Moments", { if (selectedId != null) selectedId = null else onBack() },
                subtitle = if (card == null) {
                    if (state.cards.isNotEmpty()) "${state.cards.size} moments · ${state.cards.count { it.saved }} saved"
                    else "Curated trips & celebrations"
                } else "${card.media.size} photos and videos",
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(highlights, { highlights = true }, { Text("Highlights") })
                    FilterChip(!highlights, { highlights = false }, { Text("All photos") })
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { storyVisible = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play story")
                    }
                }
                LazyVerticalGrid(GridCells.Adaptive(112.dp), Modifier.weight(1f).testTag("moment-photos"), state = gridState, contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (shown.note.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { Text(shown.note, Modifier.padding(16.dp)) }
                    items(photos, key = { it.id }) { photo -> MediaThumbnail(photo, Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 8.dp,
                        onClickWithBounds = { bounds -> onOpenMedia(photo, photos, bounds) }) }
                }
            } else {
                val cards = state.cards.filter { filter == "All" || (filter == "Saved") == it.saved }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (option in listOf("All", "Saved", "Suggested")) {
                            FilterChip(filter == option, { filter = option }, { Text(option) })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = model::toggleAnalysis,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(if (state.scanning) Icons.Filled.Pause else Icons.Filled.AutoFixHigh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.scanning) "Pause" else "Find memories")
                    }
                }
                if (cards.isEmpty() || filter == "Suggested") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Curates outings & celebrations · skips screenshots and documents",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                if (state.scanning || state.total > 0) ScanProgressCard(if (state.scanning) "Finding memories" else "Memory scan complete",
                    state.scanning, state.scanned, state.total, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), state.baseline)
                val listState = rememberLazyListState()
                LaunchedEffect(listState) { snapshotFlow { listState.isScrollInProgress }.collect { PhotoAnalysisWork.setDocumentsScrolling(it) } }
                LazyColumn(Modifier.weight(1f).testTag("moment-list"), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (state.flashbacks.isNotEmpty() && filter != "Saved") {
                        item(key = "on-this-day-header") {
                            Column(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                    Text(
                                        "On This Day",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            "${state.flashbacks.size} ${if (state.flashbacks.size == 1) "year" else "years"}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(state.flashbacks, key = { it.year }) { flashback ->
                                        FlashbackCard(
                                            flashback = flashback,
                                            onClick = {
                                                selectedId = flashback.card.id
                                                highlights = true
                                                storyVisible = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!state.loading && cards.isEmpty() && state.flashbacks.isEmpty()) item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                "No moments yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Create a moment from your favorite photos, or let on-device checks auto-group trips and celebrations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { editing = null; editorVisible = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Create moment")
                                }
                                FilledTonalButton(
                                    onClick = model::toggleAnalysis,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Find memories")
                                }
                            }
                        }
                    }
                    itemsIndexed(cards, key = { _, moment -> moment.id }) { index, moment ->
                        if (index == 0) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(fadeInSpec = tween(180), placementSpec = null, fadeOutSpec = null),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 3.dp,
                                shadowElevation = 8.dp,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.35f)
                                        .clickable { selectedId = moment.id; highlights = true; storyVisible = true }
                                ) {
                                    MediaThumbnail(
                                        moment.cover,
                                        Modifier.fillMaxSize(),
                                        cornerRadius = 0.dp
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.45f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.Black.copy(alpha = 0.90f)
                                                    )
                                                )
                                            )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.5f),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    if (moment.saved) Icons.Filled.Bookmark else Icons.Filled.AutoFixHigh,
                                                    contentDescription = null,
                                                    tint = if (moment.saved) MaterialTheme.colorScheme.primary else Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    if (moment.saved) "Saved Moment" else "Featured Story",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        var menuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                                modifier = Modifier.size(32.dp),
                                                onClick = { menuExpanded = true }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(if (moment.saved) "Edit moment" else "Save moment") },
                                                    onClick = { menuExpanded = false; editing = moment; editorVisible = true },
                                                    leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(if (moment.saved) "Remove from moments" else "Hide suggestion") },
                                                    onClick = {
                                                        menuExpanded = false
                                                        if (moment.saved) removal = moment else model.remove(moment)
                                                    },
                                                    leadingIcon = { Icon(if (moment.saved) Icons.Filled.Delete else Icons.Filled.VisibilityOff, null, Modifier.size(18.dp)) }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                moment.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "${moment.media.size} memories · Tap to open",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            val previewPhotos = remember(moment.media) {
                                                moment.highlights.ifEmpty { moment.media }.take(3)
                                            }
                                            if (previewPhotos.size > 1) {
                                                Spacer(Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    for (preview in previewPhotos) {
                                                        MediaThumbnail(
                                                            preview,
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            cornerRadius = 8.dp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            shadowElevation = 6.dp,
                                            modifier = Modifier.size(46.dp),
                                            onClick = { selectedId = moment.id; highlights = true; storyVisible = true }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Play story",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(fadeInSpec = tween(180), placementSpec = null, fadeOutSpec = null),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 2.dp,
                                shadowElevation = 4.dp,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.75f)
                                        .clickable { selectedId = moment.id; highlights = true; storyVisible = true }
                                ) {
                                    MediaThumbnail(
                                        moment.cover,
                                        Modifier.fillMaxSize(),
                                        cornerRadius = 0.dp
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.88f)
                                                    )
                                                )
                                            )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.5f),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    if (moment.saved) Icons.Filled.Bookmark else Icons.Filled.AutoFixHigh,
                                                    null,
                                                    tint = if (moment.saved) MaterialTheme.colorScheme.primary else Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    if (moment.saved) "Saved" else "Suggested",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        var menuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                                modifier = Modifier.size(28.dp),
                                                onClick = { menuExpanded = true }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(if (moment.saved) "Edit moment" else "Save moment") },
                                                    onClick = { menuExpanded = false; editing = moment; editorVisible = true },
                                                    leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(if (moment.saved) "Remove from moments" else "Hide suggestion") },
                                                    onClick = {
                                                        menuExpanded = false
                                                        if (moment.saved) removal = moment else model.remove(moment)
                                                    },
                                                    leadingIcon = { Icon(if (moment.saved) Icons.Filled.Delete else Icons.Filled.VisibilityOff, null, Modifier.size(18.dp)) }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                moment.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "${moment.media.size} photos & videos",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.22f),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f)),
                                            modifier = Modifier.size(36.dp),
                                            onClick = { selectedId = moment.id; highlights = true; storyVisible = true }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Play story",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
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

@Composable
private fun FlashbackCard(
    flashback: FlashbackGroup,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(136.dp)
            .aspectRatio(0.76f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shadowElevation = 4.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            MediaThumbnail(
                flashback.cover,
                Modifier.fillMaxSize(),
                cornerRadius = 0.dp
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ) {
                Text(
                    if (flashback.yearsAgo == 1) "1 YR AGO" else "${flashback.yearsAgo} YRS AGO",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${flashback.year}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        "${flashback.media.size} ${if (flashback.media.size == 1) "photo" else "photos"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play flashback",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
