package com.example.nativegallery.ui

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryViewerSessionViewModelTest {
    @Test
    fun activeSessionSurvivesRecreationStoreAndNormalizesEmptyList() {
        val viewModel = GalleryViewerSessionViewModel()
        val item = mediaItem("photo")

        viewModel.retain(
            visible = true,
            mediaItem = item,
            mediaItems = emptyList(),
            actionMode = ViewerActionMode.Normal
        )

        assertEquals(item, viewModel.session?.mediaItem)
        assertEquals(listOf(item), viewModel.session?.mediaItems)
    }

    @Test
    fun closedViewerClearsRetainedSession() {
        val viewModel = GalleryViewerSessionViewModel()
        val item = mediaItem("photo")
        viewModel.retain(true, item, listOf(item), ViewerActionMode.Locked)

        viewModel.retain(false, item, listOf(item), ViewerActionMode.Locked)

        assertNull(viewModel.session)
    }

    @Test
    fun pendingMediaStoreWriteSurvivesActivityRecreationStoreUntilConsumed() {
        val viewModel = GalleryViewerSessionViewModel()
        val action = PendingMediaStoreWriteAction(
            mode = MediaStoreWriteMode.DeleteLockedOriginals,
            mediaItems = listOf(mediaItem("locked"))
        )

        viewModel.pendingMediaStoreWriteActionState.value = action

        assertEquals(action, viewModel.pendingMediaStoreWriteActionState.value)
        viewModel.pendingMediaStoreWriteActionState.value = null
        assertNull(viewModel.pendingMediaStoreWriteActionState.value)
    }

    private fun mediaItem(id: String): MediaItem = MediaItem(
        id = id,
        albumId = "camera",
        type = MediaType.Photo,
        title = id,
        dateLabel = "Today"
    )
}