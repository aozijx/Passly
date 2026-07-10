package com.aozijx.passly.ui.features.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.security.crypto.MemoryCleaner
import com.aozijx.passly.security.crypto.SecureString
import com.aozijx.passly.ui.features.verification.contract.VerificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    authUseCases: AuthUseCases
) : ViewModel() {

    private val gateway = VerificationGatewayImpl(viewModelScope, authUseCases)

    val isAppPasswordEnabled: StateFlow<Boolean> = gateway.isAppPasswordEnabled

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(appPassword = SecureString.fromString(value)) }

    fun onPasswordConfirmChange(value: String) =
        _uiState.update { it.copy(appPasswordConfirm = SecureString.fromString(value)) }

    fun onShowPasswordInput() = _uiState.update { it.copy(showPasswordInput = true) }

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
            if (result is AppResult.Failure) {
                _errorEvent.tryEmit(result.error.toUiMessage())
            }
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
                is AppResult.Failure -> {
                    _uiState.update { it.copy(authInProgress = false) }
                    _errorEvent.tryEmit(result.error.toUiMessage())
                }
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
            if (!success) {
                _errorEvent.tryEmit(
                    (result as? AppResult.Failure)?.error?.toUiMessage() ?: ""
                )
            }
            onComplete(success)
        }
    }
}