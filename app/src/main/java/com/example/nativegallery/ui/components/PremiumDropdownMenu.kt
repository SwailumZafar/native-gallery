package com.example.nativegallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val revealScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 620f),
        label = "dropdown scale"
    )
    val revealAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(120),
        label = "dropdown alpha"
    )
    val menuShape = RoundedCornerShape(18.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier
            .widthIn(min = 208.dp, max = 300.dp)
            .graphicsLayer {
                alpha = revealAlpha
                scaleX = 0.975f + 0.025f * revealScale
                scaleY = 0.965f + 0.035f * revealScale
                translationY = -4f * (1f - revealScale)
                transformOrigin = TransformOrigin(0.92f, 0f)
                shadowElevation = 10.dp.toPx()
                shape = menuShape
                clip = true
            }
            .clip(menuShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = menuShape
            ),
        content = content
    )
}

@Composable
fun PremiumOverflowButton(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 540f),
        label = "overflow rotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (expanded) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
        label = "overflow scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        },
        animationSpec = tween(120),
        label = "overflow background"
    )

    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                    scaleX = iconScale
                    scaleY = iconScale
                }
            )
        }
    }
}
