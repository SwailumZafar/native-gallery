package com.example.nativegallery.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun selectionScrollStep(y: Float, height: Float, insetTop: Float, insetBottom: Float, density: Float): Float {
    val edge = 72f * density
    val top = insetTop + edge
    val bottom = height - insetBottom - edge
    return when {
        y < top -> -((top - y) / edge).coerceIn(0f, 1f) * 14f * density
        y > bottom -> ((y - bottom) / edge).coerceIn(0f, 1f) * 14f * density
        else -> 0f
    }
}

/** Holds the gesture while edge scrolling; newly exposed tiles join the same selection. */
internal fun Modifier.dragSelectWithAutoScroll(
    enabled: Boolean,
    itemsKey: Any,
    selected: () -> Set<String>,
    hit: (Offset) -> MediaItem?,
    toggle: (MediaItem) -> Unit,
    scroll: suspend (Float) -> Float,
    invalidateBounds: () -> Unit,
    topInset: Float = 64f,
    bottomInset: Float = 90f
): Modifier = pointerInput(enabled, itemsKey) {
    if (!enabled) return@pointerInput
    coroutineScope {
        var pointer: Offset? = null
        var base = emptySet<String>()
        var adding = true
        val visited = hashSetOf<String>()
        var scrolling: Job? = null
        fun selectAt(point: Offset) {
            val item = hit(point) ?: return
            if (visited.add(item.id) && (adding == (item.id !in base))) toggle(item)
        }
        fun finish() { pointer = null; scrolling?.cancel(); visited.clear() }
        try {
            detectDragGestures(
                onDragStart = { point ->
                    base = selected()
                    visited.clear()
                    adding = hit(point)?.id !in base
                    pointer = point
                    selectAt(point)
                    scrolling = launch {
                        while (isActive && pointer != null) {
                            withFrameNanos { }
                            val at = pointer ?: break
                            val step = selectionScrollStep(at.y, size.height.toFloat(), topInset * density, bottomInset * density, density)
                            if (step != 0f) {
                                if (scroll(step) != 0f) {
                                    invalidateBounds()
                                    withFrameNanos { }
                                    pointer?.let(::selectAt)
                                }
                            }
                        }
                    }
                },
                onDrag = { change, _ -> pointer = change.position; selectAt(change.position); change.consume() },
                onDragEnd = ::finish,
                onDragCancel = ::finish
            )
        } finally { finish() }
    }
}

internal fun expandMediaBundles(items: List<MediaItem>): List<MediaItem> =
    items.flatMap { it.burstItems.ifEmpty { listOf(it) } }.distinctBy { it.id }
