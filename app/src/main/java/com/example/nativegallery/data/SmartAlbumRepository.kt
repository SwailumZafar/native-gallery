package com.example.nativegallery.data

import android.content.Context
import com.example.nativegallery.model.MediaItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SmartAlbumRule(val id: String = "smart:${UUID.randomUUID()}", val name: String,
    val mediaType: String = "All", val pastDays: Int = 0, val text: String = "", val favoritesOnly: Boolean = false)

internal fun matchesSmartAlbum(rule: SmartAlbumRule, item: MediaItem, favorites: Set<String>, now: Long): Boolean =
    (rule.mediaType == "All" || (rule.mediaType == "Videos") == item.isVideo) &&
        (!rule.favoritesOnly || item.id in favorites) &&
        (rule.pastDays <= 0 || item.sortTimestampMillis in (now - rule.pastDays * 86_400_000L)..now) &&
        (rule.text.isBlank() || item.title.contains(rule.text, true) || item.relativePath.orEmpty().contains(rule.text, true))

class SmartAlbumRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("aster_smart_albums", Context.MODE_PRIVATE)
    fun load(): List<SmartAlbumRule> = runCatching {
        val array = JSONArray(preferences.getString("rules", "[]"))
        (0 until array.length()).map { index -> array.getJSONObject(index).let {
            SmartAlbumRule(it.getString("id"), it.getString("name"), it.optString("type", "All"),
                it.optInt("days", 0).coerceIn(0, 36500), it.optString("text", ""), it.optBoolean("favorites"))
        } }
    }.getOrDefault(emptyList())
    fun save(rules: List<SmartAlbumRule>) {
        val array = JSONArray()
        rules.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("type", it.mediaType)
            .put("days", it.pastDays).put("text", it.text).put("favorites", it.favoritesOnly)) }
        preferences.edit().putString("rules", array.toString()).apply()
    }
}
