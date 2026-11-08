package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.MotionSpecs
import com.example.nativegallery.model.AlbumLayoutMode
import kotlin.math.abs
import kotlin.math.roundToInt

/** Use the captured tile's actual width so adaptive Big tiles keep the right pacing. */
internal fun useHeroAlbumPacing(layout: AlbumLayoutMode, sourceWidth: Float, rootWidth: Float): Boolean =
    (layout == AlbumLayoutMode.Hero || layout == AlbumLayoutMode.BigTiles) &&
        rootWidth > 0f && sourceWidth / rootWidth > 0.60f

/** Shared across direction changes; new albums get independent motion. */
internal class AlbumTransitionMotion(opening: Boolean, val classic: Boolean = false, val hero: Boolean = false) {
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
        } else if (fast) {
            if (opening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast
        } else {
            val duration = if (opening) GalleryMotion.AlbumOpenMillis else GalleryMotion.AlbumCloseMillis
            tween(
                durationMillis = (duration * abs(target - progress.value)).roundToInt().coerceAtLeast(80),
                // Hero geometry already applies a smooth spatial curve. Feeding it the
                // normal ease-out completed almost all visible motion in the first 150 ms.
                // A linear clock applies that curve once, over the entire 400/280 ms.
                easing = if (hero) LinearEasing else if (opening) GalleryMotion.AlbumOpenEasing else GalleryMotion.AlbumCloseEasing
            )
        }
        // Start moving on the first advancing frame instead of spending the beginning
        // of a tap accelerating from rest. Direction changes keep their current position.
        val launchVelocity = when {
            classic -> progress.velocity
            hero && !fast -> progress.velocity
            opening && progress.value == 0f -> if (fast) 6f else 2.8f
            !opening && progress.value == 1f -> if (fast) -7f else -3.4f
            else -> progress.velocity
        }
        progress.animateTo(target, animation, initialVelocity = launchVelocity)
    }
}
