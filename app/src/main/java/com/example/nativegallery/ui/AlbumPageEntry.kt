package com.example.nativegallery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/** Submit the moving captured cover before composing a new album's grid. */
@Composable
internal fun rememberAlbumPageReady(albumId: String, deferNewPage: Boolean): State<Boolean> {
    val ready = remember(albumId) { mutableStateOf(!deferNewPage) }
    LaunchedEffect(albumId) {
        if (!ready.value) {
            // The page initially contains single-node cached rows, so one submitted
            // cover frame is enough. Do not hold a blank page for several frames.
            withFrameNanos { }
            ready.value = true
        }
    }
    return ready
}
