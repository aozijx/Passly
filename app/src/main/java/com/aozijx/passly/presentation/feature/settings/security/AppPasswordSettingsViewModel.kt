package com.aozijx.passly.presentation.feature.settings.security

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
class AppPasswordSettingsViewModel @Inject constructor(
    private val authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPasswordSettingsUiState())
    val uiState: StateFlow<AppPasswordSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppPasswordSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeAuthenticationMethods()
    }

    fun onAction(action: AppPasswordSettingsAction) {
        when (action) {
            AppPasswordSettingsAction.RequestAppPasswordEntry -> requestAppPasswordEntry()
            is AppPasswordSettingsAction.SetAppPassword -> setAppPassword(action.password)
            is AppPasswordSettingsAction.ChangeAppPassword -> changeAppPassword(
                action.currentPassword,
                action.newPassword,
            )
            AppPasswordSettingsAction.DisableAppPassword -> disableAppPassword()
        }
    }

    private fun setAppPassword(password: CharArray) {
        runPrimaryAuthMethodChange(
            successEffect = AppPasswordSettingsEffect.AppPasswordSet,
            operation = { authenticationMethodProvisioner.setAppPassword(password) },
        )
    }

    private fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) {
        runPrimaryAuthMethodChange(
            successEffect = AppPasswordSettingsEffect.AppPasswordChanged,
            operation = {
                authenticationMethodProvisioner.changeAppPassword(currentPassword, newPassword)
            },
        )
    }

    private fun disableAppPassword() {
        runPrimaryAuthMethodChange(
            successEffect = AppPasswordSettingsEffect.AppPasswordDisabled,
            operation = { authenticationMethodProvisioner.disableAppPassword() },
        )
    }

    private fun runPrimaryAuthMethodChange(
        successEffect: AppPasswordSettingsEffect,
        operation: suspend () -> AuthenticationResult,
    ) {
        viewModelScope.launch {
            when (operation()) {
                is AuthenticationResult.Success -> _effects.trySend(successEffect)
                is AuthenticationResult.Failure -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordError("操作失败"),
                )
                is AuthenticationResult.Cancelled -> Unit
            }
        }
    }

    private fun requestAppPasswordEntry() {
        viewModelScope.launch {
            when (val result = authenticationMethodProvisioner.authorizeAppPasswordManagement()) {
                is AuthenticationResult.Success -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordEntryAuthorized(
                        alreadyEnabled = _uiState.value.isAppPasswordEnabled,
                    ),
                )
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordEntryAuthenticationFailed(result.failure),
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
