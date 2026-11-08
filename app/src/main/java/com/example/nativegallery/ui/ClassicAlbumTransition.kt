package com.example.nativegallery.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.nativegallery.model.Album
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision

/** The saved spring-and-zoom path: a centered aperture reveals the full-size album page. */
internal fun classicAlbumBounds(source: Rect, width: Float, height: Float, progress: Float): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    return Rect(lerp(source.left, 0f, fraction), lerp(source.top, 0f, fraction),
        lerp(source.right, width, fraction), lerp(source.bottom, height, fraction))
}

private data class ClassicAlbumClip(val bounds: Rect, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(bounds, CornerRadius(radius)))
}

/** Historical geometry retained on a layer so animating the mask never redraws the grid. */
internal fun Modifier.classicAlbumPageLayer(
    source: Rect,
    width: Float,
    height: Float,
    sourceRadius: Float,
    progress: () -> Float
): Modifier = graphicsLayer {
    val fraction = progress().coerceIn(0f, 1f)
    val bounds = classicAlbumBounds(source, width, height, fraction)
    val lift = GalleryMotion.smoothstep(0.04f, 0.35f, fraction) *
        (1f - GalleryMotion.smoothstep(0.70f, 1f, fraction))
    transformOrigin = TransformOrigin(0.5f, 0.5f)
    translationX = bounds.center.x - width / 2f
    translationY = bounds.center.y - height / 2f
    scaleX = lerp(0.88f, 1f, fraction)
    scaleY = scaleX
    alpha = GalleryMotion.smoothstep(0.08f, 0.70f, fraction)
    compositingStrategy = CompositingStrategy.ModulateAlpha
    shadowElevation = 18.dp.toPx() * lift
    cameraDistance = 16f * density
    val clipLeft = (width - bounds.width) / 2f
    val clipTop = (height - bounds.height) / 2f
    shape = ClassicAlbumClip(Rect(clipLeft, clipTop, clipLeft + bounds.width, clipTop + bounds.height),
        sourceRadius * (1f - fraction))
    clip = true
}

@Composable
internal fun ClassicAlbumTransitionCover(
    album: Album,
    sourceBounds: Rect,
    rootWidth: Float,
    rootHeight: Float,
    sourceRadius: Float,
    progress: () -> Float,
    source: AlbumTransitionSource<GraphicsLayer>?,
    preview: Bitmap?
) {
    val density = LocalDensity.current
    val faceSource = album.id.startsWith("person:") && source?.available == true
    Box(Modifier
        .requiredWidth(with(density) { sourceBounds.width.toDp() })
        .requiredHeight(with(density) { sourceBounds.height.toDp() })
        .graphicsLayer {
            val fraction = progress().coerceIn(0f, 1f)
            val bounds = classicAlbumBounds(sourceBounds, rootWidth, rootHeight, fraction)
            val lift = GalleryMotion.smoothstep(0.04f, 0.35f, fraction) *
                (1f - GalleryMotion.smoothstep(0.70f, 1f, fraction))
            transformOrigin = TransformOrigin(0f, 0f)
            translationX = bounds.left
            translationY = bounds.top
            scaleX = bounds.width.coerceAtLeast(1f) / sourceBounds.width.coerceAtLeast(1f)
            scaleY = bounds.height.coerceAtLeast(1f) / sourceBounds.height.coerceAtLeast(1f)
            // The cover is fully transparent after the handoff. Its old independent
            // shadow was still rasterized over the page for the remainder of the move.
            val coverAlpha = 1f - GalleryMotion.smoothstep(0.12f, 0.60f, fraction)
            shadowElevation = 18.dp.toPx() * lift * coverAlpha
            cameraDistance = 16f * density.density
            shape = RoundedCornerShape((sourceRadius * (1f - fraction)).dp)
            clip = true
        }) {
        if (!faceSource) GalleryImage(
            imageRes = album.coverRes,
            imageUri = album.coverUri.takeUnless { preview != null },
            contentDescription = album.name,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = 1f - GalleryMotion.smoothstep(0.12f, 0.60f, progress())
            },
            cornerRadius = 0.dp,
            contentScale = ContentScale.Crop,
            thumbnailSize = 512,
            loadQuality = ImageLoadQuality.Thumbnail,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            cachedOnly = true,
            animatedPlaceholder = false,
            initialBitmap = preview,
            isVideo = album.hasVideoBadge,
            cacheRevision = album.coverMediaItem?.let(::mediaThumbnailCacheRevision)
        )
        if (source?.available == true) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                val fraction = progress().coerceIn(0f, 1f)
                alpha = (1f - GalleryMotion.smoothstep(0.12f, 0.60f, fraction)) *
                    (if (faceSource) 1f else 1f - GalleryMotion.smoothstep(0f, 0.12f, fraction))
            }.drawBehind {
                if (source.available && source.size.width > 0 && source.size.height > 0) {
                    withTransform({
                        scale(size.width / source.size.width, size.height / source.size.height, Offset.Zero)
                    }) { drawLayer(source.layer) }
                }
            })
        }
    }
}
