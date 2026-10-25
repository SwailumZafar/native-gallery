package com.example.nativegallery.ui

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.example.nativegallery.ui.components.MotionSpecs

@Composable
internal fun rememberRetainedMainPageOffset(
    transition: Transition<GalleryDestination>,
    albumNavigation: Boolean,
    useAlbumHero: Boolean,
    rootWidthPx: Float
): State<Float> = transition.animateFloat(
    transitionSpec = {
        if (useAlbumHero) snap() else spring(
            dampingRatio = MotionSpecs.AppleSpringSheetOffset.dampingRatio,
            stiffness = MotionSpecs.AppleSpringSheetOffset.stiffness,
            visibilityThreshold = (if (albumNavigation) 8f else 10f) / rootWidthPx.coerceAtLeast(1f)
        )
    },
    label = "retained main offset"
) { destination ->
    // Album covers report window bounds even while this retained page is invisible.
    // Keep those coordinates at their resting positions throughout a folder visit;
    // shifting the hidden page makes the closing cover land left of the real tile.
    when (destination) {
        GalleryDestination.Main, GalleryDestination.AlbumDetail -> 0f
        else -> -1f
    }
}
