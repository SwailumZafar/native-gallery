package com.example.nativegallery.data

import android.os.Build
import android.provider.MediaStore

object MediaStoreVisibilityPolicy {
    fun selectionClauses(): List<String> {
        return buildList {
            add("${MediaStore.MediaColumns.SIZE} > 0")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add("${MediaStore.MediaColumns.IS_PENDING} = 0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add("${MediaStore.MediaColumns.IS_TRASHED} = 0")
            }
        }
    }

    fun shouldIncludePath(path: String?): Boolean {
        if (path.isNullOrBlank()) return true
        val segments = path
            .replace('\\', '/')
            .split('/')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        return segments.none { segment ->
            segment.startsWith(".") ||
                segment in IgnoredPathSegments ||
                segment.endsWith(".trashed")
        }
    }

    private val IgnoredPathSegments = setOf(
        "cache",
        "caches",
        "tmp",
        "temp",
        "trash",
        "trashed",
        "recycle",
        "recycle bin",
        "thumbnails"
    )
}
