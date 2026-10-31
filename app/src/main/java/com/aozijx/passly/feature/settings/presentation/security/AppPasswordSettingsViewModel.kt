package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.security.AppPasswordChangeResult
import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
import com.aozijx.passly.feature.settings.security.AppPasswordManagementAccess
import com.aozijx.passly.feature.settings.security.AppPasswordSettingsInteractor
import com.aozijx.passly.feature.settings.security.clearSensitiveInput
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppPasswordSettingsViewModel @Inject constructor(
    private val appPasswordSettings: AppPasswordSettingsInteractor,
) : ViewModel() {
    private val pageEffects = PageEffects<AppPasswordSettingsEffect>()

    private val _uiState = MutableStateFlow(AppPasswordSettingsUiState())
    val uiState: StateFlow<AppPasswordSettingsUiState> = _uiState.asStateFlow()

    val effects = pageEffects.effects

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
        val owner = pageEffects.currentOwner
        if (owner == null || _uiState.value.isOperationInProgress) {
            request.clearSensitiveInput()
            return
        }
        _uiState.update { it.copy(isOperationInProgress = true) }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            when (val result = appPasswordSettings.execute(request)) {
                AppPasswordChangeResult.Completed -> emitEffect(owner,
                    when (request) {
                        is AppPasswordChangeRequest.Set -> AppPasswordSettingsEffect.AppPasswordSet
                        is AppPasswordChangeRequest.Change -> AppPasswordSettingsEffect.AppPasswordChanged
                        AppPasswordChangeRequest.Disable -> AppPasswordSettingsEffect.AppPasswordDisabled
                    },
                )
                is AppPasswordChangeResult.InvalidInput -> emitEffect(owner,
                    AppPasswordSettingsEffect.AppPasswordInputInvalid(result.reason),
                )
                is AppPasswordChangeResult.Failed -> emitEffect(owner,
                    AppPasswordSettingsEffect.AppPasswordOperationFailed(result.failure),
                )
                AppPasswordChangeResult.Cancelled -> Unit
            }
        }
        job.invokeOnCompletion {
            request.clearSensitiveInput()
            _uiState.update { it.copy(isOperationInProgress = false) }
        }
        job.start()
    }

    private fun requestAppPasswordEntry() {
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            when (val result = appPasswordSettings.authorizeManagement()) {
                is AppPasswordManagementAccess.Authorized -> emitEffect(owner,
                    AppPasswordSettingsEffect.AppPasswordEntryAuthorized(
                        alreadyEnabled = result.alreadyEnabled,
                    ),
                )
                AppPasswordManagementAccess.Cancelled -> Unit
                is AppPasswordManagementAccess.Failed -> emitEffect(owner,
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

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }

    private fun emitEffect(owner: Any, effect: AppPasswordSettingsEffect) {
        pageEffects.tryEmit(owner, effect)
    }
}
