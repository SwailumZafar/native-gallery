package com.example.nativegallery.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/** Recovers video durations when MediaStore or the platform extractor returns no usable value. */
internal object VideoDurationResolver {
    private const val AviHeaderBytes = 64 * 1024
    private const val FlvHeaderBytes = 256 * 1024
    private const val MkvHeaderBytes = 256 * 1024

    fun resolve(
        context: Context,
        uri: Uri,
        displayName: String?,
        mimeType: String?
    ): Long {
        val normalizedName = displayName.orEmpty().lowercase(Locale.US)
        val normalizedMime = mimeType.orEmpty().lowercase(Locale.US)
        val formatDuration = when {
            normalizedName.endsWith(".avi") || normalizedMime.contains("avi") ->
                readPrefix(context, uri, AviHeaderBytes, ::parseAviDurationMillis)

            normalizedName.endsWith(".flv") || normalizedMime.contains("flv") ->
                readPrefix(context, uri, FlvHeaderBytes, ::parseFlvDurationMillis)

            normalizedName.endsWith(".mkv") || normalizedMime.contains("matroska") ->
                readPrefix(context, uri, MkvHeaderBytes, ::parseMkvDurationMillis)

            else -> 0L
        }
        if (formatDuration > 0L) return formatDuration

        retrieverDuration(context, uri).takeIf { it > 0L }?.let { return it }

        // Unknown or mislabeled local files are cheap to signature-check. Each parser rejects
        // unrelated data before interpreting fields, so this does not invent a duration.
        readPrefix(context, uri, AviHeaderBytes, ::parseAviDurationMillis)
            .takeIf { it > 0L }
            ?.let { return it }
        readPrefix(context, uri, FlvHeaderBytes, ::parseFlvDurationMillis)
            .takeIf { it > 0L }
            ?.let { return it }
        return readPrefix(context, uri, MkvHeaderBytes, ::parseMkvDurationMillis)
    }

    private fun readPrefix(
        context: Context,
        uri: Uri,
        limit: Int,
        parser: (ByteArray, Int) -> Long
    ): Long = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = ByteArray(limit)
            var count = 0
            while (count < bytes.size) {
                val read = stream.read(bytes, count, bytes.size - count)
                if (read <= 0) break
                count += read
            }
            parser(bytes, count)
        } ?: 0L
    }.getOrDefault(0L)

    private fun retrieverDuration(context: Context, uri: Uri): Long {
        fun read(configure: (MediaMetadataRetriever) -> Unit): Long {
            val retriever = MediaMetadataRetriever()
            return try {
                configure(retriever)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?: 0L
            } catch (_: Throwable) {
                0L
            } finally {
                runCatching { retriever.release() }
            }
        }

        val descriptorDuration = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                read { retriever ->
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
            } ?: 0L
        }.getOrDefault(0L)
        if (descriptorDuration > 0L) return descriptorDuration
        return read { retriever -> retriever.setDataSource(context, uri) }
    }
}

/** Small persistent cache keyed by both URI and the MediaStore row fingerprint. */
internal object VideoDurationCache {
    private const val PreferencesName = "native_gallery_video_durations_v1"
    private val memory = ConcurrentHashMap<String, Long>()

    fun identity(uri: Uri, mediaId: String, fileSizeBytes: Long?, revisionMillis: Long): String {
        return "$uri|$mediaId|${fileSizeBytes ?: -1L}|$revisionMillis"
    }

    fun get(context: Context, identity: String): Long {
        memory[identity]?.takeIf { it > 0L }?.let { return it }
        val persisted = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getLong(identity, 0L)
        if (persisted > 0L) memory[identity] = persisted
        return persisted.coerceAtLeast(0L)
    }

    fun put(context: Context, identity: String, durationMs: Long) {
        if (durationMs <= 0L) return
        memory[identity] = durationMs
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putLong(identity, durationMs)
            .apply()
    }
}

internal fun parseAviDurationMillis(bytes: ByteArray, length: Int = bytes.size): Long {
    val safeLength = length.coerceIn(0, bytes.size)
    if (safeLength < 32 || !bytes.matchesAscii(0, "RIFF") || !bytes.matchesAscii(8, "AVI ")) {
        return 0L
    }

    var bestDurationMs = 0L
    var index = 12
    while (index <= safeLength - 8) {
        when {
            bytes.matchesAscii(index, "avih") && index + 28 <= safeLength -> {
                val microsecondsPerFrame = bytes.unsignedLittleEndianInt(index + 8)
                val totalFrames = bytes.unsignedLittleEndianInt(index + 24)
                if (microsecondsPerFrame > 0L && totalFrames > 0L) {
                    bestDurationMs = maxOf(bestDurationMs, safeDurationMillis(totalFrames.toDouble() * microsecondsPerFrame / 1_000.0))
                }
            }

            bytes.matchesAscii(index, "strh") && index + 44 <= safeLength -> {
                val scale = bytes.unsignedLittleEndianInt(index + 28)
                val rate = bytes.unsignedLittleEndianInt(index + 32)
                val streamLength = bytes.unsignedLittleEndianInt(index + 40)
                if (scale > 0L && rate > 0L && streamLength > 0L) {
                    bestDurationMs = maxOf(bestDurationMs, safeDurationMillis(streamLength.toDouble() * scale * 1_000.0 / rate))
                }
            }
        }
        index += 1
    }
    return bestDurationMs.coerceAtLeast(0L)
}

