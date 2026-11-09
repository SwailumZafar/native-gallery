package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import java.util.Locale

internal enum class MemoryTheme(val title: String) {
    Together("Time together"), Outdoors("Out and about"), Celebration("Celebrations"), Pets("With your pets"), Meals("Around the table")
}
internal data class MemoryEvidence(val theme: MemoryTheme?, val quality: Float, val fingerprint: Long)

internal fun isMemoryCandidate(item: MediaItem, favorite: Boolean = false): Boolean {
    if (item.isVideo || isScreenshotMedia(item) || item.sortTimestampMillis <= 0) return false
    val path = item.relativePath.orEmpty().lowercase(Locale.ROOT)
    val source = "$path/${item.title.lowercase(Locale.ROOT)}"
    if (listOf("download", "document", "receipt", "scan", "whatsapp", "telegram", "sticker", "meme", "wallpaper").any(source::contains)) return false
    if ((item.width ?: 1000) < 480 || (item.height ?: 1000) < 480) return false
    return favorite || path.contains("dcim") || path.contains("camera")
}

/** Specific photographic subjects are required. Generic sky, objects or dates alone never qualify. */
internal fun memoryTheme(labels: Map<String, Float>, textCoverage: Float, textCharacters: Int): MemoryTheme? {
    if (textCoverage >= 0.16f || textCharacters >= 220) return null
    val normalized = labels.mapKeys { it.key.lowercase(Locale.ROOT) }
    fun strongest(vararg words: String) = words.maxOf { normalized[it] ?: 0f }
    if (strongest("screenshot", "web page", "receipt", "menu", "paper", "poster", "comics", "whiteboard", "blackboard", "passport", "newspaper") >= 0.65f) return null
    return when {
        strongest("marriage", "party", "graduation", "concert", "prom", "dance", "picnic", "christmas") >= 0.65f -> MemoryTheme.Celebration
        strongest("person", "smile", "laugh", "interaction", "grandparent", "team", "crowd") >= 0.72f -> MemoryTheme.Together
        strongest("pet", "cat", "dog") >= 0.75f -> MemoryTheme.Pets
        strongest("mountain", "beach", "lake", "forest", "sunset", "vacation", "camping", "backpacking", "waterfall", "river", "glacier", "desert", "park", "skyline", "monument", "museum", "palace", "mosque", "ruins", "surfing", "skiing", "swimming") >= 0.72f -> MemoryTheme.Outdoors
        strongest("eating", "cuisine", "food") >= 0.82f -> MemoryTheme.Meals
        else -> null
    }
}

internal fun nearDuplicateMemory(a: Long, b: Long): Boolean = java.lang.Long.bitCount(a xor b) <= 6
