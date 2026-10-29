package com.example.nativegallery.data

import org.junit.Assert.*
import org.junit.Test

class GalleryMutationGateTest {
    @Test fun nestedVaultImportAndOriginalRemovalPublishOnlyAfterBothComplete() {
        val gate = GalleryMutationGate()
        gate.begin()
        gate.begin()
        assertFalse(gate.finish())
        assertTrue(gate.isWriting)
        assertFalse(gate.canPublish(gate.revision))
        assertTrue(gate.finish())
        assertFalse(gate.isWriting)
        assertTrue(gate.canPublish(gate.revision))
    }

    @Test fun aQueryStartedBeforeAnyMutationCannotReplaceTheStableLibrary() {
        val gate = GalleryMutationGate()
        val beforeImport = gate.revision
        gate.begin()
        val duringImport = gate.revision
        gate.finish()
        assertFalse(gate.canPublish(beforeImport))
        assertFalse(gate.canPublish(duringImport))
        assertTrue(gate.canPublish(gate.revision))
    }

    @Test fun unmatchedCompletionDoesNotUnderflowTheWriteCounter() {
        val gate = GalleryMutationGate()
        assertFalse(gate.finish())
        gate.begin()
        assertTrue(gate.finish())
        assertFalse(gate.finish())
        gate.begin()
        assertTrue(gate.isWriting)
        assertTrue(gate.finish())
    }
}
