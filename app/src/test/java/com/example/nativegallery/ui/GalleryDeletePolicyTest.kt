package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryDeletePolicyTest {
    @Test
    fun optimisticRemovalRequiresOneTapAccessAndDeleteOperation() {
        assertTrue(shouldApplyOptimisticMediaRemoval(true, MediaStoreWriteMode.Trash))
        assertTrue(shouldApplyOptimisticMediaRemoval(true, MediaStoreWriteMode.DeleteForever))
        assertFalse(shouldApplyOptimisticMediaRemoval(false, MediaStoreWriteMode.Trash))
        assertFalse(shouldApplyOptimisticMediaRemoval(true, MediaStoreWriteMode.RestoreFromTrash))
        assertFalse(shouldApplyOptimisticMediaRemoval(true, MediaStoreWriteMode.MoveToAlbum))
        assertFalse(shouldApplyOptimisticMediaRemoval(true, MediaStoreWriteMode.DeleteLockedOriginals))
    }
}
