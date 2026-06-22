package com.example.nativegallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Immutable
data class GalleryActionSheetItem(
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun GalleryActionSheet(
    visible: Boolean,
    title: String,
    items: List<GalleryActionSheetItem>,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(100)),
        exit = fadeOut(animationSpec = tween(120))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .bouncyClickable(pressedScale = 1f, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
                ) + fadeIn() + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = tween(100)) + scaleOut(
                    targetScale = 0.985f,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .bouncyClickable(pressedScale = 1f, onClick = {}),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(30.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 18.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        items.forEach { item ->
                            GalleryActionSheetRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryActionSheetRow(item: GalleryActionSheetItem) {
    val contentColor = if (item.destructive) {
        Color(0xFFE95454)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { item.onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Spacer(Modifier.size(22.dp))
        }
        Text(
            text = item.label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
        if (item.selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
