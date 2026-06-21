package com.example.nativegallery.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenHeader(
    title: String,
    actions: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SearchPill(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(36.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Search",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
    )
}

@Composable
fun MediaThumbnail(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onClick: (() -> Unit)? = null
) {
    val containerModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(modifier = containerModifier) {
        GalleryImage(
            imageRes = mediaItem.imageRes,
            imageUri = mediaItem.contentUri,
            contentDescription = mediaItem.title,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = cornerRadius,
            thumbnailSize = 384
        )
        if (mediaItem.isVideo && mediaItem.contentUri != null) {
            VideoBadge(
                duration = mediaItem.durationLabel.orEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
fun ResourceImage(
    imageRes: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    imageUri: Uri? = null,
    thumbnailSize: Int = 512
) {
    GalleryImage(
        imageRes = imageRes,
        imageUri = imageUri,
        contentDescription = contentDescription,
        modifier = modifier,
        cornerRadius = cornerRadius,
        thumbnailSize = thumbnailSize
    )
}

@Composable
fun GalleryImage(
    imageRes: Int?,
    imageUri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Int = 512
) {
    val bitmap = rememberContentUriBitmap(imageUri, thumbnailSize)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            imageRes != null -> {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                SkeletonBlock(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp
                )
            }
        }
    }
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val transition = rememberInfiniteTransition(label = "skeleton shimmer")
    val offset = transition.animateFloat(
        initialValue = -420f,
        targetValue = 840f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing)
        ),
        label = "skeleton shimmer offset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(offset.value, 0f),
                    end = Offset(offset.value + 360f, 360f)
                )
            )
    )
}

@Composable
fun MediaAccessNotice(
    message: String,
    actionLabel: String,
    onRequestAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Button(onClick = onRequestAccess) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun VideoBadge(
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.42f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            if (duration.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = duration,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun rememberContentUriBitmap(imageUri: Uri?, thumbnailSize: Int): Bitmap? {
    val context = LocalContext.current.applicationContext
    val cacheKey = imageUri?.let { ThumbnailMemoryCache.key(it, thumbnailSize) }
    val cachedBitmap = cacheKey?.let { ThumbnailMemoryCache.get(it) }

    val bitmapState = produceState<Bitmap?>(initialValue = cachedBitmap, imageUri, thumbnailSize) {
        value = cachedBitmap
        if (imageUri != null && cacheKey != null && cachedBitmap == null) {
            value = withContext(Dispatchers.IO) {
                loadThumbnail(context, imageUri, thumbnailSize)?.also { loaded ->
                    ThumbnailMemoryCache.put(cacheKey, loaded)
                }
            }
        }
    }
    return bitmapState.value
}

private fun loadThumbnail(context: Context, imageUri: Uri, thumbnailSize: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(imageUri, Size(thumbnailSize, thumbnailSize), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }
    } catch (_: Exception) {
        null
    }
}