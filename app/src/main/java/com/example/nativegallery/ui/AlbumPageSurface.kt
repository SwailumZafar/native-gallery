package com.example.nativegallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sqrt

internal data class AlbumPageFrame(val bounds: Rect, val scale: Float, val alpha: Float, val radius: Float)

internal fun albumPageFrame(width: Float, height: Float, cover: Rect, expansion: Float, radius: Float): AlbumPageFrame {
    val p = expansion.coerceIn(0f, 1f)
    val bounds = Rect(cover.left * (1f - p), cover.top * (1f - p),
        cover.right + (width - cover.right) * p, cover.bottom + (height - cover.bottom) * p)
    val blend = (p / 0.18f).coerceIn(0f, 1f)
    return AlbumPageFrame(bounds, max(bounds.width / width, bounds.height / height).coerceAtLeast(0.001f),
        blend * blend * (3f - 2f * blend), radius * sqrt(1f - p))
}

/** The same once-measured live page remains mounted at rest, during entry, and during return. */
@Composable
internal fun AlbumPageSurface(
    rootWidthPx: Float,
    rootHeightPx: Float,
    coverBounds: Rect,
    coverCornerRadiusDp: Float,
    moving: Boolean,
    expansion: () -> Float,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val widthDp = with(density) { rootWidthPx.toDp() }
    val heightDp = with(density) { rootHeightPx.toDp() }
    val radiusPx = with(density) { coverCornerRadiusDp.dp.toPx() }
    val path = remember { Path() }
    Box(Modifier.fillMaxSize()
        .then(if (moving) Modifier.testTag("AlbumTransitionOverlay") else Modifier)
        .pointerInput(moving) {
            if (moving) awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        }) {
        Box(Modifier.requiredSize(widthDp, heightDp)
            .graphicsLayer {
                val frame = albumPageFrame(rootWidthPx, rootHeightPx, coverBounds, expansion(), radiusPx)
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = frame.bounds.left
                translationY = frame.bounds.top
                scaleX = frame.scale
                scaleY = frame.scale
                alpha = frame.alpha
            }
            .drawWithContent {
                val frame = albumPageFrame(rootWidthPx, rootHeightPx, coverBounds, expansion(), radiusPx)
                val radius = frame.radius / frame.scale
                path.reset()
                path.addRoundRect(RoundRect(Rect(0f, 0f,
                    frame.bounds.width / frame.scale, frame.bounds.height / frame.scale), CornerRadius(radius, radius)))
                clipPath(path) { this@drawWithContent.drawContent() }
            }
            .background(MaterialTheme.colorScheme.background)
        ) { content() }
    }
}
