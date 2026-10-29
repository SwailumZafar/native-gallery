package com.example.nativegallery.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolve original orientation once; a cropped grid poster is never a geometry source. */
internal object PhotoPresentationGeometry {
    private val cache = object : LinkedHashMap<String, Pair<Int, Int>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Int, Int>>?) = size > 128
    }
    suspend fun resolve(context: Context, item: MediaItem): MediaItem {
        val uri = item.contentUri ?: return item
        val key = "$uri:${item.fileSizeBytes}:${item.dateModifiedMillis}"
        val known = synchronized(cache) { cache[key] }
        val dimensions = known ?: withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                val orientation = runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }
                }.getOrNull()
                val rotated = orientation in listOf(ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270,
                    ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE)
                if (rotated) bounds.outHeight to bounds.outWidth else bounds.outWidth to bounds.outHeight
            }.getOrNull()?.also { synchronized(cache) { cache[key] = it } }
        }
        return dimensions?.let { item.copy(width = it.first, height = it.second) } ?: item
    }
}
