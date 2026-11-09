@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SearchPill

@Composable
internal fun UsefulFeatureDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("useful-feature-dialog"), color = MaterialTheme.colorScheme.background) { content() }
    }
}

@Composable
internal fun FeatureEmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun FeaturePhotoPicker(media: List<MediaItem>, selected: Set<String>, onToggle: (MediaItem) -> Unit,
    modifier: Modifier = Modifier, onLongClick: ((MediaItem) -> Unit)? = null) {
    LazyVerticalGrid(GridCells.Adaptive(96.dp), modifier, contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        items(media, key = { it.id }) { item ->
            MediaThumbnail(item, Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 8.dp,
                selected = item.id in selected, onClick = { onToggle(item) }, onLongClick = onLongClick?.let { callback -> { callback(item) } })
        }
    }
}

@Composable
internal fun MomentEditorDialog(card: MomentCard?, media: List<MediaItem>, onDismiss: () -> Unit,
    onSave: (String, List<String>, String?, String) -> Unit) {
    var name by remember(card?.id) { mutableStateOf(card?.name.orEmpty()) }
    var note by remember(card?.id) { mutableStateOf(card?.note.orEmpty()) }
    var selected by remember(card?.id) { mutableStateOf(card?.media?.mapTo(hashSetOf()) { it.id }.orEmpty()) }
    var cover by remember(card?.id) { mutableStateOf(card?.cover?.id) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(media, query) { media.filter { query.isBlank() || it.title.contains(query, true) || it.dateLabel.contains(query, true) } }
    UsefulFeatureDialog(onDismiss) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            GalleryScreenHeader(if (card == null) "New moment" else "Edit moment", onDismiss,
                trailingContent = { TextButton(enabled = name.isNotBlank() && selected.isNotEmpty(), onClick = {
                    onSave(name, selected.toList(), cover?.takeIf(selected::contains), note); onDismiss()
                }) { Text("Save") } })
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Moment name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            OutlinedTextField(note, { note = it.take(1000) }, label = { Text("A note to remember") }, maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            SearchPill(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), "Find photos by name or date", query, onQueryChange = { query = it })
            Text("${selected.size} selected · Long-press a photo to choose the cover", Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            cover?.let { id -> media.firstOrNull { it.id == id }?.let { Text("Cover: ${it.title}", Modifier.padding(horizontal = 18.dp),
                style = MaterialTheme.typography.labelSmall, maxLines = 1) } }
            FeaturePhotoPicker(filtered, selected, { item -> selected = if (item.id in selected) selected - item.id else selected + item.id },
                Modifier.weight(1f), onLongClick = { item -> selected = selected + item.id; cover = item.id })
        }
    }
}
