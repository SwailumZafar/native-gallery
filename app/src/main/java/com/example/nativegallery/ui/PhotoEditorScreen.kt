package com.example.nativegallery.ui

import android.net.Uri
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.NormalizedEditPoint
import com.example.nativegallery.data.NormalizedCropRect
import com.example.nativegallery.data.PhotoEditFilter
import com.example.nativegallery.data.PhotoEditRecipe
import com.example.nativegallery.data.PhotoEditStroke
import com.example.nativegallery.data.PhotoEditorRepository
import com.example.nativegallery.data.normalizedCropRectForAspect
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.ImageLoadQuality
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlin.math.hypot

private enum class CropPreset(val label: String, val aspectRatio: Float?) {
    Free("Free", null),
    Original("Original", null),
    Square("1:1", 1f),
    FourThree("4:3", 4f / 3f),
    SixteenNine("16:9", 16f / 9f)
}

private enum class CropDragHandle {
    Move,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    repository: PhotoEditorRepository,
    useCompactLayout: Boolean = false,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    var recipe by remember(mediaItem.id) { mutableStateOf(PhotoEditRecipe()) }
    var history by remember(mediaItem.id) { mutableStateOf<List<PhotoEditRecipe>>(emptyList()) }
    var markupEnabled by remember(mediaItem.id) { mutableStateOf(false) }
    var cropMode by remember(mediaItem.id) { mutableStateOf(false) }
    var cropPreset by remember(mediaItem.id) { mutableStateOf(CropPreset.Original) }
    var draftCropRect by remember(mediaItem.id) { mutableStateOf(NormalizedCropRect.Full) }
    var currentStroke by remember(mediaItem.id) { mutableStateOf<List<NormalizedEditPoint>>(emptyList()) }
    var isSaving by remember(mediaItem.id) { mutableStateOf(false) }
    var saveError by remember(mediaItem.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val baseSourceAspect = (mediaItem.width?.takeIf { it > 0 } ?: 1).toFloat() /
        (mediaItem.height?.takeIf { it > 0 } ?: 1).toFloat()
    val sourceAspect = if (((recipe.rotationDegrees / 90) and 1) == 1) {
        1f / baseSourceAspect.coerceAtLeast(0.001f)
    } else {
        baseSourceAspect.coerceAtLeast(0.001f)
    }
    val visibleCropRect = if (cropMode) draftCropRect else recipe.cropRect

    fun commit(next: PhotoEditRecipe) {
        if (next == recipe) return
        history = history + recipe
        recipe = next
        saveError = null
    }

    fun leaveCropMode() {
        draftCropRect = recipe.cropRect ?: NormalizedCropRect.Full
        cropMode = false
    }

    fun applyCrop() {
        val safeCrop = draftCropRect.sanitized()
        commit(recipe.copy(cropRect = safeCrop.takeUnless { it.isFullFrame() }))
        cropMode = false
    }

    fun saveCopy() {
        if (isSaving) return
        isSaving = true
        saveError = null
        scope.launch {
            val recipeToSave = if (cropMode) {
                val safeCrop = draftCropRect.sanitized()
                recipe.copy(cropRect = safeCrop.takeUnless { it.isFullFrame() })
            } else {
                recipe
            }
            val savedUri = repository.saveEditedCopy(mediaItem, recipeToSave)
            isSaving = false
            if (savedUri != null) {
                onSaved(savedUri)
            } else {
                saveError = "Could not save the edited copy."
            }
        }
    }

    PredictiveBackHandler(enabled = cropMode) { progressFlow ->
        progressFlow.collect()
        leaveCropMode()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    top = if (useCompactLayout) 10.dp else 42.dp,
                    end = 12.dp,
                    bottom = if (useCompactLayout) 4.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (cropMode) leaveCropMode() else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close editor")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Edit photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(mediaItem.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            TextButton(
                enabled = history.isNotEmpty() && !isSaving && !cropMode,
                onClick = {
                    val previous = history.lastOrNull() ?: return@TextButton
                    recipe = previous
                    history = history.dropLast(1)
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                if (!useCompactLayout) Text("Undo")
            }
            if (useCompactLayout) {
                TextButton(enabled = !isSaving, onClick = ::saveCopy) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = if (useCompactLayout) 8.dp else 12.dp)
                .clip(RoundedCornerShape(if (useCompactLayout) 16.dp else 24.dp))
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
                contentScale = ContentScale.Fit,
                thumbnailSize = 2048,
                loadQuality = ImageLoadQuality.HighQuality,
                backgroundColor = Color.Black,
                colorFilter = editorColorFilter(recipe.filter)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(markupEnabled, cropMode, recipe.strokes, recipe.cropRect, sourceAspect) {
                        if (!markupEnabled || cropMode) return@pointerInput
                        fun normalized(offset: Offset): NormalizedEditPoint {
                            val frame = editorFrameRect(
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat(),
                                sourceAspect = sourceAspect,
                                cropRect = recipe.cropRect
                            )
                            return NormalizedEditPoint(
                                x = ((offset.x - frame.left) / frame.width.coerceAtLeast(1f)).coerceIn(0f, 1f),
                                y = ((offset.y - frame.top) / frame.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
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
                val frame = editorFrameRect(
                    containerWidth = size.width,
                    containerHeight = size.height,
                    sourceAspect = sourceAspect,
                    cropRect = recipe.cropRect
                )
                (recipe.strokes.map { it.points } + listOf(currentStroke))
                    .filter { it.size > 1 }
                    .forEach { points ->
                        val path = Path()
                        path.moveTo(
                            frame.left + points.first().x * frame.width,
                            frame.top + points.first().y * frame.height
                        )
                        points.drop(1).forEach { point ->
                            path.lineTo(
                                frame.left + point.x * frame.width,
                                frame.top + point.y * frame.height
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color.Red,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = minOf(frame.width, frame.height) * 0.012f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
            }

            if (visibleCropRect != null || cropMode) {
                CropOverlay(
                    sourceAspect = sourceAspect,
                    cropRect = visibleCropRect ?: NormalizedCropRect.Full,
                    interactive = cropMode,
                    onCropRectChange = {
                        draftCropRect = it
                        cropPreset = CropPreset.Free
                    }
                )
            }

            if (markupEnabled && !cropMode) {
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

        if (cropMode) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = cropPreset == preset,
                            onClick = {
                                cropPreset = preset
                                draftCropRect = when (preset) {
                                    CropPreset.Free -> draftCropRect
                                    CropPreset.Original -> NormalizedCropRect.Full
                                    else -> normalizedCropRectForAspect(
                                        sourceAspect = sourceAspect,
                                        targetAspect = requireNotNull(preset.aspectRatio)
                                    )
                                }
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = ::leaveCropMode) { Text("Cancel") }
                    TextButton(onClick = ::applyCrop) { Text("Apply") }
                }
            }
        } else {
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EditorToolButton(
                label = "Rotate",
                icon = Icons.AutoMirrored.Filled.RotateRight,
                selected = false,
                onClick = { commit(recipe.copy(rotationDegrees = (recipe.rotationDegrees + 90) % 360)) }
            )
            EditorToolButton(
                label = "Crop",
                icon = Icons.Filled.CropSquare,
                selected = cropMode || recipe.cropRect != null,
                onClick = {
                    if (cropMode) {
                        leaveCropMode()
                    } else {
                        markupEnabled = false
                        draftCropRect = recipe.cropRect ?: NormalizedCropRect.Full
                        cropPreset = if (recipe.cropRect == null) CropPreset.Original else CropPreset.Free
                        cropMode = true
                    }
                }
            )
            EditorToolButton(
                label = "Markup",
                icon = Icons.Filled.Draw,
                selected = markupEnabled,
                onClick = {
                    if (cropMode) leaveCropMode()
                    markupEnabled = !markupEnabled
                }
            )
            EditorToolButton(
                label = "Reset",
                icon = Icons.Filled.AutoFixHigh,
                selected = false,
                onClick = {
                    cropMode = false
                    markupEnabled = false
                    draftCropRect = NormalizedCropRect.Full
                    cropPreset = CropPreset.Original
                    commit(PhotoEditRecipe())
                }
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

        if (!useCompactLayout) {
            Button(
                enabled = !isSaving,
                onClick = ::saveCopy,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save copy")
            }
        }
    }
}

@Composable
private fun CropOverlay(
    sourceAspect: Float,
    cropRect: NormalizedCropRect,
    interactive: Boolean,
    onCropRectChange: (NormalizedCropRect) -> Unit
) {
    val latestCropRect by rememberUpdatedState(cropRect)
    val latestOnCropRectChange by rememberUpdatedState(onCropRectChange)
    val handleRadiusPx = with(LocalDensity.current) { 30.dp.toPx() }
    val borderWidthPx = with(LocalDensity.current) { 2.dp.toPx() }
    val handleLengthPx = with(LocalDensity.current) { 20.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(interactive, sourceAspect) {
                if (!interactive) return@pointerInput
                var dragHandle: CropDragHandle? = null
                detectDragGestures(
                    onDragStart = { point ->
                        val imageFrame = fittedImageRect(
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            sourceAspect = sourceAspect
                        )
                        val cropFrame = cropRectToFrame(latestCropRect, imageFrame)
                        dragHandle = findCropHandle(point, cropFrame, handleRadiusPx)
                    },
                    onDrag = { change, dragAmount ->
                        val handle = dragHandle ?: return@detectDragGestures
                        val imageFrame = fittedImageRect(
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            sourceAspect = sourceAspect
                        )
                        val next = adjustCropRect(
                            cropRect = latestCropRect,
                            handle = handle,
                            deltaX = dragAmount.x / imageFrame.width.coerceAtLeast(1f),
                            deltaY = dragAmount.y / imageFrame.height.coerceAtLeast(1f)
                        )
                        latestOnCropRectChange(next)
                        change.consume()
                    },
                    onDragEnd = { dragHandle = null },
                    onDragCancel = { dragHandle = null }
                )
            }
    ) {
        val imageFrame = fittedImageRect(size.width, size.height, sourceAspect)
        val cropFrame = cropRectToFrame(cropRect, imageFrame)
        val scrim = Color.Black.copy(alpha = if (interactive) 0.58f else 0.72f)

        drawRect(scrim, topLeft = imageFrame.topLeft, size = androidx.compose.ui.geometry.Size(imageFrame.width, cropFrame.top - imageFrame.top))
        drawRect(scrim, topLeft = Offset(imageFrame.left, cropFrame.bottom), size = androidx.compose.ui.geometry.Size(imageFrame.width, imageFrame.bottom - cropFrame.bottom))
        drawRect(scrim, topLeft = Offset(imageFrame.left, cropFrame.top), size = androidx.compose.ui.geometry.Size(cropFrame.left - imageFrame.left, cropFrame.height))
        drawRect(scrim, topLeft = Offset(cropFrame.right, cropFrame.top), size = androidx.compose.ui.geometry.Size(imageFrame.right - cropFrame.right, cropFrame.height))
        drawRect(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = cropFrame.topLeft,
            size = androidx.compose.ui.geometry.Size(cropFrame.width, cropFrame.height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidthPx)
        )

        if (interactive) {
            for (division in 1..2) {
                val fraction = division / 3f
                drawLine(
                    color = Color.White.copy(alpha = 0.42f),
                    start = Offset(cropFrame.left + cropFrame.width * fraction, cropFrame.top),
                    end = Offset(cropFrame.left + cropFrame.width * fraction, cropFrame.bottom),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.42f),
                    start = Offset(cropFrame.left, cropFrame.top + cropFrame.height * fraction),
                    end = Offset(cropFrame.right, cropFrame.top + cropFrame.height * fraction),
                    strokeWidth = 1f
                )
            }
            drawCropCorners(cropFrame, handleLengthPx, borderWidthPx * 1.8f)
        }
    }
}

private fun fittedImageRect(
    containerWidth: Float,
    containerHeight: Float,
    sourceAspect: Float
): Rect {
    val safeWidth = containerWidth.coerceAtLeast(1f)
    val safeHeight = containerHeight.coerceAtLeast(1f)
    val safeAspect = sourceAspect.coerceAtLeast(0.001f)
    val containerAspect = safeWidth / safeHeight
    return if (containerAspect > safeAspect) {
        val width = safeHeight * safeAspect
        val left = (safeWidth - width) / 2f
        Rect(left, 0f, left + width, safeHeight)
    } else {
        val height = safeWidth / safeAspect
        val top = (safeHeight - height) / 2f
        Rect(0f, top, safeWidth, top + height)
    }
}

private fun cropRectToFrame(cropRect: NormalizedCropRect, imageFrame: Rect): Rect {
    val safe = cropRect.sanitized()
    return Rect(
        left = imageFrame.left + safe.left * imageFrame.width,
        top = imageFrame.top + safe.top * imageFrame.height,
        right = imageFrame.left + safe.right * imageFrame.width,
        bottom = imageFrame.top + safe.bottom * imageFrame.height
    )
}

private fun editorFrameRect(
    containerWidth: Float,
    containerHeight: Float,
    sourceAspect: Float,
    cropRect: NormalizedCropRect?
): Rect {
    val imageFrame = fittedImageRect(containerWidth, containerHeight, sourceAspect)
    return cropRectToFrame(cropRect ?: NormalizedCropRect.Full, imageFrame)
}

private fun findCropHandle(point: Offset, cropFrame: Rect, radius: Float): CropDragHandle? {
    val corners = listOf(
        CropDragHandle.TopLeft to cropFrame.topLeft,
        CropDragHandle.TopRight to Offset(cropFrame.right, cropFrame.top),
        CropDragHandle.BottomLeft to Offset(cropFrame.left, cropFrame.bottom),
        CropDragHandle.BottomRight to cropFrame.bottomRight
    )
    val nearest = corners.minByOrNull { (_, corner) ->
        hypot(point.x - corner.x, point.y - corner.y)
    }
    if (nearest != null && hypot(point.x - nearest.second.x, point.y - nearest.second.y) <= radius) {
        return nearest.first
    }
    return CropDragHandle.Move.takeIf { cropFrame.contains(point) }
}

private fun adjustCropRect(
    cropRect: NormalizedCropRect,
    handle: CropDragHandle,
    deltaX: Float,
    deltaY: Float
): NormalizedCropRect {
    val safe = cropRect.sanitized()
    val minimumSize = 0.08f
    return when (handle) {
        CropDragHandle.Move -> {
            val width = safe.right - safe.left
            val height = safe.bottom - safe.top
            val left = (safe.left + deltaX).coerceIn(0f, 1f - width)
            val top = (safe.top + deltaY).coerceIn(0f, 1f - height)
            NormalizedCropRect(left, top, left + width, top + height)
        }
        CropDragHandle.TopLeft -> safe.copy(
            left = (safe.left + deltaX).coerceIn(0f, safe.right - minimumSize),
            top = (safe.top + deltaY).coerceIn(0f, safe.bottom - minimumSize)
        )
        CropDragHandle.TopRight -> safe.copy(
            right = (safe.right + deltaX).coerceIn(safe.left + minimumSize, 1f),
            top = (safe.top + deltaY).coerceIn(0f, safe.bottom - minimumSize)
        )
        CropDragHandle.BottomLeft -> safe.copy(
            left = (safe.left + deltaX).coerceIn(0f, safe.right - minimumSize),
            bottom = (safe.bottom + deltaY).coerceIn(safe.top + minimumSize, 1f)
        )
        CropDragHandle.BottomRight -> safe.copy(
            right = (safe.right + deltaX).coerceIn(safe.left + minimumSize, 1f),
            bottom = (safe.bottom + deltaY).coerceIn(safe.top + minimumSize, 1f)
        )
    }.sanitized(minimumSize)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropCorners(
    frame: Rect,
    length: Float,
    strokeWidth: Float
) {
    val color = Color.White
    drawLine(color, frame.topLeft, Offset(frame.left + length, frame.top), strokeWidth)
    drawLine(color, frame.topLeft, Offset(frame.left, frame.top + length), strokeWidth)
    drawLine(color, Offset(frame.right, frame.top), Offset(frame.right - length, frame.top), strokeWidth)
    drawLine(color, Offset(frame.right, frame.top), Offset(frame.right, frame.top + length), strokeWidth)
    drawLine(color, Offset(frame.left, frame.bottom), Offset(frame.left + length, frame.bottom), strokeWidth)
    drawLine(color, Offset(frame.left, frame.bottom), Offset(frame.left, frame.bottom - length), strokeWidth)
    drawLine(color, frame.bottomRight, Offset(frame.right - length, frame.bottom), strokeWidth)
    drawLine(color, frame.bottomRight, Offset(frame.right, frame.bottom - length), strokeWidth)
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
