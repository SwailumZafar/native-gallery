package com.example.nativegallery.data

import android.content.Context
import java.util.concurrent.TimeUnit

class RecentlyDeletedRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_recently_deleted",
        Context.MODE_PRIVATE
    )

    fun initialDeletedMedia(): Map<String, Long> {
        return pruneExpired(decodeDeletedMedia())
    }

    fun initialPermanentlyDeletedMediaIds(): Set<String> {
        return preferences.getStringSet(PERMANENTLY_DELETED_IDS_KEY, emptySet()).orEmpty().toSet()
    }

    fun markDeleted(mediaId: String, deletedAtMillis: Long = System.currentTimeMillis()): Map<String, Long> {
        val nextDeleted = initialDeletedMedia().toMutableMap()
        nextDeleted[mediaId] = deletedAtMillis
        saveDeleted(nextDeleted)
        return nextDeleted
    }

    fun markDeleted(mediaIds: List<String>, deletedAtMillis: Long = System.currentTimeMillis()): Map<String, Long> {
        val nextDeleted = initialDeletedMedia().toMutableMap()
        mediaIds.forEachIndexed { index, mediaId ->
            nextDeleted[mediaId] = deletedAtMillis + index
        }
        saveDeleted(nextDeleted)
        return nextDeleted
    }

    fun restore(mediaId: String): Map<String, Long> {
        val nextDeleted = initialDeletedMedia().toMutableMap()
        nextDeleted.remove(mediaId)
        saveDeleted(nextDeleted)
        return nextDeleted
    }

    fun restoreAll(): Map<String, Long> {
        saveDeleted(emptyMap())
        return emptyMap()
    }

    fun deleteForever(mediaId: String): DeleteForeverState {
        val nextDeleted = initialDeletedMedia().toMutableMap()
        nextDeleted.remove(mediaId)
        val nextPermanent = initialPermanentlyDeletedMediaIds() + mediaId
        saveDeleted(nextDeleted)
        savePermanentlyDeleted(nextPermanent)
        return DeleteForeverState(
            deletedMedia = nextDeleted,
            permanentlyDeletedMediaIds = nextPermanent
        )
    }

    fun deleteAllForever(): DeleteForeverState {
        val deletedIds = initialDeletedMedia().keys
        val nextPermanent = initialPermanentlyDeletedMediaIds() + deletedIds
        saveDeleted(emptyMap())
        savePermanentlyDeleted(nextPermanent)
        return DeleteForeverState(
            deletedMedia = emptyMap(),
            permanentlyDeletedMediaIds = nextPermanent
        )
    }

    private fun decodeDeletedMedia(): Map<String, Long> {
        return preferences.getStringSet(DELETED_MEDIA_KEY, emptySet())
            .orEmpty()
            .mapNotNull { encoded ->
                val separatorIndex = encoded.lastIndexOf(ENTRY_SEPARATOR)
                if (separatorIndex <= 0 || separatorIndex >= encoded.lastIndex) {
                    return@mapNotNull null
                }
                val mediaId = encoded.substring(0, separatorIndex)
                val deletedAt = encoded.substring(separatorIndex + 1).toLongOrNull() ?: return@mapNotNull null
                mediaId to deletedAt
            }
            .toMap()
    }

    private fun pruneExpired(deletedMedia: Map<String, Long>): Map<String, Long> {
        val nowMillis = System.currentTimeMillis()
        val pruned = deletedMedia.filterValues { deletedAtMillis ->
            nowMillis - deletedAtMillis < RetentionMillis
        }
        if (pruned.size != deletedMedia.size) {
            saveDeleted(pruned)
        }
        return pruned
    }

    private fun saveDeleted(deletedMedia: Map<String, Long>) {
        preferences.edit()
            .putStringSet(
                DELETED_MEDIA_KEY,
                deletedMedia.map { (mediaId, deletedAtMillis) ->
                    "$mediaId$ENTRY_SEPARATOR$deletedAtMillis"
                }.toSet()
            )
            .apply()
    }

    private fun savePermanentlyDeleted(mediaIds: Set<String>) {
        preferences.edit()
            .putStringSet(PERMANENTLY_DELETED_IDS_KEY, mediaIds)
            .apply()
    }

    data class DeleteForeverState(
        val deletedMedia: Map<String, Long>,
        val permanentlyDeletedMediaIds: Set<String>
    )

    companion object {
        const val RetentionDays = 30L
        val RetentionMillis: Long = TimeUnit.DAYS.toMillis(RetentionDays)

        private const val DELETED_MEDIA_KEY = "deleted_media"
        private const val PERMANENTLY_DELETED_IDS_KEY = "permanently_deleted_media_ids"
        private const val ENTRY_SEPARATOR = "|"
    }
}
