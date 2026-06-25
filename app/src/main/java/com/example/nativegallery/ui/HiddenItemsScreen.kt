package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativegallery.model.Album
import com.example.nativegallery.ui.components.ResourceImage
import com.example.nativegallery.ui.components.bouncyClickable

@Composable
fun HiddenItemsScreen(
    albums: List<Album>,
    hiddenStates: SnapshotStateMap<String, Boolean>,
    hiddenAlbumCount: Int,
    hiddenItemCount: Int,
    onBack: () -> Unit,
    onHiddenChange: (Album, Boolean) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 48.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 34.dp
        )
    ) {
        item(key = "hidden-header", contentType = "hidden-header") {
            HiddenAlbumsHeader(
                hiddenAlbumCount = hiddenAlbumCount,
                onBack = onBack
            )
            Spacer(Modifier.height(26.dp))
            HiddenAlbumsSummary(
                hiddenAlbumCount = hiddenAlbumCount,
                hiddenItemCount = hiddenItemCount
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Albums",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
        }

        item(key = "hidden-albums-list", contentType = "hidden-albums-list") {
            if (albums.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = "No albums are available to hide.",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        albums.forEachIndexed { index, album ->
                            HiddenAlbumRow(
                                album = album,
                                checked = hiddenStates[album.id] == true,
                                onCheckedChange = { checked -> onHiddenChange(album, checked) }
                            )
                            if (index != albums.lastIndex) {
                                Spacer(Modifier.height(1.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Hidden albums are separate from locked photos and videos.",
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HiddenAlbumsHeader(
    hiddenAlbumCount: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Hidden albums",
                modifier = Modifier.padding(start = 18.dp),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = hiddenAlbumsBadgeLabel(hiddenAlbumCount),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HiddenAlbumsSummary(
    hiddenAlbumCount: Int,
    hiddenItemCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hiddenAlbumsSummary(hiddenAlbumCount, hiddenItemCount),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Use locked media for individual photos or videos that need authentication.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HiddenAlbumRow(
    album: Album,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onCheckedChange(!checked) }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResourceImage(
            imageRes = album.coverRes,
            imageUri = album.coverUri,
            contentDescription = album.name,
            modifier = Modifier.size(64.dp),
            cornerRadius = 12.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = hiddenAlbumRowLabel(album.itemCount, checked),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            )
        )
    }
}

private fun hiddenAlbumsSummary(hiddenAlbumCount: Int, hiddenItemCount: Int): String {
    return if (hiddenAlbumCount > 0) {
        "%1$,d albums and %2$,d items are hidden from the gallery.".format(hiddenAlbumCount, hiddenItemCount)
    } else {
        "Choose albums to hide from Photos and Albums."
    }
}

private fun hiddenAlbumsBadgeLabel(hiddenAlbumCount: Int): String {
    return if (hiddenAlbumCount > 0) "%1$,d hidden".format(hiddenAlbumCount) else "None"
}

private fun hiddenAlbumRowLabel(itemCount: Int, checked: Boolean): String {
    return if (checked) {
        "Hidden album, %1$,d items".format(itemCount)
    } else {
        "%1$,d items".format(itemCount)
    }
}