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
        return restore(listOf(mediaId))
    }

    fun restore(mediaIds: Collection<String>): Map<String, Long> {
        val nextDeleted = restoredDeletedMedia(
            deletedMedia = initialDeletedMedia(),
            restoredMediaIds = mediaIds.toSet()
        )
        saveDeleted(nextDeleted)
        return nextDeleted
    }

    fun restoreAll(): Map<String, Long> {
        saveDeleted(emptyMap())
        return emptyMap()
    }

    fun deleteForever(mediaId: String): DeleteForeverState {
        return deleteForever(listOf(mediaId))
    }

    fun deleteForever(mediaIds: Collection<String>): DeleteForeverState {
        val nextState = permanentlyDeletedState(
            deletedMedia = initialDeletedMedia(),
            permanentlyDeletedMediaIds = initialPermanentlyDeletedMediaIds(),
            deletedMediaIds = mediaIds.toSet()
        )
        saveDeleted(nextState.deletedMedia)
        savePermanentlyDeleted(nextState.permanentlyDeletedMediaIds)
        return nextState
    }

    fun deleteAllForever(): DeleteForeverState {
        val deletedMedia = initialDeletedMedia()
        return deleteForever(deletedMedia.keys)
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

internal fun restoredDeletedMedia(
    deletedMedia: Map<String, Long>,
    restoredMediaIds: Set<String>
): Map<String, Long> {
    if (restoredMediaIds.isEmpty()) return deletedMedia
    return deletedMedia.filterKeys { mediaId -> mediaId !in restoredMediaIds }
}

internal fun permanentlyDeletedState(
    deletedMedia: Map<String, Long>,
    permanentlyDeletedMediaIds: Set<String>,
    deletedMediaIds: Set<String>
): RecentlyDeletedRepository.DeleteForeverState {
    return RecentlyDeletedRepository.DeleteForeverState(
        deletedMedia = deletedMedia.filterKeys { mediaId -> mediaId !in deletedMediaIds },
        permanentlyDeletedMediaIds = permanentlyDeletedMediaIds + deletedMediaIds
    )
}
