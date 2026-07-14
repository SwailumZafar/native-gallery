package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditorCropTest {
    @Test
    fun sanitized_ordersAndClampsCoordinates() {
        val crop = NormalizedCropRect(
            left = 1.2f,
            top = 0.8f,
            right = -0.2f,
            bottom = 0.1f
        ).sanitized()

        assertEquals(0f, crop.left, 0.0001f)
        assertEquals(0.1f, crop.top, 0.0001f)
        assertEquals(1f, crop.right, 0.0001f)
        assertEquals(0.8f, crop.bottom, 0.0001f)
        assertFalse(crop.isFullFrame())
    }

    @Test
    fun sanitized_enforcesMinimumCropSizeAtImageEdge() {
        val crop = NormalizedCropRect(0.98f, 0.99f, 1f, 1f).sanitized(minSize = 0.1f)

        assertEquals(0.9f, crop.left, 0.0001f)
        assertEquals(0.9f, crop.top, 0.0001f)
        assertEquals(1f, crop.right, 0.0001f)
        assertEquals(1f, crop.bottom, 0.0001f)
    }

    @Test
    fun aspectPreset_centersRequestedOutputRatio() {
        val squareFromLandscape = normalizedCropRectForAspect(
            sourceAspect = 16f / 9f,
            targetAspect = 1f
        )
        val resultingAspect =
            (squareFromLandscape.right - squareFromLandscape.left) * (16f / 9f) /
                (squareFromLandscape.bottom - squareFromLandscape.top)

        assertEquals(1f, resultingAspect, 0.0001f)
        assertEquals(0.5f, (squareFromLandscape.left + squareFromLandscape.right) / 2f, 0.0001f)
        assertTrue(NormalizedCropRect.Full.isFullFrame())
    }

    @Test
    fun aspectPreset_cropsHeightForPortraitSource() {
        val wideCropFromPortrait = normalizedCropRectForAspect(
            sourceAspect = 3f / 4f,
            targetAspect = 16f / 9f
        )
        val resultingAspect =
            (wideCropFromPortrait.right - wideCropFromPortrait.left) * (3f / 4f) /
                (wideCropFromPortrait.bottom - wideCropFromPortrait.top)

        assertEquals(16f / 9f, resultingAspect, 0.0001f)
        assertEquals(0.5f, (wideCropFromPortrait.top + wideCropFromPortrait.bottom) / 2f, 0.0001f)
    }
}
