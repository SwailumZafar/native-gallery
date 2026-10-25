package com.example.nativegallery.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.DisplayedBitmapCache
import com.example.nativegallery.ui.components.GalleryImageWorkScheduler
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.ThumbnailDiskCache
import com.example.nativegallery.ui.components.ThumbnailMemoryCache
import com.example.nativegallery.ui.components.prefetchMediaThumbnails
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GalleryImagePerformanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappedPreviewSurvivesEvictionAndRepeatedViewerHandoffsWithoutDecoding() {
        val uri = Uri.parse("content://gallery-performance/handoff-${System.nanoTime()}")
        val key = ThumbnailMemoryCache.key(uri, 512)
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        val viewer = mutableStateOf(false)
        val seed = mutableStateOf<Bitmap?>(null)
        ThumbnailMemoryCache.put(key, bitmap)
        GalleryImageWorkScheduler.setNavigationMoving(true)
        try {
            composeRule.setContent {
                androidx.compose.runtime.key(viewer.value) {
                    GalleryImage(
                        imageRes = null, imageUri = uri, contentDescription = "Handoff fixture",
                        modifier = Modifier.size(160.dp).testTag("handoff"),
                        cornerRadius = 0.dp, backgroundColor = Color.Black,
                        thumbnailSize = if (viewer.value) 1024 else 512,
                        cachedOnly = !viewer.value, animatedPlaceholder = false,
                        initialBitmap = seed.value
                    )
                }
            }
            composeRule.waitForIdle()
            assertSame(bitmap, DisplayedBitmapCache.get(uri))
            // The visible tile owns its image after the global LRU drops it. The new viewer
            // must draw that exact image even while navigation deliberately blocks decoding.
            ThumbnailMemoryCache.remove(key)
            repeat(5) {
                composeRule.runOnIdle {
                    seed.value = DisplayedBitmapCache.get(uri)
                    viewer.value = !viewer.value
                }
                val pixels = composeRule.onNodeWithTag("handoff").captureToImage().toPixelMap()
                assertEquals(Color.Blue, pixels[pixels.width / 2, pixels.height / 2])
                assertSame(bitmap, DisplayedBitmapCache.get(uri))
            }
        } finally {
            GalleryImageWorkScheduler.setNavigationMoving(false)
            ThumbnailMemoryCache.remove(key)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fullQualityHardwareImageRendersInTheViewer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "gallery-performance-${System.nanoTime()}.png")
        val fixture = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        fixture.eraseColor(android.graphics.Color.BLUE)
        file.outputStream().use { fixture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        fixture.recycle()
        val uri = Uri.fromFile(file)
        val key = ThumbnailMemoryCache.key(uri, 128, ImageLoadQuality.HighQuality)
        try {
            composeRule.setContent {
                GalleryImage(
                    imageRes = null,
                    imageUri = uri,
                    contentDescription = "Hardware image fixture",
                    modifier = Modifier.size(160.dp).testTag("hardware-image"),
                    cornerRadius = 0.dp,
                    thumbnailSize = 128,
                    loadQuality = ImageLoadQuality.HighQuality,
                    animatedPlaceholder = false
                )
            }
            composeRule.waitUntil(5_000L) { ThumbnailMemoryCache.get(key) != null }
            composeRule.waitForIdle()
            assertEquals(Bitmap.Config.HARDWARE, ThumbnailMemoryCache.get(key)?.config)
            val pixels = composeRule.onNodeWithTag("hardware-image").captureToImage().toPixelMap()
            assertEquals(Color.Blue, pixels[pixels.width / 2, pixels.height / 2])
        } finally {
            ThumbnailMemoryCache.remove(key)
            file.delete()
        }
    }

    @Test
    fun changingHeroCropKeepsAspectRatioWithoutRecompositionOrRemeasure() {
        val uri = Uri.parse("content://gallery-performance/hero-${System.nanoTime()}")
        val key = ThumbnailMemoryCache.key(uri, 512)
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        Canvas(bitmap).drawRect(70f, 0f, 130f, 100f, Paint().apply { color = android.graphics.Color.BLUE })
        ThumbnailMemoryCache.put(key, bitmap)
        val progress = mutableFloatStateOf(0f)
        var compositions = 0
        var measurements = 0
        try {
            composeRule.setContent {
                SideEffect { compositions += 1 }
                GalleryImage(
                    imageRes = null,
                    imageUri = uri,
                    contentDescription = "Performance fixture",
                    modifier = Modifier.size(160.dp).testTag("hero").layout { measurable, constraints ->
                        measurements += 1
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                    cornerRadius = 0.dp,
                    backgroundColor = Color.Black,
                    cachedOnly = true,
                    animatedPlaceholder = false,
                    drawBounds = {
                        val fraction = progress.floatValue
                        Rect(20f, 20f, 140f - 60f * fraction, 80f + 60f * fraction)
                    }
                )
            }
            composeRule.waitForIdle()
            val initial = composeRule.onNodeWithTag("hero").captureToImage().toPixelMap()
            assertEquals(Color.Red, initial[25, 45])
            val initialCompositions = compositions
            val initialMeasurements = measurements
            repeat(5) { frame ->
                composeRule.runOnIdle { progress.floatValue = (frame + 1) / 5f }
                composeRule.onNodeWithTag("hero").captureToImage()
            }
            val final = composeRule.onNodeWithTag("hero").captureToImage().toPixelMap()
            // A portrait crop contains only the center stripe, rather than squeezing red edges in.
            assertEquals(Color.Blue, final[25, 45])
            assertEquals(Color.Black, final[100, 45])
            assertEquals(initialCompositions, compositions)
            assertEquals(initialMeasurements, measurements)
        } finally {
            ThumbnailMemoryCache.remove(key)
        }
    }

    @Test
    fun prefetchKeepsExistingBitmapWhenDiskAlsoContainsTheThumbnail() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse("content://gallery-performance/prefetch-${System.nanoTime()}")
        val key = ThumbnailMemoryCache.key(uri, 384)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        try {
            ThumbnailDiskCache.put(context, key, bitmap)
            val deadline = SystemClock.elapsedRealtime() + 5_000L
            var persisted = false
            while (!persisted && SystemClock.elapsedRealtime() < deadline) {
                persisted = ThumbnailDiskCache.get(context, key)?.let { it.recycle(); true } ?: false
                if (!persisted) SystemClock.sleep(10)
            }
            assertTrue("Thumbnail writer did not finish", persisted)
            ThumbnailMemoryCache.put(key, bitmap)
            prefetchMediaThumbnails(
                context = context,
                mediaItems = listOf(MediaItem("cache-fixture", "test", MediaType.Photo, "Fixture", "Today", contentUri = uri)),
                thumbnailSizes = listOf(384),
                maxItems = 1,
                pinInMemory = true
            )
            assertSame(bitmap, ThumbnailMemoryCache.get(key))
        } finally {
            ThumbnailMemoryCache.remove(key)
            ThumbnailDiskCache.remove(context, key)
        }
    }
}
