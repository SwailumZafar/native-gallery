package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.example.nativegallery.ui.components.GalleryFastScroller
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GalleryFastScrollTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun aQuickThumbDragReachesBothExactListEnds() {
        lateinit var state: LazyListState
        composeRule.setContent {
            state = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().testTag("fixture-list"), state = state) {
                    items(500) { index -> Text("Row $index", Modifier.fillMaxWidth().height(64.dp)) }
                }
                GalleryFastScroller(state, Modifier.align(Alignment.CenterEnd), topPadding = 0.dp)
            }
        }
        composeRule.onNodeWithTag("fixture-list").performTouchInput { swipeUp(durationMillis = 100) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("gallery-fast-scroll-thumb").fetchSemanticsNodes().isNotEmpty()
        }
        dragTo(bottom = true)
        composeRule.waitUntil(5_000) { !state.canScrollForward }
        dragTo(bottom = false)
        composeRule.waitUntil(5_000) { !state.canScrollBackward }
    }

    private fun dragTo(bottom: Boolean) {
        val track = composeRule.onNodeWithTag("gallery-fast-scroll-track").fetchSemanticsNode().boundsInRoot
        val thumb = composeRule.onNodeWithTag("gallery-fast-scroll-thumb").fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag("gallery-fast-scroll-track").performTouchInput {
            val start = Offset(thumb.center.x - track.left, thumb.center.y - track.top)
            val end = Offset(start.x, if (bottom) track.height - thumb.height / 2f else thumb.height / 2f)
            swipe(start, end, durationMillis = 150)
        }
    }
}
