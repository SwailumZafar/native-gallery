package com.example.nativegallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
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
        if (mediaKinds.isEmpty()) {
            return GallerySnapshot(emptyList(), emptyList())
        }

        val rows = queryMedia(mediaKinds)
        return GallerySnapshot(
            mediaItems = rows.map { it.mediaItem },
            albums = buildAlbums(rows)
        )
    }

    private fun queryMedia(mediaKinds: Set<MediaKind>): List<MediaStoreRow> {
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

        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Video.Media.DURATION
        )
        val placeholders = mediaTypes.joinToString(separator = ",") { "?" }
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN ($placeholders)"
        val selectionArgs = mediaTypes.map { it.toString() }.toTypedArray()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        val rows = mutableListOf<MediaStoreRow>()

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaTypeValue = cursor.getInt(typeColumn)
                val mediaType = if (mediaTypeValue == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    MediaType.Video
                } else {
                    MediaType.Photo
                }
                val bucketId = cursor.getString(bucketIdColumn)?.takeIf { it.isNotBlank() } ?: "unknown"
                val bucketName = cursor.getString(bucketNameColumn)?.takeIf { it.isNotBlank() } ?: "Other"
                val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() } ?: bucketName
                val dateTakenMillis = cursor.getLong(dateTakenColumn)
                val dateModifiedSeconds = cursor.getLong(dateModifiedColumn)
                val durationMillis = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
                val contentUri = contentUriFor(mediaType, id)
                val mediaItem = MediaItem(
                    id = "${mediaType.name.lowercase(Locale.US)}-$id",
                    albumId = bucketId,
                    type = mediaType,
                    title = title,
                    dateLabel = dateLabel(dateTakenMillis, dateModifiedSeconds),
                    contentUri = contentUri,
                    isVideo = mediaType == MediaType.Video,
                    durationLabel = if (mediaType == MediaType.Video) formatDuration(durationMillis) else null
                )
                rows += MediaStoreRow(mediaItem, bucketName)
            }
        }

        return rows
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

    private companion object {
        val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    }
}
