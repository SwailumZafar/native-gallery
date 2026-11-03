package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import com.example.nativegallery.ui.components.MotionSpecs

/** Shared across direction changes; new albums get independent motion. */
internal class AlbumTransitionMotion(opening: Boolean) {
    val progress = Animatable(if (opening) 0f else 1f)

    suspend fun animateTo(opening: Boolean, fast: Boolean) {
        val spring = if (fast) {
            if (opening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast
        } else {
            if (opening) MotionSpecs.AppleSpringFolder else MotionSpecs.AppleSpringFolderClose
        }
        progress.animateTo(if (opening) 1f else 0f, spring)
    }
}
