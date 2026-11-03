package com.example.nativegallery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Holds the drawn card, including its gradient/labels, without copying a full-size bitmap. */
internal class AlbumTransitionSource<Layer : Any>(val layer: Layer) {
    var available by mutableStateOf(false)
    var size = IntSize.Zero
}

internal class AlbumTransitionSources<Layer : Any> {
    private val sources = mutableMapOf<String, AlbumTransitionSource<Layer>>()

    fun get(albumId: String): AlbumTransitionSource<Layer>? = sources[albumId]?.takeIf { it.available }
    fun record(albumId: String, source: AlbumTransitionSource<Layer>) { sources[albumId] = source }
    fun remove(albumId: String, source: AlbumTransitionSource<Layer>) {
        source.available = false
        if (sources[albumId] === source) sources.remove(albumId)
    }
}

internal val LocalAlbumTransitionSources = staticCompositionLocalOf<AlbumTransitionSources<GraphicsLayer>?> { null }

@Composable
internal fun Modifier.albumTransitionSource(
    albumId: String,
    activeAlbumId: State<String?>?
): Modifier {
    val sources = LocalAlbumTransitionSources.current ?: return this
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { AlbumTransitionSource(layer) }
    DisposableEffect(sources, albumId, source) {
        onDispose { sources.remove(albumId, source) }
    }
    return drawWithContent {
        val hidden = activeAlbumId?.value == albumId
        if (!hidden || !source.available) {
            layer.record { this@drawWithContent.drawContent() }
            source.size = IntSize(size.width.roundToInt(), size.height.roundToInt())
            source.available = true
            sources.record(albumId, source)
        }
        if (!hidden) drawLayer(layer)
    }
}
