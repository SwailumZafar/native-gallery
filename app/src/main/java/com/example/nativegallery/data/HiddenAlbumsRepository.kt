package com.example.nativegallery.data

import android.content.Context

class HiddenAlbumsRepository(
    context: Context,
    private val startingHiddenAlbumIds: Set<String> = emptySet()
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_hidden_albums",
        Context.MODE_PRIVATE
    )

    fun initialHiddenAlbumIds(): Set<String> {
        return preferences.getStringSet(HIDDEN_ALBUM_IDS_KEY, startingHiddenAlbumIds).orEmpty()
    }

    fun setAlbumHidden(albumId: String, hidden: Boolean) {
        val nextIds = initialHiddenAlbumIds().toMutableSet()
        if (hidden) {
            nextIds += albumId
        } else {
            nextIds -= albumId
        }
        preferences.edit()
            .putStringSet(HIDDEN_ALBUM_IDS_KEY, nextIds)
            .apply()
    }

    private companion object {
        const val HIDDEN_ALBUM_IDS_KEY = "hidden_album_ids"
    }
}
