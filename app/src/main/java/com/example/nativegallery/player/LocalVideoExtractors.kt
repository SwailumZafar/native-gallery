package com.example.nativegallery.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.flv.FlvExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import java.io.InterruptedIOException

/** Adds local-file seek fallbacks without replacing a healthy extractor-provided index. */
@UnstableApi
internal fun localVideoExtractorsFactory(): ExtractorsFactory {
    val defaults = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
    return ExtractorsFactory {
        arrayOf(
            IndexedFlvExtractor(),
            IndexedMatroskaExtractor(),
            *defaults.createExtractors()
        )
    }
}

/** Builds a keyframe index only after Media3 reports that an FLV is unseekable. */
@UnstableApi
internal class IndexedFlvExtractor(private val delegate: Extractor = FlvExtractor()) : Extractor {
    private var extractorOutput: ExtractorOutput? = null
    private var fallbackRequested = false
    private var fallbackAttempted = false
    private var delegateSeekMap: SeekMap? = null
    private var fallbackSeekMap: SeekMap? = null

    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun init(output: ExtractorOutput) {
        extractorOutput = output
        delegate.init(object : ExtractorOutput {
            override fun track(id: Int, type: Int): TrackOutput = output.track(id, type)
            override fun endTracks() = output.endTracks()
            override fun seekMap(map: SeekMap) {
                delegateSeekMap = map
                if (map.isSeekable) {
                    fallbackRequested = false
                    output.seekMap(map)
                } else {
                    if (!fallbackAttempted) fallbackRequested = true
                    output.seekMap(fallbackSeekMap ?: map)
                }
            }
        })
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (fallbackRequested && !fallbackAttempted) {
            if (input.length == C.LENGTH_UNSET.toLong()) {
                fallbackAttempted = true
                fallbackRequested = false
            } else if (input.position != 0L) {
                seekPosition.position = 0L
                return Extractor.RESULT_SEEK
            } else {
                fallbackAttempted = true
                fallbackRequested = false
                val fallbackMap = scanOrNull { scanIndex(input) }
                fallbackSeekMap = fallbackMap
                if (fallbackMap != null) extractorOutput?.seekMap(fallbackMap)
                else delegateSeekMap?.let { extractorOutput?.seekMap(it) }
                delegate.seek(0L, 0L)
                if (input.position != 0L) {
                    seekPosition.position = 0L
                    return Extractor.RESULT_SEEK
                }
            }
        }
        return delegate.read(input, seekPosition)
    }

    override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)
    override fun release() = delegate.release()

    private fun scanIndex(input: ExtractorInput): SeekMap? {
        val header = ByteArray(11)
        if (!input.readFully(header, 0, 9, true) ||
            header[0] != 'F'.code.toByte() ||
            header[1] != 'L'.code.toByte() ||
            header[2] != 'V'.code.toByte()
        ) return null

        val dataOffset = unsigned32(header, 5)
        if (dataOffset !in 9L..1_048_576L) return null
        input.skipFully((dataOffset - 9L).toInt() + 4)

        val timesUs = ArrayList<Long>()
        val positions = ArrayList<Long>()
        var lastTimestampUs = 0L
        while (input.position + 11L <= input.length) {
            val tagPosition = input.position
            if (!input.readFully(header, 0, 11, true)) break
            val dataSize = unsigned24(header, 1)
            val timestampUs = (
                unsigned24(header, 4).toLong() or
                    ((header[7].toLong() and 0xFFL) shl 24)
                ) * 1_000L
            if (dataSize < 0 || input.position + dataSize + 4L > input.length) break
            lastTimestampUs = maxOf(lastTimestampUs, timestampUs)

            var consumed = 0
            if ((header[0].toInt() and 0xFF) == 9 && dataSize > 0) {
                consumed = minOf(2, dataSize)
                input.readFully(header, 0, consumed)
                val frameType = (header[0].toInt() and 0xF0) ushr 4
                val codec = header[0].toInt() and 0x0F
                val isVideoFrame = codec != 7 || consumed < 2 || header[1].toInt() == 1
                if (frameType == 1 && isVideoFrame &&
                    (timesUs.isEmpty() || timestampUs - timesUs.last() >= 500_000L)
                ) {
                    timesUs += timestampUs
                    positions += tagPosition
                }
            }
            input.skipFully(dataSize - consumed + 4)
        }
        if (timesUs.isEmpty() || lastTimestampUs <= 0L) return null

        val timeArray = timesUs.toLongArray()
        val positionArray = positions.toLongArray()
        val durationUs = lastTimestampUs + 40_000L
        return binarySearchSeekMap(timeArray, positionArray, durationUs)
    }

    private fun unsigned24(bytes: ByteArray, start: Int): Int =
        ((bytes[start].toInt() and 0xFF) shl 16) or
            ((bytes[start + 1].toInt() and 0xFF) shl 8) or
            (bytes[start + 2].toInt() and 0xFF)

    private fun unsigned32(bytes: ByteArray, start: Int): Long =
        ((bytes[start].toLong() and 0xFFL) shl 24) or
            ((bytes[start + 1].toLong() and 0xFFL) shl 16) or
            ((bytes[start + 2].toLong() and 0xFFL) shl 8) or
            (bytes[start + 3].toLong() and 0xFFL)
}

