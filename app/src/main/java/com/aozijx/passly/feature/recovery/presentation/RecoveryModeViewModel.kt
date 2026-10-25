package com.aozijx.passly.feature.recovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeEffect
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeUiAction
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeUiState
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeMutation
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeReducer
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.sensitive.OwnedChars
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryModeViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val methodProvisioner: AuthenticationMethodProvisioner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryModeUiState())
    val uiState: StateFlow<RecoveryModeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RecoveryModeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: RecoveryModeUiAction) {
        when (action) {
            RecoveryModeUiAction.SetPasswordClicked -> showSetPasswordDialog()
            is RecoveryModeUiAction.NewPasswordChanged -> updateNewPassword(action.value)
            is RecoveryModeUiAction.ConfirmPasswordChanged -> updateConfirmPassword(action.value)
            RecoveryModeUiAction.SubmitNewPassword -> submitNewPassword()
            RecoveryModeUiAction.ExitClicked -> exitRecovery()
            RecoveryModeUiAction.DismissPasswordDialog -> dismissPasswordDialog()
        }
    }

    private fun showSetPasswordDialog() {
        if (!ensureRecoveryMode()) return
        mutate(RecoveryModeMutation.PasswordDialogOpened)
    }

    private fun updateNewPassword(value: String) {
        val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
        setup.newPassword.wipe()
        mutate(RecoveryModeMutation.NewPasswordChanged(OwnedChars.fromString(value)))
    }

    private fun updateConfirmPassword(value: String) {
        val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
        setup.confirmPassword.wipe()
        mutate(RecoveryModeMutation.ConfirmPasswordChanged(OwnedChars.fromString(value)))
    }

    private fun submitNewPassword() {
        val state = _uiState.value
        if (!ensureRecoveryMode()) return
        val setup = state.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
        if (setup.phase == RecoveryPasswordSetupPhase.Saving) return
        val password = setup.newPassword.toCharArray()
        val confirm = setup.confirmPassword.toCharArray()

        if (password.isEmpty() || !password.contentEquals(confirm)) {
            MemoryCleaner.wipeCharArray(password)
            MemoryCleaner.wipeCharArray(confirm)
            mutate(
                RecoveryModeMutation.ValidationFailed(
                    RecoveryPasswordSetupFailure.PasswordMismatchOrEmpty,
                ),
            )
            return
        }

        mutate(RecoveryModeMutation.PasswordSetupStarted)
        viewModelScope.launch {
            try {
                when (val result = methodProvisioner.setAppPassword(password)) {
                    is AuthenticationResult.Success -> {
                        wipePasswords()
                        mutate(RecoveryModeMutation.PasswordSetupCompleted)
                        _effect.send(RecoveryModeEffect.PasswordResetCompleted)
                    }

                    is AuthenticationResult.Cancelled ->
                        mutate(RecoveryModeMutation.PasswordSetupStopped)

                    is AuthenticationResult.Failure ->
                        mutate(
                            RecoveryModeMutation.PasswordSetupFailed(
                                RecoveryPasswordSetupFailure.SetupFailed,
                            ),
                        )
                }
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
            }
        }
    }

    private fun exitRecovery() {
        viewModelScope.launch {
            _effect.send(RecoveryModeEffect.ExitRecovery)
        }
    }

    private fun dismissPasswordDialog() {
        wipePasswords()
        mutate(RecoveryModeMutation.PasswordDialogDismissed)
    }

    private fun ensureRecoveryMode(): Boolean {
        val recoveryMode = secureSessionAccessState.isRecoveryMode()
        if (recoveryMode) return true
        wipePasswords()
        mutate(RecoveryModeMutation.RecoveryModeRejected)
        return false
    }

    private fun mutate(mutation: RecoveryModeMutation) {
        _uiState.value = RecoveryModeReducer.reduce(_uiState.value, mutation)
    }

    private fun wipePasswords() {
        val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
        setup.newPassword.wipe()
        setup.confirmPassword.wipe()
    }

    override fun onCleared() {
        wipePasswords()
    }
}
