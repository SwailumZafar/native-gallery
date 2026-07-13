package com.example.nativegallery.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import kotlin.math.abs

object ThumbnailMemoryCache {
    private val MaxCacheKilobytes = ((Runtime.getRuntime().maxMemory() / 1024L) / 6L)
        .coerceIn(48L * 1024L, 128L * 1024L)
        .toInt()

    private val indexLock = Any()
    private val sizesByUri = mutableMapOf<String, MutableSet<Int>>()

    private val cache = object : LruCache<String, Bitmap>(MaxCacheKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (newValue == null) {
                removeIndexedSize(key)
            }
        }
    }

    fun key(uri: Uri, size: Int): String = "$uri@$size"

    fun get(key: String): Bitmap? = cache.get(key)

    fun getNearest(uri: Uri, requestedSize: Int): Bitmap? {
        get(key(uri, requestedSize))?.let { return it }

        val uriKey = uri.toString()
        val candidateSizes = synchronized(indexLock) {
            sizesByUri[uriKey]
                ?.toList()
                ?.sortedBy { cachedSize -> abs(cachedSize - requestedSize) }
                .orEmpty()
        }
        candidateSizes.forEach { cachedSize ->
            get(key(uri, cachedSize))?.let { return it }
            synchronized(indexLock) {
                sizesByUri[uriKey]?.let { sizes ->
                    sizes.remove(cachedSize)
                    if (sizes.isEmpty()) sizesByUri.remove(uriKey)
                }
            }
        }
        return null
    }

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) {
            cache.put(key, bitmap)
            indexKey(key)
        }
    }

    private fun indexKey(key: String) {
        val parsedKey = parseKey(key) ?: return
        synchronized(indexLock) {
            sizesByUri.getOrPut(parsedKey.first) { mutableSetOf() }.add(parsedKey.second)
        }
    }

    private fun removeIndexedSize(key: String) {
        val parsedKey = parseKey(key) ?: return
        synchronized(indexLock) {
            sizesByUri[parsedKey.first]?.let { sizes ->
                sizes.remove(parsedKey.second)
                if (sizes.isEmpty()) sizesByUri.remove(parsedKey.first)
            }
        }
    }

    private fun parseKey(key: String): Pair<String, Int>? {
        val separatorIndex = key.lastIndexOf('@')
        if (separatorIndex <= 0 || separatorIndex == key.lastIndex) return null
        val size = key.substring(separatorIndex + 1).toIntOrNull() ?: return null
        return key.substring(0, separatorIndex) to size
    }
}
