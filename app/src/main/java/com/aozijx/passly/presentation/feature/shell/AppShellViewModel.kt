package com.aozijx.passly.presentation.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.SessionActivityReporter
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.app.database.DatabaseLifecycleGateway
import com.aozijx.passly.app.database.DatabaseLifecycleResult
import com.aozijx.passly.domain.entry.port.SearchIndexMaintenance
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.presentation.feature.shell.AppShellAuthResult
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
    private val searchIndexMaintenance: SearchIndexMaintenance,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppShellEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // 认证回调只属于导航层，不能与 AppShell 的通知事件竞争消费。
    private val _authResults = Channel<AppShellAuthResult>(Channel.BUFFERED)
    val authResults = _authResults.receiveAsFlow()

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
            AppShellUiAction.RequestAuth -> requestAuth()
            AppShellUiAction.RequestReauth -> requestReauth()
            is AppShellUiAction.RequestSensitiveAccess -> requestSensitiveAccess(
                action.action,
                action.accessLevel
            )
        }
    }

    val isAuthorizedNow: Boolean
        get() = authenticationManager.state.value is AuthenticationState.Authenticated

    private fun requestAuth() {
        requestAuthentication(AuthenticationPurpose.UNLOCK_VAULT)
    }

    private fun requestReauth() {
        requestAuthentication(AuthenticationPurpose.REAUTHENTICATE)
    }

    private fun requestSensitiveAccess(
        action: SensitiveAccessAction,
        accessLevel: SensitiveAccessLevel
    ) {
        val purpose = when (action) {
            SensitiveAccessAction.COPY -> AuthenticationPurpose.COPY_SECRET
            SensitiveAccessAction.REVEAL -> when (accessLevel) {
                SensitiveAccessLevel.STANDARD -> AuthenticationPurpose.REVEAL_SECRET
                SensitiveAccessLevel.HIGH ->
                    AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET
            }
        }
        requestAuthentication(purpose)
    }

    private fun requestAuthentication(purpose: AuthenticationPurpose) {
        viewModelScope.launch {
            when (authenticationManager.authenticate(AuthenticationRequest(purpose))) {
                is AuthenticationResult.Success ->
                    _authResults.send(AppShellAuthResult.Success)

                is AuthenticationResult.Cancelled,
                is AuthenticationResult.Failure ->
                    _authResults.send(AppShellAuthResult.NotAuthorized)
            }
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
                    val result = runDatabaseInitialization {
                        databaseLifecycleGateway.initialize()
                    }
                    if (result !is DatabaseLifecycleResult.Failure) {
                        rebuildSearchIndex()
                    }

                    mutate(AppShellMutation.Authenticated)
                    emitEffect(AppShellEffect.NavigateToVault)
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

    /**
     * 重建搜索盲索引（首次解锁或降级回退时）。
     * 不阻塞用户操作 —— 异步执行，仅记录日志。
     */
    private fun rebuildSearchIndex() {
        viewModelScope.launch {
            val result = searchIndexMaintenance.rebuildIndex()
            result.onSuccess { count ->
                AppTelemetry.i("AppShellViewModel", "Blind index rebuild complete: $count entries")
            }.onFailure { error ->
                AppTelemetry.w("AppShellViewModel", "Blind index rebuild skipped")
            }
        }
    }
}
