package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.*
import org.junit.Test

class VaultSecurityAndSmartAlbumTest {
    @Test fun lockoutsEscalateAcrossGroupsAndRequireRecoveryAfterThirty() {
        var state = PinFailureState(0, 0)
        var now = 1_000L
        val waits = mapOf(5 to 60_000L, 10 to 120_000L, 15 to 300_000L, 20 to 600_000L, 25 to 1_800_000L)
        repeat(30) {
            state = HiddenSecurityRepository.nextPinFailureState(state.failedAttempts, state.lockoutUntilMillis, now)
            assertEquals(it + 1, state.failedAttempts)
            waits[state.failedAttempts]?.let { wait ->
                assertEquals(wait, state.remainingMillis(now))
                assertEquals(state, HiddenSecurityRepository.nextPinFailureState(state.failedAttempts, state.lockoutUntilMillis, now + 1))
                now += wait
            }
        }
        assertEquals(30, state.failedAttempts)
        assertEquals(0L, state.lockoutUntilMillis)
        val wrongRecovery = HiddenSecurityRepository.nextPinFailureState(30, 0, now)
        assertEquals(60_000L, wrongRecovery.remainingMillis(now))
    }

    @Test fun credentialKindsValidateTheirOwnInputWithoutWeakeningExistingPins() {
        assertTrue(validVaultCredential(VaultCredentialKind.Pin, "1234567890123456"))
        assertFalse(validVaultCredential(VaultCredentialKind.Pin, "12345678901234567"))
        assertTrue(validVaultCredential(VaultCredentialKind.Password, "my text password 93"))
        assertFalse(validVaultCredential(VaultCredentialKind.Password, "short"))
        assertTrue(validVaultCredential(VaultCredentialKind.Pattern, "12369"))
        assertFalse(validVaultCredential(VaultCredentialKind.Pattern, "11234"))
        assertFalse(validVaultCredential(VaultCredentialKind.Pattern, "1230"))
    }

    @Test fun savedFiltersCombineMediaTypeAgeFavoritesAndFolderText() {
        val now = 1_000_000_000L
        val video = MediaItem("v", "camera", MediaType.Video, "holiday.mp4", "Today", isVideo = true,
            relativePath = "DCIM/Camera/", sortTimestampMillis = now - 86_400_000L)
        val rule = SmartAlbumRule(id = "smart:test", name = "Recent favorite camera videos", mediaType = "Videos",
            pastDays = 7, text = "camera", favoritesOnly = true)
        assertTrue(matchesSmartAlbum(rule, video, setOf("v"), now))
        assertFalse(matchesSmartAlbum(rule, video, emptySet(), now))
        assertFalse(matchesSmartAlbum(rule, video.copy(isVideo = false), setOf("v"), now))
        assertFalse(matchesSmartAlbum(rule, video.copy(sortTimestampMillis = now - 8 * 86_400_000L), setOf("v"), now))
        assertFalse(matchesSmartAlbum(rule, video.copy(relativePath = "Movies/"), setOf("v"), now))
    }

    @Test fun albumMovesResolveExistingFilenamesWithoutOverwriting() {
        assertEquals("IMG (2).jpg", uniqueMoveFilename("IMG.jpg", setOf("img.jpg", "img (1).jpg")))
        assertEquals("new.mp4", uniqueMoveFilename("new.mp4", setOf("other.mp4")))
        assertEquals("DCIM/Camera/", canonicalAlbumMovePath("DCIM/Camera/BURST2026/"))
    }

    @Test fun burstSubfoldersJoinTheirParentWithoutCrossAlbumStacking() {
        fun row(id: String, album: String, path: String, title: String) = MediaStoreGalleryRepository.MediaStoreRow(
            MediaItem(id, album, MediaType.Photo, title, "Today", relativePath = path), album)
        val camera = row("ordinary", "camera-id", "DCIM/Camera/", "IMG.jpg")
        val burst = row("frame", "burst-id", "DCIM/Camera/BURST2026/", "BURST2026_1.jpg")
        val normalized = normalizeBurstAlbumRows(listOf(camera, burst))
        assertEquals("camera-id", normalized[1].mediaItem.albumId)
        assertEquals("camera-id", normalized[1].albumName)
        val other = row("other", "other-camera", "Pictures/Other/", "BURST2026_2.jpg")
        assertEquals(3, groupBurstRows(normalized + other).size)
    }
}
