package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.*
import org.junit.Test

class LiveGridInteractionTest {
    private fun photo(id: String) = MediaItem(id, "camera", MediaType.Photo, id, "Today")
    @Test fun selectionScrollsInBothDirectionsAndStopsInTheMiddle() {
        assertTrue(selectionScrollStep(40f, 800f, 60f, 90f, 1f) < 0f)
        assertTrue(selectionScrollStep(740f, 800f, 60f, 90f, 1f) > 0f)
        assertEquals(0f, selectionScrollStep(400f, 800f, 60f, 90f, 1f), 0f)
    }
    @Test fun bundleActionsIncludeEveryFrameExactlyOnce() {
        val frames = listOf(photo("one"), photo("two"), photo("three"))
        val stack = frames.first().copy(isBurst = true, burstItems = frames)
        assertEquals(frames, expandMediaBundles(listOf(stack, frames[1])))
    }
    @Test fun liveRemovalKeepsOldSlotsUntilTheExitAnimationFinishes() {
        val a = photo("a"); val b = photo("b"); val c = photo("c")
        assertEquals(listOf(a, b.copy(title = "updated"), c), retainDepartingMedia(listOf(a, b), listOf(b.copy(title = "updated"), c)))
    }
    @Test fun patternFillsSkippedMiddleDotsAndNeverRepeatsThem() {
        assertEquals("123", extendVaultPattern("1", 3))
        assertEquals("159", extendVaultPattern("1", 9))
        assertEquals("123", extendVaultPattern("123", 2))
    }
}
