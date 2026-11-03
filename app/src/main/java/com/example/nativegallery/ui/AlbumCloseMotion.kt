package com.example.nativegallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal data class AlbumCloseFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
    val alpha: Float,
    val cornerRadius: Float
)

/** A full page scales into its cover; it never becomes a moving crop of full-size tiles. */
internal fun albumCloseFrame(
    rootWidth: Float,
    rootHeight: Float,
    cover: Rect,
    expansion: Float,
    coverCornerRadius: Float
): AlbumCloseFrame {
    val p = expansion.coerceIn(0f, 1f)
    val width = cover.width + (rootWidth - cover.width) * p
    val height = cover.height + (rootHeight - cover.height) * p
    val fade = (p / 0.20f).coerceIn(0f, 1f)
    return AlbumCloseFrame(
        left = cover.left * (1f - p),
        top = cover.top * (1f - p),
        width = width,
        height = height,
        scale = max(width / rootWidth, height / rootHeight).coerceAtLeast(0.001f),
        alpha = fade * fade * (3f - 2f * fade),
        cornerRadius = coverCornerRadius * (1f - p)
    )
}

@Composable
internal fun AlbumClosingPage(
    rootWidthPx: Float,
    rootHeightPx: Float,
    coverBounds: Rect,
    coverCornerRadiusDp: Float,
    expansion: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val rootWidthDp = with(density) { rootWidthPx.toDp() }
    val rootHeightDp = with(density) { rootHeightPx.toDp() }
    val cornerPx = with(density) { coverCornerRadiusDp.dp.toPx() }
    val clippingPath = remember { Path() }
    Box(modifier.requiredSize(rootWidthDp, rootHeightDp)
        .graphicsLayer {
            val frame = albumCloseFrame(rootWidthPx, rootHeightPx, coverBounds, expansion(), cornerPx)
            transformOrigin = TransformOrigin(0f, 0f)
            translationX = frame.left
            translationY = frame.top
            scaleX = frame.scale
            scaleY = frame.scale
            alpha = frame.alpha
        }
        .drawWithContent {
            val frame = albumCloseFrame(rootWidthPx, rootHeightPx, coverBounds, expansion(), cornerPx)
            val radius = frame.cornerRadius / frame.scale
            clippingPath.reset()
            clippingPath.addRoundRect(RoundRect(
                Rect(0f, 0f, frame.width / frame.scale, frame.height / frame.scale),
                CornerRadius(radius, radius)
            ))
            clipPath(clippingPath) { this@drawWithContent.drawContent() }
        }
        .background(MaterialTheme.colorScheme.background)
    ) { content() }
}
