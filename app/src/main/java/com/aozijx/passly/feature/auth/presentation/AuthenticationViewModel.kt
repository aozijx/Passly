package com.aozijx.passly.feature.auth.presentation

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner
) : ViewModel() {

    val methodAvailability = authenticationManager.methods

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState: StateFlow<AuthenticationUiState> = _uiState.asStateFlow()

    fun verifyWithBiometric() {
        resetInputState()
        authenticate(AuthenticationMethod.BIOMETRIC)
    }

    fun onAppPasswordChange(value: String) {
        _uiState.value.appPassword.wipe()
        _uiState.update {
            it.copy(
                appPassword = SecureString.fromString(value),
                verificationFailure = null
            )
        }
    }

    fun onRecoveryCodeChange(value: String) {
        _uiState.value.recoveryCode.wipe()
        _uiState.update {
            it.copy(
                recoveryCode = SecureString.fromString(value),
                verificationFailure = null
            )
        }
    }

    fun onNewAppPasswordChange(value: String) {
        _uiState.value.newAppPassword.wipe()
        _uiState.update {
            it.copy(
                newAppPassword = SecureString.fromString(value),
                setupFailure = null
            )
        }
    }

    fun onConfirmAppPasswordChange(value: String) {
        _uiState.value.confirmAppPassword.wipe()
        _uiState.update {
            it.copy(
                confirmAppPassword = SecureString.fromString(value),
                setupFailure = null
            )
        }
    }

    fun onInputExpanded(method: AuthenticationMethod, expanded: Boolean) {
        if (expanded) {
            _uiState.value.expandedMethod
                ?.takeUnless { it == method }
                ?.let(::clearCredential)
            _uiState.update {
                it.copy(expandedMethod = method, verificationFailure = null)
            }
        } else {
            clearCredential(method)
            _uiState.update {
                if (it.expandedMethod == method) {
                    it.copy(expandedMethod = null, verificationFailure = null)
                } else {
                    it
                }
            }
        }
    }

    fun verifyWithAppPassword() {
        authenticate(
            method = AuthenticationMethod.APP_PASSWORD,
            credential = _uiState.value.appPassword.toCharArray()
        )
    }

    fun unlockWithRecoveryCode() {
        authenticate(
            method = AuthenticationMethod.RECOVERY_CODE,
            credential = _uiState.value.recoveryCode.toCharArray(),
            purpose = AuthenticationPurpose.RECOVER_AUTH_METHODS
        )
    }

    fun recoverBiometric(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(
                methodProvisioner.rotateBiometricPolicy(invalidateOnEnrollment = true) is
                        AuthenticationResult.Success
            )
        }
    }

    fun onShowSetPasswordDialog() = _uiState.update {
        it.copy(showSetPasswordDialog = true, setupFailure = null)
    }

    fun onDismissSetPasswordDialog() {
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                newAppPassword = SecureString.EMPTY,
                confirmAppPassword = SecureString.EMPTY,
                setupFailure = null
            )
        }
    }

    fun bootstrapAppPassword() {
        val password = _uiState.value.newAppPassword.toCharArray()
        val confirm = _uiState.value.confirmAppPassword.toCharArray()

        if (password.isEmpty() || !password.contentEquals(confirm)) {
            MemoryCleaner.wipeCharArray(password)
            MemoryCleaner.wipeCharArray(confirm)
            return
        }

        _uiState.update {
            it.copy(isSettingAppPassword = true, setupFailure = null)
        }
        viewModelScope.launch {
            try {
                when (val result = methodProvisioner.setAppPassword(password)) {
                    is AuthenticationResult.Success -> {
                        _uiState.value.newAppPassword.wipe()
                        _uiState.value.confirmAppPassword.wipe()
                        resetInputState()
                        _uiState.update {
                            it.copy(
                                showSetPasswordDialog = false,
                                newAppPassword = SecureString.EMPTY,
                                confirmAppPassword = SecureString.EMPTY,
                                setupFailure = null
                            )
                        }
                    }

                    is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> {
                        _uiState.update { it.copy(setupFailure = result.failure) }
                    }
                }
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                _uiState.update { it.copy(isSettingAppPassword = false) }
            }
        }
    }

    fun clearVerificationFailure() {
        _uiState.update { it.copy(verificationFailure = null) }
    }

    private fun authenticate(
        method: AuthenticationMethod,
        credential: CharArray? = null,
        purpose: AuthenticationPurpose = AuthenticationPurpose.UNLOCK_VAULT
    ) {
        if (_uiState.value.activeMethod != null) {
            credential?.let(MemoryCleaner::wipeCharArray)
            return
        }
        _uiState.update {
            it.copy(activeMethod = method, verificationFailure = null)
        }
        viewModelScope.launch {
            try {
                when (
                    val result = authenticationManager.authenticate(
                        AuthenticationRequest(
                            purpose = purpose,
                            allowedMethods = setOf(method)
                        ),
                        credential
                    )
                ) {
                    is AuthenticationResult.Success -> resetInputState()
                    is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                verificationFailure = AuthenticationVerificationFailure(
                                    method = method,
                                    failure = result.failure
                                )
                            )
                        }
                    }
                }
            } finally {
                credential?.let(MemoryCleaner::wipeCharArray)
                _uiState.update { it.copy(activeMethod = null) }
            }
        }
    }

    private fun clearCredential(method: AuthenticationMethod) {
        when (method) {
            AuthenticationMethod.APP_PASSWORD -> {
                _uiState.value.appPassword.wipe()
                _uiState.update { it.copy(appPassword = SecureString.EMPTY) }
            }

            AuthenticationMethod.RECOVERY_CODE -> {
                _uiState.value.recoveryCode.wipe()
                _uiState.update { it.copy(recoveryCode = SecureString.EMPTY) }
            }

            AuthenticationMethod.BIOMETRIC -> Unit
        }
    }

    private fun resetInputState() {
        _uiState.value.appPassword.wipe()
        _uiState.value.recoveryCode.wipe()
        _uiState.update {
            it.copy(
                appPassword = SecureString.EMPTY,
                recoveryCode = SecureString.EMPTY,
                expandedMethod = null,
                verificationFailure = null
            )
        }
    }

    override fun onCleared() {
        _uiState.value.appPassword.wipe()
        _uiState.value.recoveryCode.wipe()
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
        super.onCleared()
    }
}
