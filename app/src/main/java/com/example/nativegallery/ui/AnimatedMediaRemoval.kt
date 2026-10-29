package com.example.nativegallery.ui

import androidx.compose.runtime.*
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.delay

internal val LocalDepartingMediaIds = staticCompositionLocalOf<Set<String>> { emptySet() }
internal data class AnimatedMediaList(val items: List<MediaItem>, val departing: Set<String>)

internal fun retainDepartingMedia(previous: List<MediaItem>, incoming: List<MediaItem>): List<MediaItem> {
    val incomingById = incoming.associateBy { it.id }
    val oldIds = previous.mapTo(hashSetOf()) { it.id }
    return previous.map { incomingById[it.id] ?: it } + incoming.filterNot { it.id in oldIds }
}

@Composable
internal fun rememberAnimatedMediaRemoval(incoming: List<MediaItem>, key: Any? = Unit): AnimatedMediaList {
    var state by remember(key) { mutableStateOf(AnimatedMediaList(incoming, emptySet())) }
    LaunchedEffect(incoming, key) {
        val nextIds = incoming.mapTo(hashSetOf()) { it.id }
        val removed = state.items.mapTo(hashSetOf()) { it.id } - nextIds
        if (removed.isEmpty()) state = AnimatedMediaList(incoming, emptySet())
        else {
            state = AnimatedMediaList(retainDepartingMedia(state.items, incoming), removed)
            delay(210)
            state = AnimatedMediaList(incoming, emptySet())
        }
    }
    return state
}
