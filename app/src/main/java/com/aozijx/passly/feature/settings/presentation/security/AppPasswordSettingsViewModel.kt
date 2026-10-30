package com.aozijx.passly.feature.settings.presentation.security

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppPasswordSettingsViewModel @Inject constructor(
    private val appPasswordSettings: AppPasswordSettingsInteractor,
) : ViewModel() {

    private data class PageEffect(val owner: Any, val effect: AppPasswordSettingsEffect)

    private val _uiState = MutableStateFlow(AppPasswordSettingsUiState())
    val uiState: StateFlow<AppPasswordSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PageEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.effect }
    private var activePageOwner: Any? = null

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
        val owner = activePageOwner ?: return
        viewModelScope.launch {
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
    }

    private fun requestAppPasswordEntry() {
        val owner = activePageOwner ?: return
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
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }

    private fun emitEffect(owner: Any, effect: AppPasswordSettingsEffect) {
        if (activePageOwner === owner) _effects.trySend(PageEffect(owner, effect))
    }
}
