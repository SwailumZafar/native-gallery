package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyDeletedStateTest {
    @Test
    fun restoreBatch_removesOnlyRequestedMedia() {
        val deletedMedia = mapOf("a" to 10L, "b" to 20L, "c" to 30L)

        assertEquals(
            mapOf("b" to 20L),
            restoredDeletedMedia(
                deletedMedia = deletedMedia,
                restoredMediaIds = setOf("a", "c", "missing")
            )
        )
    }

    @Test
    fun permanentDeleteBatch_removesRequestedMediaAndPreservesHistory() {
        val state = permanentlyDeletedState(
            deletedMedia = mapOf("a" to 10L, "b" to 20L, "c" to 30L),
            permanentlyDeletedMediaIds = setOf("older"),
            deletedMediaIds = setOf("a", "c")
        )

        assertEquals(mapOf("b" to 20L), state.deletedMedia)
        assertEquals(setOf("older", "a", "c"), state.permanentlyDeletedMediaIds)
    }
}
