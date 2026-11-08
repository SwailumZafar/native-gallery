package com.example.nativegallery.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.Album
import com.example.nativegallery.ui.theme.GalleryTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises the real overlay using only a generated solid cover and colored test tiles.
 * No gallery repository, MediaStore URI, real photo, or MainActivity is opened by this test.
 */
class HeroPacingInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()
    private lateinit var renderedFixture: GraphicsLayer

    @Test fun normalHeroMovesThroughoutOpeningAndClosingWithSyntheticContent() {
        composeRule.mainClock.autoAdvance = false
        val cover = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(211, 87, 72))
        }
        val album = Album("synthetic-hero-pacing", "Test album", 12, emptyList())
        val motion = AlbumTransitionMotion(true, hero = true)
        var transition by mutableStateOf<AlbumTransitionSpec?>(null)
        var settled by mutableStateOf(false)
        var visible by mutableStateOf(true)
        var initial: AlbumTransitionSpec? = null
        var width = 0f
        var height = 0f
        var source = Rect.Zero
        try {
            composeRule.setContent {
                GalleryTheme(darkTheme = false) {
                    if (visible) {
                        val fixtureLayer = rememberGraphicsLayer()
                        renderedFixture = fixtureLayer
                        val density = LocalDensity.current
                        width = with(density) { 200.dp.toPx() }
                        height = with(density) { 440.dp.toPx() }
                        source = with(density) { Rect(8.dp.toPx(), 100.dp.toPx(), 192.dp.toPx(), 185.dp.toPx()) }
                        Box(Modifier.size(200.dp, 440.dp).drawWithContent {
                            fixtureLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(fixtureLayer)
                        }.background(Color(0xFFE9E4EF)).testTag("synthetic-hero-root")) {
                            if (transition == null && !settled) Box(Modifier.offset(8.dp, 100.dp)
                                .size(184.dp, 85.dp).background(Color(0xFFD35748)))
                            PositionAwareAlbumTransitionOverlay(
                                transition = transition,
                                settledAlbum = album.takeIf { settled },
                                committed = true,
                                rootWidthPx = width,
                                rootHeightPx = height,
                                rootBoundsInWindow = Rect(0f, 0f, width, height),
                                onCommitted = {},
                                onFinished = {
                                    settled = it.mode == AlbumTransitionMode.Opening
                                    transition = null
                                }
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(Color.White)
                                    drawRect(Color(0xFF564478), Offset(size.width * 0.08f, size.height * 0.06f),
                                        Size(size.width * 0.62f, size.height * 0.035f))
                                    val cell = size.width / 3f
                                    repeat(12) { index ->
                                        drawRect(if (index % 2 == 0) Color(0xFF79B5BC) else Color(0xFFE6B76D),
                                            Offset((index % 3) * cell, size.height * 0.19f + (index / 3) * cell),
                                            Size(cell - 2f, cell - 2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            composeRule.runOnIdle {
                initial = AlbumTransitionSpec(1, album, source, 24f, AlbumTransitionMode.Opening,
                    previewBitmap = cover, heroPacing = true, motion = motion)
                transition = initial
            }
            repeat(2) { composeRule.mainClock.advanceTimeByFrame(); composeRule.waitForIdle() }
            saveFrame("open-000")
            composeRule.mainClock.advanceTimeBy(96)
            composeRule.waitForIdle()
            assertTrue("Normal Hero must not finish in the first quarter", expansion(source, width, height, motion) in 0.07f..0.16f)
            saveFrame("open-096")
            composeRule.mainClock.advanceTimeBy(96)
            composeRule.waitForIdle()
            assertTrue(expansion(source, width, height, motion) in 0.40f..0.58f)
            saveFrame("open-192")
            composeRule.mainClock.advanceTimeBy(96)
            composeRule.waitForIdle()
            assertTrue(expansion(source, width, height, motion) in 0.82f..0.94f)
            saveFrame("open-288")
            composeRule.mainClock.advanceTimeBy(128)
            composeRule.waitForIdle()
            assertEquals(1f, motion.progress.value, 0f)
            saveFrame("open-416")

            composeRule.runOnIdle {
                settled = false
                transition = requireNotNull(initial).copy(key = 2, mode = AlbumTransitionMode.Closing)
            }
            repeat(2) { composeRule.mainClock.advanceTimeByFrame(); composeRule.waitForIdle() }
            saveFrame("close-000")
            for (time in listOf(64, 128, 192)) {
                composeRule.mainClock.advanceTimeBy(64)
                composeRule.waitForIdle()
                saveFrame("close-$time")
            }
            composeRule.mainClock.advanceTimeBy(96)
            composeRule.waitForIdle()
            assertEquals(0f, motion.progress.value, 0f)
            assertTrue(!settled && transition == null)
            saveFrame("close-288")
        } finally {
            composeRule.runOnIdle { visible = false }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            cover.recycle()
        }
    }

    private fun expansion(source: Rect, width: Float, height: Float, motion: AlbumTransitionMotion): Float =
        (albumContainerFrame(source, width, height, motion.progress.value).bounds.height - source.height) / (height - source.height)

    private fun saveFrame(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Read only the controlled fixture's retained layer. Window capture tries to
        // advance/redraw a deliberately frozen clock and can time out on emulators.
        val bitmap = runBlocking {
            withContext(Dispatchers.Main) { renderedFixture.toImageBitmap() }
        }.asAndroidBitmap()
        File(context.getExternalFilesDir(null), "hero-pacing-fixture-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
