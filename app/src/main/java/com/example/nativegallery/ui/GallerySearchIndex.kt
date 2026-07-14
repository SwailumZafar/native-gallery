package com.example.nativegallery.ui

import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem

internal data class GallerySearchResult(
    val normalizedQuery: String,
    val mediaItems: List<MediaItem>,
    val albums: List<Album>
)

internal class GallerySearchIndex private constructor(
    private val mediaDocuments: List<MediaSearchDocument>,
    private val albumDocuments: List<AlbumSearchDocument>
) {
    fun search(query: String): GallerySearchResult {
        val normalizedQuery = normalizeGallerySearchQuery(query)
        if (normalizedQuery.isBlank()) {
            return GallerySearchResult(
                normalizedQuery = normalizedQuery,
                mediaItems = mediaDocuments.map { it.mediaItem },
                albums = albumDocuments.map { it.album }
            )
        }

        val matchingMedia = ArrayList<MediaItem>()
        val matchingAlbumIds = HashSet<String>()
        mediaDocuments.forEach { document ->
            if (document.searchableText.contains(normalizedQuery)) {
                matchingMedia += document.mediaItem
                matchingAlbumIds += document.mediaItem.albumId
            }
        }
        val hasMatchingMedia = matchingMedia.isNotEmpty()
        val matchingAlbums = albumDocuments
            .asSequence()
            .filter { document ->
                document.normalizedName.contains(normalizedQuery) ||
                    (document.album.isAllPhotos && hasMatchingMedia) ||
                    document.album.id in matchingAlbumIds
            }
            .map { it.album }
            .toList()

        return GallerySearchResult(
            normalizedQuery = normalizedQuery,
            mediaItems = matchingMedia,
            albums = matchingAlbums
        )
    }

    companion object {
        fun build(mediaItems: List<MediaItem>, albums: List<Album>): GallerySearchIndex {
            val albumNameById = albums.associate { album -> album.id to album.name }
            return GallerySearchIndex(
                mediaDocuments = mediaItems.map { mediaItem ->
                    MediaSearchDocument(
                        mediaItem = mediaItem,
                        searchableText = searchableText(
                            mediaItem.title,
                            mediaItem.dateLabel,
                            mediaItem.type.name,
                            mediaItem.mimeType,
                            albumNameById[mediaItem.albumId]
                        )
                    )
                },
                albumDocuments = albums.map { album ->
                    AlbumSearchDocument(
                        album = album,
                        normalizedName = normalizeGallerySearchQuery(album.name)
                    )
                }
            )
        }
    }
}

internal fun normalizeGallerySearchQuery(query: String): String = query.trim().lowercase()

private data class MediaSearchDocument(
    val mediaItem: MediaItem,
    val searchableText: String
)

private data class AlbumSearchDocument(
    val album: Album,
    val normalizedName: String
)

private fun searchableText(vararg values: String?): String {
    return values
        .asSequence()
        .filterNotNull()
        .joinToString(separator = "\u0000") { value -> value.lowercase() }
}
