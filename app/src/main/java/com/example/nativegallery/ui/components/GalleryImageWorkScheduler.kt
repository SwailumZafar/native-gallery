package com.example.nativegallery.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/** Cache hits stay immediate; new decodes and large bitmap uploads yield to visible motion. */
internal object GalleryImageWorkScheduler {
    private val movingSurfaces = MutableStateFlow(0)

    fun setNavigationMoving(moving: Boolean) = setMoving(Navigation, moving)
    fun setViewerMoving(moving: Boolean) = setMoving(Viewer, moving)

    private fun setMoving(surface: Int, moving: Boolean) {
        movingSurfaces.update { current ->
            if (moving) current or surface else current and surface.inv()
        }
    }

    suspend fun awaitIdle() {
        movingSurfaces.first { it == 0 }
    }

    private const val Navigation = 1
    private const val Viewer = 2
}
