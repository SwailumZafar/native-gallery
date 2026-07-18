package com.example.nativegallery.data

import android.provider.MediaStore
import com.example.nativegallery.model.MediaType
import java.util.Locale

internal data class MediaStoreFormatSelection(
    val clause: String,
    val args: Array<String>
)

internal object MediaStoreMediaFormatPolicy {
    private val ImageExtensions = setOf(
        "jpg",
        "jpeg",
        "png",
        "webp",
        "gif",
        "bmp",
        "heic",
        "heif",
        "avif",
        "dng"
    )

    private val VideoExtensions = setOf(
        "mp4",
        "m4v",
        "3gp",
        "3gpp",
        "mkv",
        "webm",
        "avi",
        "mov",
        "qt",
        "flv",
        "wmv",
        "asf",
        "ts",
        "m2ts",
        "mts",
        "ogv"
    )

    private val ImageMimePrefixes = listOf("image/")
    private val VideoMimePrefixes = listOf("video/")

    private val ExplicitImageMimes = mapOf(
        "image/jpg" to MediaType.Photo,
        "image/jpeg" to MediaType.Photo,
        "image/png" to MediaType.Photo,
        "image/webp" to MediaType.Photo,
        "image/gif" to MediaType.Photo,
        "image/bmp" to MediaType.Photo,
        "image/x-ms-bmp" to MediaType.Photo,
        "image/heic" to MediaType.Photo,
        "image/heif" to MediaType.Photo,
        "image/avif" to MediaType.Photo,
        "image/x-adobe-dng" to MediaType.Photo
    )

    private val ExplicitVideoMimes = mapOf(
        "video/mp4" to MediaType.Video,
        "video/3gpp" to MediaType.Video,
        "video/3gpp2" to MediaType.Video,
        "video/x-matroska" to MediaType.Video,
        "video/webm" to MediaType.Video,
        "video/avi" to MediaType.Video,
        "video/x-msvideo" to MediaType.Video,
        "video/quicktime" to MediaType.Video,
        "video/x-flv" to MediaType.Video,
        "video/x-ms-wmv" to MediaType.Video,
        "video/x-ms-asf" to MediaType.Video,
        "video/mp2ts" to MediaType.Video,
        "video/ogg" to MediaType.Video
    )

    fun selectionFor(mediaKinds: Set<MediaKind>): MediaStoreFormatSelection? {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        val mediaTypes = buildList {
            if (MediaKind.Images in mediaKinds) add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            if (MediaKind.Videos in mediaKinds) add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        }

        if (mediaTypes.isNotEmpty()) {
            clauses += "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${mediaTypes.joinToString(",") { "?" }})"
            args += mediaTypes.map { it.toString() }
        }

        val mimePrefixes = buildList {
            if (MediaKind.Images in mediaKinds) addAll(ImageMimePrefixes)
            if (MediaKind.Videos in mediaKinds) addAll(VideoMimePrefixes)
        }
        mimePrefixes.forEach { prefix ->
            clauses += "LOWER(${MediaStore.MediaColumns.MIME_TYPE}) LIKE ?"
            args += "$prefix%"
        }

        val extensions = buildList {
            if (MediaKind.Images in mediaKinds) addAll(ImageExtensions)
            if (MediaKind.Videos in mediaKinds) addAll(VideoExtensions)
        }
        extensions.forEach { extension ->
            clauses += "LOWER(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ?"
            args += "%.$extension"
        }

        if (clauses.isEmpty()) return null
        return MediaStoreFormatSelection(
            clause = clauses.joinToString(separator = " OR ", prefix = "(", postfix = ")"),
            args = args.toTypedArray()
        )
    }

    fun mediaTypeFor(
        mediaStoreType: Int,
        mimeType: String?,
        displayName: String?
    ): MediaType? {
        return when (mediaStoreType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaType.Photo
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaType.Video
            else -> inferredMediaType(mimeType, displayName)
        }
    }

    fun isNativeMediaStoreType(mediaStoreType: Int, mediaType: MediaType): Boolean {
        return when (mediaType) {
            MediaType.Photo -> mediaStoreType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            MediaType.Video -> mediaStoreType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        }
    }

    fun normalizedMimeType(mimeType: String?, displayName: String?): String? {
        val extensionMimeType = mimeTypeForExtension(displayName)
        val normalized = mimeType
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
        return when (normalized) {
            null -> extensionMimeType
            "application/octet-stream", "binary/octet-stream" -> extensionMimeType ?: normalized
            else -> normalized
        }
    }

    private fun inferredMediaType(mimeType: String?, displayName: String?): MediaType? {
        normalizedMimeType(mimeType, displayName)?.let { normalizedMime ->
            ExplicitImageMimes[normalizedMime]?.let { return it }
            ExplicitVideoMimes[normalizedMime]?.let { return it }
            if (normalizedMime.startsWith("image/")) return MediaType.Photo
            if (normalizedMime.startsWith("video/")) return MediaType.Video
        }

        return when (extensionFor(displayName)) {
            in ImageExtensions -> MediaType.Photo
            in VideoExtensions -> MediaType.Video
            else -> null
        }
    }

    private fun mimeTypeForExtension(displayName: String?): String? {
        return when (extensionFor(displayName)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "avif" -> "image/avif"
            "dng" -> "image/x-adobe-dng"
            "mp4", "m4v" -> "video/mp4"
            "3gp", "3gpp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov", "qt" -> "video/quicktime"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "asf" -> "video/x-ms-asf"
            "ts", "m2ts", "mts" -> "video/mp2ts"
            "ogv" -> "video/ogg"
            else -> null
        }
    }

    private fun extensionFor(displayName: String?): String? {
        val title = displayName?.trim()?.lowercase(Locale.US) ?: return null
        val dotIndex = title.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == title.lastIndex) return null
        return title.substring(dotIndex + 1)
    }
}
