package com.example.nativegallery.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Fluid harmonic spring entrance/exit transitions for media selection bars.
 */
val SelectionBarSpringEnter: EnterTransition = slideInVertically(
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
    initialOffsetY = { it + 36 }
) + scaleIn(
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
    initialScale = 0.86f
) + fadeIn(tween(140))

val SelectionBarSpringExit: ExitTransition = slideOutVertically(
    animationSpec = spring(dampingRatio = 0.86f, stiffness = 550f),
    targetOffsetY = { it + 36 }
) + scaleOut(
    animationSpec = tween(120),
    targetScale = 0.86f
) + fadeOut(tween(90))

val SelectionTopBarSpringEnter: EnterTransition = slideInVertically(
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
    initialOffsetY = { -it - 36 }
) + scaleIn(
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
    initialScale = 0.86f
) + fadeIn(tween(140))

val SelectionTopBarSpringExit: ExitTransition = slideOutVertically(
    animationSpec = spring(dampingRatio = 0.86f, stiffness = 550f),
    targetOffsetY = { -it - 36 }
) + scaleOut(
    animationSpec = tween(120),
    targetScale = 0.86f
) + fadeOut(tween(90))

@Composable
fun MediaSelectionTopBar(
    selectedCount: Int,
    canSelectAll: Boolean,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = mediaSelectionAccentColor()
    Surface(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .height(58.dp),
        color = Color(0xFF171717),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 9.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val closeInteractionSource = remember { MutableInteractionSource() }
            TextButton(
                onClick = onClear,
                interactionSource = closeInteractionSource,
                modifier = Modifier.solarScale(closeInteractionSource, scaleDown = 0.92f)
            ) {
                Text(
                    text = "Close",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = "%1$,d selected".format(selectedCount),
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = "$selectedCount selected"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.94f),
                textAlign = TextAlign.Center
            )
            val selectAllInteractionSource = remember { MutableInteractionSource() }
            TextButton(
                onClick = onSelectAll,
                enabled = canSelectAll,
                interactionSource = selectAllInteractionSource,
                modifier = Modifier.solarScale(selectAllInteractionSource, scaleDown = 0.92f, enabled = canSelectAll)
            ) {
                Text(
                    text = "Select all",
                    color = if (canSelectAll) {
                        accentColor
                    } else {
                        Color.White.copy(alpha = 0.38f)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun MediaSelectionActionBar(
    onShare: () -> Unit,
    onLock: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    selectedCount: Int = 0,
    onCompare: (() -> Unit)? = null,
    onCollage: (() -> Unit)? = null
) {
    val accentColor = mediaSelectionAccentColor()
    val showCompare = selectedCount == 2 && onCompare != null
    val showCollage = selectedCount in 2..6 && onCollage != null

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showCompare || showCollage) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCompare && onCompare != null) {
                    val compareSrc = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onCompare,
                        interactionSource = compareSrc,
                        modifier = Modifier.solarScale(compareSrc, scaleDown = 0.93f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF20222A),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Compare, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                "Compare shots",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
                if (showCollage && onCollage != null) {
                    val collageSrc = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onCollage,
                        interactionSource = collageSrc,
                        modifier = Modifier.solarScale(collageSrc, scaleDown = 0.93f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF20222A),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.GridView, null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Text(
                                "Create collage",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(72.dp),
            color = Color(0xFF171717),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CapsuleAction(Icons.Outlined.Share, "Share selected", "Share", onShare, Modifier.weight(1f), tint = accentColor)
                CapsuleAction(Icons.Outlined.Lock, "Lock selected", "Lock", onLock, Modifier.weight(1f), tint = accentColor)
                CapsuleAction(Icons.Outlined.Folder, "Move selected", "Move", onMove, Modifier.weight(1f), tint = accentColor)
                CapsuleAction(
                    icon = Icons.Outlined.Delete,
                    contentDescription = "Delete selected",
                    label = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun mediaSelectionAccentColor(): Color {
    val backgroundLuminance = MaterialTheme.colorScheme.background.luminance()
    val darkWeight = ((0.72f - backgroundLuminance) / 0.58f).coerceIn(0f, 1f)
    return lerp(Color(0xFF32DCC6), MaterialTheme.colorScheme.primary, darkWeight)
}

@Composable
private fun CapsuleAction(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(38.dp)
                .solarScale(interactionSource, scaleDown = 0.88f, enabled = enabled)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = label,
            color = if (enabled) tint else tint.copy(alpha = 0.38f),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}
