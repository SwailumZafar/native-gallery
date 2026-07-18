package com.example.nativegallery.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.data.HiddenSecurityRepository
import com.example.nativegallery.data.PinFailureState
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
    val pinCreationEventId: Int = 0
)

internal object LockedMediaSecurityPolicy {
    fun pinCreationError(pin: String, confirmPin: String): String? = when {
        pin != confirmPin -> "PINs do not match."
        !HiddenSecurityRepository.isValidPin(pin) -> "Use 4 to 12 digits for the PIN."
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
        LockedMediaSecurityUiState(hasPin = repository.hasPin())
    )
    val uiState: StateFlow<LockedMediaSecurityUiState> = _uiState.asStateFlow()

    fun createPin(pin: String, confirmPin: String) {
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
        val generation = ++operationGeneration
        _uiState.update { it.copy(authMessage = null, operationInProgress = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val remaining = repository.pinLockoutUntilMillis() - System.currentTimeMillis()
                when {
                    remaining > 0L -> PinUnlockResult.LockedOut(remaining)
                    repository.verifyPin(pin) -> {
                        repository.clearFailedPinAttempts()
                        PinUnlockResult.Success
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
                        operationInProgress = false
                    )
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
                            operationInProgress = false
                        )
                    }
                }
            }
        }
    }

    fun unlockWithBiometric() {
        operationGeneration += 1
        _uiState.update {
            it.copy(isUnlocked = true, authMessage = null, operationInProgress = false)
        }
        viewModelScope.launch(Dispatchers.Default) { repository.clearFailedPinAttempts() }
    }

    fun lock(message: String? = null) {
        operationGeneration += 1
        _uiState.update {
            it.copy(isUnlocked = false, authMessage = message, operationInProgress = false)
        }
    }

    fun showMessage(message: String?) {
        _uiState.update { it.copy(authMessage = message, operationInProgress = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(authMessage = null) }
    }

    private sealed interface PinUnlockResult {
        data object Success : PinUnlockResult
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
