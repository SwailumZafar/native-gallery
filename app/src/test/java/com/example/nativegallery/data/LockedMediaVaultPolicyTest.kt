package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedMediaVaultPolicyTest {
    @Test
    fun mimeTypeKeepsKnownFormatsAndUsesMediaKindFallbacks() {
        assertEquals("image/heic", LockedMediaVaultPolicy.mimeType(" image/heic ", isVideo = false))
        assertEquals("video/x-matroska", LockedMediaVaultPolicy.mimeType("video/x-matroska", isVideo = true))
        assertEquals("image/*", LockedMediaVaultPolicy.mimeType(null, isVideo = false))
        assertEquals("video/*", LockedMediaVaultPolicy.mimeType("unknown", isVideo = true))
    }

    @Test
    fun restorePolicyProducesConcreteMimeTypesAndUsefulExtensions() {
        assertEquals("image/jpeg", LockedMediaVaultPolicy.restorableMimeType(null, isVideo = false))
        assertEquals("video/mp4", LockedMediaVaultPolicy.restorableMimeType("video/*", isVideo = true))
        assertEquals(
            "holiday.mkv",
            LockedMediaVaultPolicy.restoredDisplayName("holiday", "video/x-matroska", isVideo = true)
        )
        assertEquals(
            "IMG_42.heic",
            LockedMediaVaultPolicy.restoredDisplayName("folder/IMG_42", "image/heic", isVideo = false)
        )
    }

    @Test
    fun restoredCopyMustContainReadableBytes() {
        assertTrue(LockedMediaVaultPolicy.isRestoredCopyUsable(1L))
        assertFalse(LockedMediaVaultPolicy.isRestoredCopyUsable(0L))
        assertFalse(LockedMediaVaultPolicy.isRestoredCopyUsable(-1L))
    }
    @Test
    fun originalRestorePathIsNormalizedAndTraversalIsRejected() {
        assertEquals("DCIM/Camera/", LockedMediaVaultPolicy.normalizedOriginalRelativePath("/DCIM\\Camera/"))
        assertEquals(null, LockedMediaVaultPolicy.normalizedOriginalRelativePath("Pictures/../Private"))
        assertEquals("Pictures/Native Gallery Restored/", LockedMediaVaultPolicy.fallbackRelativePath(false))
        assertEquals("Movies/Native Gallery Restored/", LockedMediaVaultPolicy.fallbackRelativePath(true))
    }

    @Test
    fun prefetchLimitIsBoundedToFiveVisibleRows() {
        assertEquals(10, LockedMediaVaultPolicy.prefetchLimit(1))
        assertEquals(20, LockedMediaVaultPolicy.prefetchLimit(4))
        assertEquals(24, LockedMediaVaultPolicy.prefetchLimit(8))
    }
}