package com.example.nativegallery.ui.components

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal fun photoDecodePixelBudget(memoryBytes: Long = Runtime.getRuntime().maxMemory()): Long =
    (memoryBytes / 24L).coerceIn(2_000_000L, 12_000_000L)

internal fun photoDecodeDimensions(width: Int, height: Int, maxSide: Int, pixelBudget: Long): Pair<Int, Int> {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    val scale = minOf(1.0, maxSide.coerceAtLeast(1).toDouble() / max(w, h),
        sqrt(pixelBudget.coerceAtLeast(1).toDouble() / (w.toDouble() * h)))
    return (w * scale).roundToInt().coerceAtLeast(1) to (h * scale).roundToInt().coerceAtLeast(1)
}
