package com.example.nativegallery.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Rect
import com.example.nativegallery.ui.components.MotionSpecs
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class AlbumTransitionMotionTest {
    private class Frames : MonotonicFrameClock {
        private val times = Channel<Long>(Channel.UNLIMITED)
        private var time = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(times.receive())
        suspend fun tick() { time += 16_666_667L; times.send(time); yield() }
    }

    @Test fun newAlbumsHaveIndependentOpeningAndClosingEndpoints() {
        assertEquals(0f, AlbumTransitionMotion(true).progress.value, 0f)
        assertEquals(1f, AlbumTransitionMotion(false).progress.value, 0f)
        assertNotSame(AlbumTransitionMotion(true).progress, AlbumTransitionMotion(true).progress)
    }

    private val heroSource = Rect(54f, 802f, 1210f, 1326f)

    private fun heroExpansion(motion: AlbumTransitionMotion): Float =
        (albumContainerFrame(heroSource, 1264f, 2780f, motion.progress.value, smoothPacing = true).bounds.height - heroSource.height) /
            (2780f - heroSource.height)

    @Test fun normalHeroSpreadsVisibleExpansionAcrossTheEntire400Milliseconds(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(true)
        val animation = launch(frames) { motion.animateTo(true, false) }
        yield()
        frames.tick() // Animation origin.
        repeat(6) { frames.tick() } // 100 ms.
        assertTrue("Hero must not be nearly open at 100 ms: ${heroExpansion(motion)}", heroExpansion(motion) in 0.09f..0.12f)
        repeat(6) { frames.tick() } // 200 ms.
        assertEquals(0.5f, heroExpansion(motion), 0.015f)
        repeat(6) { frames.tick() } // 300 ms.
        assertTrue("Hero must still be moving in its last quarter", heroExpansion(motion) in 0.88f..0.92f)
        repeat(6) { frames.tick() } // 400 ms.
        withTimeout(3000) { animation.join() }
        assertEquals(1f, heroExpansion(motion), 0f)
    }

    @Test fun normalHeroClosingSpreadsItsCollapseAcross280Milliseconds(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(false)
        val animation = launch(frames) { motion.animateTo(false, false) }
        yield()
        frames.tick()
        repeat(4) { frames.tick() } // 66.7 ms.
        assertTrue(heroExpansion(motion) in 0.88f..0.94f)
        repeat(4) { frames.tick() } // 133.3 ms.
        assertTrue(heroExpansion(motion) in 0.50f..0.60f)
        repeat(4) { frames.tick() } // 200 ms.
        assertTrue(heroExpansion(motion) in 0.12f..0.20f)
        repeat(5) { frames.tick() }
        withTimeout(3000) { animation.join() }
        assertEquals(0f, heroExpansion(motion), 0f)
    }

    @Test fun normalHeroRemainsVisiblyDistinctFromFastEnhanced(): Unit = runBlocking {
        val frames = Frames()
        val fastFrames = Frames()
        val normal = AlbumTransitionMotion(true)
        val fast = AlbumTransitionMotion(true)
        val normalJob = launch(frames) { normal.animateTo(true, false) }
        val fastJob = launch(fastFrames) { fast.animateTo(true, true) }
        repeat(10) { frames.tick(); fastFrames.tick() }
        assertTrue(heroExpansion(normal) in 0.20f..0.35f)
        assertTrue("Enhanced must stay quicker while continuing to move", heroExpansion(fast) > heroExpansion(normal) + 0.15f)
        assertTrue("Enhanced must not finish all visible motion early", heroExpansion(fast) < 0.99f)
        normalJob.cancelAndJoin()
        fastJob.cancelAndJoin()
    }

    @Test fun bothLivingFlowSpeedsShareSmoothPacingAcrossSizesAndDirections(): Unit = runBlocking {
        val sources = listOf(heroSource, Rect(54f, 802f, 578f, 1326f),
            Rect(450f, 497f, 816f, 863f), Rect(50f, 300f, 1100f, 480f), Rect(40f, 300f, 116f, 376f))
        for (fast in listOf(false, true)) for (opening in listOf(true, false)) {
            val frames = Frames()
            val motion = AlbumTransitionMotion(opening)
            val animation = launch(frames) { motion.animateTo(opening, fast) }
            val duration = if (fast) {
                if (opening) EnhancedAlbumOpenMillis else EnhancedAlbumCloseMillis
            } else if (opening) 400 else 280
            yield()
            frames.tick()
            repeat((duration / 16.666667f).toInt()) { step ->
                frames.tick()
                val elapsed = (step + 1) * 16.666667f
                val clock = elapsed / duration
                val expected = albumSmoothFraction(if (opening) clock else 1f - clock)
                for (source in sources) {
                    val frame = albumContainerFrame(source, 1264f, 2780f, motion.progress.value, smoothPacing = true)
                    val expansion = (frame.bounds.width - source.width) / (1264f - source.width)
                    assertEquals("All card sizes must follow Hero pacing at $elapsed ms", expected, expansion, 0.00001f)
                }
            }
            animation.cancelAndJoin()
        }
    }

    @Test fun reversingNormalHeroRetainsItsPositionAndReturnsToTheCover(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(true)
        val opening = launch(frames) { motion.animateTo(true, false) }
        repeat(10) { frames.tick() }
        opening.cancelAndJoin()
        val position = heroExpansion(motion)
        val closing = launch(frames) { motion.animateTo(false, false) }
        yield()
        assertEquals(position, heroExpansion(motion), 0f)
        withTimeout(3000) { repeat(60) { frames.tick() }; closing.join() }
        assertEquals(0f, heroExpansion(motion), 0f)
    }

    @Test fun classicModeMatchesTheSavedHistoricalSpringsInBothDirectionsAndSpeeds(): Unit = runBlocking {
        for (opening in listOf(true, false)) for (fast in listOf(false, true)) {
            val frames = Frames()
            val historicalFrames = Frames()
            val motion = AlbumTransitionMotion(opening, classic = true)
            val historical = Animatable(if (opening) 0f else 1f)
            val actualJob = launch(frames) { motion.animateTo(opening, fast) }
            val historicalJob = launch(historicalFrames) {
                historical.animateTo(if (opening) 1f else 0f, spring(
                    dampingRatio = if (fast) { if (opening) 0.84f else 0.86f } else { if (opening) 0.82f else 0.84f },
                    stiffness = if (fast) { if (opening) 620f else 680f } else { if (opening) 300f else 320f }
                ))
            }
            repeat(90) {
                frames.tick()
                historicalFrames.tick()
                assertEquals("Classic motion must follow the saved spring", historical.value, motion.progress.value, 0.0001f)
            }
            withTimeout(3000) { actualJob.join(); historicalJob.join() }
        }
    }

    @Test fun classicDirectionChangesRetainTheirCurrentPosition(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(true, classic = true)
        val opening = launch(frames) { motion.animateTo(true, false) }
        repeat(7) { frames.tick() }
        opening.cancelAndJoin()
        val interrupted = motion.progress.value
        assertTrue(interrupted > 0f && interrupted < 1f)
        val closing = launch(frames) { motion.animateTo(false, false) }
        yield()
        assertEquals(interrupted, motion.progress.value, 0f)
        withTimeout(3000) { repeat(90) { frames.tick() }; closing.join() }
        assertEquals(0f, motion.progress.value, 0f)
    }

    @Test fun enhancedPreservesThePreviousSettlingTimeInBothDirections(): Unit = runBlocking {
        println("Enhanced clocks: open=$EnhancedAlbumOpenMillis ms, close=$EnhancedAlbumCloseMillis ms")
        assertTrue(EnhancedAlbumOpenMillis < 400)
        assertTrue(EnhancedAlbumCloseMillis < 280)
        for (opening in listOf(true, false)) {
            val frames = Frames()
            val referenceFrames = Frames()
            val motion = AlbumTransitionMotion(opening)
            val reference = Animatable(if (opening) 0f else 1f)
            val actualJob = launch(frames) { motion.animateTo(opening, true) }
            val referenceJob = launch(referenceFrames) {
                reference.animateTo(if (opening) 1f else 0f,
                    if (opening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast,
                    initialVelocity = if (opening) 6f else -7f)
            }
            var actualEnd = 0
            var referenceEnd = 0
            repeat(90) { frame ->
                frames.tick(); referenceFrames.tick()
                if (!actualJob.isActive && actualEnd == 0) actualEnd = frame + 1
                if (!referenceJob.isActive && referenceEnd == 0) referenceEnd = frame + 1
            }
            withTimeout(3000) { actualJob.join(); referenceJob.join() }
            assertTrue("Enhanced's settling clock must stay within one frame of the prior version: $actualEnd/$referenceEnd",
                kotlin.math.abs(actualEnd - referenceEnd) <= 1)
        }
    }

    @Test fun enhancedReducesTheHeroLaunchAndPeakFrameJumpWithoutAnIdleTail(): Unit = runBlocking {
        for (opening in listOf(true, false)) {
            val frames = Frames()
            val previousFrames = Frames()
            val motion = AlbumTransitionMotion(opening)
            val previous = Animatable(if (opening) 0f else 1f)
            val job = launch(frames) { motion.animateTo(opening, true) }
            val previousJob = launch(previousFrames) {
                previous.animateTo(if (opening) 1f else 0f,
                    if (opening) MotionSpecs.AppleSpringFolderFast else MotionSpecs.AppleSpringFolderCloseFast,
                    initialVelocity = if (opening) 6f else -7f)
            }
            var last = if (opening) 0f else 1f
            var oldLast = last
            var peak = 0f
            var oldPeak = 0f
            repeat(60) { frame ->
                frames.tick(); previousFrames.tick()
                val position = heroExpansion(motion)
                val oldPosition = albumSmoothFraction(previous.value)
                val delta = kotlin.math.abs(position - last)
                if (frame == 1) assertTrue("Enhanced must ease away from its starting rectangle", delta < 0.015f)
                peak = maxOf(peak, delta)
                oldPeak = maxOf(oldPeak, kotlin.math.abs(oldPosition - oldLast))
                if (job.isActive && frame > 1) assertTrue("Visible motion must continue until settling", delta > 0f)
                assertTrue(if (opening) position >= last else position <= last)
                last = position
                oldLast = oldPosition
            }
            assertTrue("Peak Hero frame jump must drop substantially: $peak/$oldPeak", peak < oldPeak * 0.85f)
            withTimeout(3000) { job.join(); previousJob.join() }
        }
    }

    @Test fun freshTransitionsMovePromptlyWithoutJumpingTheirStartingBounds(): Unit = runBlocking {
        for (opening in listOf(true, false)) {
            val frames = Frames()
            val motion = AlbumTransitionMotion(opening)
            val animation = launch(frames) { motion.animateTo(opening, fast = false) }
            yield()
            frames.tick()
            assertEquals(if (opening) 0f else 1f, motion.progress.value, 0f)
            frames.tick()
            val distance = if (opening) motion.progress.value else 1f - motion.progress.value
            assertTrue("First advancing frame must respond visibly: $distance", distance > 0.02f)
            animation.cancelAndJoin()
        }
    }

    @Test fun normalModeGlidesLongerThanFastModeInBothDirections(): Unit = runBlocking {
        for (opening in listOf(true, false)) {
            val frames = Frames()
            val normal = AlbumTransitionMotion(opening)
            val fast = AlbumTransitionMotion(opening)
            val normalJob = launch(frames) { normal.animateTo(opening, false) }
            // Separate clocks give each animation the same elapsed time.
            val fastFrames = Frames()
            val fastJob = launch(fastFrames) { fast.animateTo(opening, true) }
            repeat(10) { frames.tick(); fastFrames.tick() }
            val normalTravel = if (opening) normal.progress.value else 1f - normal.progress.value
            val fastTravel = if (opening) fast.progress.value else 1f - fast.progress.value
            assertTrue(normalTravel < 0.95f)
            assertTrue(fastTravel > normalTravel)
            normalJob.cancelAndJoin()
            fastJob.cancelAndJoin()
        }
    }

    @Test fun normalOpeningUses400MillisecondsAndClosingUses280Milliseconds(): Unit = runBlocking {
        for (opening in listOf(true, false)) {
            val frames = Frames()
            val motion = AlbumTransitionMotion(opening)
            val animation = launch(frames) { motion.animateTo(opening, false) }
            repeat(if (opening) 24 else 17) { frames.tick() }
            assertTrue("The transition must still be settling before its requested duration", animation.isActive)
            repeat(2) { frames.tick() }
            withTimeout(3000) { animation.join() }
            assertEquals(if (opening) 1f else 0f, motion.progress.value, 0f)
        }
    }

    @Test fun reversingAnOpeningKeepsItsCurrentPositionAndReturnsToTheCover(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(true)
        val opening = launch(frames) { motion.animateTo(opening = true, fast = false) }
        repeat(7) { frames.tick() }
        opening.cancelAndJoin()
        val interrupted = motion.progress.value
        assertTrue(interrupted > 0f && interrupted < 1f)
        val closing = launch(frames) { motion.animateTo(opening = false, fast = false) }
        yield()
        assertEquals(interrupted, motion.progress.value, 0f)
        withTimeout(3000) { repeat(150) { frames.tick() }; closing.join() }
        assertEquals(0f, motion.progress.value, 0f)
    }

    @Test fun reversingAFastClosingDoesNotRestartAtTheCover(): Unit = runBlocking {
        val frames = Frames()
        val motion = AlbumTransitionMotion(false)
        val closing = launch(frames) { motion.animateTo(opening = false, fast = true) }
        repeat(5) { frames.tick() }
        closing.cancelAndJoin()
        val interrupted = motion.progress.value
        assertTrue(interrupted > 0f && interrupted < 1f)
        val opening = launch(frames) { motion.animateTo(opening = true, fast = true) }
        yield()
        assertEquals(interrupted, motion.progress.value, 0f)
        withTimeout(3000) { repeat(150) { frames.tick() }; opening.join() }
        assertEquals(1f, motion.progress.value, 0f)
    }
}
