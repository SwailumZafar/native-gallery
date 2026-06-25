package com.example.nativegallery.data

import android.content.Context

class HiddenMediaRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_hidden_media",
        Context.MODE_PRIVATE
    )

    fun initialHiddenMediaIds(): Set<String> {
        return preferences.getStringSet(HIDDEN_MEDIA_IDS_KEY, emptySet()).orEmpty().toSet()
    }

    fun setMediaHidden(mediaId: String, hidden: Boolean): Set<String> {
        return setMediaHidden(setOf(mediaId), hidden)
    }

    fun setMediaHidden(mediaIds: Set<String>, hidden: Boolean): Set<String> {
        val nextIds = initialHiddenMediaIds().toMutableSet()
        if (hidden) {
            nextIds += mediaIds
        } else {
            nextIds -= mediaIds
        }
        preferences.edit()
            .putStringSet(HIDDEN_MEDIA_IDS_KEY, nextIds)
            .apply()
        return nextIds
    }

    private companion object {
        const val HIDDEN_MEDIA_IDS_KEY = "hidden_media_ids"
    }
}