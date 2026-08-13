package com.aozijx.passly.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.feature.auth.contract.AuthenticationIntent
import com.aozijx.passly.feature.auth.contract.AuthenticationUiState
import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.crypto.SecureString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var recoveryRevealTapCount = 0

    fun onIntent(intent: AuthenticationIntent) {
        when (intent) {
            AuthenticationIntent.BiometricClicked -> verifyWithBiometric()
            AuthenticationIntent.LockIconClicked -> revealRecoveryUnlock()
            AuthenticationIntent.BackPressed -> onBackPressed()
            is AuthenticationIntent.AppPasswordChanged -> onAppPasswordChange(intent.value)
            AuthenticationIntent.AppPasswordSubmitted -> verifyWithAppPassword()
            is AuthenticationIntent.RecoveryCodeChanged -> onRecoveryCodeChange(intent.value)
            AuthenticationIntent.RecoveryCodeSubmitted -> unlockWithRecoveryCode()
            is AuthenticationIntent.InputExpanded -> onInputExpanded(intent.method, intent.expanded)
            is AuthenticationIntent.NewAppPasswordChanged -> onNewAppPasswordChange(intent.value)
            is AuthenticationIntent.ConfirmAppPasswordChanged -> onConfirmAppPasswordChange(intent.value)
            AuthenticationIntent.SetPasswordClicked -> onShowSetPasswordDialog()
            AuthenticationIntent.SetPasswordConfirmed -> bootstrapAppPassword()
            AuthenticationIntent.DismissSetPasswordDialog -> onDismissSetPasswordDialog()
            AuthenticationIntent.ClearVerificationFailure -> clearVerificationFailure()
        }
    }

    private fun revealRecoveryUnlock() {
        val state = _uiState.value
        if (AuthenticationMethod.RECOVERY_CODE !in methodAvailability.value || state.recoveryUnlockVisible) return
        recoveryRevealTapCount += 1
        if (recoveryRevealTapCount >= RECOVERY_REVEAL_TAP_THRESHOLD) {
            recoveryRevealTapCount = 0
            mutate(AuthenticationMutation.RecoveryUnlockVisibilityChanged(true))
        }
    }

    private fun onBackPressed() {
        when (val expandedMethod = _uiState.value.expandedMethod) {
            AuthenticationMethod.APP_PASSWORD,
            AuthenticationMethod.RECOVERY_CODE -> onInputExpanded(expandedMethod, false)

            AuthenticationMethod.BIOMETRIC,
            null -> {
                recoveryRevealTapCount = 0
                mutate(AuthenticationMutation.RecoveryUnlockVisibilityChanged(false))
            }
        }
    }

    private fun verifyWithBiometric() {
        resetInputState()
        authenticate(AuthenticationMethod.BIOMETRIC)
    }

    private fun onAppPasswordChange(value: String) {
        _uiState.value.appPassword.wipe()
        mutate(AuthenticationMutation.AppPasswordChanged(SecureString.fromString(value)))
    }

    private fun onRecoveryCodeChange(value: String) {
        _uiState.value.recoveryCode.wipe()
        mutate(AuthenticationMutation.RecoveryCodeChanged(SecureString.fromString(value)))
    }

    private fun onNewAppPasswordChange(value: String) {
        _uiState.value.newAppPassword.wipe()
        mutate(AuthenticationMutation.NewAppPasswordChanged(SecureString.fromString(value)))
    }

    private fun onConfirmAppPasswordChange(value: String) {
        _uiState.value.confirmAppPassword.wipe()
        mutate(AuthenticationMutation.ConfirmAppPasswordChanged(SecureString.fromString(value)))
    }

    private fun onInputExpanded(method: AuthenticationMethod, expanded: Boolean) {
        if (expanded) {
            _uiState.value.expandedMethod
                ?.takeUnless { it == method }
                ?.let(::clearCredential)
            mutate(AuthenticationMutation.ExpandedMethodChanged(method))
        } else {
            clearCredential(method)
            if (_uiState.value.expandedMethod == method) {
                mutate(AuthenticationMutation.ExpandedMethodChanged(null))
            }
        }
    }

    private fun verifyWithAppPassword() {
        authenticate(
            method = AuthenticationMethod.APP_PASSWORD,
            credential = _uiState.value.appPassword.toCharArray()
        )
    }

    private fun unlockWithRecoveryCode() {
        authenticate(
            method = AuthenticationMethod.RECOVERY_CODE,
            credential = _uiState.value.recoveryCode.toCharArray(),
            purpose = AuthenticationPurpose.RECOVER_AUTH_METHODS
        )
    }

    private fun onShowSetPasswordDialog() =
        mutate(AuthenticationMutation.SetPasswordDialogVisibilityChanged(true))

    private fun onDismissSetPasswordDialog() {
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
        mutate(AuthenticationMutation.SetPasswordDialogVisibilityChanged(false))
    }

    private fun bootstrapAppPassword() {
        val password = _uiState.value.newAppPassword.toCharArray()
        val confirm = _uiState.value.confirmAppPassword.toCharArray()

        if (password.isEmpty() || !password.contentEquals(confirm)) {
            MemoryCleaner.wipeCharArray(password)
            MemoryCleaner.wipeCharArray(confirm)
            return
        }

        mutate(AuthenticationMutation.SetupStarted)
        viewModelScope.launch {
            try {
                when (val result = methodProvisioner.setAppPassword(password)) {
                    is AuthenticationResult.Success -> {
                        wipeSetupPasswords()
                        resetInputState()
                        mutate(AuthenticationMutation.SetupCompleted)
                    }

                    is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> {
                        mutate(AuthenticationMutation.SetupFailed(result.failure))
                    }
                }
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                mutate(AuthenticationMutation.SetupFinished)
            }
        }
    }

    private fun clearVerificationFailure() {
        mutate(AuthenticationMutation.VerificationFailureCleared)
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
        mutate(AuthenticationMutation.AuthenticationStarted(method))
        viewModelScope.launch {
            try {
                when (
                    val result = authenticationManager.authenticate(
                        AuthenticationRequest(
                            purpose = purpose,
                            allowedMethods = setOf(method)
                        ),
                        when (method) {
                            AuthenticationMethod.BIOMETRIC -> AuthInput.Interactive
                            AuthenticationMethod.APP_PASSWORD ->
                                AuthInput.AppPassword.from(requireNotNull(credential))
                            AuthenticationMethod.RECOVERY_CODE ->
                                AuthInput.RecoveryCode.from(requireNotNull(credential))
                        }
                    )
                ) {
                    is AuthenticationResult.Success -> resetInputState()
                    is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> {
                        mutate(AuthenticationMutation.AuthenticationFailed(method, result.failure))
                    }
                }
            } finally {
                credential?.let(MemoryCleaner::wipeCharArray)
                mutate(AuthenticationMutation.AuthenticationFinished)
            }
        }
    }

    private fun clearCredential(method: AuthenticationMethod) {
        when (method) {
            AuthenticationMethod.APP_PASSWORD -> {
                _uiState.value.appPassword.wipe()
                mutate(AuthenticationMutation.AppPasswordChanged(EmptySensitiveValue))
            }

            AuthenticationMethod.RECOVERY_CODE -> {
                _uiState.value.recoveryCode.wipe()
                mutate(AuthenticationMutation.RecoveryCodeChanged(EmptySensitiveValue))
            }

            AuthenticationMethod.BIOMETRIC -> Unit
        }
    }

    private fun resetInputState() {
        recoveryRevealTapCount = 0
        wipeUnlockInputs()
        mutate(AuthenticationMutation.UnlockInputsReset)
    }

    private fun mutate(mutation: AuthenticationMutation) {
        _uiState.value = AuthenticationReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        wipeUnlockInputs()
        wipeSetupPasswords()
        super.onCleared()
    }

    private fun wipeUnlockInputs() {
        _uiState.value.appPassword.wipe()
        _uiState.value.recoveryCode.wipe()
    }

    private fun wipeSetupPasswords() {
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
    }

    private companion object {
        const val RECOVERY_REVEAL_TAP_THRESHOLD = 12
    }
}
