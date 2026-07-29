package com.example.nativegallery.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.nativegallery.model.MediaItem

internal data class RetainedViewerSession(
    val mediaItem: MediaItem,
    val mediaItems: List<MediaItem>,
    val actionMode: ViewerActionMode
)

/**
 * Retains the active viewer across Activity recreation, including phone rotation and fold/unfold
 * size changes. Media remains in memory only; process-death recovery still returns to the gallery.
 */
internal class GalleryViewerSessionViewModel : ViewModel() {
    var session: RetainedViewerSession? = null
        private set

    /**
     * Android may recreate the Activity while its system MediaStore confirmation is visible.
     * Keeping this action in the Activity-scoped ViewModel makes the result/rollback idempotent
     * across rotation and fold/unfold changes.
     */
    val pendingMediaStoreWriteActionState = mutableStateOf<PendingMediaStoreWriteAction?>(null)

    fun retain(
        visible: Boolean,
        mediaItem: MediaItem?,
        mediaItems: List<MediaItem>,
        actionMode: ViewerActionMode
    ) {
        session = if (visible && mediaItem != null) {
            RetainedViewerSession(
                mediaItem = mediaItem,
                mediaItems = mediaItems.ifEmpty { listOf(mediaItem) },
                actionMode = actionMode
            )
        } else {
            null
        }
    }
}