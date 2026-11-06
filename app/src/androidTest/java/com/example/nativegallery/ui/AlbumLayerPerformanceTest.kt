package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlbumLayerPerformanceTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun movingAndClippingTheAlbumDoesNotRedrawOrRemeasureItsGrid() =
        checkRetainedGrid(wide = false)

    @Test fun heroCenterMaskDoesNotRedrawOrRemeasureItsGridInEitherDirection() =
        checkRetainedGrid(wide = true)

    private fun checkRetainedGrid(wide: Boolean) {
        val progress = mutableFloatStateOf(0.3f)
        var compositions = 0
        var measurements = 0
        var gridDraws = 0
        composeRule.setContent {
            SideEffect { compositions++ }
            val density = LocalDensity.current
            val width = with(density) { 160.dp.toPx() }
            val height = with(density) { 320.dp.toPx() }
            Box(Modifier.size(160.dp, 320.dp).testTag("page").albumPageLayer(
                source = Rect(width * 0.1f, height * 0.3f, width * if (wide) 0.95f else 0.5f, height * 0.5f),
                width = width, height = height, sourceRadius = 18f,
                progress = { progress.floatValue }
            ).layout { measurable, constraints ->
                measurements++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }.drawBehind {
                gridDraws++
                drawRect(Color.Blue)
            })
        }
        composeRule.onNodeWithTag("page").captureToImage()
        val initialCompositions = compositions
        val initialMeasurements = measurements
        val initialDraws = gridDraws
        for (value in listOf(0.5f, 0.8f, 1f, 0.7f, 0.2f, 0f)) {
            composeRule.runOnIdle { progress.floatValue = value }
            composeRule.onNodeWithTag("page").captureToImage()
        }
        assertEquals(initialCompositions, compositions)
        assertEquals(initialMeasurements, measurements)
        assertEquals("The retained grid must not redraw for a changing clip", initialDraws, gridDraws)
    }
}
