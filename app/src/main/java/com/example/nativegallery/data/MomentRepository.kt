package com.example.nativegallery.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedMoment(val id: String = UUID.randomUUID().toString(), val name: String,
    val mediaIds: List<String>, val coverId: String? = null, val note: String = "", val createdMillis: Long = System.currentTimeMillis())

data class MomentLibrary(val moments: List<SavedMoment> = emptyList(), val dismissedSuggestions: Set<String> = emptySet())

class MomentRepository(context: Context, preferenceName: String = "photo_lane_moments") {
    internal val cacheNamespace = preferenceName
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    suspend fun load(): MomentLibrary = withContext(Dispatchers.IO) {
        val root = runCatching { JSONObject(preferences.getString("library", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val array = root.optJSONArray("moments") ?: JSONArray()
        val moments = (0 until array.length()).mapNotNull { index -> runCatching {
            val item = array.getJSONObject(index)
            val ids = item.getJSONArray("media")
            SavedMoment(item.getString("id"), item.getString("name"), (0 until ids.length()).map { ids.getString(it) }.distinct(),
                item.optString("cover").takeIf { it.isNotBlank() }, item.optString("note"), item.optLong("created"))
        }.getOrNull() }
        val hidden = root.optJSONArray("dismissed") ?: JSONArray()
        MomentLibrary(moments, (0 until hidden.length()).map { hidden.optString(it) }.filter { it.isNotBlank() }.toSet())
    }
    suspend fun save(library: MomentLibrary) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        library.moments.forEach { moment -> array.put(JSONObject().put("id", moment.id).put("name", moment.name.take(120))
            .put("media", JSONArray(moment.mediaIds.distinct())).put("cover", moment.coverId.orEmpty())
            .put("note", moment.note.take(1000)).put("created", moment.createdMillis)) }
        check(preferences.edit().putString("library", JSONObject().put("moments", array)
            .put("dismissed", JSONArray(library.dismissedSuggestions.toList())).toString()).commit()) { "Could not save moments" }
    }
}