internal fun parseFlvDurationMillis(bytes: ByteArray, length: Int = bytes.size): Long {
    val safeLength = length.coerceIn(0, bytes.size)
    if (safeLength < 18 || !bytes.matchesAscii(0, "FLV")) return 0L
    val durationKey = "duration".toByteArray(Charsets.US_ASCII)
    for (index in 0..safeLength - durationKey.size - 9) {
        var matches = true
        durationKey.indices.forEach { offset ->
            if (bytes[index + offset] != durationKey[offset]) matches = false
        }
        if (!matches || bytes[index + durationKey.size].toInt() != 0) continue
        val valueOffset = index + durationKey.size + 1
        val durationSeconds = ByteBuffer.wrap(bytes, valueOffset, 8)
            .order(ByteOrder.BIG_ENDIAN)
            .double
        if (durationSeconds.isFinite() && durationSeconds > 0.0) {
            return safeDurationMillis(durationSeconds * 1_000.0)
        }
    }
    return 0L
}

internal fun parseMkvDurationMillis(bytes: ByteArray, length: Int = bytes.size): Long {
    val safeLength = length.coerceIn(0, bytes.size)
    if (safeLength < 32 ||
        bytes[0] != 0x1A.toByte() || bytes[1] != 0x45.toByte() ||
        bytes[2] != 0xDF.toByte() || bytes[3] != 0xA3.toByte()
    ) return 0L

    val infoIndex = bytes.indexOfSequence(byteArrayOf(0x15, 0x49, 0xA9.toByte(), 0x66), safeLength)
    val searchStart = infoIndex.coerceAtLeast(0)
    val searchEnd = minOf(safeLength, searchStart + 16 * 1024)
    var timecodeScaleNs = 1_000_000L

    val scaleIndex = bytes.indexOfSequence(
        byteArrayOf(0x2A, 0xD7.toByte(), 0xB1.toByte()),
        safeLength,
        searchStart,
        searchEnd
    )
    if (scaleIndex >= 0) {
        val size = bytes.ebmlOneByteSize(scaleIndex + 3, safeLength)
        val valueOffset = scaleIndex + 4
        if (size in 1..8 && valueOffset + size <= safeLength) {
            timecodeScaleNs = bytes.unsignedBigEndian(valueOffset, size).takeIf { it > 0L }
                ?: timecodeScaleNs
        }
    }

    val durationIndex = bytes.indexOfSequence(
        byteArrayOf(0x44, 0x89.toByte()),
        safeLength,
        searchStart,
        searchEnd
    )
    if (durationIndex < 0) return 0L
    val size = bytes.ebmlOneByteSize(durationIndex + 2, safeLength)
    val valueOffset = durationIndex + 3
    val durationUnits = when {
        size == 4 && valueOffset + 4 <= safeLength -> {
            val bits = bytes.unsignedBigEndian(valueOffset, 4).toInt()
            Float.fromBits(bits).toDouble()
        }

        size == 8 && valueOffset + 8 <= safeLength -> {
            Double.fromBits(bytes.unsignedBigEndian(valueOffset, 8))
        }

        else -> return 0L
    }
    if (!durationUnits.isFinite() || durationUnits <= 0.0) return 0L
    return safeDurationMillis(durationUnits * timecodeScaleNs.toDouble() / 1_000_000.0)
}

private fun safeDurationMillis(value: Double): Long =
    if (value.isFinite() && value > 0.0 && value < Long.MAX_VALUE.toDouble() / 1_000.0) {
        value.roundToLong().coerceAtLeast(1L)
    } else 0L

private fun ByteArray.matchesAscii(offset: Int, value: String): Boolean {
    if (offset < 0 || offset + value.length > size) return false
    return value.indices.all { index -> this[offset + index] == value[index].code.toByte() }
}

private fun ByteArray.unsignedLittleEndianInt(offset: Int): Long {
    if (offset < 0 || offset + 4 > size) return 0L
    return (this[offset].toLong() and 0xFFL) or
        ((this[offset + 1].toLong() and 0xFFL) shl 8) or
        ((this[offset + 2].toLong() and 0xFFL) shl 16) or
        ((this[offset + 3].toLong() and 0xFFL) shl 24)
}

private fun ByteArray.unsignedBigEndian(offset: Int, count: Int): Long {
    var result = 0L
    repeat(count) { index -> result = (result shl 8) or (this[offset + index].toLong() and 0xFFL) }
    return result
}

private fun ByteArray.ebmlOneByteSize(offset: Int, length: Int): Int {
    if (offset !in 0 until length) return -1
    val first = this[offset].toInt() and 0xFF
    return if (first and 0x80 != 0) first and 0x7F else -1
}

private fun ByteArray.indexOfSequence(
    sequence: ByteArray,
    length: Int,
    start: Int = 0,
    endExclusive: Int = length
): Int {
    val safeEnd = minOf(length, size, endExclusive)
    if (sequence.isEmpty() || start < 0 || safeEnd - start < sequence.size) return -1
    for (index in start..safeEnd - sequence.size) {
        if (sequence.indices.all { offset -> this[index + offset] == sequence[offset] }) return index
    }
    return -1
}
