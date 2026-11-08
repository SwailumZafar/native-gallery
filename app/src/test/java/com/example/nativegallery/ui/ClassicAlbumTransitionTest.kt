package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicAlbumTransitionTest {
    @Test fun historicalApertureTravelsFromTheExactCardToTheFullPage() {
        val source = Rect(24f, 240f, 224f, 400f)
        assertEquals(source, classicAlbumBounds(source, 400f, 800f, 0f))
        assertEquals(Rect(12f, 120f, 312f, 600f), classicAlbumBounds(source, 400f, 800f, 0.5f))
        assertEquals(Rect(0f, 0f, 400f, 800f), classicAlbumBounds(source, 400f, 800f, 1f))
    }

    @Test fun springOvershootCannotMoveTheClipPastEitherEndpoint() {
        val source = Rect(10f, 200f, 390f, 350f)
        assertEquals(source, classicAlbumBounds(source, 400f, 800f, -0.1f))
        assertEquals(Rect(0f, 0f, 400f, 800f), classicAlbumBounds(source, 400f, 800f, 1.1f))
    }
}
