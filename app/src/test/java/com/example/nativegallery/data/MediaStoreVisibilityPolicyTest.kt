package com.example.nativegallery.data

import android.provider.MediaStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreVisibilityPolicyTest {
    @Test
    fun clauses_followSdkPendingAndTrashCapabilities() {
        val api28 = MediaStoreVisibilityPolicy.selectionClauses(sdkInt = 28)
        assertTrue(api28.single().contains(MediaStore.MediaColumns.SIZE))

        val api29 = MediaStoreVisibilityPolicy.selectionClauses(sdkInt = 29)
        assertTrue(api29.any { it.contains(MediaStore.MediaColumns.IS_PENDING) })
        assertFalse(api29.any { it.contains(MediaStore.MediaColumns.IS_TRASHED) })

        val visibleApi30 = MediaStoreVisibilityPolicy.selectionClauses(sdkInt = 30)
        val trashedApi30 = MediaStoreVisibilityPolicy.selectionClauses(
            onlyTrashed = true,
            sdkInt = 30
        )
        assertTrue(visibleApi30.any { it == "${MediaStore.MediaColumns.IS_TRASHED} = 0" })
        assertTrue(trashedApi30.any { it == "${MediaStore.MediaColumns.IS_TRASHED} = 1" })
    }

    @Test
    fun pathFilter_excludesHiddenCacheAndTrashSegments() {
        assertTrue(MediaStoreVisibilityPolicy.shouldIncludePath("DCIM/Camera"))
        assertFalse(MediaStoreVisibilityPolicy.shouldIncludePath("Pictures/.private"))
        assertFalse(MediaStoreVisibilityPolicy.shouldIncludePath("Android/cache/export"))
        assertFalse(MediaStoreVisibilityPolicy.shouldIncludePath("DCIM/thumbnails"))
        assertFalse(MediaStoreVisibilityPolicy.shouldIncludePath("Pictures/item.trashed"))
    }
}
