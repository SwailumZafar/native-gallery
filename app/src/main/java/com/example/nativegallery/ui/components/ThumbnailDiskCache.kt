package com.example.nativegallery.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors

object ThumbnailDiskCache {
    private const val DirectoryName = "gallery_thumbnails_v1"
    private const val MaxBytes = 192L * 1024L * 1024L
    private const val AccessTimeUpdateIntervalMillis = 6L * 60L * 60L * 1000L
    private val writesSinceTrim = AtomicInteger(0)
    private val lock = Any()
    private val writer = Executors.newSingleThreadExecutor()

    fun get(context: Context, cacheKey: String): Bitmap? {
        val file = cacheFile(context, cacheKey)
        if (!file.isFile) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        if (bitmap == null) {
            runCatching { file.delete() }
            return null
        }
        val now = System.currentTimeMillis()
        if (now - file.lastModified() >= AccessTimeUpdateIntervalMillis) {
            runCatching { file.setLastModified(now) }
        }
        return bitmap
    }

    fun put(context: Context, cacheKey: String, bitmap: Bitmap) {
        val appContext = context.applicationContext
        writer.execute {
            putBlocking(appContext, cacheKey, bitmap)
        }
    }

    private fun putBlocking(context: Context, cacheKey: String, bitmap: Bitmap) {
        synchronized(lock) {
            val target = cacheFile(context, cacheKey)
            if (target.isFile) return
            val directory = target.parentFile ?: return
            if (!directory.exists() && !directory.mkdirs()) return
            val temporary = File(directory, "${target.name}.tmp")
            val written = runCatching {
                FileOutputStream(temporary).use { stream ->
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    bitmap.compress(format, 86, stream)
                }
            }.getOrDefault(false)
            if (written) {
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            } else {
                temporary.delete()
            }
            if (writesSinceTrim.incrementAndGet() >= 32) {
                writesSinceTrim.set(0)
                trim(directory)
            }
        }
    }

    private fun cacheFile(context: Context, cacheKey: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(cacheKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(File(context.cacheDir, DirectoryName), "$digest.webp")
    }

    private fun trim(directory: File) {
        val files = directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= MaxBytes) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MaxBytes) return
            val length = file.length()
            if (file.delete()) total -= length
        }
    }
}
