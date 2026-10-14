package com.example.nativegallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp


@Composable
fun PremiumDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit
) {
    val reveal by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) {
            spring(dampingRatio = 0.78f, stiffness = 520f)
        } else {
            tween(durationMillis = 140)
        },
        label = "dropdown reveal"
    )
    val revealAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 100 else 120),
        label = "dropdown alpha"
    )
    val menuShape = RoundedCornerShape(22.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = menuShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .widthIn(min = 216.dp, max = 304.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.92f, 0.04f)
                scaleX = 0.45f + 0.55f * reveal
                scaleY = 0.35f + 0.65f * reveal
                alpha = revealAlpha
            },
        content = content
    )
}

@Composable
fun PremiumDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.solarScale(
            interactionSource = interactionSource,
            scaleDown = 0.965f,
            enabled = enabled
        ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingIconColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f),
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
        )
    )
}

@Composable
fun PremiumOverflowButton(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val rotationState = animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 540f),
        label = "overflow rotation"
    )
    val iconScaleState = animateFloatAsState(
        targetValue = if (expanded) 0.9f else 1f,
        animationSpec = MotionSpecs.AppleSpringSnappy,
        label = "overflow scale"
    )
    Surface(
        modifier = modifier
            .size(48.dp)
            .solarTactile(scaleDown = 0.88f, onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotationState.value
                    scaleX = iconScaleState.value
                    scaleY = iconScaleState.value
                }
            )
        }
    }
}
