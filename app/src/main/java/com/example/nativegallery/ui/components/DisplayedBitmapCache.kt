package com.example.nativegallery.ui.components

import android.graphics.Bitmap
import android.net.Uri
import java.lang.ref.WeakReference

/** References only images already held by composed UI; LRU eviction must not erase a tapped preview. */
internal object DisplayedBitmapCache {
    private val entries = LinkedHashMap<Any, Pair<String, WeakReference<Bitmap>>>()

    @Synchronized
    fun register(owner: Any, uri: Uri, bitmap: Bitmap) {
        entries[owner] = uri.toString() to WeakReference(bitmap)
        while (entries.size > 512) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun unregister(owner: Any) { entries.remove(owner) }

    @Synchronized
    fun get(uri: Uri?): Bitmap? {
        if (uri == null) return null
        val identity = uri.toString()
        return entries.values.asSequence()
            .filter { it.first == identity }
            .mapNotNull { it.second.get()?.takeUnless(Bitmap::isRecycled) }
            .maxByOrNull { it.width.toLong() * it.height }
    }
}
