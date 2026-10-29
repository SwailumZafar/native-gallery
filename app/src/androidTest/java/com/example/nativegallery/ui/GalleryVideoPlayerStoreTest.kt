package com.example.nativegallery.ui

import androidx.lifecycle.ViewModelStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.player.GalleryVideoPlayerStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(UnstableApi::class)
class GalleryVideoPlayerStoreTest {
    @Test fun rapidReopenReusesThePlayerAndStaleDisposalCannotMuteTheNewLease() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val models = ViewModelStore()
            val store = GalleryVideoPlayerStore()
            models.put("video", store)
            try {
                val first = store.acquire(instrumentation.targetContext)
                first.player.playWhenReady = true
                store.park(first)
                assertFalse(first.player.playWhenReady)
                assertEquals(0f, first.player.volume, 0.001f)
                val reopened = store.acquire(instrumentation.targetContext)
                assertSame(first.player, reopened.player)
                reopened.player.volume = 0.6f
                reopened.player.playWhenReady = true
                store.park(first)
                assertEquals(0.6f, reopened.player.volume, 0.001f)
                store.park(reopened)
                assertFalse(reopened.player.playWhenReady)
            } finally {
                models.clear()
            }
        }
    }
}
