package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import com.example.nativegallery.ui.components.GalleryMotion
import kotlin.math.max

/** Ancestor clipping must not change the cover's aspect ratio or its return position. */
internal fun LayoutCoordinates.albumBoundsInWindow(): Rect {
    val corners = arrayOf(
        localToWindow(Offset.Zero), localToWindow(Offset(size.width.toFloat(), 0f)),
        localToWindow(Offset(0f, size.height.toFloat())),
        localToWindow(Offset(size.width.toFloat(), size.height.toFloat()))
    )
    return Rect(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y })
}

internal fun albumLargeTileFraction(source: Rect, width: Float): Float =
    GalleryMotion.smoothstep(0.60f, 0.90f, source.width / width.coerceAtLeast(1f))

internal data class AlbumContainerFrame(
    val bounds: Rect,
    val contentScale: Float,
    val translation: Offset,
    val contentClip: Rect
)

internal data class AlbumCoverTransform(val scale: Float, val translation: Offset)

/** Cover captures must crop uniformly; independent X/Y scaling stretches faces and labels. */
internal fun albumCoverTransform(width: Float, height: Float, bounds: Rect): AlbumCoverTransform {
    val scale = max(bounds.width / width.coerceAtLeast(1f), bounds.height / height.coerceAtLeast(1f))
    return AlbumCoverTransform(scale, Offset(bounds.center.x - width * scale / 2f, bounds.center.y - height * scale / 2f))
}

internal fun albumCoverAlpha(source: Rect, progress: Float, width: Float = Float.POSITIVE_INFINITY): Float {
    // The slow-motion reference retains the page until the final cover-sized part
    // of the collapse. A large cover must not replace the still-tall shrinking grid.
    val banner = GalleryMotion.smoothstep(1.2f, 2.2f, source.width / source.height.coerceAtLeast(1f))
    val large = albumLargeTileFraction(source, width)
    return 1f - GalleryMotion.smoothstep(0f, lerp(lerp(0.16f, 0.20f, banner), 0.26f, large), progress)
}

/** Uniform page scaling, with a top anchor for small tiles and a center mask for wide cards. */
internal fun albumContainerFrame(source: Rect, width: Float, height: Float, progress: Float): AlbumContainerFrame {
    val fraction = progress.coerceIn(0f, 1f)
    val large = albumLargeTileFraction(source, width)
    // A native album is a miniature page for most of its journey. Its height only
    // compresses into the cover near the source, rather than cropping away rows
    // throughout the whole collapse. The same curve makes reversals continuous.
    val pageHeightAtSourceWidth = source.width / width * height
    val extraPageHeight = (pageHeightAtSourceWidth - source.height).coerceAtLeast(0f) *
        (1f - fraction) * GalleryMotion.smoothstep(0f, 0.22f, fraction) * (1f - large)
    // A wide card cannot become a tall miniature page at its original width.
    // Morph its aperture between the exact rectangles instead. Keep the page
    // centered behind that aperture so both ends reveal/collapse together.
    val top = lerp(source.top, 0f, fraction)
    val bounds = Rect(
        lerp(source.left, 0f, fraction),
        top,
        lerp(source.right, width, fraction),
        top + lerp(source.height, height, fraction) + extraPageHeight
    )
    val scale = max(bounds.width / width, bounds.height / height).coerceAtLeast(0.001f)
    val clipWidth = bounds.width / scale
    val clipLeft = (width - clipWidth) / 2f
    val translationY = lerp(bounds.top, bounds.center.y - height * scale / 2f, large)
    val clipTop = (bounds.top - translationY) / scale
    return AlbumContainerFrame(
        bounds = bounds,
        contentScale = scale,
        translation = Offset(bounds.center.x - width * scale / 2f, translationY),
        contentClip = Rect(clipLeft, clipTop, clipLeft + clipWidth, clipTop + bounds.height / scale)
    )
}
