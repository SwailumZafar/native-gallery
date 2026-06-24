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

object GalleryMotionSpec {
    const val DampingRatio = 0.75f
    const val Stiffness = 300f
    const val PressedScale = 0.95f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = GalleryMotionSpec.PressedScale,
    pressDampingRatio: Float = GalleryMotionSpec.DampingRatio,
    pressStiffness: Float = GalleryMotionSpec.Stiffness,
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