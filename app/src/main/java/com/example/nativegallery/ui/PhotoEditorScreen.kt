package com.example.nativegallery.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.NormalizedEditPoint
import com.example.nativegallery.data.PhotoEditFilter
import com.example.nativegallery.data.PhotoEditRecipe
import com.example.nativegallery.data.PhotoEditStroke
import com.example.nativegallery.data.PhotoEditorRepository
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.ImageLoadQuality
import kotlinx.coroutines.launch

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    repository: PhotoEditorRepository,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    var recipe by remember(mediaItem.id) { mutableStateOf(PhotoEditRecipe()) }
    var history by remember(mediaItem.id) { mutableStateOf<List<PhotoEditRecipe>>(emptyList()) }
    var markupEnabled by remember(mediaItem.id) { mutableStateOf(false) }
    var currentStroke by remember(mediaItem.id) { mutableStateOf<List<NormalizedEditPoint>>(emptyList()) }
    var isSaving by remember(mediaItem.id) { mutableStateOf(false) }
    var saveError by remember(mediaItem.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun commit(next: PhotoEditRecipe) {
        if (next == recipe) return
        history = history + recipe
        recipe = next
        saveError = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 42.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close editor")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Edit photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(mediaItem.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            TextButton(
                enabled = history.isNotEmpty() && !isSaving,
                onClick = {
                    val previous = history.lastOrNull() ?: return@TextButton
                    recipe = previous
                    history = history.dropLast(1)
                }
            ) {
                Icon(Icons.Filled.Undo, contentDescription = null)
                Text("Undo")
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            GalleryImage(
                imageRes = mediaItem.imageRes,
                imageUri = mediaItem.contentUri,
                contentDescription = mediaItem.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = recipe.rotationDegrees.toFloat() },
                cornerRadius = 0.dp,
                contentScale = if (recipe.cropSquare) ContentScale.Crop else ContentScale.Fit,
                thumbnailSize = 2048,
                loadQuality = ImageLoadQuality.HighQuality,
                backgroundColor = Color.Black,
                colorFilter = editorColorFilter(recipe.filter)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(markupEnabled, recipe.strokes) {
                        if (!markupEnabled) return@pointerInput
                        fun normalized(offset: Offset): NormalizedEditPoint {
                            return NormalizedEditPoint(
                                x = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                y = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            )
                        }
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(normalized(offset))
                            },
                            onDrag = { change, _ ->
                                currentStroke = currentStroke + normalized(change.position)
                                change.consume()
                            },
                            onDragEnd = {
                                if (currentStroke.size > 1) {
                                    commit(recipe.copy(strokes = recipe.strokes + PhotoEditStroke(currentStroke)))
                                }
                                currentStroke = emptyList()
                            },
                            onDragCancel = { currentStroke = emptyList() }
                        )
                    }
            ) {
                (recipe.strokes.map { it.points } + listOf(currentStroke))
                    .filter { it.size > 1 }
                    .forEach { points ->
                        val path = Path()
                        path.moveTo(points.first().x * size.width, points.first().y * size.height)
                        points.drop(1).forEach { point ->
                            path.lineTo(point.x * size.width, point.y * size.height)
                        }
                        drawPath(
                            path = path,
                            color = Color.Red,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = size.minDimension * 0.012f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
            }

            if (markupEnabled) {
                Text(
                    text = "Draw on the photo",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoEditFilter.entries.forEach { filter ->
                FilterChip(
                    selected = recipe.filter == filter,
                    onClick = { commit(recipe.copy(filter = filter)) },
                    label = { Text(filter.name) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EditorToolButton(
                label = "Rotate",
                icon = Icons.Filled.RotateRight,
                selected = false,
                onClick = { commit(recipe.copy(rotationDegrees = (recipe.rotationDegrees + 90) % 360)) }
            )
            EditorToolButton(
                label = "Crop",
                icon = Icons.Filled.CropSquare,
                selected = recipe.cropSquare,
                onClick = { commit(recipe.copy(cropSquare = !recipe.cropSquare)) }
            )
            EditorToolButton(
                label = "Markup",
                icon = Icons.Filled.Draw,
                selected = markupEnabled,
                onClick = { markupEnabled = !markupEnabled }
            )
            EditorToolButton(
                label = "Reset",
                icon = Icons.Filled.AutoFixHigh,
                selected = false,
                onClick = { commit(PhotoEditRecipe()) }
            )
        }

        saveError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            enabled = !isSaving,
            onClick = {
                isSaving = true
                saveError = null
                scope.launch {
                    val savedUri = repository.saveEditedCopy(mediaItem, recipe)
                    isSaving = false
                    if (savedUri != null) {
                        onSaved(savedUri)
                    } else {
                        saveError = "Could not save the edited copy."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(if (isSaving) "Saving..." else "Save copy")
        }
    }
}

@Composable
private fun EditorToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(label)
        }
    }
}

private fun editorColorFilter(filter: PhotoEditFilter): ColorFilter? {
    val matrix = when (filter) {
        PhotoEditFilter.Original -> return null
        PhotoEditFilter.Mono -> ColorMatrix().apply { setToSaturation(0f) }
        PhotoEditFilter.Warm -> ColorMatrix(
            floatArrayOf(
                1.08f, 0f, 0f, 0f, 0f,
                0f, 1.02f, 0f, 0f, 0f,
                0f, 0f, 0.90f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        PhotoEditFilter.Cool -> ColorMatrix(
            floatArrayOf(
                0.92f, 0f, 0f, 0f, 0f,
                0f, 1.00f, 0f, 0f, 0f,
                0f, 0f, 1.10f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    return ColorFilter.colorMatrix(matrix)
}
