@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.nativegallery.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativegallery.model.MediaItem

internal fun originalMediaShareIntent(media: List<MediaItem>): Intent {
    val items = media.filter { it.contentUri != null }
    require(items.isNotEmpty())
    val uris = items.map { requireNotNull(it.contentUri) }
    return Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = when { items.all { !it.isVideo } -> "image/*"; items.all { it.isVideo } -> "video/*"; else -> "*/*" }
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        clipData = ClipData.newRawUri("Selected media", uris.first()).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

@Composable
internal fun ShareOptionsSheet(items: List<MediaItem>, onDismiss: () -> Unit, onOriginals: () -> Unit, onPrivate: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (items.size == 1) "Share this ${if (items.first().isVideo) "video" else "photo"}" else "Share ${items.size} items",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Surface(onClick = onOriginals, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Share, null)
                    Column { Text("Share originals", style = MaterialTheme.typography.titleMedium)
                        Text("Keep their quality and metadata", style = MaterialTheme.typography.bodySmall) }
                }
            }
            val available = items.isNotEmpty() && items.all { !it.isVideo }
            Surface(onClick = onPrivate, enabled = available, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Security, null)
                    Column { Text("Private copies", style = MaterialTheme.typography.titleMedium)
                        Text(if (available) "Remove metadata and review faces or text to hide" else "Select photos to prepare private copies",
                            style = MaterialTheme.typography.bodySmall) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
