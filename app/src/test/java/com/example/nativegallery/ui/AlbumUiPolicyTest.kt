package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import com.example.nativegallery.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumUiPolicyTest {
    @Test
    fun dropTargetAcceptsAnAlbumPlacedAcrossAnotherCardsEdge() {
        val targetId = selectAlbumDropTargetId(
            draggedAlbumId = "camera",
            draggedBounds = Rect(70f, 70f, 120f, 120f),
            orderedAlbumIds = listOf("camera", "screenshots"),
            boundsByAlbumId = mapOf(
                "camera" to Rect(0f, 0f, 50f, 50f),
                "screenshots" to Rect(100f, 100f, 200f, 200f)
            )
        )

        assertEquals("screenshots", targetId)
    }

    @Test
    fun dropTargetRejectsCardsThatAreStillFarAway() {
        val targetId = selectAlbumDropTargetId(
            draggedAlbumId = "camera",
            draggedBounds = Rect(0f, 0f, 50f, 50f),
            orderedAlbumIds = listOf("camera", "screenshots"),
            boundsByAlbumId = mapOf(
                "camera" to Rect(0f, 0f, 50f, 50f),
                "screenshots" to Rect(300f, 300f, 400f, 400f)
            )
        )

        assertNull(targetId)
    }

    @Test
    fun hiddenAlbumsComeFirstWithoutChangingOrderWithinEachGroup() {
        val albums = listOf(
            album("camera"),
            album("screenshots"),
            album("downloads"),
            album("videos")
        )

        val ordered = orderAlbumsForHiddenItems(
            albums = albums,
            hiddenAlbumIds = setOf("screenshots", "videos")
        )

        assertEquals(
            listOf("screenshots", "videos", "camera", "downloads"),
            ordered.map(Album::id)
        )
    }

    private fun album(id: String): Album = Album(
        id = id,
        name = id,
        itemCount = 1,
        coverMediaIds = emptyList()
    )
}
