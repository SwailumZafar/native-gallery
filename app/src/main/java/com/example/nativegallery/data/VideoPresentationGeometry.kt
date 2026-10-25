package com.example.nativegallery.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

internal data class VideoDisplayDimensions(val width: Int, val height: Int)

internal fun videoDisplayDimensions(width: Int, height: Int, rotation: Int, pixelRatio: Float = 1f): VideoDisplayDimensions? {
    if (width <= 0 || height <= 0) return null
    val ratio = pixelRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val displayWidth = (width * ratio).roundToInt().coerceAtLeast(1)
    val rotated = ((rotation % 360) + 360) % 360 in listOf(90, 270)
    return if (rotated) VideoDisplayDimensions(height, displayWidth) else VideoDisplayDimensions(displayWidth, height)
}

/** Small metadata reads run off the UI thread, coalesce per file and never hold opening indefinitely. */
internal object VideoPresentationGeometry {
    private val lock = Any()
    private val cache = LinkedHashMap<String, VideoDisplayDimensions>()
    private val pending = mutableMapOf<String, Deferred<VideoDisplayDimensions?>>()
    private val workers = Semaphore(2)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun key(item: MediaItem) = "${item.contentUri}:${item.fileSizeBytes}:${item.dateModifiedMillis}"

    fun record(item: MediaItem, dimensions: VideoDisplayDimensions) = synchronized(lock) {
        cache[key(item)] = dimensions
        while (cache.size > 128) cache.remove(cache.keys.first())
    }

    suspend fun resolve(context: Context, item: MediaItem): MediaItem {
        val uri = item.contentUri ?: return item
        val identity = key(item)
        val cached = synchronized(lock) { cache[identity] }
        if (cached != null) return item.copy(width = cached.width, height = cached.height)
        val task = synchronized(lock) {
            pending[identity] ?: if (pending.size >= 8) null else scope.async(start = CoroutineStart.LAZY) {
                workers.withPermit {
                    runCatching {
                        val reader = MediaMetadataRetriever()
                        try {
                            reader.setDataSource(context.applicationContext, uri)
                            videoDisplayDimensions(
                                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                            )?.also { record(item, it) }
                        } finally { reader.release() }
                    }.getOrNull()
                }
            }.also { request ->
                pending[identity] = request
                request.invokeOnCompletion { synchronized(lock) { if (pending[identity] === request) pending.remove(identity) } }
            }
        }
        // The independent bounded worker can finish caching metadata after this deadline.
        val dimensions = task?.let { withTimeoutOrNull(250L) { it.await() } } ?: return item
        return item.copy(width = dimensions.width, height = dimensions.height)
    }
}
