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

    fun moveToAlbum(mediaItems: List<MediaItem>, albumName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val safeAlbumName = albumName
            .trim()
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .trim('.', ' ')
            .take(80)
        if (safeAlbumName.isBlank()) return false

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/$safeAlbumName/")
        }
        val uris = mediaItems.mapNotNull { it.contentUri }.distinct()
        if (uris.isEmpty()) return false
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
