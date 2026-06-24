package com.example.nativegallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun Modifier.galleryRubberBandOverscroll(
    enabled: Boolean = true,
    resistance: Float = 0.4f,
    maxPull: Dp = 84.dp
): Modifier {
    if (!enabled) return this

    val scope = rememberCoroutineScope()
    val maxPullPx = with(LocalDensity.current) { maxPull.toPx() }.coerceAtLeast(1f)
    val pullOffset = remember { Animatable(0f) }
    val connection = remember(maxPullPx, resistance) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val nextOffset = (pullOffset.value + available.y * resistance)
                    .coerceIn(-maxPullPx, maxPullPx)
                if (nextOffset != pullOffset.value) {
                    scope.launch { pullOffset.snapTo(nextOffset) }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullOffset.value != 0f) {
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = GalleryMotionSpec.DampingRatio,
                            stiffness = GalleryMotionSpec.Stiffness
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    return nestedScroll(connection).graphicsLayer {
        translationY = pullOffset.value
        val stretch = 1f + (abs(pullOffset.value) / maxPullPx) * 0.018f
        scaleY = stretch
    }
}