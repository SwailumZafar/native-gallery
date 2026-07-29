package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaManagementAccessPolicyTest {
    @Test
    fun preAndroid12AlwaysUsesTheSystemConfirmationFallback() {
        val status = MediaManagementAccessPolicy.evaluate(
            sdkInt = 30,
            permissionDeclared = true,
            canManageMedia = true,
            requestActivityAvailable = true
        )

        assertEquals(MediaManagementAccessState.UnsupportedAndroidVersion, status.state)
        assertFalse(status.isGranted)
        assertFalse(status.canRequest)
        assertEquals(
            MediaStoreConfirmationStrategy.AndroidSystemConfirmation,
            status.confirmationStrategy
        )
    }

    @Test
    fun declaredPermissionAndAvailableSettingsScreenAreRequestable() {
        val status = MediaManagementAccessPolicy.evaluate(
            sdkInt = 31,
            permissionDeclared = true,
            canManageMedia = false,
            requestActivityAvailable = true
        )

        assertEquals(MediaManagementAccessState.Requestable, status.state)
        assertTrue(status.canRequest)
        assertFalse(status.isGranted)
    }

    @Test
    fun grantedAccessEnablesOneTapEvenWhenRequestScreenIsUnavailable() {
        val status = MediaManagementAccessPolicy.evaluate(
            sdkInt = 35,
            permissionDeclared = true,
            canManageMedia = true,
            requestActivityAvailable = false
        )

        assertEquals(MediaManagementAccessState.Granted, status.state)
        assertTrue(status.isGranted)
        assertFalse(status.canRequest)
        assertEquals(MediaStoreConfirmationStrategy.OneTap, status.confirmationStrategy)
    }

    @Test
    fun missingDeclarationAndMissingSettingsHandlerDegradeSafely() {
        val missingDeclaration = MediaManagementAccessPolicy.evaluate(
            sdkInt = 35,
            permissionDeclared = false,
            canManageMedia = false,
            requestActivityAvailable = true
        )
        val missingHandler = MediaManagementAccessPolicy.evaluate(
            sdkInt = 35,
            permissionDeclared = true,
            canManageMedia = false,
            requestActivityAvailable = false
        )

        assertEquals(
            MediaManagementAccessState.MissingManifestPermission,
            missingDeclaration.state
        )
        assertEquals(
            MediaManagementAccessState.RequestActivityUnavailable,
            missingHandler.state
        )
        assertEquals(
            MediaStoreConfirmationStrategy.AndroidSystemConfirmation,
            missingHandler.confirmationStrategy
        )
    }
}
