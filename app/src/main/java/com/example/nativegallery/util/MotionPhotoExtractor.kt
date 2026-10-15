package com.example.nativegallery.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs

object MotionPhotoExtractor {
    private val FTYP = byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())

    suspend fun extractMotionVideo(
        context: Context,
        imageUri: Uri,
        knownOffset: Long? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "motion_photos").apply { mkdirs() }
        val cacheKey = abs(imageUri.toString().hashCode())
        val cachedVideoFile = File(cacheDir, "motion_$cacheKey.mp4")
        if (cachedVideoFile.exists() && cachedVideoFile.length() > 1024) {
            return@withContext Uri.fromFile(cachedVideoFile)
        }

        try {
            val contentResolver = context.contentResolver
            val offset = if (knownOffset != null && knownOffset > 0) {
                knownOffset
            } else {
                val inputStream = contentResolver.openInputStream(imageUri) ?: return@withContext null
                val detected = findMp4Offset(inputStream)
                inputStream.close()
                detected
            }
            if (offset < 0) return@withContext null

            val streamForExtract = contentResolver.openInputStream(imageUri) ?: return@withContext null
            streamForExtract.use { input ->
                var skipped = 0L
                while (skipped < offset) {
                    val s = input.skip(offset - skipped)
                    if (s <= 0) break
                    skipped += s
                }
                FileOutputStream(cachedVideoFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (cachedVideoFile.length() > 1024) {
                Uri.fromFile(cachedVideoFile)
            } else {
                cachedVideoFile.delete()
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun getMotionVideoUri(context: Context, imageUri: Uri): Uri? =
        extractMotionVideo(context, imageUri, -1L)

    private fun findMp4Offset(input: InputStream): Long {
        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L
        val prevWindow = ByteArray(8)
        var prevWindowLen = 0

        var bytesRead: Int
        var foundOffset = -1L
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val combined = ByteArray(prevWindowLen + bytesRead)
            System.arraycopy(prevWindow, 0, combined, 0, prevWindowLen)
            System.arraycopy(buffer, 0, combined, prevWindowLen, bytesRead)

            for (i in 0 until combined.size - 7) {
                if (combined[i + 4] == FTYP[0] &&
                    combined[i + 5] == FTYP[1] &&
                    combined[i + 6] == FTYP[2] &&
                    combined[i + 7] == FTYP[3]
                ) {
                    val b0 = combined[i].toInt() and 0xFF
                    val b1 = combined[i + 1].toInt() and 0xFF
                    val b2 = combined[i + 2].toInt() and 0xFF
                    val b3 = combined[i + 3].toInt() and 0xFF
                    val boxLen = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                    if (boxLen in 8..1024) {
                        foundOffset = (totalRead - prevWindowLen) + i
                    }
                }
            }

            prevWindowLen = bytesRead.coerceAtMost(8)
            System.arraycopy(buffer, bytesRead - prevWindowLen, prevWindow, 0, prevWindowLen)
            totalRead += bytesRead
        }
        return foundOffset
    }
}
