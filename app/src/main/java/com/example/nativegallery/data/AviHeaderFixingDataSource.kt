package com.example.nativegallery.data

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Advertises an AVI index that is already present but omitted from the main header flags.
 * The original media is never modified; only the first 512 streamed bytes can be patched.
 */
@OptIn(UnstableApi::class)
internal class AviHeaderFixingDataSource(
    private val upstream: DataSource
) : DataSource {
    private var prefix = ByteArray(0)
    private var prefixPosition = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        clearPrefix()
        val available = upstream.open(dataSpec)
        if (dataSpec.position != 0L) return available

        val limit = minOf(
            512L,
            available.takeIf { it >= 0L } ?: 512L,
            dataSpec.length.takeIf { it >= 0L } ?: 512L
        ).toInt()
        if (limit <= 0) return available

        val header = ByteArray(limit)
        var count = 0
        while (count < limit) {
            val read = upstream.read(header, count, limit - count)
            if (read == C.RESULT_END_OF_INPUT) break
            if (read <= 0) throw IOException("Media source returned an empty read")
            count += read
        }
        prefix = header.copyOf(count)
        header.fill(0)
        patchAviIndexFlag(prefix)
        return available
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (prefixPosition < prefix.size) {
            val count = minOf(length, prefix.size - prefixPosition)
            prefix.copyInto(buffer, offset, prefixPosition, prefixPosition + count)
            prefixPosition += count
            return count
        }
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun close() {
        clearPrefix()
        upstream.close()
    }

    private fun clearPrefix() {
        prefix.fill(0)
        prefix = ByteArray(0)
        prefixPosition = 0
    }
}

internal fun patchAviIndexFlag(bytes: ByteArray): Boolean {
    if (bytes.size < 32 ||
        !bytes.hasAsciiAt(0, "RIFF") ||
        !bytes.hasAsciiAt(8, "AVI ")
    ) return false

    for (index in 12..bytes.size - 21) {
        if (bytes.hasAsciiAt(index, "avih")) {
            bytes[index + 20] = (bytes[index + 20].toInt() or 0x10).toByte()
            return true
        }
    }
    return false
}

private fun ByteArray.hasAsciiAt(offset: Int, value: String): Boolean {
    if (offset < 0 || offset + value.length > size) return false
    return value.indices.all { index -> this[offset + index] == value[index].code.toByte() }
}
