package com.example.nativegallery.data

import android.provider.MediaStore
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreMediaFormatPolicyTest {
    @Test
    fun selectionForVideos_includesNativeTypeAndBroadVideoExtensions() {
        val selection = MediaStoreMediaFormatPolicy.selectionFor(setOf(MediaKind.Videos))

        assertNotNull(selection)
        val args = selection!!.args.toSet()
        assertTrue(selection.clause.contains(MediaStore.Files.FileColumns.MEDIA_TYPE))
        assertTrue(args.contains(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()))
        assertTrue(args.contains("video/%"))
        assertTrue(args.contains("%.mkv"))
        assertTrue(args.contains("%.webm"))
        assertTrue(args.contains("%.avi"))
        assertTrue(args.contains("%.mov"))
    }

    @Test
    fun selectionForImages_includesNativeTypeAndBroadImageExtensions() {
        val selection = MediaStoreMediaFormatPolicy.selectionFor(setOf(MediaKind.Images))

        assertNotNull(selection)
        val args = selection!!.args.toSet()
        assertTrue(args.contains(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()))
        assertTrue(args.contains("image/%"))
        assertTrue(args.contains("%.heic"))
        assertTrue(args.contains("%.heif"))
        assertTrue(args.contains("%.avif"))
        assertTrue(args.contains("%.dng"))
    }

    @Test
    fun mediaTypeFor_infersMatroskaWhenMediaStoreLeavesTypeUnknown() {
        val mediaType = MediaStoreMediaFormatPolicy.mediaTypeFor(
            mediaStoreType = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
            mimeType = "application/octet-stream",
            displayName = "camera_export.MKV"
        )

        assertEquals(MediaType.Video, mediaType)
        assertEquals(
            "video/x-matroska",
            MediaStoreMediaFormatPolicy.normalizedMimeType(
                mimeType = "application/octet-stream",
                displayName = "camera_export.MKV"
            )
        )
    }

    @Test
    fun mediaTypeFor_infersModernImageWhenMediaStoreLeavesTypeUnknown() {
        val mediaType = MediaStoreMediaFormatPolicy.mediaTypeFor(
            mediaStoreType = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
            mimeType = null,
            displayName = "portrait.AVIF"
        )

        assertEquals(MediaType.Photo, mediaType)
        assertEquals(
            "image/avif",
            MediaStoreMediaFormatPolicy.normalizedMimeType(
                mimeType = null,
                displayName = "portrait.AVIF"
            )
        )
    }
}
