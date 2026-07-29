package com.example.nativegallery.data

import android.content.Context

data class AlbumArrangementState(
    val orderIds: List<String> = emptyList(),
    val pinnedAlbumId: String? = null
)

/**
 * Keeps album ordering deterministic when MediaStore adds or removes buckets.
 * Unknown saved IDs are retained so a temporarily unavailable album can return
 * to its previous position after storage access is restored.
 */
object AlbumArrangementPolicy {
    fun arrangedIds(
        availableIds: List<String>,
        state: AlbumArrangementState
    ): List<String> {
        val available = availableIds.distinct()
        val availableSet = available.toHashSet()
        val stored = state.orderIds.asSequence()
            .filter { it in availableSet }
            .distinct()
            .toList()
        val storedSet = stored.toHashSet()
        val unseen = available.filterNot(storedSet::contains)
        val arranged = (stored + unseen).toMutableList()
        state.pinnedAlbumId
            ?.takeIf { it in availableSet }
            ?.let { pinnedId ->
                arranged.remove(pinnedId)
                arranged.add(0, pinnedId)
            }
        return arranged
    }

    fun move(
        availableIds: List<String>,
        state: AlbumArrangementState,
        movedId: String,
        targetId: String,
        placeAfterTarget: Boolean
    ): AlbumArrangementState {
        if (movedId == targetId) return state
        val arranged = arrangedIds(availableIds, state).toMutableList()
        if (!arranged.remove(movedId)) return state
        val targetIndex = arranged.indexOf(targetId)
        if (targetIndex < 0) return state
        val insertionIndex = (targetIndex + if (placeAfterTarget) 1 else 0)
            .coerceIn(0, arranged.size)
        arranged.add(insertionIndex, movedId)

        val availableSet = availableIds.toHashSet()
        val unavailableSavedIds = state.orderIds.filterNot(availableSet::contains)
        return state.copy(
            orderIds = arranged + unavailableSavedIds,
            pinnedAlbumId = state.pinnedAlbumId.takeUnless { it == movedId }
        )
    }

    fun swap(
        availableIds: List<String>,
        state: AlbumArrangementState,
        movedId: String,
        targetId: String
    ): AlbumArrangementState {
        if (movedId == targetId) return state
        val arranged = arrangedIds(availableIds, state).toMutableList()
        val movedIndex = arranged.indexOf(movedId)
        val targetIndex = arranged.indexOf(targetId)
        if (movedIndex < 0 || targetIndex < 0) return state
        arranged[movedIndex] = targetId
        arranged[targetIndex] = movedId

        val availableSet = availableIds.toHashSet()
        val unavailableSavedIds = state.orderIds.filterNot(availableSet::contains)
        val nextPinnedAlbumId = when (state.pinnedAlbumId) {
            movedId -> null
            targetId -> movedId
            else -> state.pinnedAlbumId
        }
        return state.copy(
            orderIds = arranged + unavailableSavedIds,
            pinnedAlbumId = nextPinnedAlbumId
        )
    }

    fun togglePin(
        availableIds: List<String>,
        state: AlbumArrangementState,
        albumId: String
    ): AlbumArrangementState {
        if (albumId !in availableIds) return state
        val nextPinnedId = albumId.takeUnless { state.pinnedAlbumId == albumId }
        val nextState = state.copy(pinnedAlbumId = nextPinnedId)
        return if (nextPinnedId == null) {
            nextState
        } else {
            val arranged = arrangedIds(availableIds, nextState)
            val availableSet = availableIds.toHashSet()
            val unavailableSavedIds = state.orderIds.filterNot(availableSet::contains)
            nextState.copy(orderIds = arranged + unavailableSavedIds)
        }
    }
}

/** Length-prefixed values avoid delimiter collisions in MediaStore bucket IDs. */
internal object AlbumOrderCodec {
    fun encode(ids: List<String>): String = buildString {
        ids.distinct().forEach { id ->
            append(id.length)
            append(':')
            append(id)
        }
    }

    fun decode(encoded: String?): List<String> {
        if (encoded.isNullOrEmpty()) return emptyList()
        val values = mutableListOf<String>()
        var cursor = 0
        while (cursor < encoded.length) {
            val colon = encoded.indexOf(':', startIndex = cursor)
            if (colon <= cursor) return emptyList()
            val length = encoded.substring(cursor, colon).toIntOrNull() ?: return emptyList()
            val valueStart = colon + 1
            if (length < 0 || length > encoded.length - valueStart) return emptyList()
            val valueEnd = valueStart + length
            values += encoded.substring(valueStart, valueEnd)
            cursor = valueEnd
        }
        return values.distinct()
    }
}

class AlbumArrangementRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun load(): AlbumArrangementState = AlbumArrangementState(
        orderIds = AlbumOrderCodec.decode(preferences.getString(OrderKey, null)),
        pinnedAlbumId = preferences.getString(PinnedAlbumKey, null)
    )

    fun save(state: AlbumArrangementState): AlbumArrangementState {
        preferences.edit()
            .putString(OrderKey, AlbumOrderCodec.encode(state.orderIds))
            .putString(PinnedAlbumKey, state.pinnedAlbumId)
            .apply()
        return state
    }

    private companion object {
        const val PreferencesName = "native_gallery_album_arrangement"
        const val OrderKey = "album_order"
        const val PinnedAlbumKey = "pinned_album"
    }
}
