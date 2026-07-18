package com.aozijx.passly.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.crypto.SecureString
import com.aozijx.passly.feature.verification.model.VerificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner
) : ViewModel() {

    val isAppPasswordEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.appPassword }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authenticationManager.methods.collect { methods ->
                _uiState.update { it.copy(recoveryCodeAvailable = methods.recoveryCode) }
            }
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
        authenticate(AuthenticationMethod.RECOVERY_CODE)
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

    fun verifyWithBiometric() = authenticate(AuthenticationMethod.BIOMETRIC)

    fun verifyWithAppPassword() {
        authenticate(AuthenticationMethod.APP_PASSWORD)
    }

    fun bootstrapAppPassword(onComplete: (Boolean) -> Unit) {
        if (_uiState.value.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        val password = _uiState.value.appPassword.toCharArray()
        viewModelScope.launch {
            val result = methodProvisioner.setAppPassword(password)
            MemoryCleaner.wipeCharArray(password)
            _uiState.update { it.copy(authInProgress = false) }
            val success = result is AuthenticationResult.Success
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

    private fun authenticate(method: AuthenticationMethod) {
        if (_uiState.value.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        viewModelScope.launch {
            val result = authenticationManager.authenticate(
                AuthenticationRequest(
                    purpose = AuthenticationPurpose.UNLOCK_VAULT,
                    allowedMethods = setOf(method)
                )
            )
            _uiState.update { it.copy(authInProgress = false) }
            if (result is AuthenticationResult.Success && method == AuthenticationMethod.RECOVERY_CODE) {
                clearRecoveryCodeState()
            }
        }
    }
}
