package com.example.nativegallery.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.TextureView
import android.view.Choreographer
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.nativegallery.data.VideoFrameDecoder
import com.example.nativegallery.data.videoThumbnailCandidateTimesUs
import com.example.nativegallery.player.galleryVideoMediaSourceFactory
import com.example.nativegallery.player.galleryVideoRenderersFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** A last resort for containers supported by Media3 but not Android's metadata retriever. */
@OptIn(UnstableApi::class)
internal object VideoThumbnailDecoder {
    private val texture = MutableStateFlow<TextureView?>(null)
    private val foreground = MutableStateFlow(false)
    private val interactionBlocked = MutableStateFlow(true)
    private val decoderMutex = Mutex()
    private val readiness = combine(texture, foreground, interactionBlocked) { view, resumed, blocked ->
        view?.takeIf { resumed && !blocked && it.isAvailable }
    }

    fun setInteractionBlocked(blocked: Boolean) { interactionBlocked.value = blocked }
    fun setForeground(resumed: Boolean) { foreground.value = resumed }
    fun attach(view: TextureView) { if (view.isAvailable) texture.value = view }
    fun detach(view: TextureView) { if (texture.value === view) texture.value = null }

    suspend fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap? = withContext(Dispatchers.Main.immediate) {
        decoderMutex.withLock {
            // Releasing a decoder during a transition is immediate. The same visible request
            // waits and retries after the viewer/transition finishes instead of competing for it.
            while (true) {
                // A held scroll/viewer can outlast a timeout. Keep the visible request pending
                // until codecs are allowed again; disposing the tile still cancels this wait.
                val view = readiness.first { it != null } ?: continue
                val result = coroutineScope {
                    val rendering = async {
                        withTimeoutOrNull(2_500L) { render(context, uri, view, maxSide) }
                    }
                    val pauseWatcher = launch {
                        readiness.first { it !== view }
                        rendering.cancel(ThumbnailDecodePaused())
                    }
                    try {
                        rendering.await() to false
                    } catch (_: ThumbnailDecodePaused) {
                        null to true
                    } finally {
                        pauseWatcher.cancel()
                    }
                }
                if (!result.second) return@withLock result.first
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
    }

    private suspend fun render(context: Context, uri: Uri, view: TextureView, maxSide: Int): Bitmap? {
        val player = ExoPlayer.Builder(context, galleryVideoRenderersFactory(context))
            .setMediaSourceFactory(galleryVideoMediaSourceFactory(context))
            .build()
        return try {
            player.volume = 0f
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            player.setVideoTextureView(view)
            if (!awaitFrame(player, timeoutMs = 1_200L) {
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.playWhenReady = true
                    player.prepare()
                }
            ) return null

            val durationMs = player.duration.takeIf { it > 0L } ?: 0L
            val candidates = videoThumbnailCandidateTimesUs(durationMs)
                .filter { it >= 0L }.distinct().take(4)
            var bestFrame: Bitmap? = null
            var bestLuminance = -1.0
            for ((index, positionUs) in (listOf(0L) + candidates).distinct().withIndex()) {
                if (index > 0 && !awaitFrame(player) { player.seekTo(positionUs / 1_000L) }) continue
                repeat(2) { awaitDisplayFrame() }
                if (!view.isAvailable) break
                val videoSize = player.videoSize
                val width = videoSize.width.coerceAtLeast(1)
                val height = videoSize.height.coerceAtLeast(1)
                val scale = (maxSide.toFloat() / maxOf(width, height)).coerceAtMost(1f)
                val frame = view.getBitmap(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1)
                ) ?: continue
                if (!VideoFrameDecoder.isFrameTooDark(frame)) {
                    bestFrame?.recycle()
                    return frame
                }
                val lum = VideoFrameDecoder.frameAverageLuminance(frame)
                if (lum > bestLuminance) {
                    bestFrame?.recycle()
                    bestFrame = frame
                    bestLuminance = lum
                } else {
                    frame.recycle()
                }
            }
            bestFrame
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            player.clearVideoTextureView(view)
            player.release()
        }
    }

    private suspend fun awaitFrame(player: ExoPlayer, timeoutMs: Long = 1_000L, action: () -> Unit): Boolean {
        var listener: Player.Listener? = null
        return try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    listener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            if (continuation.isActive) continuation.resume(true)
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                    player.addListener(requireNotNull(listener))
                    action()
                }
            } ?: false
        } finally {
            listener?.let(player::removeListener)
        }
    }

    private class ThumbnailDecodePaused : CancellationException()

    private suspend fun awaitDisplayFrame() = suspendCancellableCoroutine<Unit> { continuation ->
        val choreographer = Choreographer.getInstance()
        val callback = Choreographer.FrameCallback {
            if (continuation.isActive) continuation.resume(Unit)
        }
        choreographer.postFrameCallback(callback)
        continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }
}

/** Kept beneath the opaque Gallery surface, but attached so SurfaceTexture can really render. */
@Composable
internal fun VideoThumbnailDecoderHost(themeChanging: Boolean = false) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, themeChanging) {
        if (themeChanging) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            VideoThumbnailDecoder.setForeground(true)
            try { awaitCancellation() } finally { VideoThumbnailDecoder.setForeground(false) }
        }
    }
    DisposableEffect(Unit) {
        onDispose { VideoThumbnailDecoder.setForeground(false) }
    }
    AndroidView(
        modifier = Modifier.size(192.dp, 108.dp),
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        VideoThumbnailDecoder.attach(this@apply)
                    }
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        VideoThumbnailDecoder.detach(this@apply)
                        return true
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
        onRelease = { VideoThumbnailDecoder.detach(it) }
    )
}
