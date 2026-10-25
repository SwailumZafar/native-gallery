package com.example.nativegallery.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class LocalVideoExtractorsTest {
    @Test
    fun healthyNativeFlvIndexDoesNotScanTheFile() {
        val nativeMap = object : SeekMap {
            override fun isSeekable() = true
            override fun getDurationUs() = 5_000_000L
            override fun getSeekPoints(timeUs: Long) = SeekMap.SeekPoints(SeekPoint.START)
        }
        val output = CapturingOutput()
        val extractor = IndexedFlvExtractor(HeaderDelegate(nativeMap))
        extractor.init(output)
        val input = input(ByteArray(100), 0)
        repeat(3) { extractor.read(input, PositionHolder()) }
        assertEquals(3L, input.position)
        assertEquals(nativeMap, output.maps.last())
    }

    @Test
    fun missingFlvIndexIgnoresSequenceHeadersAndSurvivesDelegateRestart() {
        val header = byteArrayOf('F'.code.toByte(), 'L'.code.toByte(), 'V'.code.toByte(), 1, 1, 0, 0, 0, 9, 0, 0, 0, 0)
        val data = header + tag(0, byteArrayOf(0x17, 0)) +
            tag(0, byteArrayOf(0x17, 1)) + tag(1_000, byteArrayOf(0x17, 1)) +
            tag(2_000, byteArrayOf(0x27, 1))
        val output = CapturingOutput()
        val extractor = IndexedFlvExtractor(HeaderDelegate(SeekMap.Unseekable(C.TIME_UNSET)))
        extractor.init(output)
        var currentInput = input(data, 0)
        val position = PositionHolder()
        repeat(8) {
            if (extractor.read(currentInput, position) == Extractor.RESULT_SEEK) {
                currentInput = input(data, position.position.toInt())
            }
        }
        val map = output.maps.last()
        assertTrue(map.isSeekable)
        assertEquals(2_040_000L, map.durationUs)
        assertEquals((header.size + tag(0, byteArrayOf(0x17, 0)).size).toLong(), map.getSeekPoints(0).first.position)
        assertEquals(1_000_000L, map.getSeekPoints(1_500_000L).first.timeUs)
    }

    @Test
    fun matroskaFallbackUsesClusterPositionsWithoutInventingAnEndTime() {
        fun cluster(timeMs: Int): ByteArray = byteArrayOf(
            0x1F, 0x43, 0xB6.toByte(), 0x75, 0x84.toByte(),
            0xE7.toByte(), 0x82.toByte(), (timeMs shr 8).toByte(), timeMs.toByte()
        )
        val prefix = byteArrayOf(
            0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 0x80.toByte(),
            0x18, 0x53, 0x80.toByte(), 0x67, 0xFF.toByte()
        )
        val data = prefix + cluster(0) + cluster(1_000) + cluster(2_000)
        val output = CapturingOutput()
        val extractor = IndexedMatroskaExtractor(HeaderDelegate(SeekMap.Unseekable(C.TIME_UNSET)))
        extractor.init(output)
        var currentInput = input(data, 0)
        val position = PositionHolder()
        repeat(8) {
            if (extractor.read(currentInput, position) == Extractor.RESULT_SEEK) {
                currentInput = input(data, position.position.toInt())
            }
        }
        val map = output.maps.last()
        assertTrue(map.isSeekable)
        assertEquals(C.TIME_UNSET, map.durationUs)
        val point = map.getSeekPoints(1_500_000L).first
        assertEquals(1_000_000L, point.timeUs)
        assertEquals((prefix.size + cluster(0).size).toLong(), point.position)
    }

    private fun tag(timeMs: Int, payload: ByteArray): ByteArray {
        val header = ByteArray(11)
        header[0] = 9
        header[3] = payload.size.toByte()
        header[4] = (timeMs shr 16).toByte()
        header[5] = (timeMs shr 8).toByte()
        header[6] = timeMs.toByte()
        return header + payload + ByteArray(4)
    }

    private fun input(bytes: ByteArray, start: Int): ExtractorInput {
        var position = start
        return DefaultExtractorInput({ buffer, offset, length ->
            if (position == bytes.size) C.RESULT_END_OF_INPUT else {
                val count = minOf(length, bytes.size - position)
                bytes.copyInto(buffer, offset, position, position + count)
                position += count
                count
            }
        }, start.toLong(), bytes.size.toLong())
    }

    private class HeaderDelegate(private val map: SeekMap) : Extractor {
        private lateinit var output: ExtractorOutput
        override fun sniff(input: ExtractorInput) = true
        override fun init(output: ExtractorOutput) { this.output = output }
        override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
            if (input.position == 0L) output.seekMap(map)
            input.skipFully(1)
            return Extractor.RESULT_CONTINUE
        }
        override fun seek(position: Long, timeUs: Long) = Unit
        override fun release() = Unit
    }

    private class CapturingOutput : ExtractorOutput {
        val maps = mutableListOf<SeekMap>()
        override fun seekMap(seekMap: SeekMap) { maps += seekMap }
        override fun track(id: Int, type: Int): TrackOutput = error("No samples expected")
        override fun endTracks() = Unit
    }
}
