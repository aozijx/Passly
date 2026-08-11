package com.aozijx.passly.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.domain.authentication.SensitiveAccessLevel
import com.aozijx.passly.domain.diagnostics.usecase.DatabaseInitOutcome
import com.aozijx.passly.domain.diagnostics.usecase.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.entry.repository.SearchIndexMaintenance
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.main.contract.MainEffect
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.main.contract.MainUiState
import com.aozijx.passly.feature.main.presentation.MainMutation
import com.aozijx.passly.feature.main.presentation.MainReducer
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
class MainViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val authenticationManager: AuthenticationManager,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases,
    private val searchIndexMaintenance: SearchIndexMaintenance,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeSettings()
        observeAuthStates()
        observeDatabaseFailures()
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.Lock -> lock(LockReason.USER)
            MainIntent.ExitRecovery -> lock(LockReason.RECOVERY_EXIT)
            MainIntent.UpdateInteraction -> authenticationManager.onUserInteraction()
            MainIntent.RetryDatabaseInitialization -> initializeDatabase()
            MainIntent.RecoverDatabase -> recoverDatabase()
            MainIntent.RequestAuth -> requestAuth()
            MainIntent.RequestReauth -> requestReauth()
            is MainIntent.RequestSensitiveAccess -> requestSensitiveAccess(
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
        authenticationManager.authenticate(AuthenticationRequest(purpose)) { result ->
            if (result is AuthenticationResult.Success) {
                emitEffect(MainEffect.AuthSuccess)
            } else if (result is AuthenticationResult.Failure) {
                emitEffect(MainEffect.AuthError("认证失败"))
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

                    mutate(MainMutation.Authenticated)
                    emitEffect(MainEffect.NavigateToVault)
                } else if (recoveryMode) {
                    mutate(MainMutation.RecoveryModeEntered)
                } else {
                    mutate(MainMutation.SessionLocked)
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
                    mutate(MainMutation.SettingsChanged(appearance, interfacePrefs))
                }
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            val outcome = runDatabaseInitialization {
                databaseLifecycleUseCases.retryAndReport()
            }
            if (outcome.success) authenticationManager.clearDatabaseFailure()
        }
    }

    private suspend fun runDatabaseInitialization(
        block: suspend () -> DatabaseInitOutcome
    ): DatabaseInitOutcome {
        mutate(MainMutation.DatabaseInitializationStarted(clearError = true))
        val outcome = block()
        mutate(MainMutation.DatabaseInitializationFinished(outcome.error))
        outcome.error?.let { error ->
            emitEffect(
                MainEffect.ShowError(
                    "数据库错误: ${error.toUiMessage("数据库初始化失败")}"
                )
            )
        }
        return outcome
    }

    private fun observeDatabaseFailures() {
        viewModelScope.launch {
            authenticationManager.databaseFailure.collect { error ->
                if (error != null) {
                    mutate(MainMutation.DatabaseFailureObserved(error))
                }
            }
        }
    }

    private fun recoverDatabase() {
        viewModelScope.launch {
            mutate(MainMutation.DatabaseInitializationStarted(clearError = false))
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
                    mutate(MainMutation.DatabaseInitializationFinished(recoveryError))
                    if (sessionRecovered) {
                        val recoveryMessage = outcome.recoveryId?.let {
                            "故障库已保留（恢复编号：$it），已创建新数据库"
                        } ?: "已创建新数据库"
                        emitEffect(MainEffect.ShowToast(recoveryMessage))
                        rebuildSearchIndex()
                    } else {
                        authenticationManager.lock(LockReason.INTEGRITY_FAILURE)
                        emitEffect(
                            MainEffect.ShowError(
                                outcome.error?.toUiMessage("创建新数据库失败")
                                    ?: "创建新数据库失败"
                            )
                        )
                    }
                }

                is AuthenticationResult.Cancelled ->
                    mutate(MainMutation.DatabaseInitializationStopped)
                is AuthenticationResult.Failure -> {
                    mutate(MainMutation.DatabaseInitializationStopped)
                    emitEffect(MainEffect.ShowError("身份验证失败，未创建新数据库"))
                }
            }
        }
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.trySend(effect)
    }

    private fun mutate(mutation: MainMutation) {
        _uiState.value = MainReducer.reduce(_uiState.value, mutation)
    }

    /**
     * 重建搜索盲索引（首次解锁或降级回退时）。
     * 不阻塞用户操作 —— 异步执行，仅记录日志。
     */
    private fun rebuildSearchIndex() {
        viewModelScope.launch {
            val result = searchIndexMaintenance.rebuildIndex()
            result.onSuccess { count ->
                AppTelemetry.i("MainViewModel", "Blind index rebuild complete: $count entries")
            }.onFailure { error ->
                AppTelemetry.w("MainViewModel", "Blind index rebuild skipped")
            }
        }
    }
}
