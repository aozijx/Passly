package com.aozijx.passly.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.authentication.AuthFeedbackPresenter
import com.aozijx.passly.security.crypto.SecureString
import com.github.f4b6a3.uuid.UuidCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val feedback: AuthFeedbackPresenter
) : ViewModel() {

    val state: StateFlow<AuthenticationState> = authenticationManager.state
    val methodAvailability = authenticationManager.methods

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    fun verifyWithBiometric() {
        resetInputState()
        authenticate(AuthenticationMethod.BIOMETRIC)
    }

    fun onAppPasswordChange(value: String) {
        _uiState.value.appPassword.wipe()
        _uiState.update { it.copy(appPassword = SecureString.fromString(value)) }
    }

    fun onRecoveryCodeChange(value: String) {
        _uiState.value.recoveryCode.wipe()
        _uiState.update { it.copy(recoveryCode = SecureString.fromString(value)) }
    }

    fun onNewAppPasswordChange(value: String) {
        _uiState.value.newAppPassword.wipe()
        _uiState.update { it.copy(newAppPassword = SecureString.fromString(value)) }
    }

    fun onConfirmAppPasswordChange(value: String) {
        _uiState.value.confirmAppPassword.wipe()
        _uiState.update { it.copy(confirmAppPassword = SecureString.fromString(value)) }
    }

    fun onInputExpanded(method: AuthenticationMethod, expanded: Boolean) {
        if (expanded) {
            val previous = _uiState.value.expandedMethod
            if (previous != null && previous != method) clearCredential(previous)
            _uiState.update { it.copy(expandedMethod = method) }
        } else {
            clearCredential(method)
            _uiState.update {
                if (it.expandedMethod == method) it.copy(expandedMethod = null) else it
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
            credential = _uiState.value.recoveryCode.toCharArray()
        )
    }

    fun onShowSetPasswordDialog() = _uiState.update { it.copy(showSetPasswordDialog = true) }

    fun onDismissSetPasswordDialog() {
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                newAppPassword = SecureString.EMPTY,
                confirmAppPassword = SecureString.EMPTY
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

        resetInputState()
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                isSettingAppPassword = true,
                newAppPassword = SecureString.EMPTY,
                confirmAppPassword = SecureString.EMPTY
            )
        }
        viewModelScope.launch {
            try {
                val result = methodProvisioner.setAppPassword(password)
                feedback.present(result, UuidCreator.getTimeOrderedEpoch().toString())
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                _uiState.update {
                    it.copy(
                        showSetPasswordDialog = false,
                        isSettingAppPassword = false
                    )
                }
            }
        }
    }

    private fun authenticate(method: AuthenticationMethod, credential: CharArray? = null) {
        if (_uiState.value.activeMethod != null) {
            credential?.let(MemoryCleaner::wipeCharArray)
            return
        }
        val request = AuthenticationRequest(
            purpose = AuthenticationPurpose.UNLOCK_VAULT,
            allowedMethods = setOf(method)
        )
        _uiState.update { it.copy(activeMethod = method) }
        viewModelScope.launch {
            try {
                authenticationManager.authenticate(request, credential)
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
                expandedMethod = null
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
