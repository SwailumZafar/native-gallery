package com.example.nativegallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.ui.components.GalleryImage

@Composable
fun PhotoViewerOverlay(
    mediaItem: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit
) {
    BackHandler(enabled = visible && mediaItem != null, onBack = onClose)

    AnimatedVisibility(
        visible = visible && mediaItem != null,
        enter = fadeIn(animationSpec = tween(120)) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(140)) + scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
        )
    ) {
        if (mediaItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                GalleryImage(
                    imageRes = mediaItem.imageRes,
                    imageUri = mediaItem.contentUri,
                    contentDescription = mediaItem.title,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    contentScale = ContentScale.Fit,
                    thumbnailSize = 1440
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .padding(start = 6.dp, top = 38.dp, end = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close photo",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = mediaItem.title,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Photo options",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
