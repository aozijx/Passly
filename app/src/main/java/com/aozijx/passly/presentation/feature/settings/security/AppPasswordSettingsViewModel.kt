package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.security.AppPasswordChangeResult
import com.aozijx.passly.feature.settings.security.AppPasswordManagementAccess
import com.aozijx.passly.feature.settings.security.AppPasswordSettingsInteractor
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
    private val appPasswordSettings: AppPasswordSettingsInteractor,
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
            is AppPasswordSettingsAction.SetAppPassword -> runChange(
                successEffect = AppPasswordSettingsEffect.AppPasswordSet,
            ) { appPasswordSettings.set(action.password) }
            is AppPasswordSettingsAction.ChangeAppPassword -> runChange(
                successEffect = AppPasswordSettingsEffect.AppPasswordChanged,
            ) {
                appPasswordSettings.change(action.currentPassword, action.newPassword)
            }
            AppPasswordSettingsAction.DisableAppPassword -> runChange(
                successEffect = AppPasswordSettingsEffect.AppPasswordDisabled,
                operation = appPasswordSettings::disable,
            )
        }
    }

    private fun runChange(
        successEffect: AppPasswordSettingsEffect,
        operation: suspend () -> AppPasswordChangeResult,
    ) {
        viewModelScope.launch {
            when (operation()) {
                AppPasswordChangeResult.Completed -> _effects.trySend(successEffect)
                is AppPasswordChangeResult.Failed -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordError("操作失败"),
                )
                AppPasswordChangeResult.Cancelled -> Unit
            }
        }
    }

    private fun requestAppPasswordEntry() {
        viewModelScope.launch {
            when (val result = appPasswordSettings.authorizeManagement()) {
                is AppPasswordManagementAccess.Authorized -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordEntryAuthorized(
                        alreadyEnabled = result.alreadyEnabled,
                    ),
                )
                AppPasswordManagementAccess.Cancelled -> Unit
                is AppPasswordManagementAccess.Failed -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordEntryAuthenticationFailed(result.failure),
                )
            }
        }
    }

    private fun observeAuthenticationMethods() {
        viewModelScope.launch {
            appPasswordSettings.isEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(isAppPasswordEnabled = enabled)
                }
            }
        }
    }
}
