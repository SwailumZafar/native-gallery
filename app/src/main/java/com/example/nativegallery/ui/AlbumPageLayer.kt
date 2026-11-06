package com.example.nativegallery.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private data class AlbumPageClip(val bounds: Rect, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(bounds, CornerRadius(radius)))
}

/** Transform and clip the retained display list without invalidating the grid's draw pass. */
internal fun Modifier.albumPageLayer(
    source: Rect,
    width: Float,
    height: Float,
    sourceRadius: Float,
    progress: () -> Float,
    reveal: () -> Float = { 1f }
): Modifier = graphicsLayer {
    val fraction = progress().coerceIn(0f, 1f)
    val frame = albumContainerFrame(source, width, height, fraction)
    transformOrigin = TransformOrigin(0f, 0f)
    translationX = frame.translation.x
    translationY = frame.translation.y
    scaleX = frame.contentScale
    scaleY = frame.contentScale
    alpha = reveal()
    compositingStrategy = CompositingStrategy.ModulateAlpha
    shape = AlbumPageClip(frame.contentClip, sourceRadius * (1f - fraction) / frame.contentScale)
    clip = true
}
