package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedMediaOperationsTest {
    @Test
    fun importOutcomeRequestsRemovalOnlyForSuccessfullyEncryptedItems() {
        val outcome = LockedMediaOperationsPolicy.afterImport(
            requestedIds = listOf("one", "two", "two", "three"),
            importedIds = listOf("one", "three", "not-requested")
        )

        assertEquals(setOf("one", "two", "three"), outcome.requestedIds)
        assertEquals(setOf("one", "three"), outcome.importedIds)
        assertEquals(setOf("two"), outcome.failedIds)
        assertEquals(LockedMediaImportNextStep.RequestOriginalRemoval, outcome.nextStep)
    }

    @Test
    fun cancelledRemovalRollsBackOnlyCopiesWhoseOriginalStillExists() {
        val resolution = LockedMediaOperationsPolicy.afterOriginalRemoval(
            importedIds = setOf("one", "two", "three"),
            result = LockedMediaOriginalRemovalResult.CancelledOrFailed,
            originalsStillPresentIds = setOf("one", "three", "unrelated")
        )

        assertEquals(setOf("two"), resolution.committedIds)
        assertEquals(setOf("one", "three"), resolution.rollbackIds)
    }

    @Test
    fun serviceImportsOncePerIdAndHidesOnlySuccessfulCopies() {
        val importedTitles = mutableListOf<String>()
        val hiddenCalls = mutableListOf<Pair<Set<String>, Boolean>>()
        val operations = testOperations(
            importToVault = { item ->
                importedTitles += item.title
                item.id != "failed"
            },
            setHidden = { ids, hidden ->
                hiddenCalls += ids to hidden
                setOf("already-hidden") + if (hidden) ids else emptySet()
            }
        )

        val execution = operations.importIntoVault(
            listOf(
                mediaItem("saved", "Saved"),
                mediaItem("saved", "Duplicate"),
                mediaItem("failed", "Failed")
            )
        )

        assertEquals(listOf("Saved", "Failed"), importedTitles)
        assertEquals(setOf("saved"), execution.outcome.importedIds)
        assertEquals(setOf("failed"), execution.outcome.failedIds)
        assertEquals(listOf(setOf("saved") to true), hiddenCalls)
        assertEquals(setOf("already-hidden", "saved"), execution.updatedHiddenMediaIds)
    }

    @Test
    fun failedImportsDoNotMutateHiddenState() {
        var hiddenWasCalled = false
        val operations = testOperations(
            importToVault = { false },
            setHidden = { _, _ ->
                hiddenWasCalled = true
                emptySet()
            }
        )

        val execution = operations.importIntoVault(listOf(mediaItem("failed")))

        assertEquals(LockedMediaImportNextStep.NothingImported, execution.outcome.nextStep)
        assertTrue(execution.outcome.importedIds.isEmpty())
        assertNull(execution.updatedHiddenMediaIds)
        assertFalse(hiddenWasCalled)
    }

    @Test
    fun cancelledRemovalDeletesAndUnhidesOnlySafeRollbackCopies() {
        val deletedIds = mutableListOf<String>()
        val hiddenCalls = mutableListOf<Pair<Set<String>, Boolean>>()
        val operations = testOperations(
            originalStillExists = { it == "original-remains" },
            deleteFromVault = deletedIds::add,
            setHidden = { ids, hidden ->
                hiddenCalls += ids to hidden
                setOf("still-locked")
            }
        )

        val execution = operations.resolveOriginalRemoval(
            importedIds = setOf("original-remains", "original-gone"),
            result = LockedMediaOriginalRemovalResult.CancelledOrFailed
        )

        assertEquals(setOf("original-remains"), execution.resolution.rollbackIds)
        assertEquals(setOf("original-gone"), execution.resolution.committedIds)
        assertEquals(listOf("original-remains"), deletedIds)
        assertEquals(listOf(setOf("original-remains") to false), hiddenCalls)
        assertEquals(setOf("still-locked"), execution.updatedHiddenMediaIds)
    }

    @Test
    fun approvedRemovalCommitsWithoutProbingOrRollingBack() {
        var originalWasProbed = false
        var vaultWasDeleted = false
        var hiddenWasChanged = false
        val operations = testOperations(
            originalStillExists = {
                originalWasProbed = true
                true
            },
            deleteFromVault = { vaultWasDeleted = true },
            setHidden = { _, _ ->
                hiddenWasChanged = true
                emptySet()
            }
        )

        val execution = operations.resolveOriginalRemoval(
            importedIds = setOf("one"),
            result = LockedMediaOriginalRemovalResult.Approved
        )

        assertEquals(setOf("one"), execution.resolution.committedIds)
        assertTrue(execution.resolution.rollbackIds.isEmpty())
        assertNull(execution.updatedHiddenMediaIds)
        assertFalse(originalWasProbed)
        assertFalse(vaultWasDeleted)
        assertFalse(hiddenWasChanged)
    }

    private fun testOperations(
        importToVault: (MediaItem) -> Boolean = { true },
        originalStillExists: (String) -> Boolean = { false },
        deleteFromVault: (String) -> Unit = {},
        setHidden: (Set<String>, Boolean) -> Set<String> = { ids, _ -> ids }
    ): LockedMediaOperations {
        return LockedMediaOperations(
            importToVault = importToVault,
            originalStillExists = originalStillExists,
            deleteFromVault = deleteFromVault,
            setHidden = setHidden
        )
    }

    private fun mediaItem(id: String, title: String = id): MediaItem {
        return MediaItem(
            id = id,
            albumId = "camera",
            type = MediaType.Photo,
            title = title,
            dateLabel = "Today"
        )
    }
}
