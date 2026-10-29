package com.example.nativegallery.ui.components

import android.graphics.Bitmap
import android.net.Uri
import java.lang.ref.WeakReference

/** References only images already held by composed UI; LRU eviction must not erase a tapped preview. */
internal object DisplayedBitmapCache {
    private data class Entry(val uri: String, val bitmap: WeakReference<Bitmap>, val sourceKey: Any?)
    private val entries = LinkedHashMap<Any, Entry>()

    @Synchronized
    fun register(owner: Any, uri: Uri, bitmap: Bitmap, sourceKey: Any? = null) {
        entries[owner] = Entry(uri.toString(), WeakReference(bitmap), sourceKey)
        while (entries.size > 512) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun unregister(owner: Any) { entries.remove(owner) }

    @Synchronized
    fun get(uri: Uri?): Bitmap? {
        if (uri == null) return null
        val identity = uri.toString()
        return entries.values.asSequence()
            .filter { it.uri == identity }
            .mapNotNull { it.bitmap.get()?.takeUnless(Bitmap::isRecycled) }
            .maxByOrNull { it.width.toLong() * it.height }
    }

    @Synchronized
    fun getSmallest(uri: Uri?): Bitmap? {
        if (uri == null) return null
        return entries.values.asSequence().filter { it.uri == uri.toString() }
            .mapNotNull { it.bitmap.get()?.takeUnless(Bitmap::isRecycled) }
            .minByOrNull { it.width.toLong() * it.height }
    }

    @Synchronized
    fun getForSource(uri: Uri?, sourceKey: Any?): Bitmap? {
        if (uri == null || sourceKey == null) return null
        return entries.values.asSequence().filter { it.uri == uri.toString() && it.sourceKey == sourceKey }
            .mapNotNull { it.bitmap.get()?.takeUnless(Bitmap::isRecycled) }.lastOrNull()
    }
}
