package com.example.nativegallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.nativegallery.data.VaultCredentialKind
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun extendVaultPattern(pattern: String, dot: Int): String {
    if (dot !in 1..9 || ('0' + dot) in pattern) return pattern
    val previous = pattern.lastOrNull()?.digitToInt()
    var next = pattern
    if (previous != null) {
        val x1 = (previous - 1) % 3; val y1 = (previous - 1) / 3
        val x2 = (dot - 1) % 3; val y2 = (dot - 1) / 3
        if ((abs(x1 - x2) == 2 || abs(y1 - y2) == 2) && (x1 + x2) % 2 == 0 && (y1 + y2) % 2 == 0) {
            val middle = (y1 + y2) / 2 * 3 + (x1 + x2) / 2 + 1
            if (('0' + middle) !in next) next += ('0' + middle)
        }
    }
    return next + ('0' + dot)
}

@Composable
internal fun VaultCredentialField(kind: VaultCredentialKind, value: String, onValueChange: (String) -> Unit,
    label: String, enabled: Boolean = true) {
    if (kind == VaultCredentialKind.Pattern) {
        val color = MaterialTheme.colorScheme.primary
        val muted = MaterialTheme.colorScheme.outlineVariant
        val current by rememberUpdatedState(value)
        val update by rememberUpdatedState(onValueChange)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth().widthIn(max = 280.dp).aspectRatio(1f)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    var gesturePattern = ""
                    fun visit(at: Offset) {
                        val column = (at.x / (size.width / 3f) - 0.5f).roundToInt().coerceIn(0, 2)
                        val row = (at.y / (size.height / 3f) - 0.5f).roundToInt().coerceIn(0, 2)
                        val center = Offset((column + 0.5f) * size.width / 3, (row + 0.5f) * size.height / 3)
                        if ((at - center).getDistance() < size.width / 7f) {
                            gesturePattern = extendVaultPattern(gesturePattern, row * 3 + column + 1)
                            update(gesturePattern)
                        }
                    }
                    detectDragGestures(onDragStart = { gesturePattern = ""; visit(it) }, onDrag = { change, _ -> visit(change.position); change.consume() })
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    fun center(dot: Char) = Offset(((dot.digitToInt() - 1) % 3 + 0.5f) * size.width / 3,
                        ((dot.digitToInt() - 1) / 3 + 0.5f) * size.height / 3)
                    value.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = 0.6f), center(a), center(b), 4.dp.toPx()) }
                    (1..9).forEach { dot -> drawCircle(if (('0' + dot) in value) color else muted, 9.dp.toPx(), center('0' + dot)) }
                }
                Column(Modifier.fillMaxSize()) {
                    repeat(3) { row -> Row(Modifier.weight(1f)) {
                        repeat(3) { col ->
                            val dot = row * 3 + col + 1
                            Box(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "Pattern dot $dot" }
                                .clickable(enabled = enabled) { onValueChange(extendVaultPattern(value, dot)) })
                        }
                    } }
                }
            }
            TextButton(onClick = { onValueChange("") }, enabled = enabled) { Text("Clear pattern") }
        }
    } else OutlinedTextField(value, { next ->
        onValueChange(if (kind == VaultCredentialKind.Pin) next.filter { it in '0'..'9' }.take(16) else next.take(128))
    }, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = enabled, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = if (kind == VaultCredentialKind.Pin) KeyboardType.NumberPassword else KeyboardType.Password))
}

@Composable
internal fun VaultSecurityDialog(state: LockedMediaSecurityUiState, model: LockedMediaSecurityViewModel, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var kind by remember { mutableStateOf(state.credentialKind) }
    var secret by remember(tab, kind) { mutableStateOf("") }
    var confirm by remember(tab, kind) { mutableStateOf("") }
    var prompt by remember { mutableStateOf("My recovery phrase") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Vault settings") }, text = {
        Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Access", "Recovery", "Decoy").forEachIndexed { i, title -> FilterChip(tab == i, { tab = i; model.clearMessage() }, { Text(title) }) }
            }
            when (tab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        VaultCredentialKind.entries.forEach { option -> FilterChip(kind == option, { kind = option }, { Text(option.name) }) }
                    }
                    VaultCredentialField(kind, secret, { secret = it }, "New ${kind.name}", !state.operationInProgress)
                    VaultCredentialField(kind, confirm, { confirm = it }, "Confirm ${kind.name}", !state.operationInProgress)
                    Button(onClick = { model.changeCredential(kind, secret, confirm); secret = ""; confirm = "" }, enabled = !state.operationInProgress) { Text("Save credential") }
                }
                1 -> {
                    Text("Five wrong attempts lock access for 1 minute. Further groups of five increase the wait. After 30, your credential and recovery phrase are required.")
                    Text(if (state.hasRecovery) "Recovery is configured." else "Set recovery before you need it. Keep a private copy outside this phone.")
                    OutlinedTextField(prompt, { prompt = it.take(160) }, label = { Text("Your recovery prompt") })
                    VaultCredentialField(VaultCredentialKind.Password, secret, { secret = it }, "Recovery phrase (12+ characters)")
                    VaultCredentialField(VaultCredentialKind.Password, confirm, { confirm = it }, "Confirm recovery phrase")
                    Button(onClick = { model.configureRecovery(prompt, secret, confirm); secret = ""; confirm = "" }, enabled = !state.operationInProgress) { Text("Save recovery") }
                }
                2 -> {
                    Text("A separate PIN opens only the media you choose for the decoy view. Your other private media stays hidden.")
                    VaultCredentialField(VaultCredentialKind.Pin, secret, { secret = it }, "Decoy PIN")
                    VaultCredentialField(VaultCredentialKind.Pin, confirm, { confirm = it }, "Confirm decoy PIN")
                    Button(onClick = { model.configureDecoy(secret, confirm); secret = ""; confirm = "" }, enabled = !state.operationInProgress) { Text("Save decoy PIN") }
                    if (state.hasDecoyPin) TextButton(onClick = model::removeDecoy) { Text("Disable decoy access") }
                }
            }
            state.authMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
