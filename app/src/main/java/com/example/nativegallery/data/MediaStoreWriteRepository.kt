package com.example.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import com.example.nativegallery.model.MediaItem

class MediaStoreWriteRepository(context: Context) {
    private val appContext = context.applicationContext

    fun createTrashRequest(mediaItems: List<MediaItem>, trashed: Boolean): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return null
        return IntentSenderRequest.Builder(
            MediaStore.createTrashRequest(appContext.contentResolver, uris, trashed).intentSender
        ).build()
    }

    fun createDeleteRequest(mediaItems: List<MediaItem>): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return null
        return IntentSenderRequest.Builder(
            MediaStore.createDeleteRequest(appContext.contentResolver, uris).intentSender
        ).build()
    }

    fun createWriteRequest(mediaItems: List<MediaItem>): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return null
        return IntentSenderRequest.Builder(
            MediaStore.createWriteRequest(appContext.contentResolver, uris).intentSender
        ).build()
    }

    fun moveToAlbum(
        mediaItems: List<MediaItem>,
        albumName: String,
        targetRelativePath: String? = null
    ): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptySet()
        val safeAlbumName = albumName
            .trim()
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .trim('.', ' ')
            .take(80)
        if (safeAlbumName.isBlank()) return emptySet()

        val destinationPath = targetRelativePath
            ?.replace('\\', '/')
            ?.split('/')
            ?.map(String::trim)
            ?.filter { it.isNotBlank() && it != "." && it != ".." }
            ?.joinToString(separator = "/", postfix = "/")
            ?.takeIf(String::isNotBlank)
            ?: "DCIM/$safeAlbumName/"

        val occupiedNames = hashSetOf<String>()
        runCatching {
            appContext.contentResolver.query(MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(destinationPath), null)?.use { cursor ->
                while (cursor.moveToNext()) occupiedNames += cursor.getString(0).lowercase(java.util.Locale.ROOT)
            }
        }
        return mediaItems.distinctBy { it.contentUri }.mapNotNull { item ->
            val uri = item.contentUri ?: return@mapNotNull null
            val filename = uniqueMoveFilename(item.title, occupiedNames)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, destinationPath)
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            }
            val moved = runCatching { appContext.contentResolver.update(uri, values, null, null) > 0 }
                .onFailure { android.util.Log.w("GalleryMove", "MediaStore rejected an album move", it) }.getOrDefault(false)
            if (moved) { occupiedNames += filename.lowercase(java.util.Locale.ROOT); item.id } else null
        }.toSet()
    }

    fun setTrashedDirectly(mediaItems: List<MediaItem>, trashed: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return false
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_TRASHED, if (trashed) 1 else 0)
        }
        var updatedCount = 0
        uris.forEach { uri ->
            updatedCount += runCatching {
                appContext.contentResolver.update(uri, values, null, null)
            }.getOrDefault(0)
        }
        return updatedCount == uris.size
    }

    fun deleteDirectly(mediaItems: List<MediaItem>): Boolean {
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return false
        return uris.all { uri ->
            runCatching {
                appContext.contentResolver.delete(uri, null, null) > 0
            }.getOrDefault(false)
        }
    }
}

internal fun uniqueMoveFilename(name: String, occupied: Set<String>): String {
    if (name.lowercase(java.util.Locale.ROOT) !in occupied) return name
    val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
    val stem = name.take(dot); val extension = name.drop(dot)
    var suffix = 1
    while ("$stem ($suffix)$extension".lowercase(java.util.Locale.ROOT) in occupied) suffix++
    return "$stem ($suffix)$extension"
}

internal fun canonicalAlbumMovePath(path: String): String {
    val parts = path.replace('\\', '/').trim('/').split('/')
    val burst = parts.indexOfFirst { it.startsWith("BURST", true) || it.startsWith(".BURST", true) }
    return if (burst >= 2 && parts.first().equals("DCIM", true)) parts.take(burst).joinToString("/", postfix = "/")
        else parts.joinToString("/", postfix = "/")
}
