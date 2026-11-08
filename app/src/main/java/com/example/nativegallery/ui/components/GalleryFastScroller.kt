package com.example.nativegallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class FastScrollTarget(val index: Int, val offset: Int)

internal fun fastScrollTarget(fraction: Float, totalItems: Int, visibleItems: Int, itemSize: Int): FastScrollTarget {
    val maxFirst = (totalItems - visibleItems.coerceAtLeast(1)).coerceAtLeast(0)
    val safeFraction = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    if (maxFirst == 0) return FastScrollTarget(0, 0)
    // Let LazyColumn clamp the last row to its exact viewport end, including partial rows.
    if (safeFraction == 1f) return FastScrollTarget((totalItems - 1).coerceAtLeast(0), 0)
    val exact = safeFraction * maxFirst
    val index = floor(exact).toInt().coerceIn(0, maxFirst)
    return FastScrollTarget(index, ((exact - index) * itemSize.coerceAtLeast(1)).roundToInt())
}

@Composable
fun GalleryFastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    topPadding: Dp = 104.dp
) {
    val totalItems by remember(listState) {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val visibleItems by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }
    if (totalItems <= visibleItems.coerceAtLeast(1) + 4) return

    val fraction = remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val maxFirst = (info.totalItemsCount - info.visibleItemsInfo.size.coerceAtLeast(1)).coerceAtLeast(1)
            val firstVisible = info.visibleItemsInfo.firstOrNull()
            val itemProgress = firstVisible
                ?.size
                ?.takeIf { it > 0 }
                ?.let { size -> listState.firstVisibleItemScrollOffset.toFloat() / size }
                ?: 0f
            when {
                !listState.canScrollBackward -> 0f
                !listState.canScrollForward -> 1f
                else -> ((listState.firstVisibleItemIndex + itemProgress) / maxFirst).coerceIn(0f, 1f)
            }
        }
    }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }
    var pendingTarget by remember(listState) { mutableStateOf<FastScrollTarget?>(null) }
    LaunchedEffect(listState) {
        snapshotFlow { pendingTarget }.filterNotNull().conflate().collect {
            // Touch samples can arrive faster than display frames. Measure only the
            // latest destination once per frame, rather than every intermediate sample.
            withFrameNanos { }
            val latest = pendingTarget
            if (latest != null && (listState.firstVisibleItemIndex != latest.index ||
                    listState.firstVisibleItemScrollOffset != latest.offset)) {
                listState.requestScrollToItem(latest.index, latest.offset)
            }
        }
    }
    val hapticFeedback = LocalHapticFeedback.current
    val thumbHeight = 58.dp
    val thumbHeightPx = with(LocalDensity.current) { thumbHeight.roundToPx() }

    LaunchedEffect(listState.isScrollInProgress, dragging) {
        if (listState.isScrollInProgress || dragging) {
            visible = true
        } else if (visible) {
            delay(1_000L)
            visible = false
        }
    }

    val dragModifier = if (visible) {
        Modifier.pointerInput(listState, totalItems, trackHeightPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1)
                val currentThumbTop = travelPx * fraction.value
                if (down.position.y !in currentThumbTop..(currentThumbTop + thumbHeightPx)) {
                    return@awaitEachGesture
                }

                dragging = true
                pendingTarget = null
                dragFraction = fraction.value
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                down.consume()
                try {
                    val grabOffset = down.position.y - currentThumbTop
                    val sizes = listState.layoutInfo.visibleItemsInfo.map { it.size }.filter { it > 0 }.sorted()
                    val representativeItemSize = sizes.getOrNull(sizes.size / 2) ?: 1
                    var change = down
                    do {
                        val event = awaitPointerEvent()
                        change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val requestedFraction = (
                            (change.position.y - grabOffset) / travelPx
                            ).coerceIn(0f, 1f)
                        dragFraction = requestedFraction

                        // Request the destination item directly. dispatchRawDelta has to measure
                        // every intermediate row for a large drag, which is both laggy and prone to
                        // leaving the thumb behind on media-heavy lists.
                        val layoutInfo = listState.layoutInfo
                        val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        pendingTarget = fastScrollTarget(requestedFraction, totalItems, visibleCount, representativeItemSize)
                        change.consume()
                    } while (change.pressed)
                } finally {
                    dragging = false
                }
            }
        }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(44.dp)
    ) {
        val bottomPadding = 82.dp
        val minimumTravel = 56.dp
        val maximumSafeTop = (
            maxHeight - bottomPadding - thumbHeight - minimumTravel
            ).coerceAtLeast(0.dp)
        val effectiveTopPadding = topPadding.coerceIn(0.dp, maximumSafeTop)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = effectiveTopPadding, bottom = bottomPadding)
                .onSizeChanged { trackHeightPx = it.height }
                .testTag("gallery-fast-scroll-track")
                .then(dragModifier),
            contentAlignment = Alignment.TopCenter
        ) {
            val travel = (trackHeightPx - thumbHeightPx).coerceAtLeast(0)
            val thumbScale by animateFloatAsState(
                targetValue = if (dragging) 1.12f else 1f,
                animationSpec = MotionSpecs.AppleSpringSnappy,
                label = "fastScrollerThumbScale"
            )
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(170)),
                modifier = Modifier.graphicsLayer {
                    translationY = travel * if (dragging) dragFraction else fraction.value
                }
            ) {
                Surface(
                    modifier = Modifier
                        .testTag("gallery-fast-scroll-thumb")
                        .width(34.dp)
                        .height(thumbHeight)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .systemGestureExclusion(),
                    color = Color(0xFF353535),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = if (dragging) 9.dp else 5.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropUp,
                            contentDescription = "Scroll toward top",
                            tint = Color(0xFFF2F2F2),
                            modifier = Modifier
                                .width(31.dp)
                                .height(27.dp)
                                .offset(y = (-6).dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Scroll toward bottom",
                            tint = Color(0xFFF2F2F2),
                            modifier = Modifier
                                .width(31.dp)
                                .height(27.dp)
                                .offset(y = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
