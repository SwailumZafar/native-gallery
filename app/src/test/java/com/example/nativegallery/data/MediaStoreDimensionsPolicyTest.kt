package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreDimensionsPolicyTest {
    @Test
    fun quarterTurnOrientationSwapsStoredDimensions() {
        assertEquals(
            3000 to 4000,
            MediaStoreDimensionsPolicy.orientedDimensions(4000, 3000, 90)
        )
        assertEquals(
            3000 to 4000,
            MediaStoreDimensionsPolicy.orientedDimensions(4000, 3000, 270)
        )
    }

    @Test
    fun ordinaryOrientationKeepsDimensionsAndInvalidValuesBecomeNull() {
        assertEquals(4000 to 3000, MediaStoreDimensionsPolicy.orientedDimensions(4000, 3000, 0))
        assertEquals(null to null, MediaStoreDimensionsPolicy.orientedDimensions(0, -1, 90))
    }
}
