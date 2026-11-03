package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import kotlin.math.abs

/** Shared across direction changes; new albums get independent motion. */
internal class AlbumTransitionMotion(opening: Boolean) {
    val progress = Animatable(if (opening) 0f else 1f)

    suspend fun animateTo(opening: Boolean, fast: Boolean) {
        val target = if (opening) 1f else 0f
        val duration = ((if (opening) 400 else 300) * (if (fast) 0.75f else 1f) * abs(target - progress.value))
            .toInt().coerceAtLeast(1)
        progress.animateTo(target, tween(duration, easing = if (opening) ReferenceAlbumOpening else ReferenceAlbumClosing))
    }
}

// Smoothed samples from the actual screen viewport in the reference, excluding its side bars.
// These retain the gradual first half of closing; a generic fast-out ease collapsed it too early.
internal val ReferenceAlbumOpening = SampledAlbumEasing(floatArrayOf(
    0f, 0f, .10f, .12f, .20f, .34f, .30f, .54f, .40f, .69f,
    .50f, .80f, .60f, .89f, .70f, .95f, .80f, .98f, 1f, 1f
))
internal val ReferenceAlbumClosing = SampledAlbumEasing(floatArrayOf(
    0f, 0f, .056f, .018f, .112f, .087f, .168f, .203f, .224f, .292f,
    .280f, .395f, .336f, .461f, .392f, .585f, .448f, .638f,
    .504f, .712f, .560f, .765f, .616f, .853f, .72f, .94f, .82f, .99f, 1f, 1f
))

/** Monotone cubic interpolation avoids both velocity corners and overshoot between samples. */
internal class SampledAlbumEasing(private val points: FloatArray) : Easing {
    private val count = points.size / 2
    private val slopes = FloatArray(count - 1) { i ->
        (points[(i + 1) * 2 + 1] - points[i * 2 + 1]) / (points[(i + 1) * 2] - points[i * 2])
    }
    private val tangents = FloatArray(count) { i ->
        when (i) {
            0, count - 1 -> 0f
            else -> if (slopes[i - 1] * slopes[i] <= 0f) 0f else 2f / (1f / slopes[i - 1] + 1f / slopes[i])
        }
    }
    override fun transform(fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        var i = 0
        while (i < count - 2 && f > points[(i + 1) * 2]) i++
        val width = points[(i + 1) * 2] - points[i * 2]
        val t = (f - points[i * 2]) / width
        val t2 = t * t
        val t3 = t2 * t
        return ((2f * t3 - 3f * t2 + 1f) * points[i * 2 + 1] +
            (t3 - 2f * t2 + t) * width * tangents[i] +
            (-2f * t3 + 3f * t2) * points[(i + 1) * 2 + 1] +
            (t3 - t2) * width * tangents[i + 1]).coerceIn(0f, 1f)
    }
}
