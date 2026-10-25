package com.example.nativegallery.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDurationResolverTest {
    @Test
    fun aviDurationUsesMainHeaderFrameTiming() {
        val bytes = ByteArray(96)
        bytes.putAscii(0, "RIFF")
        bytes.putAscii(8, "AVI ")
        bytes.putAscii(12, "avih")
        bytes.putLittleEndianInt(20, 40_000)
        bytes.putLittleEndianInt(36, 50)

        assertEquals(2_000L, parseAviDurationMillis(bytes))
    }

    @Test
    fun flvDurationPreservesGenuinelyShortVideo() {
        val bytes = ByteArray(80)
        bytes.putAscii(0, "FLV")
        bytes.putAscii(16, "duration")
        bytes[24] = 0
        ByteBuffer.wrap(bytes, 25, 8).order(ByteOrder.BIG_ENDIAN).putDouble(0.375)

        assertEquals(375L, parseFlvDurationMillis(bytes))
    }

    @Test
    fun mkvDurationAppliesTimecodeScale() {
        val bytes = ByteArray(96)
        bytes[0] = 0x1A
        bytes[1] = 0x45
        bytes[2] = 0xDF.toByte()
        bytes[3] = 0xA3.toByte()
        bytes[12] = 0x15
        bytes[13] = 0x49
        bytes[14] = 0xA9.toByte()
        bytes[15] = 0x66
        bytes[20] = 0x2A
        bytes[21] = 0xD7.toByte()
        bytes[22] = 0xB1.toByte()
        bytes[23] = 0x83.toByte()
        bytes[24] = 0x0F
        bytes[25] = 0x42
        bytes[26] = 0x40
        bytes[32] = 0x44
        bytes[33] = 0x89.toByte()
        bytes[34] = 0x88.toByte()
        ByteBuffer.wrap(bytes, 35, 8).order(ByteOrder.BIG_ENDIAN).putDouble(500.0)

        assertEquals(500L, parseMkvDurationMillis(bytes))
    }

    @Test
    fun thumbnailCandidatesStayUsefulForSubSecondClips() {
        val candidates = videoThumbnailCandidateTimesUs(420L)

        assertTrue(210_000L in candidates)
        assertTrue(0L in candidates)
        assertTrue(-1L in candidates)
        assertFalse(candidates.any { it > 420_000L })
    }

    @Test
    fun aviHeaderPatchOnlyChangesAviIndexFlag() {
        val avi = ByteArray(64)
        avi.putAscii(0, "RIFF")
        avi.putAscii(8, "AVI ")
        avi.putAscii(12, "avih")

        assertTrue(patchAviIndexFlag(avi))
        assertEquals(0x10, avi[32].toInt() and 0x10)

        val unrelated = ByteArray(64)
        assertFalse(patchAviIndexFlag(unrelated))
        assertTrue(unrelated.all { it == 0.toByte() })
    }

    private fun ByteArray.putAscii(offset: Int, value: String) {
        value.forEachIndexed { index, character -> this[offset + index] = character.code.toByte() }
    }

    private fun ByteArray.putLittleEndianInt(offset: Int, value: Int) {
        ByteBuffer.wrap(this, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
    }
}
