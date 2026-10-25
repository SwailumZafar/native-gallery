package com.example.nativegallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch

/**
 * Retargets a spring without observing pointer-frequency state in the caller's composition.
 * Read the returned state from a graphics layer or drawing block, just like the gesture target.
 * Animatable retains velocity when an in-flight animation is retargeted.
 */
@Composable
internal fun <T, V : AnimationVector> rememberDeferredAnimation(
    initialValue: T,
    typeConverter: TwoWayConverter<T, V>,
    targetValue: () -> T,
    animationSpec: () -> AnimationSpec<T>,
    label: String,
    animationKey: Any? = Unit
): State<T> {
    val animation = remember(animationKey) { Animatable(initialValue, typeConverter, label = label) }
    val currentTarget = rememberUpdatedState(targetValue)
    val currentSpec = rememberUpdatedState(animationSpec)
    LaunchedEffect(animation) {
        snapshotFlow { currentTarget.value() }.collect { target ->
            // Start the replacement before cancelling its predecessor so animateTo captures
            // the current velocity. collectLatest would cancel and zero that velocity first.
            launch {
                if (target != animation.targetValue) {
                    animation.animateTo(target, currentSpec.value())
                }
            }
        }
    }
    return animation.asState()
}
