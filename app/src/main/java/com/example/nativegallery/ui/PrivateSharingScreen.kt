@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.example.nativegallery.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativegallery.data.PrivacyMaskKind
import com.example.nativegallery.data.privatePhotosShareIntent
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail

@Composable
internal fun PrivateSharingDialog(sessionId: String, selectedIds: List<String>, media: List<MediaItem>, onDismiss: () -> Unit,
    model: PrivateSharingViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val photos = remember(media, selectedIds) { val byId = media.associateBy { it.id }; selectedIds.mapNotNull(byId::get) }
    LaunchedEffect(sessionId) { model.openSession(sessionId, selectedIds, media) }
    LaunchedEffect(media) { model.updateAvailable(media) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    fun act(action: () -> Unit) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); action() }
    LaunchedEffect(state.shareUris) {
        if (state.shareUris.isNotEmpty()) {
            if (runCatching { context.startActivity(Intent.createChooser(privatePhotosShareIntent(state.shareUris), "Share private copies")) }.isSuccess) model.shared()
            else model.shareFailed()
        }
    }
    val dismiss = { model.clear(); onDismiss() }
    UsefulFeatureDialog(dismiss) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize().padding(bottom = 16.dp)) {
                GalleryScreenHeader("Private copies", dismiss,
                    subtitle = if (state.total > 1) "Photo ${state.index + 1} of ${state.total} · Tap a thumbnail to switch" else "Review the copy before sharing")
                if (state.photo == null) {
                    FeatureEmptyState("Selected photos unavailable", state.error ?: "Close this preview and select your photos again.")
                    TextButton(dismiss, Modifier.padding(16.dp)) { Text("Close") }
                } else {
                    val enabled = !state.loading && !state.exporting
                    if (photos.size > 1) {
                        val strip = rememberLazyListState()
                        LaunchedEffect(state.index) { strip.animateScrollToItem(state.index) }
                        LazyRow(Modifier.fillMaxWidth().testTag("private-selected-photos"), state = strip,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                                Box {
                                    MediaThumbnail(photo.copy(title = "Selected photo ${index + 1} of ${photos.size}"), Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), cornerRadius = 12.dp,
                                        selected = state.index == index, onClick = { if (enabled && !state.rendering && index != state.index) act { model.move(index - state.index) } })
                                    Surface(Modifier.align(Alignment.BottomEnd), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape = MaterialTheme.shapes.small) {
                                        Text("${index + 1}", Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    state.error?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        if (maxWidth > maxHeight * 1.35f) {
                            Row(Modifier.fillMaxSize()) {
                                PrivatePreview(state, model, enabled, { act(it) }, Modifier.weight(1.15f).fillMaxHeight())
                                PrivateControls(state, model, enabled, { act(it) }, Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
                            }
                        } else Column(Modifier.fillMaxSize()) {
                            PrivatePreview(state, model, enabled, { act(it) }, Modifier.weight(1f).fillMaxWidth())
                            PrivateControls(state, model, enabled, { act(it) }, Modifier.fillMaxWidth().heightIn(max = 176.dp).verticalScroll(rememberScrollState()))
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Copies remove metadata. Originals stay untouched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(20.dp)) {
                                if (state.rendering || state.exporting) CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 2.dp)
                            }
                            Text(if (state.exporting) "Preparing ${state.exportedCount}/${state.total}" else "${state.index + 1} / ${state.total}", Modifier.widthIn(min = 32.dp), style = MaterialTheme.typography.labelMedium)
                            if (state.index > 0) TextButton({ act { model.move(-1) } }, enabled = enabled && !state.rendering) { Text("Previous") }
                            Button({ act { if (state.index < state.total - 1) model.move(1) else model.share() } }, Modifier.weight(1f),
                                enabled = state.preview != null && enabled && !state.rendering && state.error == null) {
                                Text(when { state.exporting -> "Preparing…"; state.index < state.total - 1 -> "Next"; state.total > 1 -> "Share ${state.total} copies"; else -> "Share copy" })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateControls(state: PrivateSharingUiState, model: PrivateSharingViewModel, enabled: Boolean,
    act: (() -> Unit) -> Unit, modifier: Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((kind, label) in listOf(PrivacyMaskKind.Face to "Faces", PrivacyMaskKind.SensitiveText to "Sensitive text", PrivacyMaskKind.Text to "All text")) {
                val ids = state.regions.filter { it.kind == kind || (kind == PrivacyMaskKind.Text && it.kind == PrivacyMaskKind.SensitiveText) }.map { it.id }
                FilterChip(ids.isNotEmpty() && state.selected.containsAll(ids), { act { model.toggleGroup(kind) } }, { Text("$label (${ids.size})") },
                    enabled = enabled && ids.isNotEmpty(), border = null)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!state.solid, { act { model.setSolid(false) } }, { Text("Pixelate faces") }, enabled = enabled, border = null)
            FilterChip(state.solid, { act { model.setSolid(true) } }, { Text("Solid covers") }, enabled = enabled, border = null)
            TextButton({ act(model::undo) }, enabled = enabled && state.regions.any { it.kind == PrivacyMaskKind.Manual }) { Text("Undo area") }
        }
        Text("Tap an area to toggle it. Drag on the photo to cover something else.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (state.error != null) TextButton(model::retry) { Text("Try again") }
    }
}

@Composable
private fun PrivatePreview(state: PrivateSharingUiState, model: PrivateSharingViewModel, enabled: Boolean,
    act: (() -> Unit) -> Unit, modifier: Modifier) {
    Box(modifier.padding(12.dp).testTag("private-preview-area"), contentAlignment = Alignment.Center) {
        val bitmap = state.preview
        if (bitmap == null) { if (state.loading) CircularProgressIndicator(Modifier.size(28.dp)) }
        else {
            Image(bitmap.asImageBitmap(), "Private share preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            var start by remember(state.photo?.id) { mutableStateOf<Offset?>(null) }
            var end by remember(state.photo?.id) { mutableStateOf<Offset?>(null) }
            Canvas(Modifier.fillMaxSize().pointerInput(bitmap, state.regions, state.selected, enabled) {
                if (enabled) detectTapGestures { point ->
                    privatePreviewPoint(point.x, point.y, size.width.toFloat(), size.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat())?.let { normalized ->
                        val region = state.regions.lastOrNull { it.contains(normalized.first, normalized.second) }
                        act { if (region != null) model.toggle(region.id) else model.addRegion(normalized.first - 0.08f, normalized.second - 0.04f, normalized.first + 0.08f, normalized.second + 0.04f) }
                    }
                }
            }.pointerInput(bitmap, enabled) {
                if (enabled) detectDragGestures(onDragStart = { start = it; end = it }, onDragCancel = { start = null; end = null }, onDragEnd = {
                    val first = start?.let { privatePreviewPoint(it.x, it.y, size.width.toFloat(), size.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat()) }
                    val last = end?.let { privatePreviewPoint(it.x, it.y, size.width.toFloat(), size.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat(), clamp = true) }
                    if (first != null && last != null) act { model.addRegion(first.first, first.second, last.first, last.second) }
                    start = null; end = null
                }) { change, _ -> change.consume(); end = change.position }
            }) {
                val a = start; val b = end
                if (a != null && b != null) drawRect(Color.White, Offset(minOf(a.x, b.x), minOf(a.y, b.y)),
                    androidx.compose.ui.geometry.Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** Fit coordinates must never turn a tap in the letterbox into a mask on the photo. */
internal fun privatePreviewPoint(x: Float, y: Float, width: Float, height: Float, imageWidth: Float, imageHeight: Float,
    clamp: Boolean = false): Pair<Float, Float>? {
    if (minOf(width, height, imageWidth, imageHeight) <= 0f) return null
    val scale = minOf(width / imageWidth, height / imageHeight)
    val left = (width - imageWidth * scale) / 2f; val top = (height - imageHeight * scale) / 2f
    val nx = (x - left) / (imageWidth * scale); val ny = (y - top) / (imageHeight * scale)
    if (!clamp && (nx !in 0f..1f || ny !in 0f..1f)) return null
    return nx.coerceIn(0f, 1f) to ny.coerceIn(0f, 1f)
}
