package com.example.nativegallery.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

object ThumbnailMemoryCache {
    private const val MaxCacheKilobytes = 48 * 1024

    private val cache = object : LruCache<String, Bitmap>(MaxCacheKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun key(uri: Uri, size: Int): String = "$uri@$size"

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) {
            cache.put(key, bitmap)
        }
    }
}
