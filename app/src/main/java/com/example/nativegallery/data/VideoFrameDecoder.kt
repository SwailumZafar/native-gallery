package com.example.nativegallery.data

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Extracts a useful video poster when the platform thumbnail service cannot decode one. */
internal object VideoFrameDecoder {
    private const val DecoderTimeoutMillis = 4_000L
    private const val CodecPollMicros = 10_000L
    private val codecFallbackLock = Any()

    fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        retrieverFrame(context, uri, maxSide)?.let { return it }
        return synchronized(codecFallbackLock) {
            decodeCodecFrames(context, uri, maxSide)
        }
    }

    fun decode(file: File, maxSide: Int): Bitmap? {
        retrieverFrame(file, maxSide)?.let { return it }
        return synchronized(codecFallbackLock) {
            decodeCodecFrames(file, maxSide)
        }
    }

    private fun retrieverFrame(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val descriptorFrame = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                retrieverFrame(maxSide) { retriever ->
                    if (descriptor.declaredLength >= 0L) {
                        retriever.setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.declaredLength
                        )
                    } else {
                        retriever.setDataSource(descriptor.fileDescriptor)
                    }
                }
            }
        }.getOrNull()
        if (descriptorFrame != null) return descriptorFrame
        return retrieverFrame(maxSide) { retriever -> retriever.setDataSource(context, uri) }
    }

    private fun retrieverFrame(file: File, maxSide: Int): Bitmap? {
        return retrieverFrame(maxSide) { retriever -> retriever.setDataSource(file.absolutePath) }
    }

    private inline fun retrieverFrame(
        maxSide: Int,
        configure: (MediaMetadataRetriever) -> Unit
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            configure(retriever)
            retriever.firstUsableFrame(maxSide)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaMetadataRetriever.firstUsableFrame(maxSide: Int): Bitmap? {
        val durationMs = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: 0L
        val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: maxSide
        val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: maxSide
        val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0
        val orientedWidth = if (rotation % 180 == 0) width else height
        val orientedHeight = if (rotation % 180 == 0) height else width
        val scale = (maxSide.toFloat() / max(orientedWidth, orientedHeight)).coerceAtMost(1f)
        val targetWidth = (orientedWidth * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (orientedHeight * scale).roundToInt().coerceAtLeast(1)

        var bestCandidate: Bitmap? = null
        var bestLuminance = -1.0

        for (timeUs in videoThumbnailCandidateTimesUs(durationMs)) {
            val frameProviders = buildList<() -> Bitmap?> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1 && timeUs >= 0L) {
                    add({ runCatching {
                        getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetWidth,
                            targetHeight
                        )
                    }.getOrNull() })
                    add({ runCatching {
                        getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                            targetWidth,
                            targetHeight
                        )
                    }.getOrNull() })
                }
                add({ runCatching {
                    if (timeUs >= 0L) {
                        getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } else {
                        getFrameAtTime()
                    }
                }.getOrNull() })
                if (timeUs >= 0L) {
                    add({ runCatching {
                        getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    }.getOrNull() })
                }
            }
            for (frameProvider in frameProviders) {
                val candidate = frameProvider() ?: continue
                if (!isFrameTooDark(candidate)) {
                    bestCandidate?.recycle()
                    return candidate.scaledToFit(maxSide)
                }
                val lum = frameAverageLuminance(candidate)
                if (lum > bestLuminance) {
                    bestCandidate?.recycle()
                    bestCandidate = candidate
                    bestLuminance = lum
                } else {
                    candidate.recycle()
                }
            }
        }
        if (bestCandidate != null) {
            return bestCandidate.scaledToFit(maxSide)
        }
        return null
    }

    private fun decodeCodecFrames(context: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            val durationMs = extractorDurationMs { extractor -> extractor.setDataSource(descriptor) }
            val deadline = SystemClock.elapsedRealtime() + DecoderTimeoutMillis
            var bestFrame: Bitmap? = null
            var bestLuminance = -1.0
            for (timeUs in videoThumbnailCandidateTimesUs(durationMs).filter { it >= 0L }.take(4)) {
                if (SystemClock.elapsedRealtime() >= deadline) break
                val frame = decodeCodecFrame(maxSide, timeUs, deadline) { extractor ->
                    extractor.setDataSource(descriptor)
                } ?: continue
                if (!isFrameTooDark(frame)) {
                    bestFrame?.recycle()
                    return@use frame
                }
                val lum = frameAverageLuminance(frame)
                if (lum > bestLuminance) {
                    bestFrame?.recycle()
                    bestFrame = frame
                    bestLuminance = lum
                } else {
                    frame.recycle()
                }
            }
            bestFrame
        }
    }.getOrNull()

    private fun decodeCodecFrames(file: File, maxSide: Int): Bitmap? = runCatching {
        val durationMs = extractorDurationMs { extractor -> extractor.setDataSource(file.absolutePath) }
        val deadline = SystemClock.elapsedRealtime() + DecoderTimeoutMillis
        var bestFrame: Bitmap? = null
        var bestLuminance = -1.0
        for (timeUs in videoThumbnailCandidateTimesUs(durationMs).filter { it >= 0L }.take(4)) {
            if (SystemClock.elapsedRealtime() >= deadline) break
            val frame = decodeCodecFrame(maxSide, timeUs, deadline) { extractor ->
                extractor.setDataSource(file.absolutePath)
            } ?: continue
            if (!isFrameTooDark(frame)) {
                bestFrame?.recycle()
                return@runCatching frame
            }
            val lum = frameAverageLuminance(frame)
            if (lum > bestLuminance) {
                bestFrame?.recycle()
                bestFrame = frame
                bestLuminance = lum
            } else {
                frame.recycle()
            }
        }
        bestFrame
    }.getOrNull()

    private inline fun extractorDurationMs(configure: (MediaExtractor) -> Unit): Long {
        val extractor = MediaExtractor()
        return try {
            configure(extractor)
            (0 until extractor.trackCount)
                .asSequence()
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { format ->
                    format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                }
                ?.longOrNull(MediaFormat.KEY_DURATION)
                ?.div(1_000L)
                ?.coerceAtLeast(0L)
                ?: 0L
        } finally {
            extractor.release()
        }
    }

    private inline fun decodeCodecFrame(
        maxSide: Int,
        timeUs: Long,
        deadline: Long,
        configure: (MediaExtractor) -> Unit
    ): Bitmap? {
        val extractor = MediaExtractor()
        return try {
            configure(extractor)
            decodeFirstFrame(extractor, maxSide, timeUs.coerceAtLeast(0L), deadline)
        } finally {
            extractor.release()
        }
    }

    private fun MediaExtractor.setDataSource(descriptor: AssetFileDescriptor) {
        if (descriptor.declaredLength >= 0L) {
            setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.declaredLength)
        } else {
            setDataSource(descriptor.fileDescriptor)
        }
    }

    private fun decodeFirstFrame(extractor: MediaExtractor, maxSide: Int, timeUs: Long, deadline: Long): Bitmap? {
        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("video/") == true
        } ?: return null
        val format = extractor.getTrackFormat(trackIndex)
        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return null
        val rotationDegrees = format.integerOrNull(MediaFormat.KEY_ROTATION) ?: 0
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        extractor.selectTrack(trackIndex)
        extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val decoder = MediaCodec.createDecoderByType(mimeType)
        return try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            decodeOutputFrame(decoder, extractor, maxSide, rotationDegrees, deadline)
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
        }
    }

    private fun decodeOutputFrame(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        maxSide: Int,
        rotationDegrees: Int,
        deadline: Long
    ): Bitmap? {
        val bufferInfo = MediaCodec.BufferInfo()
        var inputEnded = false
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!inputEnded) {
                val inputIndex = decoder.dequeueInputBuffer(CodecPollMicros)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex) ?: return null
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEnded = true
                    } else {
                        decoder.queueInputBuffer(
                            inputIndex,
                            0,
                            sampleSize,
                            extractor.sampleTime.coerceAtLeast(0L),
                            0
                        )
                        extractor.advance()
                    }
                }
            }

            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, CodecPollMicros)
            if (outputIndex >= 0) {
                val frame = runCatching {
                    decoder.getOutputImage(outputIndex)?.use { image -> image.toThumbnail(maxSide) }
                }.getOrNull()
                decoder.releaseOutputBuffer(outputIndex, false)
                if (frame != null) return frame.rotated(rotationDegrees)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return null
            }
        }
        return null
    }

    internal fun frameAverageLuminance(bitmap: Bitmap): Double {
        if (bitmap.width <= 0 || bitmap.height <= 0) return 0.0
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            val readable = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return 0.0
            return try { frameAverageLuminance(readable) } finally { readable.recycle() }
        }
        var luminanceSum = 0.0
        val sampleCount = 25
        for (row in 1..5) {
            for (column in 1..5) {
                val x = (bitmap.width * column / 6).coerceIn(0, bitmap.width - 1)
                val y = (bitmap.height * row / 6).coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                luminanceSum += 0.299 * red + 0.587 * green + 0.114 * blue
            }
        }
        return luminanceSum / sampleCount
    }

    internal fun isFrameTooDark(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 0 || bitmap.height <= 0) return true
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            val readable = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
            return try { isFrameTooDark(readable) } finally { readable.recycle() }
        }
        var darkSamples = 0
        var luminanceSum = 0.0
        val sampleCount = 25
        for (row in 1..5) {
            for (column in 1..5) {
                val x = (bitmap.width * column / 6).coerceIn(0, bitmap.width - 1)
                val y = (bitmap.height * row / 6).coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
                luminanceSum += luminance
                if (luminance < 24.0) darkSamples += 1
            }
        }
        return darkSamples == sampleCount && luminanceSum / sampleCount < 8.0
    }

    private fun Image.toThumbnail(maxSide: Int): Bitmap? {
        if (planes.size < 3 || maxSide <= 0) return null
        val crop = cropRect
        val sourceWidth = crop.width()
        val sourceHeight = crop.height()
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val scale = (maxSide.toFloat() / max(sourceWidth, sourceHeight)).coerceAtMost(1f)
        val targetWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
        val pixels = IntArray(targetWidth * targetHeight)
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()

        for (targetY in 0 until targetHeight) {
            val sourceY = crop.top + (targetY * sourceHeight / targetHeight)
            for (targetX in 0 until targetWidth) {
                val sourceX = crop.left + (targetX * sourceWidth / targetWidth)
                val yValue = yBuffer.sample(yPlane.rowStride, yPlane.pixelStride, sourceX, sourceY)
                val uValue = uBuffer.sample(uPlane.rowStride, uPlane.pixelStride, sourceX / 2, sourceY / 2)
                val vValue = vBuffer.sample(vPlane.rowStride, vPlane.pixelStride, sourceX / 2, sourceY / 2)
                pixels[targetY * targetWidth + targetX] = yuvToArgb(yValue, uValue, vValue)
            }
        }
        return Bitmap.createBitmap(pixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    }

    private fun java.nio.ByteBuffer.sample(
        rowStride: Int,
        pixelStride: Int,
        x: Int,
        y: Int
    ): Int {
        if (limit() <= 0) return 0
        val index = (y * rowStride + x * pixelStride).coerceIn(0, limit() - 1)
        return get(index).toInt() and 0xFF
    }

    private fun yuvToArgb(yValue: Int, uValue: Int, vValue: Int): Int {
        val y = (yValue - 16).coerceAtLeast(0)
        val u = uValue - 128
        val v = vValue - 128
        val red = ((298 * y + 409 * v + 128) shr 8).coerceIn(0, 255)
        val green = ((298 * y - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255)
        val blue = ((298 * y + 516 * u + 128) shr 8).coerceIn(0, 255)
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun Bitmap.rotated(rotationDegrees: Int): Bitmap {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        if (normalized == 0) return this
        val rotated = Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            Matrix().apply { postRotate(normalized.toFloat()) },
            true
        )
        if (rotated !== this) recycle()
        return rotated
    }

    private fun Bitmap.scaledToFit(maxSide: Int): Bitmap {
        val scale = (maxSide.toFloat() / max(width, height)).coerceAtMost(1f)
        if (scale >= 1f) return this
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true
        )
        if (scaled !== this) recycle()
        return scaled
    }

    private fun MediaFormat.integerOrNull(key: String): Int? {
        return if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null
    }

    private fun MediaFormat.longOrNull(key: String): Long? {
        return if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null
    }
}

/** Candidate order avoids black intros while preserving genuinely short clips. */
internal fun videoThumbnailCandidateTimesUs(durationMs: Long): LongArray {
    val durationUs = durationMs.coerceIn(0L, Long.MAX_VALUE / 1_000L) * 1_000L
    val candidates = when {
        durationUs > 10_000_000L -> longArrayOf(
            durationUs / 4,
            durationUs / 5 * 2,
            durationUs / 20 * 3,
            durationUs / 2,
            minOf(durationUs / 10, 15_000_000L),
            5_000_000L,
            2_000_000L,
            -1L,
            0L
        )

        durationUs > 2_000_000L -> longArrayOf(
            durationUs / 2,
            durationUs / 4,
            1_000_000L,
            -1L,
            0L
        )

        durationUs > 0L -> longArrayOf(
            durationUs / 2,
            durationUs / 4,
            (durationUs - 1L).coerceAtLeast(0L),
            -1L,
            0L
        )

        else -> longArrayOf(15_000_000L, 10_000_000L, 5_000_000L, 2_000_000L, 1_000_000L, -1L, 0L)
    }
    return candidates.distinct().toLongArray()
}
