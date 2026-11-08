package com.example.nativegallery.ui

import com.example.nativegallery.model.Album
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumEntryPreloadTest {
    private val albums = (0..12).map { Album("album-$it", "Album $it", 10, emptyList()) }

    @Test fun bigTilesSkipTheLeadingHeroWhenResolvingGridRows() {
        val visible = albumEntryPreloadCandidates(albums,
            listOf("header", "album-hero", "big-album-row-2-0", "big-album-row-2-1"), true)
        assertEquals(albums.take(5), visible)
    }

    @Test fun mediumAndBasicLayoutsUseTheActualVisibleColumns() {
        assertEquals(albums.subList(4, 6), albumEntryPreloadCandidates(albums, listOf("big-album-row-2-2"), false))
        assertEquals(albums.subList(6, 9), albumEntryPreloadCandidates(albums, listOf("basic-album-row-3-2"), false))
    }

    @Test fun heroAndListLayoutsPreserveIdsContainingHyphensAndVisualOrder() {
        val reordered = albums.reversed()
        assertEquals(listOf(albums[8], albums[2]), albumEntryPreloadCandidates(reordered,
            listOf("hero-album-album-8", "list-album-album-2", "hero-album-album-8"), false))
    }

    @Test fun staleKeysAreIgnoredAndWarmupIsBounded() {
        assertEquals(emptyList<Album>(), albumEntryPreloadCandidates(albums,
            listOf("header", "list-album-missing", "big-album-row-2--1", "basic-album-row-3-2147483647"), false))
        assertEquals(8, albumEntryPreloadCandidates(albums, albums.map { "list-album-${it.id}" }, false).size)
    }
}
