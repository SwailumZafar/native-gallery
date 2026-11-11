package com.example.nativegallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CollageTemplate(val label: String) {
    Grid("Grid"),
    Bento("Bento"),
    Filmstrip("Filmstrip"),
    Polaroid("Polaroid")
}

private enum class CollageBackdrop(val label: String, val color: Color, val isDark: Boolean) {
    Obsidian("Obsidian", Color(0xFF111217), true),
    White("Studio", Color(0xFFFFFFFF), false),
    Cream("Cream", Color(0xFFF5EFE6), false),
    RacingRed("Crimson", Color(0xFFEF233C), true),
    IceGrey("Ice Grey", Color(0xFFDFE5EB), false)
}

@Composable
fun CollageMakerScreen(
    availableMedia: List<MediaItem>,
    initialSelection: List<MediaItem> = emptyList(),
    onDismiss: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectablePhotos = remember(availableMedia) {
        availableMedia.filter { !it.isVideo }
    }

    var selectedPhotos by remember {
        mutableStateOf(
            if (initialSelection.isNotEmpty()) {
                initialSelection.filter { !it.isVideo }.take(6)
            } else emptyList()
        )
    }

    var template by rememberSaveable { mutableStateOf(CollageTemplate.Bento) }
    var backdrop by rememberSaveable { mutableStateOf(CollageBackdrop.Obsidian) }
    var spacingDp by rememberSaveable { mutableFloatStateOf(10f) }
    var radiusDp by rememberSaveable { mutableFloatStateOf(14f) }
    var swapFirstIndex by remember { mutableIntStateOf(-1) }
    var saving by remember { mutableStateOf(false) }

    UsefulFeatureDialog(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("collage-maker-screen")
        ) {
            GalleryScreenHeader(
                title = "Collage Studio",
                onBack = onDismiss,
                subtitle = if (selectedPhotos.size < 2) "Select 2 to 6 photos to craft your layout" else "${selectedPhotos.size} photos · Tap two frames to swap",
                trailingContent = {
                    if (selectedPhotos.size >= 2) {
                        TextButton(onClick = { selectedPhotos = emptyList(); swapFirstIndex = -1 }) {
                            Text("Re-pick")
                        }
                    }
                }
            )

            if (selectedPhotos.size < 2) {
                val pickedIds = remember(selectedPhotos) { selectedPhotos.mapTo(hashSetOf()) { it.id } }
                Column(Modifier.fillMaxSize()) {
                    FeaturePhotoPicker(
                        media = selectablePhotos,
                        selected = pickedIds,
                        onToggle = { item ->
                            selectedPhotos = if (item.id in pickedIds) {
                                selectedPhotos.filterNot { it.id == item.id }
                            } else if (selectedPhotos.size < 6) {
                                selectedPhotos + item
                            } else {
                                selectedPhotos
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${selectedPhotos.size} of 6 selected (min 2)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Button(
                                onClick = { },
                                enabled = false,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Select at least 2 photos")
                            }
                        }
                    }
                }
            } else {
                // Live Collage Canvas Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val aspect = when (template) {
                        CollageTemplate.Filmstrip -> 0.56f
                        CollageTemplate.Polaroid -> 0.85f
                        else -> 0.85f
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight(0.95f)
                            .aspectRatio(aspect, matchHeightConstraintsFirst = true),
                        shape = RoundedCornerShape(16.dp),
                        color = backdrop.color,
                        shadowElevation = 10.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        CollagePreviewCanvas(
                            photos = selectedPhotos,
                            template = template,
                            backdrop = backdrop,
                            spacingDp = spacingDp,
                            radiusDp = radiusDp,
                            selectedSwapIndex = swapFirstIndex,
                            onTileTap = { idx ->
                                if (swapFirstIndex == -1) {
                                    swapFirstIndex = idx
                                } else if (swapFirstIndex == idx) {
                                    swapFirstIndex = -1
                                } else {
                                    val mutable = selectedPhotos.toMutableList()
                                    val tmp = mutable[swapFirstIndex]
                                    mutable[swapFirstIndex] = mutable[idx]
                                    mutable[idx] = tmp
                                    selectedPhotos = mutable
                                    swapFirstIndex = -1
                                }
                            }
                        )
                    }
                }

                // Controls Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Layout choices
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (tpl in CollageTemplate.entries) {
                                FilterChip(
                                    selected = template == tpl,
                                    onClick = { template = tpl },
                                    label = { Text(tpl.label, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Backdrop colors
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Backdrop",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            for (bg in CollageBackdrop.entries) {
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { backdrop = bg },
                                    shape = CircleShape,
                                    color = bg.color,
                                    border = BorderStroke(
                                        if (backdrop == bg) 2.5.dp else 1.dp,
                                        if (backdrop == bg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {}
                            }
                        }

                        // Spacing & Radius Sliders
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Gap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = spacingDp,
                                onValueChange = { spacingDp = it },
                                valueRange = 0f..22f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("Curve", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = radiusDp,
                                onValueChange = { radiusDp = it },
                                valueRange = 0f..28f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Save CTA
                        Button(
                            onClick = {
                                if (!saving) {
                                    saving = true
                                    scope.launch {
                                        val savedUri = renderAndSaveCollage(
                                            context = context,
                                            photos = selectedPhotos,
                                            template = template,
                                            bgColor = backdrop.color.toArgb(),
                                            isDark = backdrop.isDark,
                                            spacingFraction = spacingDp / 320f,
                                            radiusFraction = radiusDp / 320f
                                        )
                                        saving = false
                                        if (savedUri != null) {
                                            Toast.makeText(context, "Collage saved to Pictures/Collages", Toast.LENGTH_SHORT).show()
                                            onSaved(savedUri)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Could not save collage", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !saving
                        ) {
                            if (saving) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Exporting high-res collage…")
                            } else {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Collage to Gallery", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePreviewCanvas(
    photos: List<MediaItem>,
    template: CollageTemplate,
    backdrop: CollageBackdrop,
    spacingDp: Float,
    radiusDp: Float,
    selectedSwapIndex: Int,
    onTileTap: (Int) -> Unit
) {
    val gap = spacingDp.dp
    val corner = radiusDp.dp
    val pad = (gap + 4.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(pad),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        when (template) {
            CollageTemplate.Filmstrip -> {
                for ((i, photo) in photos.withIndex()) {
                    CollagePhotoTile(
                        photo = photo,
                        index = i,
                        selectedForSwap = selectedSwapIndex == i,
                        corner = corner,
                        onTap = { onTileTap(i) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
            CollageTemplate.Bento -> {
                if (photos.size == 2) {
                    CollagePhotoTile(photos[0], 0, selectedSwapIndex == 0, corner, { onTileTap(0) }, Modifier.weight(1.4f).fillMaxWidth())
                    CollagePhotoTile(photos[1], 1, selectedSwapIndex == 1, corner, { onTileTap(1) }, Modifier.weight(1f).fillMaxWidth())
                } else {
                    CollagePhotoTile(photos[0], 0, selectedSwapIndex == 0, corner, { onTileTap(0) }, Modifier.weight(1.35f).fillMaxWidth())
                    val rest = photos.drop(1)
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for ((idx, photo) in rest.withIndex()) {
                            val globalIdx = idx + 1
                            CollagePhotoTile(photo, globalIdx, selectedSwapIndex == globalIdx, corner, { onTileTap(globalIdx) }, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            CollageTemplate.Polaroid -> {
                val rows = photos.chunked(2)
                for ((rIdx, rowItems) in rows.withIndex()) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for ((cIdx, photo) in rowItems.withIndex()) {
                            val globalIdx = rIdx * 2 + cIdx
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(corner.coerceAtLeast(4.dp)),
                                color = if (backdrop.isDark) Color(0xFF1F2128) else Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Column(Modifier.fillMaxSize().padding(6.dp)) {
                                    CollagePhotoTile(photo, globalIdx, selectedSwapIndex == globalIdx, (corner / 2).coerceAtLeast(2.dp), { onTileTap(globalIdx) }, Modifier.weight(1f).fillMaxWidth())
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        photo.dateLabel.ifBlank { "Memory" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (backdrop.isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
            CollageTemplate.Grid -> {
                val chunk = if (photos.size <= 3) 1 else 2
                val rows = photos.chunked(chunk)
                for ((rIdx, rowItems) in rows.withIndex()) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for ((cIdx, photo) in rowItems.withIndex()) {
                            val globalIdx = rIdx * chunk + cIdx
                            CollagePhotoTile(photo, globalIdx, selectedSwapIndex == globalIdx, corner, { onTileTap(globalIdx) }, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePhotoTile(
    photo: MediaItem,
    index: Int,
    selectedForSwap: Boolean,
    corner: androidx.compose.ui.unit.Dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .then(
                if (selectedForSwap) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(corner))
                else Modifier
            )
            .clickable(onClick = onTap)
    ) {
        GalleryImage(
            imageRes = photo.imageRes,
            imageUri = photo.contentUri,
            contentDescription = "Collage photo ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            cornerRadius = corner,
            contentScale = ContentScale.Crop,
            thumbnailSize = 800,
            loadQuality = ImageLoadQuality.HighQuality,
            backgroundColor = Color.DarkGray,
            fallbackImageUri = photo.previewUri,
            cacheRevision = mediaThumbnailCacheRevision(photo)
        )
    }
}

private suspend fun renderAndSaveCollage(
    context: Context,
    photos: List<MediaItem>,
    template: CollageTemplate,
    bgColor: Int,
    isDark: Boolean,
    spacingFraction: Float,
    radiusFraction: Float
): Uri? = withContext(Dispatchers.IO) {
    val width = if (template == CollageTemplate.Filmstrip) 1280 else 1800
    val height = when (template) {
        CollageTemplate.Filmstrip -> 2280
        else -> 2120
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(bgColor)

    val gap = width * spacingFraction.coerceIn(0f, 0.08f)
    val pad = gap + width * 0.015f
    val cornerPx = width * radiusFraction.coerceIn(0f, 0.08f)

    // Compute slot rects based on template
    val slots = computeCollageSlots(photos.size, template, width.toFloat(), height.toFloat(), pad, gap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDark) android.graphics.Color.rgb(31, 33, 40) else android.graphics.Color.WHITE
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDark) android.graphics.Color.argb(180, 255, 255, 255) else android.graphics.Color.argb(170, 0, 0, 0)
        textSize = width * 0.022f
    }

    for (i in photos.indices) {
        val rect = slots.getOrNull(i) ?: break
        val item = photos[i]
        val loaded = loadSampledBitmap(context, item.contentUri, 1200) ?: continue

        if (template == CollageTemplate.Polaroid) {
            val r = cornerPx.coerceAtLeast(8f)
            canvas.drawRoundRect(rect, r, r, cardBgPaint)
            val innerPad = gap * 0.5f + 12f
            val bottomChin = rect.height() * 0.14f
            val imgRect = RectF(
                rect.left + innerPad,
                rect.top + innerPad,
                rect.right - innerPad,
                rect.bottom - bottomChin
            )
            drawCenterCropRounded(canvas, loaded, imgRect, (r / 2f).coerceAtLeast(4f), paint)
            val label = item.dateLabel.ifBlank { "Memory" }
            canvas.drawText(label, imgRect.left + 4f, rect.bottom - bottomChin * 0.35f, textPaint)
        } else {
            drawCenterCropRounded(canvas, loaded, rect, cornerPx, paint)
        }
        loaded.recycle()
    }

    val filename = "Collage_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Collages")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val uri = resolver.insert(collection, values) ?: return@withContext null
    return@withContext try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (_: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        null
    } finally {
        bitmap.recycle()
    }
}

private fun computeCollageSlots(
    count: Int,
    template: CollageTemplate,
    w: Float,
    h: Float,
    pad: Float,
    gap: Float
): List<RectF> {
    val contentW = w - pad * 2f
    val contentH = h - pad * 2f
    val result = mutableListOf<RectF>()

    when (template) {
        CollageTemplate.Filmstrip -> {
            val rowH = (contentH - gap * (count - 1)) / count
            for (i in 0 until count) {
                val top = pad + i * (rowH + gap)
                result.add(RectF(pad, top, pad + contentW, top + rowH))
            }
        }
        CollageTemplate.Bento -> {
            val heroH = if (count == 2) contentH * 0.58f else contentH * 0.56f
            result.add(RectF(pad, pad, pad + contentW, pad + heroH))
            val subTop = pad + heroH + gap
            val subH = contentH - heroH - gap
            if (count == 2) {
                result.add(RectF(pad, subTop, pad + contentW, subTop + subH))
            } else {
                val restCount = count - 1
                val subW = (contentW - gap * (restCount - 1)) / restCount
                for (i in 0 until restCount) {
                    val left = pad + i * (subW + gap)
                    result.add(RectF(left, subTop, left + subW, subTop + subH))
                }
            }
        }
        CollageTemplate.Polaroid, CollageTemplate.Grid -> {
            val cols = if (count <= 3 && template == CollageTemplate.Grid) 1 else 2
            val rowsCount = (count + cols - 1) / cols
            val rowH = (contentH - gap * (rowsCount - 1)) / rowsCount
            var idx = 0
            for (r in 0 until rowsCount) {
                val itemsInRow = minOf(cols, count - idx)
                val colW = (contentW - gap * (itemsInRow - 1)) / itemsInRow
                val top = pad + r * (rowH + gap)
                for (c in 0 until itemsInRow) {
                    val left = pad + c * (colW + gap)
                    result.add(RectF(left, top, left + colW, top + rowH))
                    idx++
                }
            }
        }
    }
    return result
}

private fun drawCenterCropRounded(canvas: Canvas, src: Bitmap, dest: RectF, radius: Float, paint: Paint) {
    canvas.save()
    val path = Path().apply {
        addRoundRect(dest, radius, radius, Path.Direction.CW)
    }
    canvas.clipPath(path)

    val scaleX = dest.width() / src.width.toFloat()
    val scaleY = dest.height() / src.height.toFloat()
    val scale = maxOf(scaleX, scaleY)

    val matrix = Matrix().apply {
        postScale(scale, scale)
        val dx = dest.left + (dest.width() - src.width * scale) / 2f
        val dy = dest.top + (dest.height() - src.height * scale) / 2f
        postTranslate(dx, dy)
    }
    canvas.drawBitmap(src, matrix, paint)
    canvas.restore()
}

private fun loadSampledBitmap(context: Context, uri: Uri?, targetSize: Int): Bitmap? {
    if (uri == null) return null
    return runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (opts.outWidth / (sample * 2) >= targetSize && opts.outHeight / (sample * 2) >= targetSize) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    }.getOrNull()
}
