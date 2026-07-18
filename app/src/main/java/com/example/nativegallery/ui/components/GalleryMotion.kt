package com.example.nativegallery.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object GalleryMotion {
    const val SharedBoundsMillis = 420
    const val AlbumOpenMillis = 420
    const val AlbumCloseMillis = 400
    const val ViewerHeroOpenMillis = 360
    const val ViewerHeroCloseMillis = 300
    const val SecondaryOpenMillis = 260
    const val SecondaryCloseMillis = 230

    const val ContainerOpenDamping = 0.86f
    const val ContainerOpenStiffness = 360f
    const val ContainerCloseDamping = 0.90f
    const val ContainerCloseStiffness = 320f
    const val ViewerSpringBackDamping = 0.92f
    const val ViewerSpringBackStiffness = 280f

    const val AlbumHeroOpenDamping = ContainerOpenDamping
    const val AlbumHeroOpenStiffness = ContainerOpenStiffness
    const val MediaOpenDamping = 0.91f
    const val MediaOpenStiffness = 260f
    const val ViewerDismissDamping = 0.9f
    const val ViewerDismissStiffness = 310f
    const val ViewerTransformDamping = 0.88f
    const val ViewerTransformStiffness = 360f
    const val SkeletonVisibleMillis = 1600L
    const val SkeletonShimmerMillis = 1350
    const val PullThresholdDp = 72
    const val ViewerChromeFadeMillis = 90
    const val ViewerChromeCloseDelayMillis = 70L
    const val ViewerActionEnterMillis = 180
    const val ViewerActionExitMillis = 140
    const val ViewerDetailsEnterMillis = 220
    const val ViewerDetailsExitMillis = 160
    const val BottomSelectionOffsetPx = 120
    const val BottomNavIndicatorStiffness = 380f
    const val BottomNavIndicatorDamping = 0.77f
    const val BottomNavPressedScale = 0.9f
    const val BottomNavPressStiffness = 500f
    const val BottomNavPressDamping = 0.67f
    const val TilePressedScale = 0.975f
    const val PressDamping = 0.86f

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun easeOutCubic(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = GalleryMotion.TilePressedScale,
    pressDampingRatio: Float = GalleryMotion.PressDamping,
    pressStiffness: Float = Spring.StiffnessMedium,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = pressDampingRatio,
            stiffness = pressStiffness
        ),
        label = "gallery press bounce"
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.combinedClickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick
    )
}
