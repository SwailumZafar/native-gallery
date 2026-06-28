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
    const val AlbumOpenMillis = 240
    const val AlbumHeroOpenDamping = 0.86f
    const val AlbumHeroOpenStiffness = 430f
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