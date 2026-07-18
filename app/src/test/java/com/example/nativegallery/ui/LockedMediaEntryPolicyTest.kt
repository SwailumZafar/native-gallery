package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedMediaEntryPolicyTest {
    @Test
    fun biometricAutoLaunchRequiresLockedConfiguredAndAvailableState() {
        assertTrue(
            shouldAutoLaunchLockedMediaBiometric(
                isUnlocked = false,
                hasPin = true,
                biometricAvailable = true
            )
        )
        assertFalse(
            shouldAutoLaunchLockedMediaBiometric(
                isUnlocked = true,
                hasPin = true,
                biometricAvailable = true
            )
        )
        assertFalse(
            shouldAutoLaunchLockedMediaBiometric(
                isUnlocked = false,
                hasPin = false,
                biometricAvailable = true
            )
        )
        assertFalse(
            shouldAutoLaunchLockedMediaBiometric(
                isUnlocked = false,
                hasPin = true,
                biometricAvailable = false
            )
        )
    }
}