package com.example.nativegallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridThumbnailQualityTest {
    @Test fun squareVideoTilesKeepEnoughPixelsFromWidePostersWithoutUnboundedFrames() {
        val request = gridVideoThumbnailSize(312, 312, 384)
        assertTrue(request * 9 / 16 >= 312)
        assertEquals(384, gridVideoThumbnailSize(60, 90, 384))
        assertTrue(gridVideoThumbnailSize(2048, 2048, 384) <= 1536)
    }
    @Test fun cropDetailIncludesEnoughPixelsOnBothEdges() {
        assertEquals(512 to 384, gridThumbnailDecodeDimensions(4000, 3000, 384, 384, 2_000_000))
        assertEquals(384 to 512, gridThumbnailDecodeDimensions(3000, 4000, 384, 384, 2_000_000))
        assertEquals(1024 to 768, gridThumbnailDecodeDimensions(4000, 3000, 1024, 400, 2_000_000))
    }
    @Test fun smallOriginalsAreNotUpscaledAndPanoramasStayBounded() {
        assertEquals(80 to 60, gridThumbnailDecodeDimensions(80, 60, 384, 384, 2_000_000))
        val large = gridThumbnailDecodeDimensions(60000, 6000, 1536, 1536, 1_000_000)
        assertTrue(large.first <= 4096)
        assertTrue(large.first.toLong() * large.second <= 1_002_000L)
    }
    @Test fun cachedDetailSeparatesEditsTileShapesAndLossyPreviews() {
        val key = gridThumbnailDetailKey("content://photos/1", "100:123", 384, 384)
        assertNotEquals(key, gridThumbnailDetailKey("content://photos/1", "100:124", 384, 384))
        assertNotEquals(key, gridThumbnailDetailKey("content://photos/1", "100:123", 1024, 384))
        assertNotEquals(key, imageLoadCacheKey("content://photos/1", 384, ImageLoadQuality.Thumbnail))
        assertEquals(384, gridThumbnailDimensionBucket(313))
        assertEquals(1536, gridThumbnailDimensionBucket(4000))
    }
}
