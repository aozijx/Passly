package com.aozijx.passly.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.auth.VerificationGateway
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.domain.usecase.auth.RecoveryCodeUseCases
import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.crypto.SecureString
import com.aozijx.passly.feature.verification.model.VerificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val gateway: VerificationGateway,
    private val recoveryCodeUseCases: RecoveryCodeUseCases
) : ViewModel() {

    val isAppPasswordEnabled: StateFlow<Boolean> = gateway.isAppPasswordEnabled

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val available = recoveryCodeUseCases.hasRecoveryCode()
            _uiState.update { it.copy(recoveryCodeAvailable = available) }
        }
    }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(appPassword = SecureString.fromString(value)) }

    fun onPasswordConfirmChange(value: String) =
        _uiState.update { it.copy(appPasswordConfirm = SecureString.fromString(value)) }

    fun onShowPasswordInput() = _uiState.update { it.copy(showPasswordInput = true) }

    fun onToggleRecoveryCodeInput() = _uiState.update {
        it.copy(showRecoveryCodeInput = !it.showRecoveryCodeInput)
    }

    fun onRecoveryCodeChange(value: String) {
        _uiState.value.recoveryCode.wipe()
        _uiState.update { it.copy(recoveryCode = SecureString.fromString(value)) }
    }

    fun unlockWithRecoveryCode() {
        val current = _uiState.value
        if (current.authInProgress) return
        val code = current.recoveryCode.toCharArray()
        if (code.isEmpty()) return
        _uiState.update { it.copy(authInProgress = true) }
        viewModelScope.launch {
            val result = recoveryCodeUseCases.unlock(code)
            when (result) {
                is AppResult.Success -> clearRecoveryCodeState()
                is AppResult.Failure -> {
                    _uiState.update { it.copy(authInProgress = false) }
                    AppMessageCenter.publish(result.error.toUiMessage())
                }
            }
        }
    }

    fun onShowSetPasswordDialog() = _uiState.update { it.copy(showSetPasswordDialog = true) }

    fun onDismissSetPasswordDialog() {
        val current = _uiState.value
        current.appPassword.wipe()
        current.appPasswordConfirm.wipe()
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                appPassword = SecureString.EMPTY,
                appPasswordConfirm = SecureString.EMPTY
            )
        }
    }

    fun verifyWithBiometric(launcher: BiometricPromptLauncher, title: String, subtitle: String) {
        if (_uiState.value.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        gateway.verifyWithBiometric(launcher, title, subtitle) { result ->
            _uiState.update { it.copy(authInProgress = false) }
        }
    }

    fun verifyWithAppPassword() {
        val current = _uiState.value
        if (current.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        gateway.verifyWithAppPassword(current.appPassword.toCharArray()) { result ->
            when (result) {
                is AppResult.Success ->
                    _uiState.update {
                        it.copy(
                            authInProgress = false,
                            appPassword = SecureString.EMPTY,
                            showPasswordInput = false
                        )
                    }
                is AppResult.Failure -> _uiState.update { it.copy(authInProgress = false) }
            }
        }
    }

    fun bootstrapAppPassword(onComplete: (Boolean) -> Unit) {
        if (_uiState.value.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        val password = _uiState.value.appPassword.toCharArray()
        gateway.bootstrapAppPassword(password) { result ->
            MemoryCleaner.wipeCharArray(password)
            _uiState.update { it.copy(authInProgress = false) }
            val success = result.isSuccess
            onComplete(success)
        }
    }

    override fun onCleared() {
        _uiState.value.appPassword.wipe()
        _uiState.value.appPasswordConfirm.wipe()
        _uiState.value.recoveryCode.wipe()
        super.onCleared()
    }

    private fun clearRecoveryCodeState() {
        _uiState.value.recoveryCode.wipe()
        _uiState.update {
            it.copy(
                authInProgress = false,
                recoveryCode = SecureString.EMPTY,
                showRecoveryCodeInput = false
            )
        }
    }
}
