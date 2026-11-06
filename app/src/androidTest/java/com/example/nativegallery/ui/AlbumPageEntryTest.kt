package com.example.nativegallery.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlbumPageEntryTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun aCapturedCoverGetsMovingFramesBeforeTheGridIsMounted() {
        composeRule.mainClock.autoAdvance = false
        val album = mutableStateOf("settled")
        var pageMounted = false
        composeRule.setContent {
            val ready = rememberAlbumPageReady(album.value, deferNewPage = album.value == "opening").value
            SideEffect { pageMounted = ready }
        }
        composeRule.waitForIdle()
        assertTrue(pageMounted)
        composeRule.runOnIdle { album.value = "opening" }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        assertFalse("Heavy album content must not block the cover's first frame", pageMounted)
        repeat(6) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
        assertTrue(pageMounted)
    }
}
