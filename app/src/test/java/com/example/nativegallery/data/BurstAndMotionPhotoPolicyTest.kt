package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BurstAndMotionPhotoPolicyTest {

    private fun createPhoto(
        id: String,
        title: String,
        albumId: String = "camera",
        sortTimestampMillis: Long = 1000000L,
        isVideo: Boolean = false,
        relativePath: String? = "DCIM/Camera/"
    ): MediaItem {
        return MediaItem(
            id = id,
            albumId = albumId,
            type = if (isVideo) MediaType.Video else MediaType.Photo,
            title = title,
            dateLabel = "Today",
            isVideo = isVideo,
            sortTimestampMillis = sortTimestampMillis,
            relativePath = relativePath
        )
    }

    @Test
    fun burstGroupKey_detectsStandardBurstNaming() {
        val item1 = createPhoto("1", "BURST20261015120000_COVER.jpg")
        val item2 = createPhoto("2", "BURST20261015120000_1.jpg")
        val item3 = createPhoto("3", "BURST20261015120000_2.jpg")

        val key1 = extractBurstGroupKey(item1, "Camera")
        val key2 = extractBurstGroupKey(item2, "Camera")
        val key3 = extractBurstGroupKey(item3, "Camera")

        assertNotNull(key1)
        assertEquals(key1, key2)
        assertEquals(key1, key3)
    }

    @Test
    fun burstGroupKey_ignoresVideos() {
        val video = createPhoto("v1", "BURST20261015120000_1.mp4", isVideo = true)
        val key = extractBurstGroupKey(video, "Camera")
        assertNull(key)
    }

    @Test
    fun burstGroupKey_returnsNullForNormalPhoto() {
        val photo = createPhoto("p1", "IMG_20261015_120000.jpg")
        val key = extractBurstGroupKey(photo, "Camera")
        assertNull(key)
    }

    @Test
    fun burstGroupKey_folderClusteringGroupsNearbyPhotos() {
        val itemA = createPhoto("a", "IMG_001.jpg", sortTimestampMillis = 10000L, relativePath = "DCIM/Burst/")
        val itemB = createPhoto("b", "IMG_002.jpg", sortTimestampMillis = 11000L, relativePath = "DCIM/Burst/")
        val itemC = createPhoto("c", "IMG_003.jpg", sortTimestampMillis = 30000L, relativePath = "DCIM/Burst/")

        val keyA = extractBurstGroupKey(itemA, "Burst")
        val keyB = extractBurstGroupKey(itemB, "Burst")
        val keyC = extractBurstGroupKey(itemC, "Burst")

        assertNotNull(keyA)
        assertEquals(keyA, keyB)
        assertFalse(keyA == keyC)
    }

    @Test
    fun groupBurstRows_collapsesPeersIntoSingleStackWithCover() {
        val row1 = MediaStoreGalleryRepository.MediaStoreRow(
            createPhoto("1", "BURST20261015120000_1.jpg", sortTimestampMillis = 1000L),
            "Camera"
        )
        val rowCover = MediaStoreGalleryRepository.MediaStoreRow(
            createPhoto("2", "BURST20261015120000_COVER.jpg", sortTimestampMillis = 1500L),
            "Camera"
        )
        val row3 = MediaStoreGalleryRepository.MediaStoreRow(
            createPhoto("3", "BURST20261015120000_3.jpg", sortTimestampMillis = 2000L),
            "Camera"
        )
        val normalRow = MediaStoreGalleryRepository.MediaStoreRow(
            createPhoto("4", "IMG_20261015_999.jpg", sortTimestampMillis = 3000L),
            "Camera"
        )

        val grouped = groupBurstRows(listOf(row1, rowCover, row3, normalRow))

        // Should produce 2 rows: the collapsed burst stack + normalRow
        assertEquals(2, grouped.size)
        val burstRow = grouped[0]
        assertTrue(burstRow.mediaItem.isBurst)
        assertEquals(3, burstRow.mediaItem.burstCount)
        assertEquals("BURST20261015120000_COVER.jpg", burstRow.mediaItem.title)
        assertEquals(3, burstRow.mediaItem.burstItems.size)

        val uncollapsed = grouped[1]
        assertFalse(uncollapsed.mediaItem.isBurst)
        assertEquals(1, uncollapsed.mediaItem.burstCount)
    }

    @Test
    fun groupBurstRows_singleBurstItemNotCollapsed() {
        val singleBurst = MediaStoreGalleryRepository.MediaStoreRow(
            createPhoto("1", "BURST20261015120000_1.jpg", sortTimestampMillis = 1000L),
            "Camera"
        )
        val grouped = groupBurstRows(listOf(singleBurst))
        assertEquals(1, grouped.size)
        assertFalse(grouped[0].mediaItem.isBurst)
        assertEquals(1, grouped[0].mediaItem.burstCount)
    }

    @Test
    fun isMotionPhotoCandidate_detectsGoogleSamsungOnePlusPatterns() {
        assertTrue(isMotionPhotoCandidate("MVIMG_20261015_120000.jpg", "image/jpeg", "DCIM/Camera/"))
        assertTrue(isMotionPhotoCandidate("IMG_20261015_120000_MP.jpg", "image/jpeg", "DCIM/Camera/"))
        assertTrue(isMotionPhotoCandidate("IMG_20261015_MOTION.jpg", "image/jpeg", "DCIM/Camera/"))
        assertTrue(isMotionPhotoCandidate("standard.jpg", "image/jpeg", "DCIM/Motion/standard.jpg"))

        assertFalse(isMotionPhotoCandidate("IMG_20261015_120000.jpg", "image/jpeg", "DCIM/Camera/"))
        assertFalse(isMotionPhotoCandidate("VID_20261015_120000.mp4", "video/mp4", "DCIM/Camera/"))
    }

    @Test
    fun viewerTransitionStyle_optionsAndDefaults() {
        val normal = ViewerTransitionStyle.Normal
        val fast = ViewerTransitionStyle.Fast

        assertEquals("Normal", normal.label)
        assertEquals("Fast Enhanced", fast.label)
        assertEquals(ViewerTransitionStyle.Normal, GallerySettings().viewerTransitionStyle)
    }

    @Test
    fun isBurstClutterAlbum_detectsVariations() {
        assertTrue(isBurstClutterAlbum("Burst", "DCIM/Camera/"))
        assertTrue(isBurstClutterAlbum("Burst photos", "DCIM/Camera/"))
        assertTrue(isBurstClutterAlbum("burst_shots", "Pictures/"))
        assertTrue(isBurstClutterAlbum("Camera", "DCIM/Burst/"))
        assertTrue(isBurstClutterAlbum("Camera", "DCIM/burst_shots/"))

        assertFalse(isBurstClutterAlbum("Camera", "DCIM/Camera/"))
        assertFalse(isBurstClutterAlbum("Screenshots", "Pictures/Screenshots/"))
        assertFalse(isBurstClutterAlbum("Downloads", "Download/"))
    }
}
