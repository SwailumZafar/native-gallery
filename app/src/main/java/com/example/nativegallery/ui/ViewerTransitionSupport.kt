package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** The opening hero owns input until the viewer can receive it. */
@Composable
internal fun MediaOpeningInputBarrier(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    })
}

internal fun gridReturnViewport(bounds: Rect, topInsetPx: Float, bottomInsetPx: Float): Rect {
    val top = (bounds.top + topInsetPx.coerceAtLeast(0f)).coerceAtMost(bounds.bottom)
    val bottom = (bounds.bottom - bottomInsetPx.coerceAtLeast(0f)).coerceAtLeast(top)
    return Rect(bounds.left, top, bounds.right, bottom)
}

/** A partly clipped row is revealed before closing; oversized tiles may cover the viewport. */
internal fun Rect.isVisibleGridReturnTarget(viewport: Rect): Boolean {
    if (!left.isFinite() || !top.isFinite() || !right.isFinite() || !bottom.isFinite() ||
        width < 4f || height < 4f || viewport.width < 4f || viewport.height < 4f
    ) return false
    val overlap = intersect(viewport)
    return overlap.width >= minOf(width, viewport.width) - 1f &&
        overlap.height >= minOf(height, viewport.height) - 1f
}
