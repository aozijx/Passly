package com.aozijx.passly.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiAction
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val methodProvisioner: AuthenticationMethodProvisioner
) : ViewModel() {

    private val _uiState = MutableStateFlow(BootstrapUiState())
    val uiState: StateFlow<BootstrapUiState> = _uiState.asStateFlow()

    fun onAction(action: BootstrapUiAction) {
        when (action) {
            is BootstrapUiAction.NewAppPasswordChanged -> onNewAppPasswordChange(action.value)
            is BootstrapUiAction.ConfirmAppPasswordChanged -> onConfirmAppPasswordChange(action.value)
            BootstrapUiAction.SetPasswordClicked -> onShowSetPasswordDialog()
            BootstrapUiAction.SetPasswordConfirmed -> bootstrapAppPassword()
            BootstrapUiAction.DismissSetPasswordDialog -> onDismissSetPasswordDialog()
        }
    }

    private fun onNewAppPasswordChange(value: String) {
        _uiState.value.newAppPassword.wipe()
        mutate(BootstrapMutation.NewAppPasswordChanged(OwnedChars.fromString(value)))
    }

    private fun onConfirmAppPasswordChange(value: String) {
        _uiState.value.confirmAppPassword.wipe()
        mutate(BootstrapMutation.ConfirmAppPasswordChanged(OwnedChars.fromString(value)))
    }

    private fun onShowSetPasswordDialog() =
        mutate(BootstrapMutation.SetPasswordDialogVisibilityChanged(true))

    private fun onDismissSetPasswordDialog() {
        wipeSetupPasswords()
        mutate(BootstrapMutation.SetPasswordDialogVisibilityChanged(false))
    }

    private fun bootstrapAppPassword() {
        val password = _uiState.value.newAppPassword.toCharArray()
        val confirm = _uiState.value.confirmAppPassword.toCharArray()

        if (password.isEmpty() || !password.contentEquals(confirm)) {
            MemoryCleaner.wipeCharArray(password)
            MemoryCleaner.wipeCharArray(confirm)
            return
        }

        mutate(BootstrapMutation.SetupStarted)
        viewModelScope.launch {
            try {
                when (val result = methodProvisioner.setAppPassword(password)) {
                    is AuthenticationResult.Success -> {
                        wipeSetupPasswords()
                        mutate(BootstrapMutation.SetupCompleted)
                    }

                    is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> {
                        mutate(BootstrapMutation.SetupFailed(result.failure))
                    }
                }
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                mutate(BootstrapMutation.SetupFinished)
            }
        }
    }

    private fun mutate(mutation: BootstrapMutation) {
        _uiState.value = BootstrapReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        wipeSetupPasswords()
        super.onCleared()
    }

    private fun wipeSetupPasswords() {
        _uiState.value.newAppPassword.wipe()
        _uiState.value.confirmAppPassword.wipe()
    }
}
