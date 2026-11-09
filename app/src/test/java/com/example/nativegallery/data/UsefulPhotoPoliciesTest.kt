package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class UsefulPhotoPoliciesTest {
    private val base = Instant.parse("2026-10-01T10:00:00Z").toEpochMilli()
    private fun photo(id: String, hours: Long = 0, path: String = "DCIM/Camera/") = MediaItem(id, "camera", MediaType.Photo,
        "IMG_$id.jpg", "", relativePath = path, sortTimestampMillis = base + hours * 3_600_000L, width = 1600, height = 1200)
    private fun analyzed(media: List<MediaItem>, zone: ZoneId = ZoneId.systemDefault()) = suggestMoments(media, zone,
        media.associate { it.id to MemoryEvidence(MemoryTheme.Outdoors, 1f, it.id.hashCode().toLong()) })

    @Test fun screenshotsAreFoundByFolderOrNameAndVideosAreExcluded() {
        assertTrue(isScreenshotMedia(photo("a", path = "Pictures/Screenshots/")))
        assertTrue(isScreenshotMedia(photo("a").copy(title = "Screenshot_20261109.png")))
        assertTrue(isScreenshotMedia(photo("a").copy(title = "Screen_shot-01.png")))
        assertFalse(isScreenshotMedia(photo("a")))
        assertFalse(isScreenshotMedia(photo("a", path = "Screenshots/").copy(isVideo = true)))
    }
    @Test fun usefulScreenshotCategoriesUseWholeWordsAndKeepTicketPriority() {
        assertEquals(ScreenshotCategory.Tickets, screenshotCategory("Concert ticket Seat A12"))
        assertEquals(ScreenshotCategory.Receipts, screenshotCategory("Invoice subtotal amount paid"))
        assertEquals(ScreenshotCategory.Orders, screenshotCategory("Your order is dispatched"))
        assertEquals(ScreenshotCategory.Events, screenshotCategory("Meeting appointment 11 am"))
        assertEquals(ScreenshotCategory.Other, screenshotCategory("A border around a photo"))
    }
    @Test fun screenshotDatesRequireAnExplicitValidYear() {
        assertEquals(LocalDate.of(2030, 11, 12), screenshotDate("Concert 12 Nov 2030"))
        assertEquals(LocalDate.of(2030, 11, 12), screenshotDate("Date: 2030-11-12"))
        assertEquals(LocalDate.of(2030, 11, 12), screenshotDate("November 12, 2030"))
        assertEquals(LocalDate.of(2030, 11, 12), screenshotDate("12/11/2030"))
        assertNull(screenshotDate("31/02/2030"))
        assertNull(screenshotDate("Tomorrow, Nov 12"))
    }
    @Test fun momentsGroupPhotosAcrossAlbumsByTimeAndPreserveAllMembers() {
        val media = (0..5).map { photo("$it", it * 8L).copy(albumId = if (it % 2 == 0) "camera" else "trip") }
        val moment = analyzed(media, ZoneId.of("UTC")).single()
        assertEquals(media.map { it.id }, moment.mediaIds)
        assertEquals(media.first().sortTimestampMillis, moment.startMillis)
        assertEquals(media.last().sortTimestampMillis, moment.endMillis)
    }
    @Test fun separateTripsAndLongDailyRunsDoNotBecomeOneEndlessMoment() {
        val first = (0..3).map { photo("first$it", it.toLong()) }
        val second = (0..3).map { photo("second$it", 100L + it) }
        assertEquals(2, analyzed(first + second).size)
        val daily = (0..15).flatMap { day -> (0..3).map { shot -> photo("$day-$shot", day * 24L + shot) } }
        assertTrue(analyzed(daily, ZoneId.of("UTC")).all { it.endMillis - it.startMillis < 4 * 86_400_000L })
    }
    @Test fun suggestionsExcludeScreenshotsDownloadsAndUndatedPhotos() {
        val ordinary = (0..3).map { photo("$it", it.toLong()) }
        val excluded = listOf(photo("shot", path = "Pictures/Screenshots/"), photo("download", path = "Download/"),
            photo("unknown").copy(sortTimestampMillis = 0L), photo("video").copy(isVideo = true))
        assertEquals(ordinary.map { it.id }, analyzed(ordinary + excluded).single().mediaIds)
        assertTrue(analyzed(ordinary.take(3)).isEmpty())
    }
    @Test fun datesAloneNeverSuggestAMemory() {
        assertTrue(suggestMoments((0..10).map { photo("$it", it.toLong()) }).isEmpty())
    }
    @Test fun memoryContentRejectsDocumentsTextAndGenericObjects() {
        assertNull(memoryTheme(mapOf("Sky" to 0.99f), 0f, 0))
        assertNull(memoryTheme(mapOf("Person" to 0.9f), 0.2f, 100))
        assertNull(memoryTheme(mapOf("Person" to 0.9f, "Receipt" to 0.8f), 0f, 10))
        assertNull(memoryTheme(mapOf("Mountain" to 0.55f), 0f, 0))
        assertEquals(MemoryTheme.Outdoors, memoryTheme(mapOf("Mountain" to 0.9f), 0f, 0))
        assertEquals(MemoryTheme.Celebration, memoryTheme(mapOf("Marriage" to 0.8f, "Person" to 0.9f), 0f, 0))
        assertFalse(isMemoryCandidate(photo("meme", path = "Download/"), favorite = true))
    }
    @Test fun duplicateFramesCannotInflateAMemoryAndUnrelatedSubjectsStaySeparate() {
        val photos = (0..5).map { photo("$it").copy(sortTimestampMillis = base + it * 60_000) }
        val same = photos.associate { it.id to MemoryEvidence(MemoryTheme.Outdoors, 1f, 123L) }
        assertTrue(suggestMoments(photos, evidence = same).isEmpty())
        val mixed = photos.mapIndexed { index, item -> item.copy(sortTimestampMillis = base + index * 3_600_000) }
        val themes = mixed.mapIndexed { i, item -> item.id to MemoryEvidence(if (i < 3) MemoryTheme.Pets else MemoryTheme.Meals, 1f, i.toLong()) }.toMap()
        assertTrue(suggestMoments(mixed, evidence = themes).isEmpty())
    }
    @Test fun highlightsFavorFavoritesAndAvoidNearIdenticalBurstFrames() {
        val favorite = photo("favorite").copy(width = 800, height = 600, burstGroupId = "burst")
        val duplicate = photo("duplicate").copy(sortTimestampMillis = base + 500, width = 6000, height = 4000, burstGroupId = "burst")
        val later = photo("later", 3)
        val highlights = momentHighlights(listOf(duplicate, favorite, later), setOf(favorite.id))
        assertEquals(listOf(favorite.id, later.id), highlights.map { it.id })
        assertTrue(momentHighlights(listOf(favorite), emptySet(), 0).isEmpty())
    }
    @Test fun regionNormalizationClampsReversedDragsAndRejectsInvalidCoordinates() {
        val region = PrivacyRegion("a", 1.2f, 0.8f, -0.1f, 0.2f).sanitized()!!
        assertEquals(0f, region.left, 0f); assertEquals(1f, region.right, 0f)
        assertTrue(region.contains(0.5f, 0.5f))
        assertNull(PrivacyRegion("a", Float.NaN, 0f, 1f, 1f).sanitized())
        assertNull(PrivacyRegion("a", 0f, 0f, 0f, 1f).sanitized())
    }
    @Test fun sensitiveTextIncludesContactDetailsIdentifiersAndPasswords() {
        assertTrue(isSensitiveShareText("Email me at example@example.com"))
        assertTrue(isSensitiveShareText("Phone +92 300 1234567"))
        assertTrue(isSensitiveShareText("Passport: AB12345"))
        assertTrue(isSensitiveShareText("Your OTP is 1234"))
        assertFalse(isSensitiveShareText("Enjoy the mountains and the lake"))
    }
}
