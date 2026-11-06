package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.MotionSpecs
import kotlin.math.abs
import kotlin.math.roundToInt

/** Shared across direction changes; new albums get independent motion. */
internal class AlbumTransitionMotion(opening: Boolean) {
    val progress = Animatable(if (opening) 0f else 1f)

    suspend fun animateTo(opening: Boolean, fast: Boolean) {
        val target = if (opening) 1f else 0f
        val animation = if (fast) {
            if (opening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast
        } else {
            val duration = if (opening) GalleryMotion.AlbumOpenMillis else GalleryMotion.AlbumCloseMillis
            tween(
                durationMillis = (duration * abs(target - progress.value)).roundToInt().coerceAtLeast(80),
                easing = if (opening) GalleryMotion.AlbumOpenEasing else GalleryMotion.AlbumCloseEasing
            )
        }
        // Start moving on the first advancing frame instead of spending the beginning
        // of a tap accelerating from rest. Direction changes keep their current position.
        val launchVelocity = when {
            opening && progress.value == 0f -> if (fast) 6f else 2.8f
            !opening && progress.value == 1f -> if (fast) -7f else -3.4f
            else -> progress.velocity
        }
        progress.animateTo(target, animation, initialVelocity = launchVelocity)
    }
}
