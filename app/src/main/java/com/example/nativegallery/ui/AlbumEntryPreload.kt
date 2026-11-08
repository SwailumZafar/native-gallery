package com.example.nativegallery.ui

import com.example.nativegallery.model.Album

/** Resolve the actual visible rows, including reordered albums and the leading Big tile. */
internal fun albumEntryPreloadCandidates(
    albums: List<Album>,
    visibleKeys: List<String>,
    hasLeadingHero: Boolean
): List<Album> = buildList {
    val byId = albums.associateBy { it.id }
    visibleKeys.forEach { key ->
        when {
            key == "album-hero" -> albums.firstOrNull()?.let(::add)
            key.startsWith("hero-album-") -> byId[key.removePrefix("hero-album-")]?.let(::add)
            key.startsWith("list-album-") -> byId[key.removePrefix("list-album-")]?.let(::add)
            key.startsWith("big-album-row-") || key.startsWith("basic-album-row-") -> {
                val parts = key.split('-')
                val columns = parts.getOrNull(3)?.toIntOrNull()?.coerceIn(1, 10) ?: return@forEach
                val row = parts.getOrNull(4)?.toIntOrNull()?.takeIf { it >= 0 } ?: return@forEach
                val offset = if (hasLeadingHero && key.startsWith("big-")) 1 else 0
                val start = row.toLong() * columns + offset
                if (start < albums.size) addAll(albums.subList(start.toInt(), minOf(start.toInt() + columns, albums.size)))
            }
        }
    }
}.distinctBy { it.id }.take(8)
