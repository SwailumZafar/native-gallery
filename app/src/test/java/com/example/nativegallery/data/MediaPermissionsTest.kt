package com.example.nativegallery.data

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionsTest {
    @Test
    fun requestPermissions_matchAndroidStorageModels() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MediaPermissions.requestPermissionsForSdk(32)
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
            MediaPermissions.requestPermissionsForSdk(33)
        )
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            MediaPermissions.requestPermissionsForSdk(34)
        )
    }

    @Test
    fun access_supportsLegacyFullAndAndroid14LimitedSelection() {
        val legacy = MediaPermissions.accessForPermissions(
            sdkInt = 32,
            grantedPermissions = setOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
        assertEquals(setOf(MediaKind.Images, MediaKind.Videos), legacy.mediaKinds)
        assertFalse(legacy.isLimited)

        val selected = MediaPermissions.accessForPermissions(
            sdkInt = 34,
            grantedPermissions = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        )
        assertEquals(setOf(MediaKind.Images, MediaKind.Videos), selected.mediaKinds)
        assertTrue(selected.isLimited)

        val imagesOnly = MediaPermissions.accessForPermissions(
            sdkInt = 34,
            grantedPermissions = setOf(Manifest.permission.READ_MEDIA_IMAGES)
        )
        assertEquals(setOf(MediaKind.Images), imagesOnly.mediaKinds)
        assertFalse(imagesOnly.isLimited)
    }
}
