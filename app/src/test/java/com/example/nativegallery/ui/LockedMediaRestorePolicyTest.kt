package com.example.nativegallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedMediaRestorePolicyTest {
    @Test
    fun restoreTitleReflectsSelectionCount() {
        assertEquals("Restore this item?", lockedMediaRestoreTitle(1))
        assertEquals("Restore 3 items?", lockedMediaRestoreTitle(3))
    }

    @Test
    fun restoreMessageDistinguishesAppConfirmationFromAndroidFlow() {
        val singular = lockedMediaRestoreMessage(1)
        val plural = lockedMediaRestoreMessage(2)

        assertTrue(singular.contains("full-quality"))
        assertTrue(singular.contains("in-app confirmation"))
        assertTrue(singular.contains("not Android's system permission panel"))
        assertTrue(plural.contains("original-quality items"))
    }
}
