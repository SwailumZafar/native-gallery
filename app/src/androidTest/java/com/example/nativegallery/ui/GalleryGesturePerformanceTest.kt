package com.example.nativegallery.ui

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.ui.components.GalleryMotion
import com.example.nativegallery.ui.components.rememberDeferredAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryGesturePerformanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pointerTargetsMoveTheLayerWithoutRecompositionOrMeasurement() {
        val target = mutableStateOf(Offset.Zero)
        lateinit var renderedOffset: State<Offset>
        var compositions = 0
        var measurements = 0
        composeRule.setContent {
            val offset = rememberDeferredAnimation(
                initialValue = Offset.Zero,
                typeConverter = Offset.VectorConverter,
                targetValue = { target.value },
                animationSpec = { snap() },
                label = "pointer performance fixture"
            )
            renderedOffset = offset
            SideEffect { compositions += 1 }
            Box(Modifier.size(64.dp)
                .layout { measurable, constraints ->
                    measurements += 1
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
                .testTag("moving-layer"))
        }
        composeRule.waitForIdle()
        val initialBounds = composeRule.onNodeWithTag("moving-layer").fetchSemanticsNode().boundsInRoot
        val initialCompositions = compositions
        val initialMeasurements = measurements
        repeat(6) { frame ->
            val next = Offset((frame + 1) * 10f, (frame + 1) * 5f)
            composeRule.runOnIdle { target.value = next }
            composeRule.waitUntil(5_000L) { renderedOffset.value == next }
            composeRule.waitForIdle()
            val bounds = composeRule.onNodeWithTag("moving-layer").fetchSemanticsNode().boundsInRoot
            assertEquals(initialBounds.left + next.x, bounds.left, 1f)
            assertEquals(initialBounds.top + next.y, bounds.top, 1f)
        }
        assertEquals(initialCompositions, compositions)
        assertEquals(initialMeasurements, measurements)
    }

    @Test
    fun interruptedSpringRetainsItsMomentumThenSettlesAtTheNewTarget() {
        composeRule.mainClock.autoAdvance = false
        val target = mutableFloatStateOf(0f)
        lateinit var animated: State<Float>
        composeRule.setContent {
            animated = rememberDeferredAnimation(
                initialValue = 0f,
                typeConverter = Float.VectorConverter,
                targetValue = { target.floatValue },
                animationSpec = {
                    spring(
                        dampingRatio = GalleryMotion.ViewerTransformDamping,
                        stiffness = GalleryMotion.ViewerTransformStiffness
                    )
                },
                label = "retargeted spring fixture"
            )
            Box(Modifier.size(64.dp).graphicsLayer { translationX = animated.value })
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { target.floatValue = 200f }
        composeRule.mainClock.advanceTimeBy(80)
        var before = 0f
        composeRule.runOnIdle { before = animated.value }
        assertTrue("Spring must be moving before interruption", before > 0f && before < 200f)

        composeRule.runOnIdle { target.floatValue = before - 10f }
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.runOnIdle {
            assertTrue("Retargeting must retain the current forward velocity", animated.value > before)
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.runOnIdle { assertEquals(before - 10f, animated.value, 0.1f) }
    }
}
