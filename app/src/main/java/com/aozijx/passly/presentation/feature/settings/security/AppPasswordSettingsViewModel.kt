package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.security.AppPasswordChangeResult
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
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
            is AppPasswordSettingsAction.SubmitChange -> runChange(action.request)
        }
    }

    private fun runChange(request: AppPasswordChangeRequest) {
        viewModelScope.launch {
            when (val result = appPasswordSettings.execute(request)) {
                AppPasswordChangeResult.Completed -> _effects.trySend(
                    when (request) {
                        is AppPasswordChangeRequest.Set -> AppPasswordSettingsEffect.AppPasswordSet
                        is AppPasswordChangeRequest.Change -> AppPasswordSettingsEffect.AppPasswordChanged
                        AppPasswordChangeRequest.Disable -> AppPasswordSettingsEffect.AppPasswordDisabled
                    },
                )
                is AppPasswordChangeResult.InvalidInput -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordInputInvalid(result.reason),
                )
                is AppPasswordChangeResult.Failed -> _effects.trySend(
                    AppPasswordSettingsEffect.AppPasswordOperationFailed(result.failure),
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
