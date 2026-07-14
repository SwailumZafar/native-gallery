package com.example.nativegallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GalleryFastScroller(listState: LazyListState, modifier: Modifier = Modifier) {
    val totalItems by remember(listState) {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val visibleItems by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }
    if (visibleItems == 0 || totalItems <= visibleItems + 4) return

    val scope = rememberCoroutineScope()
    val fraction by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val maxFirst = (info.totalItemsCount - info.visibleItemsInfo.size).coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
        }
    }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    val thumbHeight = 46.dp
    val thumbHeightPx = with(LocalDensity.current) { thumbHeight.roundToPx() }
    val animationLabel = "fast scroller alpha"
    val thumbAlpha by animateFloatAsState(
        if (listState.isScrollInProgress) 0.92f else 0.50f,
        tween(140),
        label = animationLabel
    )

    fun scrollTo(y: Float) {
        if (trackHeightPx <= 0) return
        val position = (y / trackHeightPx).coerceIn(0f, 1f)
        val target = (position * (totalItems - 1)).roundToInt()
        scope.launch { listState.scrollToItem(target) }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(top = 112.dp, bottom = 86.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { trackHeightPx = it.height }
                .pointerInput(totalItems, trackHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        scrollTo(down.position.y)
                        var change = down
                        do {
                            change.consume()
                            val event = awaitPointerEvent()
                            change = event.changes.firstOrNull { it.id == down.id } ?: break
                            scrollTo(change.position.y)
                        } while (change.pressed)
                    }
                }
        )
        val travel = (trackHeightPx - thumbHeightPx).coerceAtLeast(0)
        Surface(
            modifier = Modifier
                .width(5.dp)
                .height(thumbHeight)
                .alpha(thumbAlpha)
                .offset { IntOffset(0, (travel * fraction).roundToInt()) },
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 3.dp
        ) {}
    }
}
