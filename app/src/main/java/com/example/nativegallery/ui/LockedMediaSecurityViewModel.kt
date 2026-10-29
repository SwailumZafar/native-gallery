package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.HiddenSecurityRepository
import com.example.nativegallery.data.PinFailureState
import com.example.nativegallery.data.VaultCredentialKind
import com.example.nativegallery.data.validVaultCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

@Immutable
internal data class LockedMediaSecurityUiState(
    val hasPin: Boolean,
    val isUnlocked: Boolean = false,
    val authMessage: String? = null,
    val operationInProgress: Boolean = false,
    val pinCreationEventId: Int = 0,
    val credentialKind: VaultCredentialKind = VaultCredentialKind.Pin,
    val isDecoy: Boolean = false,
    val requiresRecovery: Boolean = false,
    val hasRecovery: Boolean = false,
    val recoveryPrompt: String = "Enter your recovery phrase",
    val lockoutRemainingMillis: Long = 0L,
    val hasDecoyPin: Boolean = false,
    val deviceRecoveryVerified: Boolean = false
)

internal object LockedMediaSecurityPolicy {
    fun pinCreationError(pin: String, confirmPin: String): String? = when {
        pin != confirmPin -> "PINs do not match."
        !HiddenSecurityRepository.isValidPin(pin) -> "Use 4 to 16 digits for the PIN."
        else -> null
    }

    fun lockoutMessage(remainingMillis: Long): String {
        val seconds = ceil(remainingMillis.coerceAtLeast(1L) / 1000.0).toInt()
        return "Too many wrong PIN attempts. Try again in ${seconds}s."
    }
}

