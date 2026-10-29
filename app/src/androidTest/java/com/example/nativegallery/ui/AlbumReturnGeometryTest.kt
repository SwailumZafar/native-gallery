package com.example.nativegallery.ui

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumReturnGeometryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val destination = mutableStateOf(GalleryDestination.Main)
    private val heroVisible = mutableStateOf(false)
    private var coverBounds = Rect.Zero
    private var pageWidthPx = 0f

    @Test
    fun hiddenAlbumCoversKeepTheirExactReturnBoundsAcrossHeroHandoff() {
        showRetainedPage()
        val restingBounds = coverBounds
        composeRule.runOnIdle {
            heroVisible.value = true
            destination.value = GalleryDestination.AlbumDetail
        }
        assertStationaryForFrames(restingBounds)

        // The open hero is removed while the actual album remains visible.
        composeRule.runOnIdle { heroVisible.value = false }
        assertStationaryForFrames(restingBounds)

        composeRule.runOnIdle {
            heroVisible.value = true
            destination.value = GalleryDestination.Main
        }
        assertStationaryForFrames(restingBounds)
        composeRule.runOnIdle { heroVisible.value = false }
        assertStationaryForFrames(restingBounds)
    }

    @Test
    fun landscapeRailLaneRemainsMeasuredWhileAnAlbumCoversTheLibrary() {
        showRetainedPage(useRail = true)
        val restingBounds = coverBounds
        composeRule.runOnIdle { destination.value = GalleryDestination.AlbumDetail; heroVisible.value = true }
        assertStationaryForFrames(restingBounds)
        composeRule.runOnIdle { heroVisible.value = false }
        assertStationaryForFrames(restingBounds)
        composeRule.runOnIdle { destination.value = GalleryDestination.Main; heroVisible.value = true }
        assertStationaryForFrames(restingBounds)
        composeRule.runOnIdle { heroVisible.value = false }
        assertStationaryForFrames(restingBounds)
    }

    @Test
    fun secondaryPagesKeepTheirExistingSlideAndReturnToTheExactRestingBounds() {
        showRetainedPage()
        val restingBounds = coverBounds
        composeRule.runOnIdle { destination.value = GalleryDestination.HiddenItems }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(restingBounds.left - pageWidthPx / 10f, coverBounds.left, 0.5f)

        composeRule.runOnIdle { destination.value = GalleryDestination.Main }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(restingBounds.left, coverBounds.left, 0.5f)
        assertEquals(restingBounds.top, coverBounds.top, 0.5f)
    }

    private fun showRetainedPage(useRail: Boolean = false) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            pageWidthPx = with(LocalDensity.current) { 320.dp.toPx() }
            val transition = updateTransition(destination.value, label = "return bounds fixture")
            val albumNavigation = transition.currentState == GalleryDestination.AlbumDetail ||
                transition.targetState == GalleryDestination.AlbumDetail
            val offset = rememberRetainedMainPageOffset(
                transition, albumNavigation, heroVisible.value, pageWidthPx
            )
            Box(Modifier.size(320.dp).graphicsLayer {
                alpha = if (destination.value == GalleryDestination.Main) 1f else 0f
                translationX = offset.value * size.width / if (albumNavigation) 8f else 10f
            }) {
                GalleryMainPane(useRail, rail = { Box(Modifier.width(88.dp).fillMaxHeight()) }) {
                Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(80.dp, 40.dp).size(80.dp).onGloballyPositioned {
                    coverBounds = it.boundsInWindow()
                })
                }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertStationaryForFrames(expected: Rect) {
        repeat(24) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            assertEquals(expected.left, coverBounds.left, 0.5f)
            assertEquals(expected.top, coverBounds.top, 0.5f)
            assertEquals(expected.width, coverBounds.width, 0.5f)
            assertEquals(expected.height, coverBounds.height, 0.5f)
        }
    }
}
