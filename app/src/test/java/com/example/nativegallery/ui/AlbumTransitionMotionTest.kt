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
