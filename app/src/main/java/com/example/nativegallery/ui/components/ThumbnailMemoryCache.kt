package com.example.nativegallery.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import kotlin.math.abs

object ThumbnailMemoryCache {
    private val MaxCacheKilobytes = ((Runtime.getRuntime().maxMemory() / 1024L) / 6L)
        .coerceIn(48L * 1024L, 128L * 1024L)
        .toInt()
    private val MaxPinnedKilobytes = ((Runtime.getRuntime().maxMemory() / 1024L) / 16L)
        .coerceIn(16L * 1024L, 48L * 1024L)
        .toInt()

    private val indexLock = Any()
    private val sizesByUri = mutableMapOf<String, MutableSet<Int>>()
    private val keyReferenceCounts = mutableMapOf<String, Int>()

    private val cache = object : LruCache<String, Bitmap>(MaxCacheKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (newValue == null) unindexKey(key)
        }
    }

    private val pinnedCache = object : LruCache<String, Bitmap>(MaxPinnedKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (newValue == null) unindexKey(key)
        }
    }

    fun key(uri: Uri, size: Int): String = "$uri@$size"

    fun get(key: String): Bitmap? = pinnedCache.get(key) ?: cache.get(key)

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

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) {
            indexKey(key)
            cache.put(key, bitmap)
        }
    }

    @Synchronized
    fun replace(key: String, bitmap: Bitmap) {
        val pinned = pinnedCache.get(key) != null
        if (!pinned && cache.get(key) == null) {
            indexKey(key)
        }
        if (pinned) {
            pinnedCache.put(key, bitmap)
        } else {
            cache.put(key, bitmap)
        }
    }

    @Synchronized
    fun pin(key: String, bitmap: Bitmap) {
        if (pinnedCache.get(key) == null) {
            indexKey(key)
            pinnedCache.put(key, bitmap)
        }
    }

    private fun indexKey(key: String) {
        val parsedKey = parseKey(key) ?: return
        synchronized(indexLock) {
            keyReferenceCounts[key] = keyReferenceCounts.getOrDefault(key, 0) + 1
            sizesByUri.getOrPut(parsedKey.first) { mutableSetOf() }.add(parsedKey.second)
        }
    }

    private fun unindexKey(key: String) {
        val parsedKey = parseKey(key) ?: return
        synchronized(indexLock) {
            val remainingReferences = keyReferenceCounts.getOrDefault(key, 0) - 1
            if (remainingReferences > 0) {
                keyReferenceCounts[key] = remainingReferences
                return
            }
            keyReferenceCounts.remove(key)
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
