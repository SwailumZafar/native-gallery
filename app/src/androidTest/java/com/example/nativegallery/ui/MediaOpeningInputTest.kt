package com.example.nativegallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaOpeningInputTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun openingOwnsRepeatedTapsAndDragsThenReleasesInput() {
        val opening = mutableStateOf(true)
        var taps = 0
        var drags = 0
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().clickable { taps++ }.pointerInput(Unit) {
                    detectDragGestures { change, _ -> change.consume(); drags++ }
                })
                if (opening.value) MediaOpeningInputBarrier()
            }
        }
        repeat(4) { composeRule.onRoot().performTouchInput { click() } }
        composeRule.onRoot().performTouchInput { swipeDown() }
        composeRule.runOnIdle {
            assertEquals(0, taps)
            assertEquals(0, drags)
            opening.value = false
        }
        composeRule.onRoot().performTouchInput { click() }
        composeRule.onRoot().performTouchInput { swipeDown() }
        composeRule.runOnIdle {
            assertEquals(1, taps)
            assertTrue(drags > 0)
        }
    }
}
