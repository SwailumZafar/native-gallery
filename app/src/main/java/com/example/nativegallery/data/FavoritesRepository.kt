package com.example.nativegallery.data

import android.content.Context

class FavoritesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_favorites",
        Context.MODE_PRIVATE
    )

    fun initialFavoriteIds(): Set<String> {
        return preferences.getStringSet(FAVORITE_MEDIA_IDS_KEY, emptySet()).orEmpty().toSet()
    }

    fun setFavorite(mediaId: String, favorite: Boolean): Set<String> {
        val nextIds = initialFavoriteIds().toMutableSet()
        if (favorite) {
            nextIds += mediaId
        } else {
            nextIds -= mediaId
        }
        preferences.edit()
            .putStringSet(FAVORITE_MEDIA_IDS_KEY, nextIds)
            .apply()
        return nextIds
    }

    fun removeFavorites(mediaIds: Set<String>): Set<String> {
        if (mediaIds.isEmpty()) return initialFavoriteIds()
        val nextIds = initialFavoriteIds().toMutableSet().apply {
            removeAll(mediaIds)
        }
        preferences.edit()
            .putStringSet(FAVORITE_MEDIA_IDS_KEY, nextIds)
            .apply()
        return nextIds
    }

    private companion object {
        const val FAVORITE_MEDIA_IDS_KEY = "favorite_media_ids"
    }
}