internal class LockedMediaSecurityViewModel(
    private val repository: HiddenSecurityRepository
) : ViewModel() {
    private var operationGeneration = 0
    private val _uiState = MutableStateFlow(
        LockedMediaSecurityUiState(hasPin = repository.hasPin(), credentialKind = repository.credentialKind(),
            isDecoy = repository.lastVaultWasDecoy(),
            requiresRecovery = repository.requiresRecovery(), hasRecovery = repository.hasRecovery(),
            recoveryPrompt = repository.recoveryPrompt(), hasDecoyPin = repository.hasDecoyPin())
    )
    val uiState: StateFlow<LockedMediaSecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val remaining = (repository.pinLockoutUntilMillis() - System.currentTimeMillis()).coerceAtLeast(0)
                _uiState.update { it.copy(lockoutRemainingMillis = remaining, requiresRecovery = repository.requiresRecovery(),
                    authMessage = if (it.lockoutRemainingMillis > 0 && remaining == 0L && it.authMessage?.startsWith("Too many wrong") == true) null else it.authMessage) }
                delay(1_000)
            }
        }
    }

    fun createPin(pin: String, confirmPin: String) {
        if (_uiState.value.operationInProgress) return
        val validationError = LockedMediaSecurityPolicy.pinCreationError(pin, confirmPin)
        if (validationError != null) {
            _uiState.update { it.copy(authMessage = validationError, operationInProgress = false) }
            return
        }
        val generation = ++operationGeneration
        _uiState.update { it.copy(authMessage = null, operationInProgress = true) }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.Default) { repository.setPin(pin) }
            if (generation != operationGeneration) {
                if (saved) _uiState.update { it.copy(hasPin = true) }
                return@launch
            }
            _uiState.update { current ->
                if (saved) {
                    current.copy(
                        hasPin = true,
                        isUnlocked = true,
                        authMessage = null,
                        operationInProgress = false,
                        pinCreationEventId = current.pinCreationEventId + 1
                    )
                } else {
                    current.copy(
                        authMessage = "Could not save this PIN.",
                        operationInProgress = false
                    )
                }
            }
        }
    }

    fun unlockWithPin(pin: String) {
        if (_uiState.value.operationInProgress) return
        val generation = ++operationGeneration
        _uiState.update { it.copy(authMessage = null, operationInProgress = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val remaining = repository.pinLockoutUntilMillis() - System.currentTimeMillis()
                when {
                    remaining > 0L -> PinUnlockResult.LockedOut(remaining)
                    repository.requiresRecovery() -> PinUnlockResult.RecoveryRequired
                    repository.verifyPin(pin) -> {
                        repository.clearFailedPinAttempts()
                        repository.rememberVaultView(false)
                        PinUnlockResult.Success
                    }
                    repository.verifyDecoyPin(pin) -> {
                        repository.rememberVaultView(true)
                        PinUnlockResult.Decoy
                    }
                    else -> PinUnlockResult.Failed(repository.recordFailedPinAttempt())
                }
            }
            if (generation != operationGeneration) return@launch
            _uiState.update { current ->
                when (result) {
                    PinUnlockResult.Success -> current.copy(
                        isUnlocked = true,
                        authMessage = null,
                        operationInProgress = false, isDecoy = false, requiresRecovery = false, lockoutRemainingMillis = 0
                    )
                    PinUnlockResult.Decoy -> current.copy(isUnlocked = true, isDecoy = true, authMessage = null,
                        operationInProgress = false, lockoutRemainingMillis = 0)
                    PinUnlockResult.RecoveryRequired -> current.copy(requiresRecovery = true,
                        authMessage = "Vault locked after 30 incorrect combinations. Verify your recovery phrase and credential.", operationInProgress = false)
                    is PinUnlockResult.LockedOut -> current.copy(
                        authMessage = LockedMediaSecurityPolicy.lockoutMessage(result.remainingMillis),
                        operationInProgress = false
                    )
                    is PinUnlockResult.Failed -> {
                        val remaining = result.failureState.remainingMillis()
                        current.copy(
                            authMessage = if (remaining > 0L) {
                                LockedMediaSecurityPolicy.lockoutMessage(remaining)
                            } else {
                                "Incorrect PIN."
                            },
                            operationInProgress = false, requiresRecovery = result.failureState.failedAttempts >= 30,
                            lockoutRemainingMillis = remaining
                        )
                    }
                }
            }
        }
    }

    fun unlockWithBiometric() {
        if (repository.requiresRecovery() || repository.pinLockoutUntilMillis() > System.currentTimeMillis()) {
            showMessage("Use vault recovery after lockdown, or wait for the lockout timer.")
            return
        }
        operationGeneration += 1
        _uiState.update {
            it.copy(isUnlocked = true, isDecoy = false, authMessage = null, operationInProgress = false)
        }
        viewModelScope.launch(Dispatchers.Default) { repository.clearFailedPinAttempts(); repository.rememberVaultView(false) }
    }

    fun lock(message: String? = null) {
        operationGeneration += 1
        _uiState.update {
            it.copy(isUnlocked = false, authMessage = message, operationInProgress = false, deviceRecoveryVerified = false)
        }
    }

    fun showMessage(message: String?) {
        _uiState.update { it.copy(authMessage = message, operationInProgress = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(authMessage = null) }
    }

    fun changeCredential(kind: VaultCredentialKind, secret: String, confirm: String) {
        if (!_uiState.value.isUnlocked || _uiState.value.isDecoy || _uiState.value.operationInProgress) return
        if (secret != confirm || !validVaultCredential(kind, secret)) {
            showMessage("Credentials must match. Use 4–16 PIN digits, an 8–128 character password, or 4–9 different pattern dots.")
            return
        }
        _uiState.update { it.copy(operationInProgress = true) }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.Default) { repository.setCredential(kind, secret) }
            _uiState.update { it.copy(credentialKind = repository.credentialKind(), operationInProgress = false,
                authMessage = if (saved) "Vault credential updated." else "Could not save the credential.") }
        }
    }

    fun configureRecovery(prompt: String, secret: String, confirm: String) {
        if (!_uiState.value.isUnlocked || _uiState.value.isDecoy || _uiState.value.operationInProgress) return
        if (secret != confirm || secret.length !in 12..128 || prompt.isBlank()) {
            showMessage("Use a recovery prompt and a matching recovery phrase of 12–128 characters."); return
        }
        _uiState.update { it.copy(operationInProgress = true) }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.Default) { repository.setRecovery(prompt, secret) }
            _uiState.update { it.copy(hasRecovery = repository.hasRecovery(), recoveryPrompt = repository.recoveryPrompt(),
                operationInProgress = false, authMessage = if (saved) "Recovery phrase saved." else "Could not save recovery.") }
        }
    }

    fun configureDecoy(pin: String, confirm: String) {
        if (!_uiState.value.isUnlocked || _uiState.value.isDecoy || _uiState.value.operationInProgress) return
        if (pin != confirm || !HiddenSecurityRepository.isValidPin(pin)) { showMessage("Use matching 4–16 digit decoy PINs."); return }
        _uiState.update { it.copy(operationInProgress = true) }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.Default) { repository.setDecoyPin(pin) }
            _uiState.update { it.copy(hasDecoyPin = repository.hasDecoyPin(), operationInProgress = false,
                authMessage = if (saved) "Decoy PIN saved. Select media and add it to the decoy view." else "Choose a decoy PIN different from your real credential.") }
        }
    }

    fun removeDecoy() {
        if (!_uiState.value.isUnlocked || _uiState.value.isDecoy) return
        repository.removeDecoyPin(); repository.clearDecoyMedia()
        _uiState.update { it.copy(hasDecoyPin = false, authMessage = "Decoy access disabled.") }
    }

    fun recover(secret: String, credential: String) {
        if (_uiState.value.operationInProgress) return
        val generation = ++operationGeneration
        _uiState.update { it.copy(operationInProgress = true, authMessage = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                if (repository.pinLockoutUntilMillis() > System.currentTimeMillis()) false
                else if ((if (repository.hasRecovery()) repository.verifyRecovery(secret) else _uiState.value.deviceRecoveryVerified) && repository.verifyPin(credential)) {
                    repository.clearFailedPinAttempts(); repository.rememberVaultView(false); true
                } else { repository.recordFailedPinAttempt(); false }
            }
            if (generation != operationGeneration) return@launch
            _uiState.update { it.copy(isUnlocked = result, isDecoy = false, operationInProgress = false,
                requiresRecovery = !result, authMessage = if (result) null else "Recovery did not match. Wait before trying again.") }
        }
    }

    /** Called only after Android's own device-credential confirmation, for legacy vaults without a recovery phrase. */
    fun recoverWithDeviceCredential() {
        if (repository.hasRecovery()) return
        _uiState.update { it.copy(deviceRecoveryVerified = true, operationInProgress = false,
            authMessage = "Phone verified. Enter your vault credential to complete recovery.") }
    }

    private sealed interface PinUnlockResult {
        data object Success : PinUnlockResult
        data object Decoy : PinUnlockResult
        data object RecoveryRequired : PinUnlockResult
        data class LockedOut(val remainingMillis: Long) : PinUnlockResult
        data class Failed(val failureState: PinFailureState) : PinUnlockResult
    }
}

internal class LockedMediaSecurityViewModelFactory(
    private val repository: HiddenSecurityRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LockedMediaSecurityViewModel::class.java))
        return LockedMediaSecurityViewModel(repository) as T
    }
}
