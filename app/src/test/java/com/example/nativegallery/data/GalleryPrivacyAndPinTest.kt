package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPrivacyAndPinTest {
    @Test
    fun lockedMedia_disappearsUntilItIsUnlocked() {
        val media = listOf(media("public"), media("locked"))

        val locked = GalleryPrivacyFilter.visibleMedia(
            mediaItems = media,
            hiddenAlbumIds = emptySet(),
            lockedMediaIds = setOf("locked")
        )
        val unlocked = GalleryPrivacyFilter.visibleMedia(
            mediaItems = media,
            hiddenAlbumIds = emptySet(),
            lockedMediaIds = emptySet()
        )

        assertEquals(listOf("public"), locked.map { it.id })
        assertEquals(listOf("public", "locked"), unlocked.map { it.id })
    }

    @Test
    fun pinRulesAndLockoutTransition_areDeterministic() {
        assertTrue(HiddenSecurityRepository.isValidPin("1234"))
        assertFalse(HiddenSecurityRepository.isValidPin("12a4"))
        assertFalse(HiddenSecurityRepository.isValidPin("123"))

        val fourthFailure = HiddenSecurityRepository.nextPinFailureState(3, 0L, 1_000L)
        assertEquals(4, fourthFailure.failedAttempts)
        assertEquals(0L, fourthFailure.lockoutUntilMillis)

        val fifthFailure = HiddenSecurityRepository.nextPinFailureState(4, 0L, 1_000L)
        assertEquals(5, fifthFailure.failedAttempts)
        assertEquals(31_000L, fifthFailure.lockoutUntilMillis)

        val stillLocked = HiddenSecurityRepository.nextPinFailureState(0, 31_000L, 2_000L)
        assertEquals(31_000L, stillLocked.lockoutUntilMillis)
    }

    private fun media(id: String) = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = id,
        dateLabel = "Today"
    )
}
