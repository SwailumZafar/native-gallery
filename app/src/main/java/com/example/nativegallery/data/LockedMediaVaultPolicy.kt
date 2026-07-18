package com.example.nativegallery.data

internal object LockedMediaVaultPolicy {
    fun mimeType(mimeType: String?, isVideo: Boolean): String {
        return mimeType
            ?.trim()
            ?.takeIf { it.contains('/') }
            ?: if (isVideo) "video/*" else "image/*"
    }

    fun restorableMimeType(mimeType: String?, isVideo: Boolean): String {
        return mimeType(mimeType, isVideo).takeUnless { it.endsWith("/*") }
            ?: if (isVideo) "video/mp4" else "image/jpeg"
    }

    fun restoredDisplayName(title: String, mimeType: String, isVideo: Boolean): String {
        val cleanTitle = title
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .ifBlank { if (isVideo) "Restored video" else "Restored photo" }
        if (cleanTitle.substringAfterLast('.', missingDelimiterValue = "").isNotBlank()) {
            return cleanTitle
        }
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            "image/avif" -> "avif"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "video/x-matroska" -> "mkv"
            "video/webm" -> "webm"
            "video/quicktime" -> "mov"
            "video/3gpp" -> "3gp"
            else -> if (isVideo) "mp4" else "jpg"
        }
        return "$cleanTitle.$extension"
    }

    fun prefetchLimit(gridColumns: Int): Int {
        return (gridColumns.coerceAtLeast(2) * 5).coerceAtMost(24)
    }

    fun isRestoredCopyUsable(sizeBytes: Long): Boolean = sizeBytes > 0L

    fun normalizedOriginalRelativePath(relativePath: String?): String? {
        val normalized = relativePath
            ?.trim()
            ?.replace('\\', '/')
            ?.trim('/')
            ?.takeIf { it.isNotBlank() }
            ?: return null
        if (normalized.split('/').any { it == "." || it == ".." }) return null
        return "$normalized/"
    }

    fun fallbackRelativePath(isVideo: Boolean): String {
        val root = if (isVideo) "Movies" else "Pictures"
        return "$root/Native Gallery Restored/"
    }
}