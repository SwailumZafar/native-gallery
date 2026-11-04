package com.example.nativegallery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal data class GallerySortChoice(val key: String, val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GallerySortSheet(
    title: String,
    choices: List<GallerySortChoice>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var pendingSelection by remember(selectedKey) { mutableStateOf(selectedKey) }
    fun close(after: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide(); after(); onDismiss() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier.testTag("gallery-sort-sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.8f).dp)
            .padding(horizontal = 22.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Choose the order that works for you", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { close() }, enabled = !closing) { Icon(Icons.Filled.Close, "Close sorting") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice ->
                    val selected = choice.key == pendingSelection
                    Surface(Modifier.fillMaxWidth().selectable(selected, enabled = !closing, role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = { pendingSelection = choice.key; close { onSelect(choice.key) } }),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))) {
                        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(choice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(choice.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected, onClick = null)
                        }
                    }
                }
            }
        }
    }
}
