package com.example.nativegallery.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.nativegallery.ui.components.GalleryImageWorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One foreground player, with a short idle lease for rapid close/reopen gestures. */
@OptIn(UnstableApi::class)
internal class GalleryVideoPlayerStore : ViewModel() {
    data class Lease(val player: ExoPlayer, val generation: Long)
    private var player: ExoPlayer? = null
    private var generation = 0L
    private var cleanup: Job? = null

    fun acquire(context: Context): Lease {
        cleanup?.cancel()
        cleanup = null
        generation++
        val current = player ?: ExoPlayer.Builder(context.applicationContext,
            galleryVideoRenderersFactory(context.applicationContext))
            .setMediaSourceFactory(galleryVideoMediaSourceFactory(context.applicationContext))
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 1_500, 2_000)
                .setBackBuffer(0, false).setPrioritizeTimeOverSizeThresholds(true).build())
            .build().also { player = it }
        return Lease(current, generation)
    }

    fun park(lease: Lease) {
        if (lease.generation != generation || player !== lease.player) return
        lease.player.playWhenReady = false
        lease.player.pause()
        lease.player.volume = 0f
        cleanup?.cancel()
        cleanup = viewModelScope.launch {
            // Codec release can block. Never schedule it in the middle of the closing spring.
            GalleryImageWorkScheduler.awaitIdle()
            delay(2_000L)
            GalleryImageWorkScheduler.awaitIdle()
            if (lease.generation == generation && player === lease.player) {
                player = null
                lease.player.release()
            }
        }
    }

    override fun onCleared() {
        cleanup?.cancel()
        player?.release()
        player = null
    }
}
