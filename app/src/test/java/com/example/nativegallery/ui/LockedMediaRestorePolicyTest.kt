package com.example.nativegallery.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LockedMediaRestorePolicyTest {
    @Test
    fun restoreConfirmationUsesPlainQuestionAndCountAwareMessage() {
        assertEquals("Are you sure?", lockedMediaRestoreTitle(1))
        assertEquals("Are you sure?", lockedMediaRestoreTitle(3))
        assertEquals("Restore this item?", lockedMediaRestoreMessage(1))
        assertEquals("Restore 3 items?", lockedMediaRestoreMessage(3))
    }
}