/** Builds a cluster map only after Media3 reports that a Matroska file is unseekable. */
@UnstableApi
internal class IndexedMatroskaExtractor(
    private val delegate: Extractor = MatroskaExtractor(DefaultSubtitleParserFactory())
) : Extractor {
    private var extractorOutput: ExtractorOutput? = null
    private var fallbackRequested = false
    private var fallbackAttempted = false
    private var delegateSeekMap: SeekMap? = null
    private var fallbackSeekMap: SeekMap? = null

    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun init(output: ExtractorOutput) {
        extractorOutput = output
        delegate.init(object : ExtractorOutput {
            override fun track(id: Int, type: Int): TrackOutput = output.track(id, type)
            override fun endTracks() = output.endTracks()
            override fun seekMap(map: SeekMap) {
                delegateSeekMap = map
                if (map.isSeekable) {
                    fallbackRequested = false
                    output.seekMap(map)
                } else {
                    if (!fallbackAttempted) fallbackRequested = true
                    output.seekMap(fallbackSeekMap ?: map)
                }
            }
        })
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (fallbackRequested && !fallbackAttempted) {
            if (input.length == C.LENGTH_UNSET.toLong()) {
                fallbackAttempted = true
                fallbackRequested = false
            } else if (input.position != 0L) {
                seekPosition.position = 0L
                return Extractor.RESULT_SEEK
            } else {
                fallbackAttempted = true
                fallbackRequested = false
                val clusters = scanOrNull { scanClusters(input) }.orEmpty()
                if (clusters.size >= 2) {
                    fallbackSeekMap = clusterSeekMap(
                        clusters,
                        delegateSeekMap?.durationUs ?: C.TIME_UNSET
                    )
                    extractorOutput?.seekMap(requireNotNull(fallbackSeekMap))
                } else {
                    delegateSeekMap?.let { extractorOutput?.seekMap(it) }
                }
                delegate.seek(0L, 0L)
                if (input.position != 0L) {
                    seekPosition.position = 0L
                    return Extractor.RESULT_SEEK
                }
            }
        }
        return delegate.read(input, seekPosition)
    }

    override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)
    override fun release() = delegate.release()

    private fun scanClusters(input: ExtractorInput): List<Pair<Long, Long>> {
        val ebml = readVint(input, isId = true) ?: return emptyList()
        val ebmlSize = readVint(input, isId = false)?.first ?: return emptyList()
        if (ebml.first != 0x1A45DFA3L || ebmlSize < 0L) return emptyList()
        skip(input, ebmlSize)

        val segment = readVint(input, isId = true) ?: return emptyList()
        val segmentSize = readVint(input, isId = false)?.first ?: return emptyList()
        if (segment.first != 0x18538067L) return emptyList()
        val segmentEnd = if (segmentSize < 0L) input.length else minOf(input.length, input.position + segmentSize)
        val clusters = ArrayList<Pair<Long, Long>>()
        var timecodeScaleNs = 1_000_000L

        while (input.position < segmentEnd) {
            val elementPosition = input.position
            val id = readVint(input, isId = true)?.first ?: break
            val size = readVint(input, isId = false)?.first ?: break
            if (size < 0L || size > segmentEnd - input.position) break
            when (id) {
                0x1549A966L -> {
                    val end = input.position + size
                    while (input.position < end) {
                        val childId = readVint(input, isId = true)?.first ?: break
                        val childSize = readVint(input, isId = false)?.first ?: break
                        if (childSize < 0L || childSize > end - input.position) break
                        if (childId == 0x2AD7B1L && childSize in 1L..8L) {
                            timecodeScaleNs = readUnsigned(input, childSize.toInt()).takeIf { it > 0L }
                                ?: timecodeScaleNs
                        } else {
                            skip(input, childSize)
                        }
                    }
                    skip(input, end - input.position)
                }

                0x1F43B675L -> {
                    val end = input.position + size
                    val inspectionEnd = minOf(end, input.position + 16_384L)
                    var clusterTimecode = -1L
                    while (input.position < inspectionEnd) {
                        val childId = readVint(input, isId = true)?.first ?: break
                        val childSize = readVint(input, isId = false)?.first ?: break
                        if (childSize < 0L || childSize > end - input.position) break
                        if (childId == 0xE7L && childSize in 1L..8L) {
                            clusterTimecode = readUnsigned(input, childSize.toInt())
                            break
                        }
                        skip(input, childSize)
                    }
                    if (clusterTimecode >= 0L) {
                        val timeUs = clusterTimecode * timecodeScaleNs / 1_000L
                        if (clusters.isEmpty() || timeUs > clusters.last().first) {
                            clusters += timeUs to elementPosition
                        }
                    }
                    skip(input, end - input.position)
                }

                else -> skip(input, size)
            }
        }
        return clusters
    }

    private fun readVint(input: ExtractorInput, isId: Boolean): Pair<Long, Int>? {
        val first = ByteArray(1)
        if (!input.readFully(first, 0, 1, true)) return null
        val value = first[0].toInt() and 0xFF
        if (value == 0) return null
        var mask = 0x80
        var length = 1
        while (value and mask == 0 && length < 8) {
            mask = mask ushr 1
            length += 1
        }
        if (isId && length > 4) return null
        var result = (if (isId) value else value and (mask - 1)).toLong()
        val remaining = ByteArray(length - 1)
        if (remaining.isNotEmpty()) input.readFully(remaining, 0, remaining.size)
        remaining.forEach { byte -> result = (result shl 8) or (byte.toLong() and 0xFFL) }
        if (!isId && result == (1L shl (7 * length)) - 1L) result = -1L
        return result to length
    }

    private fun readUnsigned(input: ExtractorInput, count: Int): Long {
        val bytes = ByteArray(count)
        input.readFully(bytes, 0, count)
        var result = 0L
        bytes.forEach { byte -> result = (result shl 8) or (byte.toLong() and 0xFFL) }
        return result
    }

    private fun skip(input: ExtractorInput, count: Long) {
        var remaining = count
        while (remaining > 0L) {
            val part = minOf(remaining, 1_048_576L).toInt()
            input.skipFully(part)
            remaining -= part
        }
    }
}

