package com.example.nativegallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockedMediaSecurityPolicyTest {
    @Test
    fun pinCreationValidationProducesStableUserMessages() {
        assertEquals(
            "PINs do not match.",
            LockedMediaSecurityPolicy.pinCreationError("1234", "4321")
        )
        assertEquals(
            "Use 4 to 12 digits for the PIN.",
            LockedMediaSecurityPolicy.pinCreationError("12a4", "12a4")
        )
        assertNull(LockedMediaSecurityPolicy.pinCreationError("1234", "1234"))
    }

    @Test
    fun lockoutMessageRoundsUpRemainingSeconds() {
        assertEquals(
            "Too many wrong PIN attempts. Try again in 2s.",
            LockedMediaSecurityPolicy.lockoutMessage(1_001L)
        )
    }
}
