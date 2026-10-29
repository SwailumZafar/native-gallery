package com.example.nativegallery.ui

import com.example.nativegallery.ui.components.photoDecodePixelBudget
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Decode only the active zoomed photo more deeply, with a bounded pixel allocation. */
internal fun viewerPhotoResolution(width: Int?, height: Int?, fitSize: Int, zoomed: Boolean,
    memoryBytes: Long = Runtime.getRuntime().maxMemory()): Int {
    val fallback = if (zoomed) (fitSize.toLong() * 2).coerceAtMost(6144).toInt() else fitSize
    val w = width?.takeIf { it > 0 } ?: fallback
    val h = height?.takeIf { it > 0 } ?: fallback
    val nativeEdge = max(w, h)
    val pixelBudget = photoDecodePixelBudget(memoryBytes)
    val ratio = nativeEdge.toDouble() / min(w, h).coerceAtLeast(1)
    val boundedEdge = sqrt(pixelBudget * ratio).toInt().coerceAtLeast(1)
    return minOf(if (zoomed) nativeEdge else fitSize, nativeEdge, boundedEdge, 6144).coerceAtLeast(1)
}
