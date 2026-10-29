package com.example.nativegallery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.SmartAlbumRule

@Composable
internal fun SmartAlbumDialog(onDismiss: () -> Unit, onCreate: (SmartAlbumRule) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("All") }
    var days by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create smart album") }, text = {
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Save filters. Matching photos and videos appear here automatically, in their original folders.")
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Album name") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("All", "Photos", "Videos").forEach { value -> FilterChip(type == value, { type = value }, { Text(value) }) }
            }
            Text("Date range", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(0 to "Any", 7 to "7 days", 30 to "30 days").forEach { (value, title) -> FilterChip(days == value, { days = value }, { Text(title) }) }
            }
            OutlinedTextField(text, { text = it.take(100) }, label = { Text("Filename or folder contains") }, singleLine = true)
            Row { Checkbox(favorites, { favorites = it }); Text("Favorites only", Modifier.padding(top = 12.dp)) }
        }
    }, confirmButton = { TextButton(onClick = { onCreate(SmartAlbumRule(name = name.trim(), mediaType = type, pastDays = days, text = text.trim(), favoritesOnly = favorites)) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
