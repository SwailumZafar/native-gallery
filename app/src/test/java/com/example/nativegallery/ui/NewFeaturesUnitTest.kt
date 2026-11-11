package com.example.nativegallery.ui

import com.example.nativegallery.data.formatCoordinate
import com.example.nativegallery.data.haversineKm
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NewFeaturesUnitTest {
    private fun photo(id: String, date: LocalDate): MediaItem {
        val millis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return MediaItem(
            id = id,
            albumId = "cam",
            type = MediaType.Photo,
            title = "IMG_$id.jpg",
            dateLabel = date.toString(),
            sortTimestampMillis = millis
        )
    }

    @Test
    fun computeFlashbacks_findsExactAndNearDayPastMemories() {
        val today = LocalDate.of(2026, 11, 11)
        val media = listOf(
            photo("cur", today),
            photo("exact1", LocalDate.of(2025, 11, 11)),
            photo("exact2", LocalDate.of(2024, 11, 11)),
            photo("near3", LocalDate.of(2023, 11, 10)),
            photo("far", LocalDate.of(2023, 6, 1))
        )

        val groups = computeFlashbacks(media, emptySet(), today)
        assertEquals(3, groups.size)
        assertEquals(1, groups[0].yearsAgo)
        assertEquals("1 Year Ago Today", groups[0].title)
        assertEquals(2, groups[1].yearsAgo)
        assertEquals("2 Years Ago Today", groups[1].title)
        assertEquals(3, groups[2].yearsAgo)
        assertTrue(groups[2].title.contains("Around This Day"))
    }

    @Test
    fun haversineAndFormatCoordinatesWorkCorrectly() {
        val dist = haversineKm(33.6844, 73.0479, 33.7294, 73.0931) // ~6.5 km
        assertTrue("Expected distance ~6.5 km, got $dist", dist in 4.0..9.0)
        assertEquals("33.68° N, 73.05° E", formatCoordinate(33.6844, 73.0479))
        assertEquals("33.87° S, 151.21° E", formatCoordinate(-33.8688, 151.2093))
    }
}
