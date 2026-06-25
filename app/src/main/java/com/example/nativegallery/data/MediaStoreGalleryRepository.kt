package com.example.nativegallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.util.GalleryPerformanceMonitor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaStoreGalleryRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot {
        return GalleryPerformanceMonitor.trace("MediaStore full gallery load") {
            if (mediaKinds.isEmpty()) {
                return@trace GallerySnapshot(emptyList(), emptyList())
            }

            val rows = queryMedia(mediaKinds)
            GallerySnapshot(
                mediaItems = rows.map { it.mediaItem },
                albums = buildAlbums(rows)
            )
        }
    }

    fun loadGalleryPage(
        mediaKinds: Set<MediaKind>,
        limit: Int,
        offset: Int = 0
    ): GallerySnapshot {
        return GalleryPerformanceMonitor.trace("MediaStore gallery page load") {
            if (mediaKinds.isEmpty() || limit <= 0) {
                return@trace GallerySnapshot(emptyList(), emptyList())
            }

            val rows = queryMedia(
                mediaKinds = mediaKinds,
                limit = limit,
                offset = offset.coerceAtLeast(0)
            )
            GallerySnapshot(
                mediaItems = rows.map { it.mediaItem },
                albums = buildAlbums(rows)
            )
        }
    }

    private fun queryMedia(
        mediaKinds: Set<MediaKind>,
        limit: Int? = null,
        offset: Int = 0
    ): List<MediaStoreRow> {
        val resolver = appContext.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val mediaTypes = buildList {
            if (MediaKind.Images in mediaKinds) add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            if (MediaKind.Videos in mediaKinds) add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        }
        if (mediaTypes.isEmpty()) {
            return emptyList()
        }

        val projection = buildList {
            add(BaseColumns._ID)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.BUCKET_ID)
            add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.Video.Media.DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.MediaColumns.DATA)
            }
        }.toTypedArray()
        val placeholders = mediaTypes.joinToString(separator = ",") { "?" }
        val selection = (listOf(
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN ($placeholders)"
        ) + MediaStoreVisibilityPolicy.selectionClauses()).joinToString(separator = " AND ")
        val selectionArgs = mediaTypes.map { it.toString() }.toTypedArray()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        val rows = mutableListOf<MediaStoreRow>()

        val cursor = queryMediaCursor(
            resolver = resolver,
            collection = collection,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = sortOrder,
            limit = limit,
            offset = offset
        )

        cursor?.use { mediaCursor ->
            val idColumn = mediaCursor.getColumnIndexOrThrow(BaseColumns._ID)
            val typeColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val titleColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val bucketIdColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = mediaCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val mimeTypeColumn = mediaCursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = mediaCursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val widthColumn = mediaCursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightColumn = mediaCursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationColumn = mediaCursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaCursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                mediaCursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            }

            while (mediaCursor.moveToNext()) {
                val path = if (pathColumn >= 0 && !mediaCursor.isNull(pathColumn)) {
                    mediaCursor.getString(pathColumn)
                } else {
                    null
                }
                if (!MediaStoreVisibilityPolicy.shouldIncludePath(path)) {
                    continue
                }

                val id = mediaCursor.getLong(idColumn)
                val mediaTypeValue = mediaCursor.getInt(typeColumn)
                val mediaType = if (mediaTypeValue == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    MediaType.Video
                } else {
                    MediaType.Photo
                }
                val bucketId = mediaCursor.getString(bucketIdColumn)?.takeIf { it.isNotBlank() } ?: "unknown"
                val bucketName = mediaCursor.getString(bucketNameColumn)?.takeIf { it.isNotBlank() } ?: "Other"
                val title = mediaCursor.getString(titleColumn)?.takeIf { it.isNotBlank() } ?: bucketName
                val dateTakenMillis = mediaCursor.getLong(dateTakenColumn)
                val dateModifiedSeconds = mediaCursor.getLong(dateModifiedColumn)
                val rawDurationMillis = if (durationColumn >= 0 && !mediaCursor.isNull(durationColumn)) mediaCursor.getLong(durationColumn) else 0L
                val videoDurationMillis = rawDurationMillis.takeIf { mediaType == MediaType.Video && it > 0L }
                val mimeType = if (mimeTypeColumn >= 0 && !mediaCursor.isNull(mimeTypeColumn)) {
                    mediaCursor.getString(mimeTypeColumn)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                val fileSizeBytes = if (sizeColumn >= 0 && !mediaCursor.isNull(sizeColumn)) {
                    mediaCursor.getLong(sizeColumn).takeIf { it > 0L }
                } else {
                    null
                }
                val width = if (widthColumn >= 0 && !mediaCursor.isNull(widthColumn)) {
                    mediaCursor.getInt(widthColumn).takeIf { it > 0 }
                } else {
                    null
                }
                val height = if (heightColumn >= 0 && !mediaCursor.isNull(heightColumn)) {
                    mediaCursor.getInt(heightColumn).takeIf { it > 0 }
                } else {
                    null
                }
                val contentUri = contentUriFor(mediaType, id)
                val mediaItem = MediaItem(
                    id = "${mediaType.name.lowercase(Locale.US)}-$id",
                    albumId = bucketId,
                    type = mediaType,
                    title = title,
                    dateLabel = dateLabel(dateTakenMillis, dateModifiedSeconds),
                    contentUri = contentUri,
                    isVideo = mediaType == MediaType.Video,
                    durationLabel = videoDurationMillis?.let(::formatDuration),
                    durationMillis = videoDurationMillis,
                    mimeType = mimeType,
                    fileSizeBytes = fileSizeBytes,
                    width = width,
                    height = height
                )
                rows += MediaStoreRow(mediaItem, bucketName)
            }
        }

        return rows
    }

    private fun queryMediaCursor(
        resolver: ContentResolver,
        collection: Uri,
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        limit: Int?,
        offset: Int
    ) = if (limit == null) {
        resolver.query(collection, projection, selection, selectionArgs, sortOrder)
    } else {
        runCatching {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED)
                )
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset.coerceAtLeast(0))
            }
            resolver.query(collection, projection, queryArgs, null)
        }.getOrElse {
            val limitedSortOrder = "$sortOrder LIMIT $limit OFFSET ${offset.coerceAtLeast(0)}"
            resolver.query(collection, projection, selection, selectionArgs, limitedSortOrder)
        }
    }

    private fun buildAlbums(rows: List<MediaStoreRow>): List<Album> {
        if (rows.isEmpty()) {
            return emptyList()
        }

        val allCover = rows.first().mediaItem
        val allPhotos = Album(
            id = "all",
            name = "All photos",
            itemCount = rows.size,
            coverMediaIds = rows.take(4).map { it.mediaItem.id },
            coverUri = allCover.contentUri,
            isAllPhotos = true
        )
        val regularAlbums = rows
            .groupBy { it.mediaItem.albumId }
            .map { (albumId, albumRows) ->
                val cover = albumRows.first().mediaItem
                Album(
                    id = albumId,
                    name = albumRows.first().albumName,
                    itemCount = albumRows.size,
                    coverMediaIds = albumRows.take(4).map { it.mediaItem.id },
                    coverUri = cover.contentUri
                )
            }
        return listOf(allPhotos) + regularAlbums
    }

    private fun contentUriFor(mediaType: MediaType, id: Long): Uri {
        val collection = if (mediaType == MediaType.Video) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(collection, id)
    }

    private fun dateLabel(dateTakenMillis: Long, dateModifiedSeconds: Long): String {
        val timestampMillis = when {
            dateTakenMillis > 0L -> dateTakenMillis
            dateModifiedSeconds > 0L -> dateModifiedSeconds * 1000L
            else -> System.currentTimeMillis()
        }
        val date = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return if (date == LocalDate.now()) {
            "Today"
        } else {
            date.format(DateFormatter)
        }
    }

    private fun formatDuration(durationMillis: Long): String? {
        if (durationMillis <= 0L) {
            return null
        }
        val totalSeconds = durationMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private data class MediaStoreRow(
        val mediaItem: MediaItem,
        val albumName: String
    )

    companion object {
        const val InitialGalleryPageSize = 360

        val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    }
}
