@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.DocumentPhotoCategory
import com.example.nativegallery.data.DocumentPhotoMatch
import com.example.nativegallery.data.PhotoAnalysisWork
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail
import com.example.nativegallery.ui.components.SearchPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class DocumentSection(val category: DocumentPhotoCategory, val matches: List<DocumentPhotoMatch>)
internal fun documentSections(matches: List<DocumentPhotoMatch>): List<DocumentSection> =
    DocumentPhotoCategory.entries.mapNotNull { category ->
        matches.filter { it.category == category }.takeIf { it.isNotEmpty() }?.let { DocumentSection(category, it) }
    }
internal fun documentSectionAnchors(sections: List<DocumentSection>): Map<DocumentPhotoCategory, Int> {
    var index = 0
    return sections.associate { section -> (section.category to index).also { index += 1 + section.matches.size } }
}

@Composable
fun DocumentPhotosScreen(matches: List<DocumentPhotoMatch>, scanning: Boolean, scannedCount: Int, totalCount: Int,
    errorMessage: String?, onBack: () -> Unit, onRescan: () -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit, gridColumns: Int, baselineCount: Int = 0,
    maxContentWidth: Dp = Dp.Unspecified, contentPadding: PaddingValues = PaddingValues()) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = remember(matches, query) {
        matches.filter { query.isBlank() || it.recognizedText.contains(query, true) || it.mediaItem.title.contains(query, true) || it.category.label.contains(query, true) }
    }
    val sections = remember(visible) { documentSections(visible) }
    val anchors = remember(sections) { documentSectionAnchors(sections) }
    LaunchedEffect(gridState) { snapshotFlow { gridState.isScrollInProgress }.collect { PhotoAnalysisWork.setDocumentsScrolling(it) } }
    DisposableEffect(Unit) { onDispose { PhotoAnalysisWork.setDocumentsScrolling(false) } }
    val progress by animateFloatAsState(if (totalCount == 0) 0f else scannedCount.toFloat() / totalCount,
        tween(180, easing = LinearEasing), label = "document-scan-progress")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = if (maxContentWidth == Dp.Unspecified) 840.dp else maxContentWidth)
            .fillMaxSize().statusBarsPadding().padding(bottom = contentPadding.calculateBottomPadding()).navigationBarsPadding()) {
            GalleryScreenHeader("Document photos", onBack, subtitle = "Paper documents, receipts, menus and notes",
                trailingContent = { IconButton(onRescan, enabled = !scanning) {
                    Icon(Icons.Filled.Refresh, "Scan document photos again")
                } })
            Text("App screenshots have their own shelf in Useful screenshots.", Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SearchPill(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), "Search your document photos", query, onQueryChange = { query = it })
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selectedCategory == null, {
                    selectedCategory = null; scrollJob?.cancel()
                    scrollJob = scope.launch { gridState.animateScrollToItem(0) }
                }, { Text("All (${visible.size})") })
                for (category in DocumentPhotoCategory.entries) {
                    val count = sections.firstOrNull { it.category == category }?.matches?.size ?: 0
                    FilterChip(selectedCategory == category.name, {
                        selectedCategory = category.name; scrollJob?.cancel()
                        val target = anchors[category] ?: 0
                        scrollJob = scope.launch { gridState.animateScrollToItem(target) }
                    }, { Text("${category.label} ($count)") }, enabled = count > 0)
                }
            }
            if (scanning || scannedCount > 0) ScanProgressCard(
                if (scanning) "Scanning photos" else "Document scan complete", scanning, scannedCount, totalCount,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), baselineCount)
            errorMessage?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            LazyVerticalGrid(GridCells.Fixed(gridColumns.coerceAtLeast(2)), Modifier.weight(1f).testTag("document-sections"), state = gridState,
                contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (sections.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    FeatureEmptyState(if (scanning) "Finding your documents…" else "No document photos here yet",
                        if (query.isNotBlank()) "Try another word. Categories stay together on this page."
                        else "Photographed bills, receipts, menus, forms and notes appear in their own sections as scanning finds them.")
                }
                for (section in sections) {
                    item(key = "section-${section.category.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Text("${section.category.label} · ${section.matches.size}", Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp)
                            .testTag("document-section-${section.category.name}"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    items(section.matches, key = { it.mediaItem.id }, contentType = { "document-photo" }) { match ->
                        MediaThumbnail(match.mediaItem, Modifier.fillMaxWidth().aspectRatio(1f)
                            .animateItem(fadeInSpec = tween(160), placementSpec = null, fadeOutSpec = null), cornerRadius = 8.dp,
                            onClickWithBounds = { onOpenMedia(match.mediaItem, section.matches.map { it.mediaItem }, it) })
                    }
                }
            }
        }
    }
}
