package com.example.nativegallery.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.data.VideoDurationCache
import com.example.nativegallery.ui.components.GalleryImageWorkScheduler
import com.example.nativegallery.ui.components.ThumbnailDiskCache
import com.example.nativegallery.ui.components.ThumbnailMemoryCache
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import com.example.nativegallery.ui.components.thumbnailCacheIdentityUri
import com.example.nativegallery.ui.components.rememberVideoDurationMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlbumRowActivationTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun scrollingPastRowsDoesNotRecomposeAlreadyInteractiveRows() {
        val firstVisible = mutableIntStateOf(0)
        var compositions = 0
        composeRule.setContent {
            Column {
                repeat(8) { row ->
                    rememberAlbumRowInteractive(false, row) { firstVisible.intValue }
                    SideEffect { compositions++ }
                }
            }
        }
        composeRule.waitForIdle()
        val initial = compositions
        for (index in 1..20) {
            composeRule.runOnIdle { firstVisible.intValue = index }
            composeRule.waitForIdle()
        }
        assertEquals("Crossing a row must not recompose every visible row", initial, compositions)
    }

    @Test fun recoveredTimestampsArePresentOnTheFirstCompositionOfEveryAlbumVisit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val item = MediaItem("cached-duration", "fixture", MediaType.Video, "Recovered.mp4", "",
            contentUri = Uri.parse("content://album-duration-fixture/${System.nanoTime()}"), isVideo = true,
            fileSizeBytes = 42L, dateModifiedMillis = 100L)
        val identity = VideoDurationCache.identity(requireNotNull(item.contentUri), item.id, item.fileSizeBytes, item.dateModifiedMillis)
        VideoDurationCache.put(context, identity, 123_000L)
        val visit = mutableIntStateOf(0)
        val firstDurations = mutableMapOf<Int, Long?>()
        try {
            GalleryImageWorkScheduler.setNavigationMoving(true)
            composeRule.setContent {
                key(visit.intValue) {
                    val duration = rememberVideoDurationMillis(item)
                    val currentVisit = visit.intValue
                    SideEffect {
                        if (!firstDurations.containsKey(currentVisit)) firstDurations[currentVisit] = duration
                    }
                    AlbumOpeningRow(listOf(item), 1, Modifier.width(100.dp))
                }
            }
            repeat(6) { iteration ->
                if (iteration > 0) composeRule.runOnIdle { visit.intValue = iteration }
                composeRule.onNodeWithText("02:03").assertIsDisplayed()
                composeRule.runOnIdle { assertEquals(123_000L, firstDurations[iteration]) }
            }
            assertEquals(0L, VideoDurationCache.peek(VideoDurationCache.identity(
                requireNotNull(item.contentUri), item.id, item.fileSizeBytes, 101L)))
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
            context.getSharedPreferences("native_gallery_video_durations_v1", 0).edit().remove(identity).apply()
        }
    }

    @Test fun aDiskCachedRowAppearsWhileNavigationStillBlocksMediaDecoders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val item = MediaItem("cache-row", "fixture", MediaType.Photo, "Cached row", "",
            contentUri = Uri.parse("content://album-opening-fixture/${System.nanoTime()}"))
        val identity = thumbnailCacheIdentityUri(requireNotNull(item.contentUri), false, mediaThumbnailCacheRevision(item))
        val key = ThumbnailMemoryCache.key(identity, 384)
        val bitmap = Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        try {
            ThumbnailDiskCache.put(context, key, bitmap)
            composeRule.waitUntil(5_000) {
                ThumbnailDiskCache.get(context, key)?.also { it.recycle() } != null
            }
            GalleryImageWorkScheduler.setNavigationMoving(true)
            composeRule.setContent {
                AlbumOpeningRow(listOf(item), 1, Modifier.width(100.dp).testTag("cached-row"))
            }
            composeRule.waitUntil(5_000) { ThumbnailMemoryCache.get(key) != null }
            val pixels = composeRule.onNodeWithTag("cached-row").captureToImage().toPixelMap()
            val center = pixels[pixels.width / 2, pixels.height / 2]
            assertEquals(Color.Red.red, center.red, 0.02f)
            assertEquals(Color.Red.green, center.green, 0.02f)
            assertEquals(Color.Red.blue, center.blue, 0.02f)
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
            ThumbnailMemoryCache.remove(key)
            ThumbnailDiskCache.remove(context, key)
            bitmap.recycle()
        }
    }

    @Test fun upgradingCachedRowsKeepsTheExactHeightAtEveryGridDensity() {
        val columns = mutableIntStateOf(1)
        var cachedHeight = 0
        var interactiveHeight = 0
        composeRule.setContent {
            Column(Modifier.width(157.dp)) {
                AlbumOpeningRow(emptyList(), columns.intValue,
                    Modifier.fillMaxWidth().onSizeChanged { cachedHeight = it.height })
                Row(Modifier.fillMaxWidth().onSizeChanged { interactiveHeight = it.height },
                    horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(columns.intValue) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
                }
            }
        }
        for (count in 1..10) {
            composeRule.runOnIdle { columns.intValue = count }
            composeRule.waitForIdle()
            assertEquals("Changing row rendering must not shift the grid ($count columns)", interactiveHeight, cachedHeight)
        }
    }

    @Test fun interactiveRowsWaitForMotionAndActivateAcrossFramesWithoutResettingOnClose() {
        composeRule.mainClock.autoAdvance = false
        val moving = mutableStateOf(true)
        val ready = BooleanArray(8)
        composeRule.setContent {
            Column {
                repeat(ready.size) { row ->
                    val interactive = rememberAlbumRowInteractive(moving.value, row + 20) { 20 }.value
                    SideEffect { ready[row] = interactive }
                }
            }
        }
        repeat(10) { advanceFrame() }
        assertEquals(0, ready.count { it })
        composeRule.runOnIdle { moving.value = false }
        var sawPartialActivation = false
        repeat(16) {
            advanceFrame()
            val count = ready.count { it }
            if (count in 1 until ready.size) sawPartialActivation = true
        }
        assertTrue("The full grid must not be installed in a single frame", sawPartialActivation)
        assertTrue(ready.all { it })
        composeRule.runOnIdle { moving.value = true }
        repeat(4) { advanceFrame() }
        assertTrue("Closing must retain the already interactive grid", ready.all { it })
    }

    private fun advanceFrame() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }
}
