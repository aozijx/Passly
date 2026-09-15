package com.aozijx.passly.presentation.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.SessionActivityReporter
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.app.database.DatabaseLifecycleGateway
import com.aozijx.passly.app.database.DatabaseLifecycleResult
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.presentation.feature.shell.AppShellEffect
import com.aozijx.passly.presentation.feature.shell.AppShellUiAction
import com.aozijx.passly.presentation.feature.shell.AppShellUiState
import com.aozijx.passly.presentation.feature.shell.AppShellMutation
import com.aozijx.passly.presentation.feature.shell.AppShellReducer
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
    private val authenticationManager: AuthenticationManager,
    private val sessionActivityReporter: SessionActivityReporter,
    private val databaseSessionFailureState: DatabaseSessionFailureState,
    private val databaseLifecycleGateway: DatabaseLifecycleGateway,
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
            AppShellUiAction.Lock -> lock(LockReason.USER)
            AppShellUiAction.ExitRecovery -> lock(LockReason.RECOVERY_EXIT)
            AppShellUiAction.UpdateInteraction -> sessionActivityReporter.onUserInteraction()
            AppShellUiAction.RetryDatabaseInitialization -> initializeDatabase()
        }
    }

    private fun lock(reason: LockReason) {
        viewModelScope.launch { authenticationManager.lock(reason) }
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            authenticationManager.state.collect { state ->
                val authorized = state is AuthenticationState.Authenticated
                val recoveryMode = state is AuthenticationState.RecoveryMode
                if (authorized) {
                    runDatabaseInitialization {
                        databaseLifecycleGateway.initialize()
                    }
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
                ::Pair,
            )
                .distinctUntilChanged()
                .collect { (appearance, interfacePrefs) ->
                    mutate(AppShellMutation.SettingsChanged(appearance, interfacePrefs))
                }
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            val result = runDatabaseInitialization {
                databaseLifecycleGateway.retry()
            }
            if (result !is DatabaseLifecycleResult.Failure) {
                databaseSessionFailureState.clearDatabaseFailure()
            }
        }
    }

    private suspend fun runDatabaseInitialization(
        block: suspend () -> DatabaseLifecycleResult,
    ): DatabaseLifecycleResult {
        mutate(AppShellMutation.DatabaseInitializationStarted(clearError = true))
        val result = block()
        val error = (result as? DatabaseLifecycleResult.Failure)?.cause
        mutate(AppShellMutation.DatabaseInitializationFinished(error))
        error?.let {
            emitEffect(
                AppShellEffect.ShowError(
                    "数据库错误: ${it.toUiMessage("数据库初始化失败")}",
                )
            )
        }
        return result
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
