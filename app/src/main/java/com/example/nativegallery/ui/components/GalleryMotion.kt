package com.example.nativegallery.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

/**
 * Natural physics-based motion specifications matching Android & Solar Player.
 * Uses harmonic oscillators with zero linear robotic easing for fluid responsiveness.
 */
object MotionSpecs {
    /**
     * Snappy tactile response for touch down/up, action pills, icons, and buttons.
     */
    val AppleSpringSnappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 450f,
    )

    /**
     * Fluid layout reordering for grid cards, list items, and dynamic reflow.
     */
    val AppleSpringFluid: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 280f,
    )

    /**
     * Energetic playful spring for dialogs, popups, and badges.
     */
    val AppleSpringBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 320f,
    )

    /**
     * Smooth, non-oscillating glide for screen navigation, bottom bar, and sheets.
     */
    val AppleSpringSheet: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 300f,
    )

    /**
     * IntOffset spring for slide transitions and drag settling.
     */
    val AppleSpringOffset: SpringSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 280f,
    )

    val AppleSpringSheetOffset: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.82f,
        stiffness = 400f,
    )

    val AppleSpringPlacement: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.82f,
        stiffness = 280f,
    )

    /**
     * Physics spring for folder opening expansion.
     */
    val AppleSpringFolder: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 300f,
    )

    /**
     * Physics spring for folder closing collapse.
     */
    val AppleSpringFolderClose: SpringSpec<Float> = spring(
        dampingRatio = 0.84f,
        stiffness = 320f,
    )

    /**
     * Responsive spring for pinch-to-resize bounce recovery.
     */
    val AppleSpringPinch: SpringSpec<Float> = spring(
        dampingRatio = 0.76f,
        stiffness = 340f,
    )

    /**
     * Fluid snap spring for photo viewer paging between media items.
     */
    val AppleSpringPager: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 420f,
    )

    /**
     * Bouncy spring for modal dialogs, popups, and settings overlays.
     */
    val AppleSpringModal: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 400f,
    )
}

object GalleryMotion {
    const val SharedBoundsMillis = 420
    const val AlbumOpenMillis = 400
    const val AlbumCloseMillis = 350
    val AlbumOpenEasing = CubicBezierEasing(0.20f, 0f, 0f, 1f)
    val AlbumCloseEasing = CubicBezierEasing(0.30f, 0f, 0.20f, 1f)
    const val ViewerHeroOpenMillis = 330
    const val ViewerHeroCloseMillis = 260
    const val SecondaryOpenMillis = 260
    const val SecondaryCloseMillis = 230

    const val ContainerOpenDamping = 0.82f
    const val ContainerOpenStiffness = 300f
    const val ContainerCloseDamping = 0.84f
    const val ContainerCloseStiffness = 320f
    const val ViewerSpringBackDamping = 0.78f
    const val ViewerSpringBackStiffness = 380f

    const val AlbumHeroOpenDamping = ContainerOpenDamping
    const val AlbumHeroOpenStiffness = ContainerOpenStiffness
    const val MediaOpenDamping = 0.86f
    const val MediaOpenStiffness = 280f
    const val ViewerDismissDamping = 0.82f
    const val ViewerDismissStiffness = 360f
    const val ViewerTransformDamping = 0.84f
    const val ViewerTransformStiffness = 360f
    const val SkeletonVisibleMillis = 1600L
    const val SkeletonShimmerMillis = 1350
    const val PullThresholdDp = 72
    const val ViewerChromeFadeMillis = 90
    const val ViewerChromeCloseDelayMillis = 40L
    const val ViewerActionEnterMillis = 180
    const val ViewerActionExitMillis = 140
    const val ViewerDetailsEnterMillis = 220
    const val ViewerDetailsExitMillis = 160
    const val BottomSelectionOffsetPx = 120
    const val BottomNavIndicatorStiffness = 520f
    const val BottomNavIndicatorDamping = 0.76f
    const val BottomNavPressedScale = 0.90f
    const val BottomNavPressStiffness = 480f
    const val BottomNavPressDamping = 0.72f
    const val TilePressedScale = 0.94f
    const val PressDamping = 0.72f
    const val PressStiffness = 440f

    val ViewerHeroCloseSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.84f,
        stiffness = 360f
    )

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun easeOutCubic(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }
}

/**
 * High-performance tactile modifier for click targets (cards, buttons, icons).
 * Computes scaling purely on RenderThread via graphicsLayer to eliminate recomposition.
 */
@Composable
fun Modifier.solarTactile(
    scaleDown: Float = 0.96f,
    targetScale: Float = scaleDown,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val finalScale = if (targetScale != 0.96f) targetScale else scaleDown
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled) finalScale else 1f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "solarTactileScale",
    )
    return this
        .graphicsLayer {
            val scale = scaleState.value
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/**
 * Lightweight scale animation linked to an external InteractionSource.
 */
@Composable
fun Modifier.solarScale(
    interactionSource: InteractionSource,
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "solarScale",
    )
    return this.graphicsLayer {
        val scale = scaleState.value
        scaleX = scale
        scaleY = scale
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = GalleryMotion.TilePressedScale,
    pressDampingRatio: Float = GalleryMotion.PressDamping,
    pressStiffness: Float = GalleryMotion.PressStiffness,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = pressDampingRatio,
            stiffness = pressStiffness
        ),
        label = "gallery press bounce"
    )

    return graphicsLayer {
        val scale = scaleState.value
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
