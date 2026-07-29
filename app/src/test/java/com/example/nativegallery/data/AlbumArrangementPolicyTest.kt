package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumArrangementPolicyTest {
    @Test
    fun swap_exchangesHoveredAlbumsAndUnpinsDraggedHero() {
        val albums = listOf("all", "screenshots", "camera", "downloads")
        val state = AlbumArrangementState(
            orderIds = albums,
            pinnedAlbumId = "all"
        )

        val swapped = AlbumArrangementPolicy.swap(
            availableIds = albums,
            state = state,
            movedId = "all",
            targetId = "camera"
        )

        assertEquals(listOf("camera", "screenshots", "all", "downloads"), swapped.orderIds)
        assertNull(swapped.pinnedAlbumId)
    }

    @Test
    fun swapOntoPinnedHeroTransfersPinToDraggedAlbum() {
        val albums = listOf("all", "camera", "screenshots", "downloads")
        val state = AlbumArrangementState(
            orderIds = albums,
            pinnedAlbumId = "camera"
        )

        val swapped = AlbumArrangementPolicy.swap(
            availableIds = albums,
            state = state,
            movedId = "downloads",
            targetId = "camera"
        )

        assertEquals("downloads", swapped.pinnedAlbumId)
        assertEquals(
            listOf("downloads", "all", "screenshots", "camera"),
            AlbumArrangementPolicy.arrangedIds(albums, swapped)
        )
    }

    private val albums = listOf("all", "camera", "screenshots", "downloads")

    @Test
    fun savedOrderKeepsNewAlbumsWithoutLosingTheirSourceOrder() {
        val state = AlbumArrangementState(
            orderIds = listOf("screenshots", "camera", "temporarily-missing")
        )

        assertEquals(
            listOf("screenshots", "camera", "all", "downloads"),
            AlbumArrangementPolicy.arrangedIds(albums, state)
        )
    }

    @Test
    fun movePlacesAlbumAtDropSideAndPreservesUnavailableIds() {
        val state = AlbumArrangementState(
            orderIds = listOf("all", "camera", "screenshots", "downloads", "usb")
        )

        val moved = AlbumArrangementPolicy.move(
            availableIds = albums,
            state = state,
            movedId = "downloads",
            targetId = "all",
            placeAfterTarget = false
        )

        assertEquals(
            listOf("downloads", "all", "camera", "screenshots", "usb"),
            moved.orderIds
        )
    }

    @Test
    fun pinMovesOneAlbumToTheLeadingPositionAndToggleUnpinsIt() {
        val pinned = AlbumArrangementPolicy.togglePin(
            availableIds = albums,
            state = AlbumArrangementState(orderIds = albums),
            albumId = "screenshots"
        )

        assertEquals("screenshots", pinned.pinnedAlbumId)
        assertEquals("screenshots", AlbumArrangementPolicy.arrangedIds(albums, pinned).first())

        val unpinned = AlbumArrangementPolicy.togglePin(albums, pinned, "screenshots")
        assertNull(unpinned.pinnedAlbumId)
    }

    @Test
    fun draggingPinnedAlbumAwayAlsoUnpinsIt() {
        val state = AlbumArrangementState(
            orderIds = albums,
            pinnedAlbumId = "camera"
        )

        val moved = AlbumArrangementPolicy.move(
            availableIds = albums,
            state = state,
            movedId = "camera",
            targetId = "screenshots",
            placeAfterTarget = true
        )

        assertNull(moved.pinnedAlbumId)
        assertEquals(
            listOf("all", "screenshots", "camera", "downloads"),
            AlbumArrangementPolicy.arrangedIds(albums, moved)
        )
    }

    @Test
    fun orderCodecRoundTripsIdsContainingPunctuation() {
        val ids = listOf("all", "DCIM/Camera", "bucket:with,delimiters", "واٹس ایپ")

        assertEquals(ids, AlbumOrderCodec.decode(AlbumOrderCodec.encode(ids)))
        assertEquals(emptyList<String>(), AlbumOrderCodec.decode("broken"))
    }
}
