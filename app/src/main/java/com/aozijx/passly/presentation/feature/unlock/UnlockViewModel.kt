package com.aozijx.passly.presentation.feature.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.unlock.UnlockUiAction
import com.aozijx.passly.presentation.feature.unlock.UnlockUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager
) : ViewModel() {

    val methodAvailability = authenticationManager.methods

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var recoveryRevealTapCount = 0

    fun onAction(action: UnlockUiAction) {
        when (action) {
            UnlockUiAction.BiometricClicked -> verifyWithBiometric()
            UnlockUiAction.LockIconClicked -> revealRecoveryUnlock()
            UnlockUiAction.BackPressed -> onBackPressed()
            is UnlockUiAction.AppPasswordChanged -> onAppPasswordChange(action.value)
            UnlockUiAction.AppPasswordSubmitted -> verifyWithAppPassword()
            is UnlockUiAction.RecoveryCodeChanged -> onRecoveryCodeChange(action.value)
            UnlockUiAction.RecoveryCodeSubmitted -> unlockWithRecoveryCode()
            is UnlockUiAction.InputExpanded -> onInputExpanded(action.method, action.expanded)
            UnlockUiAction.ClearVerificationFailure -> clearVerificationFailure()
        }
    }

    private fun revealRecoveryUnlock() {
        val state = _uiState.value
        if (AuthenticationMethod.RECOVERY_CODE !in methodAvailability.value || state.recoveryUnlockVisible) return
        recoveryRevealTapCount += 1
        if (recoveryRevealTapCount >= RECOVERY_REVEAL_TAP_THRESHOLD) {
            recoveryRevealTapCount = 0
            mutate(UnlockMutation.RecoveryUnlockVisibilityChanged(true))
        }
    }

    private fun onBackPressed() {
        when (val expandedMethod = _uiState.value.expandedMethod) {
            AuthenticationMethod.APP_PASSWORD,
            AuthenticationMethod.RECOVERY_CODE -> onInputExpanded(expandedMethod, false)

            AuthenticationMethod.BIOMETRIC,
            null -> {
                recoveryRevealTapCount = 0
                mutate(UnlockMutation.RecoveryUnlockVisibilityChanged(false))
            }
        }
    }

    private fun verifyWithBiometric() {
        resetInputState()
        authenticate(AuthenticationMethod.BIOMETRIC)
    }

    private fun onAppPasswordChange(value: String) {
        _uiState.value.appPassword.wipe()
        mutate(UnlockMutation.AppPasswordChanged(OwnedChars.fromString(value)))
    }

    private fun onRecoveryCodeChange(value: String) {
        _uiState.value.recoveryCode.wipe()
        mutate(UnlockMutation.RecoveryCodeChanged(OwnedChars.fromString(value)))
    }

    private fun onInputExpanded(method: AuthenticationMethod, expanded: Boolean) {
        if (expanded) {
            _uiState.value.expandedMethod
                ?.takeUnless { it == method }
                ?.let(::clearCredential)
            mutate(UnlockMutation.ExpandedMethodChanged(method))
        } else {
            clearCredential(method)
            if (_uiState.value.expandedMethod == method) {
                mutate(UnlockMutation.ExpandedMethodChanged(null))
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

    private fun clearVerificationFailure() {
        mutate(UnlockMutation.VerificationFailureCleared)
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
        mutate(UnlockMutation.AuthenticationStarted(method))
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
                        mutate(UnlockMutation.AuthenticationFailed(method, result.failure))
                    }
                }
            } finally {
                credential?.let(MemoryCleaner::wipeCharArray)
                mutate(UnlockMutation.AuthenticationFinished)
            }
        }
    }

    private fun clearCredential(method: AuthenticationMethod) {
        when (method) {
            AuthenticationMethod.APP_PASSWORD -> {
                _uiState.value.appPassword.wipe()
                mutate(UnlockMutation.AppPasswordChanged(EmptySensitiveValue))
            }

            AuthenticationMethod.RECOVERY_CODE -> {
                _uiState.value.recoveryCode.wipe()
                mutate(UnlockMutation.RecoveryCodeChanged(EmptySensitiveValue))
            }

            AuthenticationMethod.BIOMETRIC -> Unit
        }
    }

    private fun resetInputState() {
        recoveryRevealTapCount = 0
        wipeUnlockInputs()
        mutate(UnlockMutation.UnlockInputsReset)
    }

    private fun mutate(mutation: UnlockMutation) {
        _uiState.value = UnlockReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        wipeUnlockInputs()
        super.onCleared()
    }

    private fun wipeUnlockInputs() {
        _uiState.value.appPassword.wipe()
        _uiState.value.recoveryCode.wipe()
    }

    private companion object {
        const val RECOVERY_REVEAL_TAP_THRESHOLD = 12
    }
}
