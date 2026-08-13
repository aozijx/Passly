package com.aozijx.passly.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.app.database.DatabaseInitOutcome
import com.aozijx.passly.app.database.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.entry.port.SearchIndexMaintenance
import com.aozijx.passly.data.settings.port.AppSettingsRepository
import com.aozijx.passly.app.shell.contract.AppShellEffect
import com.aozijx.passly.app.shell.contract.AppShellIntent
import com.aozijx.passly.app.shell.contract.AppShellUiState
import com.aozijx.passly.app.shell.presentation.AppShellMutation
import com.aozijx.passly.app.shell.presentation.AppShellReducer
import com.aozijx.passly.security.authentication.VaultSessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val authenticationManager: AuthenticationManager,
    private val sessionController: VaultSessionController,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases,
    private val searchIndexMaintenance: SearchIndexMaintenance,
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

    fun handleIntent(intent: AppShellIntent) {
        when (intent) {
            AppShellIntent.Lock -> lock(LockReason.USER)
            AppShellIntent.ExitRecovery -> lock(LockReason.RECOVERY_EXIT)
            AppShellIntent.UpdateInteraction -> sessionController.onUserInteraction()
            AppShellIntent.RetryDatabaseInitialization -> initializeDatabase()
            AppShellIntent.RecoverDatabase -> recoverDatabase()
            AppShellIntent.RequestAuth -> requestAuth()
            AppShellIntent.RequestReauth -> requestReauth()
            is AppShellIntent.RequestSensitiveAccess -> requestSensitiveAccess(
                intent.action,
                intent.accessLevel
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
            val result = authenticationManager.authenticate(AuthenticationRequest(purpose))
            if (result is AuthenticationResult.Success) {
                emitEffect(AppShellEffect.AuthSuccess)
            } else if (result is AuthenticationResult.Failure) {
                emitEffect(AppShellEffect.AuthError("认证失败"))
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
                    val outcome = runDatabaseInitialization {
                        databaseLifecycleUseCases.preWarmAndReport()
                    }
                    if (outcome.success) {
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
            settingsRepository.settings
                .map { settings -> settings.appearance to settings.interfacePrefs }
                .distinctUntilChanged()
                .collect { (appearance, interfacePrefs) ->
                    mutate(AppShellMutation.SettingsChanged(appearance, interfacePrefs))
                }
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            val outcome = runDatabaseInitialization {
                databaseLifecycleUseCases.retryAndReport()
            }
            if (outcome.success) sessionController.clearDatabaseFailure()
        }
    }

    private suspend fun runDatabaseInitialization(
        block: suspend () -> DatabaseInitOutcome
    ): DatabaseInitOutcome {
        mutate(AppShellMutation.DatabaseInitializationStarted(clearError = true))
        val outcome = block()
        mutate(AppShellMutation.DatabaseInitializationFinished(outcome.error))
        outcome.error?.let { error ->
            emitEffect(
                AppShellEffect.ShowError(
                    "数据库错误: ${error.toUiMessage("数据库初始化失败")}"
                )
            )
        }
        return outcome
    }

    private fun observeDatabaseFailures() {
        viewModelScope.launch {
            sessionController.databaseFailure.collect { error ->
                if (error != null) {
                    mutate(AppShellMutation.DatabaseFailureObserved(error))
                }
            }
        }
    }

    private fun recoverDatabase() {
        viewModelScope.launch {
            mutate(AppShellMutation.DatabaseInitializationStarted(clearError = false))
            val request = AuthenticationRequest(AuthenticationPurpose.RECOVER_DATABASE)
            when (
                authenticationManager.authenticate(request)
            ) {
                is AuthenticationResult.Success -> {
                    val outcome = databaseLifecycleUseCases.quarantineAndReinitialize()
                    val sessionRecovered = outcome.success &&
                        authenticationManager.completeDatabaseRecovery()
                    val recoveryError = outcome.error ?: if (!sessionRecovered) {
                        IllegalStateException("Recovered database session could not be activated")
                    } else {
                        null
                    }
                    mutate(AppShellMutation.DatabaseInitializationFinished(recoveryError))
                    if (sessionRecovered) {
                        val recoveryMessage = outcome.recoveryId?.let {
                            "故障库已保留（恢复编号：$it）。可在设置 → 数据管理 → 数据库恢复中查看"
                        } ?: "已创建新数据库"
                        emitEffect(AppShellEffect.ShowToast(recoveryMessage))
                        rebuildSearchIndex()
                    } else {
                        authenticationManager.lock(LockReason.INTEGRITY_FAILURE)
                        emitEffect(
                            AppShellEffect.ShowError(
                                outcome.error?.toUiMessage("创建新数据库失败")
                                    ?: "创建新数据库失败"
                            )
                        )
                    }
                }

                is AuthenticationResult.Cancelled ->
                    mutate(AppShellMutation.DatabaseInitializationStopped)
                is AuthenticationResult.Failure -> {
                    mutate(AppShellMutation.DatabaseInitializationStopped)
                    emitEffect(AppShellEffect.ShowError("身份验证失败，未创建新数据库"))
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
