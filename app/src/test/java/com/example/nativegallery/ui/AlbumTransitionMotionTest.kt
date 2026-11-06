package com.example.nativegallery.ui

import androidx.compose.runtime.MonotonicFrameClock
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
            assertTrue(fastTravel > 0.95f)
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
