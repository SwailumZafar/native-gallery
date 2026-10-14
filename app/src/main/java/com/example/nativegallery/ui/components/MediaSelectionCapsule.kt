package com.example.nativegallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
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
    modifier: Modifier = Modifier
) {
    val accentColor = mediaSelectionAccentColor()
    Surface(
        modifier = modifier
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
