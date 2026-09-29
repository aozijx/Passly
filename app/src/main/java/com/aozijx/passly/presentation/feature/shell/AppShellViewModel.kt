package com.aozijx.passly.presentation.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.DatabaseSessionRecovery
import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import com.aozijx.passly.presentation.shared.error.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val appearanceSettingsRepository: AppearanceSettingsRepository,
    private val interfaceSettingsRepository: InterfaceSettingsRepository,
    private val securitySettingsSource: SecuritySettingsSource,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val sessionLockController: SessionLockController,
    private val databaseSessionFailureState: DatabaseSessionFailureState,
    private val databaseSessionRecovery: DatabaseSessionRecovery,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppShellEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()


    init {
        observeSettings()
        observeAuthStates()
        observeDatabaseFailures()
    }

    fun onAction(action: AppShellUiAction) {
        when (action) {
            AppShellUiAction.ExitRecovery -> lock(LockReason.RECOVERY_EXIT)
            AppShellUiAction.RetryDatabaseSession -> retryDatabaseSession()
        }
    }

    private fun lock(reason: LockReason) {
        viewModelScope.launch { sessionLockController.lock(reason) }
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            secureSessionAccessState.authenticationState.collect { state ->
                val authorized = state is AuthenticationState.Authenticated
                val recoveryMode = state is AuthenticationState.RecoveryMode
                if (authorized) {
                    mutate(AppShellMutation.Authenticated)
                } else if (recoveryMode) {
                    mutate(AppShellMutation.RecoveryModeEntered)
                } else {
                    mutate(AppShellMutation.SessionLocked)
                }
            }
        }

    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                appearanceSettingsRepository.appearance,
                interfaceSettingsRepository.interfaceSettings,
                securitySettingsSource.security,
            ) { appearance, interfaceSettings, securitySettings ->
                AppShellMutation.SettingsChanged(
                    appearance = appearance,
                    interfaceSettings = interfaceSettings,
                    securitySettings = securitySettings,
                )
            }
                .distinctUntilChanged()
                .collect(::mutate)
        }
    }

    private fun retryDatabaseSession() {
        viewModelScope.launch {
            mutate(AppShellMutation.DatabaseRetryStarted)
            when (val result = databaseSessionRecovery.retry()) {
                DatabaseSessionRetryResult.Ready -> Unit
                DatabaseSessionRetryResult.Unavailable ->
                    mutate(AppShellMutation.DatabaseRetryFinished(error = null))

                is DatabaseSessionRetryResult.Failed -> {
                    mutate(AppShellMutation.DatabaseRetryFinished(result.cause))
                    emitEffect(
                        AppShellEffect.ShowError(
                            "数据库错误: ${result.cause.toUiMessage("数据库重试失败")}",
                        ),
                    )
                }
            }
        }
    }

    private fun observeDatabaseFailures() {
        viewModelScope.launch {
            databaseSessionFailureState.databaseFailure.collect { error ->
                if (error != null) {
                    mutate(AppShellMutation.DatabaseFailureObserved(error))
                }
            }
        }
    }

    private fun emitEffect(effect: AppShellEffect) {
        _effects.trySend(effect)
    }

    private fun mutate(mutation: AppShellMutation) {
        _uiState.value = AppShellReducer.reduce(_uiState.value, mutation)
    }

}
