package com.aozijx.passly.presentation.feature.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeAuthenticationMethods()
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            SettingsUiAction.RequestAppPasswordEntry -> requestAppPasswordEntry()
            is SettingsUiAction.SetAppPassword -> setAppPassword(action.password)
            is SettingsUiAction.ChangeAppPassword -> changeAppPassword(
                action.currentPassword,
                action.newPassword,
            )
            SettingsUiAction.DisableAppPassword -> disableAppPassword()
        }
    }

    private fun setAppPassword(password: CharArray) {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordSet,
            operation = { authenticationMethodProvisioner.setAppPassword(password) },
        )
    }

    private fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordChanged,
            operation = {
                authenticationMethodProvisioner.changeAppPassword(currentPassword, newPassword)
            },
        )
    }

    private fun disableAppPassword() {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordDisabled,
            operation = { authenticationMethodProvisioner.disableAppPassword() },
        )
    }

    private fun runPrimaryAuthMethodChange(
        successEffect: SettingsEffect,
        operation: suspend () -> AuthenticationResult,
    ) {
        viewModelScope.launch {
            when (operation()) {
                is AuthenticationResult.Success -> _effects.trySend(successEffect)
                is AuthenticationResult.Failure -> _effects.trySend(
                    SettingsEffect.AppPasswordError("操作失败"),
                )
                is AuthenticationResult.Cancelled -> Unit
            }
        }
    }

    private fun requestAppPasswordEntry() {
        viewModelScope.launch {
            when (val result = authenticationMethodProvisioner.authorizeAppPasswordManagement()) {
                is AuthenticationResult.Success -> _effects.trySend(
                    SettingsEffect.AppPasswordEntryAuthorized(
                        alreadyEnabled = _uiState.value.isAppPasswordEnabled,
                    ),
                )
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> _effects.trySend(
                    SettingsEffect.AppPasswordEntryAuthenticationFailed(result.failure),
                )
            }
        }
    }

    private fun observeAuthenticationMethods() {
        viewModelScope.launch {
            authenticationMethodAvailability.methods.collect { methods ->
                _uiState.update {
                    it.copy(isAppPasswordEnabled = AuthenticationMethod.APP_PASSWORD in methods)
                }
            }
        }
    }
}
