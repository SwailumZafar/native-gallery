package com.example.nativegallery.data

import android.content.Context
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

/** Extracts a real video frame when the platform thumbnail service cannot decode a container. */
internal object VideoFrameDecoder {
    private const val DecoderTimeoutMillis = 4_000L
    private const val CodecPollMicros = 10_000L
    private val codecFallbackLock = Any()

    fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        retrieverFrame(context, uri, maxSide)?.let { return it }
        return synchronized(codecFallbackLock) {
            runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    val extractor = MediaExtractor()
                    try {
                        if (descriptor.declaredLength >= 0L) {
                            extractor.setDataSource(
                                descriptor.fileDescriptor,
                                descriptor.startOffset,
                                descriptor.declaredLength
                            )
                        } else {
                            extractor.setDataSource(descriptor.fileDescriptor)
                        }
                        decodeFirstFrame(extractor, maxSide)
                    } finally {
                        extractor.release()
                    }
                }
            }.getOrNull()

        }
    }

    fun decode(file: File, maxSide: Int): Bitmap? {
        retrieverFrame(file, maxSide)?.let { return it }
        return synchronized(codecFallbackLock) {
            runCatching {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    decodeFirstFrame(extractor, maxSide)
                } finally {
                    extractor.release()
                }
            }.getOrNull()

        }
    }

    private fun retrieverFrame(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.firstUsableFrame(maxSide)
        } catch (_: RuntimeException) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun retrieverFrame(file: File, maxSide: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.firstUsableFrame(maxSide)
        } catch (_: RuntimeException) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaMetadataRetriever.firstUsableFrame(maxSide: Int): Bitmap? {
        val candidateTimesUs = longArrayOf(0L, 1_000_000L)
        for (timeUs in candidateTimesUs) {
            val frame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                runCatching {
                    getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        maxSide,
                        maxSide
                    )
                }.getOrNull()
            } else {
                runCatching {
                    getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.scaledToFit(maxSide)
                }.getOrNull()
            }
            if (frame != null) return frame
        }
        return null
    }

    private fun decodeFirstFrame(extractor: MediaExtractor, maxSide: Int): Bitmap? {
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
        extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val decoder = MediaCodec.createDecoderByType(mimeType)
        return try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            decodeOutputFrame(decoder, extractor, maxSide, rotationDegrees)
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
        }
    }

    private fun decodeOutputFrame(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        maxSide: Int,
        rotationDegrees: Int
    ): Bitmap? {
        val bufferInfo = MediaCodec.BufferInfo()
        val deadline = SystemClock.elapsedRealtime() + DecoderTimeoutMillis
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
                    decoder.getOutputImage(outputIndex)?.use { image ->
                        image.toThumbnail(maxSide)
                    }
                }.getOrNull()
                decoder.releaseOutputBuffer(outputIndex, false)
                if (frame != null) return frame.rotated(rotationDegrees)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return null
            }
        }
        return null
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
}