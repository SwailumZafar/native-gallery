package com.example.nativegallery.ui

import com.example.nativegallery.ui.components.photoDecodeDimensions
import com.example.nativegallery.ui.components.photoDecodePixelBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerPhotoQualityTest {
    private val memory = 512L * 1024 * 1024

    @Test fun zoomPromotesDetailWithoutUpscalingSmallOriginals() {
        assertEquals(2700, viewerPhotoResolution(4000, 3000, 2700, false, memory))
        assertEquals(4000, viewerPhotoResolution(4000, 3000, 2700, true, memory))
        assertEquals(1200, viewerPhotoResolution(1200, 800, 2700, true, memory))
    }

    @Test fun largeSensorsAndPanoramasStayBounded() {
        assertEquals(4000, viewerPhotoResolution(8000, 6000, 2700, true, memory))
        assertEquals(6144, viewerPhotoResolution(24000, 1000, 2700, true, memory))
        assertTrue(viewerPhotoResolution(8000, 6000, 2700, true, 128L * 1024 * 1024) < 4000)
    }

    @Test fun missingMetadataStillAllowsABoundedZoomUpgrade() {
        assertTrue(viewerPhotoResolution(null, null, 2700, true, memory) > 2700)
        assertTrue(viewerPhotoResolution(-1, 0, 0, true, memory) > 0)
    }

    @Test fun decoderUsesRealHeaderDimensionsToEnforceThePixelBudget() {
        val budget = photoDecodePixelBudget(memory)
        for ((w, h) in listOf(8000 to 6000, 24000 to 1000, 8192 to 8192, 100 to 200)) {
            val (dw, dh) = photoDecodeDimensions(w, h, 6144, budget)
            assertTrue(dw <= w && dh <= h)
            assertTrue(dw.toLong() * dh <= budget + dw + dh)
            assertEquals(w.toDouble() / h, dw.toDouble() / dh, 0.05)
        }
        assertEquals(100 to 200, photoDecodeDimensions(100, 200, 2700, budget))
    }
}
