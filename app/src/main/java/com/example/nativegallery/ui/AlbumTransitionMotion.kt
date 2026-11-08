package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.MotionSpecs
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// Preserve Enhanced's previous endpoint timing, but spread its visible journey
// across that clock instead of combining a launch kick with front-loaded springs.
private fun enhancedSettleMillis(spec: SpringSpec<Float>, launchVelocity: Float): Int = ceil(
    spec.vectorize(Float.VectorConverter).getDurationNanos(
        AnimationVector1D(0f), AnimationVector1D(1f), AnimationVector1D(launchVelocity)
    ) / 1_000_000.0
).toInt()

internal val EnhancedAlbumOpenMillis = enhancedSettleMillis(MotionSpecs.AppleSpringFolderFast, 6f)
internal val EnhancedAlbumCloseMillis = enhancedSettleMillis(MotionSpecs.AppleSpringFolderCloseFast, 7f)

/** Shared across direction changes; new albums get independent motion. */
internal class AlbumTransitionMotion(opening: Boolean, val classic: Boolean = false) {
    val progress = Animatable(if (opening) 0f else 1f)

    suspend fun animateTo(opening: Boolean, fast: Boolean) {
        val target = if (opening) 1f else 0f
        val animation: AnimationSpec<Float> = if (classic) {
            // Historical folder springs, restored independently of the newer motion presets.
            when {
                fast && opening -> spring(dampingRatio = 0.84f, stiffness = 620f)
                fast -> spring(dampingRatio = 0.86f, stiffness = 680f)
                opening -> spring(dampingRatio = 0.82f, stiffness = 300f)
                else -> spring(dampingRatio = 0.84f, stiffness = 320f)
            }
        } else {
            val duration = when {
                fast && opening -> EnhancedAlbumOpenMillis
                fast -> EnhancedAlbumCloseMillis
                opening -> GalleryMotion.AlbumOpenMillis
                else -> GalleryMotion.AlbumCloseMillis
            }
            tween(
                durationMillis = (duration * abs(target - progress.value)).roundToInt().coerceAtLeast(80),
                // Geometry applies the smooth curve once for either LivingFlow speed.
                easing = LinearEasing
            )
        }
        progress.animateTo(target, animation, initialVelocity = progress.velocity)
    }
}
