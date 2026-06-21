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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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

@Composable
fun HiddenItemsScreen(
    albums: List<Album>,
    hiddenStates: SnapshotStateMap<String, Boolean>,
    onBack: () -> Unit,
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
        item {
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
                        text = "Hidden items",
                        modifier = Modifier.padding(start = 18.dp),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(
                text = "Choose albums to hide from Photos and Albums.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(26.dp))
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
                            onCheckedChange = { checked -> hiddenStates[album.id] = checked }
                        )
                        if (index != albums.lastIndex) {
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Hidden items can be shown again anytime.",
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text = "%,d items".format(album.itemCount),
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
