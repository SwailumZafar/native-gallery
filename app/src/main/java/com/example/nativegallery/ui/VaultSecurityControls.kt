package com.example.nativegallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.BackHandler
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
    label: String, enabled: Boolean = true, onSubmit: (() -> Unit)? = null) {
    if (kind == VaultCredentialKind.Pattern) {
        val color = MaterialTheme.colorScheme.primary
        val muted = MaterialTheme.colorScheme.outlineVariant
        val update by rememberUpdatedState(onValueChange)
        val haptics = LocalHapticFeedback.current
        var pointer by remember { mutableStateOf<Offset?>(null) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Box(Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).testTag("vault-pattern-$label")
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    var gesturePattern = ""
                    fun dotAt(at: Offset): Int? {
                        val column = (at.x / (size.width / 3f) - 0.5f).roundToInt().coerceIn(0, 2)
                        val row = (at.y / (size.height / 3f) - 0.5f).roundToInt().coerceIn(0, 2)
                        val center = Offset((column + 0.5f) * size.width / 3, (row + 0.5f) * size.height / 3)
                        return if ((at - center).getDistance() < size.width / 7f) row * 3 + column + 1 else null
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val firstDot = dotAt(down.position) ?: return@awaitEachGesture
                        gesturePattern = extendVaultPattern("", firstDot)
                        update(gesturePattern)
                        pointer = down.position
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        down.consume()
                        var previous = down.position
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            // Interpolate fast swipes so the drawn path cannot skip a crossed dot.
                            val distance = (change.position - previous).getDistance()
                            val steps = (distance / (size.width / 24f)).toInt().coerceAtLeast(1)
                            val previousPattern = gesturePattern
                            for (step in 1..steps) {
                                val at = previous + (change.position - previous) * (step.toFloat() / steps)
                                dotAt(at)?.let { gesturePattern = extendVaultPattern(gesturePattern, it) }
                            }
                            update(gesturePattern)
                            if (gesturePattern != previousPattern) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pointer = change.position.takeIf { change.pressed }
                            previous = change.position
                            change.consume()
                        } while (event.changes.any { it.id == down.id && it.pressed })
                    }
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    fun center(dot: Char) = Offset(((dot.digitToInt() - 1) % 3 + 0.5f) * size.width / 3,
                        ((dot.digitToInt() - 1) / 3 + 0.5f) * size.height / 3)
                    value.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = 0.6f), center(a), center(b), 4.dp.toPx()) }
                    pointer?.let { at -> value.lastOrNull()?.let { drawLine(color.copy(alpha = 0.6f), center(it), at, 4.dp.toPx()) } }
                    (1..9).forEach { dot -> drawCircle(if (('0' + dot) in value) color else muted, 9.dp.toPx(), center('0' + dot)) }
                }
                Column(Modifier.fillMaxSize()) {
                    repeat(3) { row -> Row(Modifier.weight(1f)) {
                        repeat(3) { col ->
                            val dot = row * 3 + col + 1
                            Box(Modifier.weight(1f).fillMaxHeight().semantics {
                                contentDescription = "Pattern dot $dot"
                                if (enabled) onClick { onValueChange(extendVaultPattern(value, dot)); true }
                            })
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
        keyboardOptions = KeyboardOptions(keyboardType = if (kind == VaultCredentialKind.Pin) KeyboardType.NumberPassword else KeyboardType.Password,
            imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit?.invoke() }))
}

internal fun VaultCredentialKind.label(): String = if (this == VaultCredentialKind.Pin) "PIN" else name
internal fun VaultCredentialKind.requirements(): String = when (this) {
    VaultCredentialKind.Pin -> "Use 4–16 digits."
    VaultCredentialKind.Pattern -> "Connect at least 4 different dots."
    VaultCredentialKind.Password -> "Use 8–128 characters."
}

private enum class VaultSettingsPage { Home, Methods, Credential, ConfirmCredential, Recovery, ConfirmRecovery, Decoy, ConfirmDecoy, DisableDecoy }

@Composable
internal fun VaultSecurityDialog(state: LockedMediaSecurityUiState, model: LockedMediaSecurityViewModel, onDismiss: () -> Unit) =
    VaultSettingsSheet(state, model::changeCredential, model::configureRecovery, model::configureDecoy,
        model::removeDecoy, model::clearMessage, onDismiss)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultSettingsSheet(
    state: LockedMediaSecurityUiState,
    onCredential: (VaultCredentialKind, String, String) -> Unit,
    onRecovery: (String, String, String) -> Unit,
    onDecoy: (String, String) -> Unit,
    onRemoveDecoy: () -> Unit,
    onClearMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(VaultSettingsPage.Home) }
    var kind by remember { mutableStateOf(state.credentialKind) }
    var first by remember { mutableStateOf("") }
    var value by remember(page, kind) { mutableStateOf("") }
    val busy = state.operationInProgress
    val confirming = page in setOf(VaultSettingsPage.ConfirmCredential, VaultSettingsPage.ConfirmRecovery, VaultSettingsPage.ConfirmDecoy)
    val recovery = page in setOf(VaultSettingsPage.Recovery, VaultSettingsPage.ConfirmRecovery)
    val decoy = page in setOf(VaultSettingsPage.Decoy, VaultSettingsPage.ConfirmDecoy)
    val fieldKind = if (recovery) VaultCredentialKind.Password else if (decoy) VaultCredentialKind.Pin else kind
    val valid = if (recovery) value.length in 12..128 && value.isNotBlank()
        else com.example.nativegallery.data.validVaultCredential(fieldKind, value)
    fun navigate(next: VaultSettingsPage) { first = ""; value = ""; onClearMessage(); page = next }
    fun back() = navigate(when (page) {
        VaultSettingsPage.Credential -> VaultSettingsPage.Methods
        VaultSettingsPage.ConfirmCredential -> VaultSettingsPage.Credential
        VaultSettingsPage.ConfirmRecovery -> VaultSettingsPage.Recovery
        VaultSettingsPage.ConfirmDecoy -> VaultSettingsPage.Decoy
        else -> VaultSettingsPage.Home
    })
    LaunchedEffect(state.settingsSavedEventId) {
        if (state.settingsSavedEventId > 0) { first = ""; value = ""; page = VaultSettingsPage.Home }
    }
    BackHandler(page != VaultSettingsPage.Home && !busy) { back() }
    ModalBottomSheet(onDismissRequest = { if (page != VaultSettingsPage.Home && !busy) back() else onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.92f).testTag("vault-settings-sheet"),
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page != VaultSettingsPage.Home) IconButton(onClick = ::back, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to vault settings")
                }
                Text(when (page) {
                    VaultSettingsPage.Home -> "Vault settings"
                    VaultSettingsPage.Methods -> "Unlock method"
                    VaultSettingsPage.Credential, VaultSettingsPage.ConfirmCredential -> "${if (confirming) "Confirm" else "Set"} ${kind.label()}"
                    VaultSettingsPage.Recovery, VaultSettingsPage.ConfirmRecovery -> "${if (confirming) "Confirm" else "Set"} recovery phrase"
                    else -> "Decoy access"
                }, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                if (page == VaultSettingsPage.Home) TextButton(onClick = onDismiss) { Text("Done") }
            }
            when (page) {
                VaultSettingsPage.Home -> {
                    Text("Choose how you protect your private photos and videos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VaultSettingItem("Unlock method", state.credentialKind.label(), onClick = { navigate(VaultSettingsPage.Methods) })
                    VaultSettingItem("Recovery", if (state.hasRecovery) "Recovery phrase saved" else "Not configured", icon = Icons.Filled.Restore,
                        onClick = { navigate(VaultSettingsPage.Recovery) })
                    VaultSettingItem("Decoy access", if (state.hasDecoyPin) "Separate PIN enabled" else "Off", icon = Icons.Filled.VisibilityOff,
                        onClick = { navigate(VaultSettingsPage.Decoy) })
                    Text("Locked media locks when you leave Photo Lane.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VaultSettingsPage.Methods -> {
                    Text("Your saved method is shown when you unlock the vault.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VaultCredentialKind.entries.forEach { option ->
                        VaultSettingItem(option.label(), option.requirements(), selected = option == state.credentialKind) {
                            kind = option; navigate(VaultSettingsPage.Credential)
                        }
                    }
                }
                VaultSettingsPage.DisableDecoy -> {
                    Text("Disable the separate decoy PIN? Your media stays in Locked media.")
                    Button(onClick = onRemoveDecoy, enabled = !busy) { Text("Disable decoy access") }
                }
                else -> {
                    Text(if (confirming) "Enter it again to confirm." else when {
                        recovery -> "Use a phrase of 12–128 characters. Keep a private copy outside this phone. It is required with your vault credential after repeated incorrect attempts."
                        decoy -> "A separate PIN opens only the items you add to the decoy view. Your other private media stays hidden."
                        else -> fieldKind.requirements()
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val label = when {
                        recovery -> if (confirming) "Confirm recovery phrase" else "Recovery phrase"
                        decoy -> if (confirming) "Confirm decoy PIN" else "Decoy PIN"
                        else -> "${if (confirming) "Confirm" else "New"} ${kind.label()}"
                    }
                    fun submit() {
                        if (busy || !valid || (confirming && first != value)) return
                        if (!confirming) {
                            first = value
                            page = when (page) {
                                VaultSettingsPage.Credential -> VaultSettingsPage.ConfirmCredential
                                VaultSettingsPage.Recovery -> VaultSettingsPage.ConfirmRecovery
                                else -> VaultSettingsPage.ConfirmDecoy
                            }
                        } else when {
                            recovery -> onRecovery("Recovery phrase", first, value)
                            decoy -> onDecoy(first, value)
                            else -> onCredential(kind, first, value)
                        }
                    }
                    VaultCredentialField(fieldKind, value, { value = it }, label, !busy, ::submit)
                    if (confirming && value.isNotEmpty() && value != first) Text("Doesn’t match yet. Try again.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Button(onClick = ::submit, enabled = !busy && valid && (!confirming || first == value), modifier = Modifier.fillMaxWidth()) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(if (confirming) "Save" else "Continue")
                    }
                    if (page == VaultSettingsPage.Decoy && state.hasDecoyPin) TextButton(onClick = { navigate(VaultSettingsPage.DisableDecoy) }) { Text("Disable decoy access") }
                }
            }
            state.authMessage?.let { Text(it, color = if (page == VaultSettingsPage.Home) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VaultSettingItem(title: String, summary: String, selected: Boolean = false, icon: ImageVector = Icons.Filled.Lock, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (selected) Icons.Filled.Check else Icons.Filled.ChevronRight, if (selected) "Current method" else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
