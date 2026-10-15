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

    @Test
    fun albumDetailGridModes_supportSixDensitiesAndProperPinchTransitions() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6), AlbumDetailGridMode.entries.map { it.columns })

        // Test cycle
        assertEquals(AlbumDetailGridMode.Spacious, AlbumDetailGridMode.Single.next())
        assertEquals(AlbumDetailGridMode.Comfortable, AlbumDetailGridMode.Spacious.next())
        assertEquals(AlbumDetailGridMode.Compact, AlbumDetailGridMode.Comfortable.next())
        assertEquals(AlbumDetailGridMode.Dense, AlbumDetailGridMode.Compact.next())
        assertEquals(AlbumDetailGridMode.Micro, AlbumDetailGridMode.Dense.next())
        assertEquals(AlbumDetailGridMode.Single, AlbumDetailGridMode.Micro.next())

        // Test zoomIn (fewer columns, larger pictures)
        assertEquals(AlbumDetailGridMode.Single, AlbumDetailGridMode.Single.zoomIn())
        assertEquals(AlbumDetailGridMode.Single, AlbumDetailGridMode.Spacious.zoomIn())
        assertEquals(AlbumDetailGridMode.Spacious, AlbumDetailGridMode.Comfortable.zoomIn())
        assertEquals(AlbumDetailGridMode.Comfortable, AlbumDetailGridMode.Compact.zoomIn())
        assertEquals(AlbumDetailGridMode.Compact, AlbumDetailGridMode.Dense.zoomIn())
        assertEquals(AlbumDetailGridMode.Dense, AlbumDetailGridMode.Micro.zoomIn())

        // Test zoomOut (more columns, smaller pictures)
        assertEquals(AlbumDetailGridMode.Spacious, AlbumDetailGridMode.Single.zoomOut())
        assertEquals(AlbumDetailGridMode.Comfortable, AlbumDetailGridMode.Spacious.zoomOut())
        assertEquals(AlbumDetailGridMode.Compact, AlbumDetailGridMode.Comfortable.zoomOut())
        assertEquals(AlbumDetailGridMode.Dense, AlbumDetailGridMode.Compact.zoomOut())
        assertEquals(AlbumDetailGridMode.Micro, AlbumDetailGridMode.Dense.zoomOut())
        assertEquals(AlbumDetailGridMode.Micro, AlbumDetailGridMode.Micro.zoomOut())
    }

    @Test
    fun albumDetailGridModes_containCorrectLabels() {
        assertEquals("Single", AlbumDetailGridMode.Single.label())
        assertEquals("Spacious", AlbumDetailGridMode.Spacious.label())
        assertEquals("Comfort", AlbumDetailGridMode.Comfortable.label())
        assertEquals("Compact", AlbumDetailGridMode.Compact.label())
        assertEquals("Dense", AlbumDetailGridMode.Dense.label())
        assertEquals("Micro", AlbumDetailGridMode.Micro.label())
    }

    @Test
    fun calculateAlbumGridAnchorRow_maintainsPositionAcrossColumnChanges() {
        // 20 items, viewing row 2 with 3 columns (items 6, 7, 8) -> anchor item is 6
        // Changing to 2 columns: anchor item 6 / 2 columns = row 3
        assertEquals(3, calculateAlbumGridAnchorRow(firstVisibleRow = 2, oldColumns = 3, newColumns = 2, totalItems = 20))

        // Viewing row 3 with 2 columns (items 6, 7) -> anchor item is 6
        // Changing to 4 columns: anchor item 6 / 4 columns = row 1
        assertEquals(1, calculateAlbumGridAnchorRow(firstVisibleRow = 3, oldColumns = 2, newColumns = 4, totalItems = 20))

        // Viewing row 0 stays at row 0 regardless of column changes
        assertEquals(0, calculateAlbumGridAnchorRow(firstVisibleRow = 0, oldColumns = 4, newColumns = 1, totalItems = 20))

        // Empty list safely returns 0
        assertEquals(0, calculateAlbumGridAnchorRow(firstVisibleRow = 5, oldColumns = 3, newColumns = 4, totalItems = 0))

        // Invalid column inputs safely return 0
        assertEquals(0, calculateAlbumGridAnchorRow(firstVisibleRow = 2, oldColumns = 0, newColumns = 4, totalItems = 20))
        assertEquals(0, calculateAlbumGridAnchorRow(firstVisibleRow = 2, oldColumns = 3, newColumns = 0, totalItems = 20))

        // Row index past the end clamps to the last item
        assertEquals(4, calculateAlbumGridAnchorRow(firstVisibleRow = 999, oldColumns = 4, newColumns = 4, totalItems = 20))
    }

    @Test
    fun folderTransitionStyles_containOnlyNormalAndFast() {
        val styles = com.example.nativegallery.data.FolderTransitionStyle.entries.map { it.name }
        assertEquals(listOf("Normal", "Fast"), styles)

        assertEquals("Normal", com.example.nativegallery.data.FolderTransitionStyle.Normal.label)
        assertEquals("Fast Enhanced", com.example.nativegallery.data.FolderTransitionStyle.Fast.label)
        assertEquals("Original smooth fluid LivingFlow transition", com.example.nativegallery.data.FolderTransitionStyle.Normal.description)
        assertEquals("Snappy high-speed LivingFlow transition with accelerated response", com.example.nativegallery.data.FolderTransitionStyle.Fast.description)
    }

    private fun album(id: String): Album = Album(
        id = id,
        name = id,
        itemCount = 1,
        coverMediaIds = emptyList()
    )
}