@UnstableApi
private fun clusterSeekMap(clusters: List<Pair<Long, Long>>, durationUs: Long): SeekMap {
    val timesUs = clusters.map { it.first }.toLongArray()
    val positions = clusters.map { it.second }.toLongArray()
    // The final cluster's start is not the video's end; keep unknown duration unknown.
    val resolvedDuration = durationUs.takeIf { it > 0L } ?: C.TIME_UNSET
    return binarySearchSeekMap(timesUs, positions, resolvedDuration)
}

@UnstableApi
private fun binarySearchSeekMap(
    timesUs: LongArray,
    positions: LongArray,
    durationUs: Long
): SeekMap = object : SeekMap {
    override fun isSeekable(): Boolean = true
    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        var low = 0
        var high = timesUs.lastIndex
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (timesUs[middle] <= timeUs) low = middle else high = middle - 1
        }
        val first = SeekPoint(timesUs[low], positions[low])
        val next = if (low < timesUs.lastIndex) {
            SeekPoint(timesUs[low + 1], positions[low + 1])
        } else {
            first
        }
        return if (first.timeUs >= timeUs || first == next) {
            SeekMap.SeekPoints(first)
        } else {
            SeekMap.SeekPoints(first, next)
        }
    }
}

private inline fun <T> scanOrNull(scan: () -> T): T? = try {
    scan()
} catch (interrupted: InterruptedIOException) {
    // Media3 interrupts its loader when the user leaves a video; let cancellation through.
    throw interrupted
} catch (_: Exception) {
    null
}
