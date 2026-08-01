package com.example.nativegallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImageLoadCachePolicyTest {
    @Test
    fun thumbnailAndFullQualityBitmapsNeverShareAnExactCacheEntry() {
        val uri = "content://native.gallery.vault/item"

        val thumbnailKey = imageLoadCacheKey(uri, 1440, ImageLoadQuality.Thumbnail)
        val fullQualityKey = imageLoadCacheKey(uri, 1440, ImageLoadQuality.HighQuality)

        assertNotEquals(thumbnailKey, fullQualityKey)
        assertEquals("$uri@1440", thumbnailKey)
        assertEquals("$uri#full-quality@1440", fullQualityKey)
    }

    @Test
    fun qualityIdentityAlsoSeparatesNearestSizeLookups() {
        val uri = "content://native.gallery.vault/item"

        assertNotEquals(
            imageLoadCacheIdentity(uri, ImageLoadQuality.Thumbnail),
            imageLoadCacheIdentity(uri, ImageLoadQuality.HighQuality)
        )
    }
}
