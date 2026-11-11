@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.example.nativegallery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.PlaceCluster
import com.example.nativegallery.data.PlacesRepository
import com.example.nativegallery.data.PlacesScanProgress
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryScreenHeader
import com.example.nativegallery.ui.components.MediaThumbnail
import kotlin.math.hypot

@Composable
internal fun PlacesScreen(
    mediaItems: List<MediaItem>,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem, List<MediaItem>, Rect) -> Unit,
    viewerOpen: Boolean = false
) {
    val context = LocalContext.current
    val repository = remember(context) { PlacesRepository(context.applicationContext) }
    var hasMediaLocationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var requestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaLocationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
    var progress by remember {
        mutableStateOf(PlacesScanProgress(emptyList(), 0, mediaItems.count { !it.isVideo }, 0))
    }
    var scanning by remember { mutableStateOf(true) }
    var selectedClusterId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(hasMediaLocationPermission) {
        if (!hasMediaLocationPermission && !requestedLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

    LaunchedEffect(mediaItems, hasMediaLocationPermission) {
        scanning = true
        repository.scanPlaces(mediaItems, allowCacheNegatives = hasMediaLocationPermission) { p ->
            progress = p
        }
        scanning = false
    }

    val selectedCluster = remember(progress.clusters, selectedClusterId) {
        progress.clusters.firstOrNull { it.id == selectedClusterId }
    }

    BackHandler(enabled = selectedCluster != null && !viewerOpen) {
        selectedClusterId = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("places-screen")
        ) {
            GalleryScreenHeader(
                title = selectedCluster?.name ?: "Places",
                onBack = {
                    if (selectedCluster != null) selectedClusterId = null else onBack()
                },
                subtitle = if (selectedCluster != null) {
                    "${selectedCluster.media.size} photos · ${selectedCluster.coordinateLabel}"
                } else {
                    if (scanning && progress.total > 0) {
                        "Scanning EXIF locations ${progress.scanned}/${progress.total}…"
                    } else {
                        "${progress.clusters.size} places · ${progress.geotaggedCount} geotagged photos"
                    }
                }
            )

            AnimatedContent(
                targetState = selectedCluster,
                modifier = Modifier.weight(1f),
                contentKey = { it?.id ?: "map-hub" },
                label = "places-navigation"
            ) { cluster ->
                if (cluster != null) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(112.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(cluster.media, key = { it.id }) { item ->
                            MediaThumbnail(
                                mediaItem = item,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                cornerRadius = 8.dp,
                                onClickWithBounds = { bounds ->
                                    onOpenMedia(item, cluster.media, bounds)
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (scanning) {
                            item(key = "places-progress") {
                                LinearProgressIndicator(
                                    progress = {
                                        if (progress.total > 0) progress.scanned.toFloat() / progress.total else 0f
                                    },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        if (progress.clusters.isNotEmpty()) {
                            item(key = "places-radar-map") {
                                InteractivePlacesRadarMap(
                                    clusters = progress.clusters,
                                    onClusterSelected = { selectedClusterId = it.id }
                                )
                            }
                        }

                        if (!scanning && progress.clusters.isEmpty()) {
                            item(key = "places-empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Public,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "No geotagged photos found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (!hasMediaLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            "Allow media location access so NativeGallery can read GPS coordinates stored inside your photos."
                                        } else {
                                            "Enable location tags in your camera settings to see memories plotted by city and region on your interactive map."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!hasMediaLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        FilledTonalButton(
                                            onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Allow media location")
                                        }
                                    }
                                }
                            }
                        }

                        items(progress.clusters, key = { it.id }) { place ->
                            PlaceClusterCard(
                                cluster = place,
                                onClick = { selectedClusterId = place.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractivePlacesRadarMap(
    clusters: List<PlaceCluster>,
    onClusterSelected: (PlaceCluster) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val pulseTransition = rememberInfiniteTransition(label = "pin-pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseR"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseA"
    )

    // Compute bounding box around user's clusters (with generous margin)
    val lats = clusters.map { it.latitude }
    val lons = clusters.map { it.longitude }
    val minLat = (lats.minOrNull() ?: 0.0)
    val maxLat = (lats.maxOrNull() ?: 0.0)
    val minLon = (lons.minOrNull() ?: 0.0)
    val maxLon = (lons.maxOrNull() ?: 0.0)
    val latSpan = (maxLat - minLat).coerceAtLeast(1.0)
    val lonSpan = (maxLon - minLon).coerceAtLeast(1.0)

    val paddedMinLat = minLat - latSpan * 0.25
    val paddedMaxLat = maxLat + latSpan * 0.25
    val paddedMinLon = minLon - lonSpan * 0.25
    val paddedMaxLon = maxLon + lonSpan * 0.25

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F131A),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(clusters) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val nearest = clusters.minByOrNull { c ->
                                val x = ((c.longitude - paddedMinLon) / (paddedMaxLon - paddedMinLon)).toFloat() * w
                                val y = (1f - ((c.latitude - paddedMinLat) / (paddedMaxLat - paddedMinLat)).toFloat()) * h
                                hypot(x - tapOffset.x, y - tapOffset.y)
                            }
                            if (nearest != null) {
                                val x = ((nearest.longitude - paddedMinLon) / (paddedMaxLon - paddedMinLon)).toFloat() * w
                                val y = (1f - ((nearest.latitude - paddedMinLat) / (paddedMaxLat - paddedMinLat)).toFloat()) * h
                                if (hypot(x - tapOffset.x, y - tapOffset.y) <= 90f) {
                                    onClusterSelected(nearest)
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val gridColor = Color.White.copy(alpha = 0.07f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Latitude / Longitude radar grid lines
                for (i in 1..4) {
                    val y = h * i / 5f
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                    val x = w * i / 5f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                }

                // Draw connecting travel lines between clusters
                val coords = clusters.map { c ->
                    val x = ((c.longitude - paddedMinLon) / (paddedMaxLon - paddedMinLon)).toFloat() * w
                    val y = (1f - ((c.latitude - paddedMinLat) / (paddedMaxLat - paddedMinLat)).toFloat()) * h
                    Offset(x, y)
                }
                for (i in 0 until coords.size - 1) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = coords[i],
                        end = coords[i + 1],
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Draw pins
                for ((idx, point) in coords.withIndex()) {
                    if (idx == 0) {
                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha),
                            radius = 12.dp.toPx() * pulseRadius,
                            center = point,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = 12.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = point
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Map, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                    Text(
                        "Tap any glowing pin to jump to that location",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceClusterCard(
    cluster: PlaceCluster,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2x2 mosaic preview or single big cover
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                val preview = cluster.media.take(4)
                if (preview.size < 4) {
                    MediaThumbnail(preview.first(), Modifier.fillMaxSize(), cornerRadius = 0.dp)
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            MediaThumbnail(preview[0], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp)
                            MediaThumbnail(preview[1], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp)
                        }
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            MediaThumbnail(preview[2], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp)
                            MediaThumbnail(preview[3], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp)
                        }
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = cluster.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = cluster.coordinateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Photo, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "${cluster.media.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
