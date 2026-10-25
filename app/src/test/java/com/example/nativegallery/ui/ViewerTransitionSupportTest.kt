package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerTransitionSupportTest {
    private val viewport = Rect(0f, 100f, 400f, 800f)

    @Test
    fun returnViewportExcludesTheHeaderAndBottomNavigation() {
        assertEquals(viewport, gridReturnViewport(Rect(0f, 0f, 400f, 900f), 100f, 100f))
        assertFalse(Rect(10f, 50f, 110f, 150f).isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(10f, 750f, 110f, 850f).isVisibleGridReturnTarget(viewport))
        assertTrue(Rect(10f, 150f, 110f, 250f).isVisibleGridReturnTarget(viewport))
    }

    @Test
    fun offscreenAndPartlyClippedTargetsMustBeRevealedBeforeClosing() {
        assertFalse(Rect(10f, -500f, 110f, -400f).isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(10f, 900f, 110f, 1000f).isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(-50f, 150f, 50f, 250f).isVisibleGridReturnTarget(viewport))
        assertTrue(Rect(0f, 100f, 100f, 200f).isVisibleGridReturnTarget(viewport))
    }

    @Test
    fun oversizedLandscapeTilesCanFillTheAvailableViewport() {
        assertTrue(Rect(0f, 50f, 400f, 950f).isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(0f, 150f, 400f, 1050f).isVisibleGridReturnTarget(viewport))
    }

    @Test
    fun missingOrInvalidGeometryCannotBecomeACloseTarget() {
        assertFalse(Rect.Zero.isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(0f, Float.NaN, 100f, 200f).isVisibleGridReturnTarget(viewport))
        assertFalse(Rect(0f, 100f, 100f, 200f).isVisibleGridReturnTarget(Rect.Zero))
        val collapsed = gridReturnViewport(Rect(0f, 0f, 400f, 500f), 600f, 200f)
        assertFalse(Rect(0f, 100f, 100f, 200f).isVisibleGridReturnTarget(collapsed))
    }
}
