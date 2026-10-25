package com.example.nativegallery.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

@androidx.annotation.OptIn(UnstableApi::class)
class AviHeaderFixingDataSourceTest {
    @Test
    fun splitHeadersReplayAllBytesAndOnlyPatchTheIndexFlag() {
        val original = ByteArray(1_024) { (it % 251).toByte() }
        "RIFF".toByteArray().copyInto(original, 0)
        "AVI ".toByteArray().copyInto(original, 8)
        "avih".toByteArray().copyInto(original, 24)
        original[44] = 0
        val source = AviHeaderFixingDataSource(ShortReads(original))
        assertEquals(1_024L, source.open(spec()))
        assertArrayEquals(original.copyOf().also { it[44] = 0x10 }, drain(source))
        source.close()
        assertEquals(0, original[44].toInt())
    }

    @Test
    fun boundedOpensAndRandomAccessPreserveRequestedByteRange() {
        val original = ByteArray(1_024) { (it % 251).toByte() }
        val source = AviHeaderFixingDataSource(ShortReads(original))
        assertEquals(13L, source.open(spec(length = 13L)))
        assertArrayEquals(original.copyOfRange(0, 13), drain(source))
        source.close()
        assertEquals(17L, source.open(spec(position = 700L, length = 17L)))
        assertArrayEquals(original.copyOfRange(700, 717), drain(source))
        source.close()
    }

    private fun spec(position: Long = 0L, length: Long = C.LENGTH_UNSET.toLong()) =
        DataSpec.Builder().setUri(mock(Uri::class.java)).setPosition(position).setLength(length).build()

    private fun drain(source: DataSource): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val bytes = ByteArray(7)
        while (true) {
            val count = source.read(bytes, 0, bytes.size)
            if (count == C.RESULT_END_OF_INPUT) return output.toByteArray()
            output.write(bytes, 0, count)
        }
    }

    private class ShortReads(private val bytes: ByteArray) : DataSource {
        private var position = 0
        private var end = 0
        override fun open(dataSpec: DataSpec): Long {
            position = dataSpec.position.toInt()
            end = if (dataSpec.length >= 0L) minOf(bytes.size, position + dataSpec.length.toInt()) else bytes.size
            return (end - position).toLong()
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position >= end) return C.RESULT_END_OF_INPUT
            val count = minOf(length, 3, end - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
        override fun addTransferListener(transferListener: TransferListener) = Unit
        override fun getUri(): Uri? = null
        override fun close() = Unit
    }
}
