package com.aozijx.passly.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.error.ui.toUiMessage
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private val _effects = MutableSharedFlow<MainEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<MainEffect> = _effects.asSharedFlow()

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
        }
    }

    fun isAuthorizedNow(): Boolean =
        authenticationManager.state.value is AuthenticationState.Authenticated

    fun requestAuth(
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        requestAuthentication(AuthenticationPurpose.UNLOCK_VAULT, onSuccess, onError)
    }

    fun requestReauth(
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        requestAuthentication(AuthenticationPurpose.REAUTHENTICATE, onSuccess, onError)
    }

    fun requestSensitiveAccess(
        action: SensitiveAccessAction,
        accessLevel: SensitiveAccessLevel,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        val purpose = when (action) {
            SensitiveAccessAction.COPY -> AuthenticationPurpose.COPY_SECRET
            SensitiveAccessAction.REVEAL -> when (accessLevel) {
                SensitiveAccessLevel.STANDARD -> AuthenticationPurpose.REVEAL_SECRET
                SensitiveAccessLevel.HIGH ->
                AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET
            }
        }
        requestAuthentication(purpose, onSuccess, onError)
    }

    private fun requestAuthentication(
        purpose: AuthenticationPurpose,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        authenticationManager.authenticate(AuthenticationRequest(purpose)) { result ->
            if (result is AuthenticationResult.Success) onSuccess()
            else if (result is AuthenticationResult.Failure) onError?.invoke("认证失败")
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

                    _uiState.update {
                        it.copy(isAuthorized = true, isRecoveryMode = false)
                    }
                    emitEffect(MainEffect.NavigateToVault)
                } else if (recoveryMode) {
                    _uiState.update {
                        it.copy(
                            isAuthorized = false,
                            isRecoveryMode = true,
                            isDatabaseInitializing = false,
                            databaseError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isAuthorized = false, isRecoveryMode = false)
                    }
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
                    _uiState.update {
                        it.copy(
                            themeMode = appearance.themeMode,
                            isDynamicColor = appearance.isDynamicColor,
                            manualThemeColorArgb = appearance.manualThemeColorArgb,
                            fontFamily = appearance.fontFamily,
                            language = appearance.language,
                            outerCornerRadiusDp = interfacePrefs.outerCornerRadiusDp,
                            innerCornerRadiusDp = interfacePrefs.innerCornerRadiusDp,
                            groupItemSpacingDp = interfacePrefs.groupItemSpacingDp,
                            groupContentPaddingDp = interfacePrefs.groupContentPaddingDp
                        )
                    }
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
        _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
        val outcome = block()
        _uiState.update {
            it.copy(isDatabaseInitializing = false, databaseError = outcome.error)
        }
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
                    _uiState.update {
                        it.copy(
                            isDatabaseInitializing = false,
                            databaseError = error,
                            isAuthorized = false
                        )
                    }
                }
            }
        }
    }

    private fun recoverDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDatabaseInitializing = true) }
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
                    _uiState.update {
                        it.copy(
                            isDatabaseInitializing = false,
                            databaseError = recoveryError
                        )
                    }
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
                    _uiState.update { it.copy(isDatabaseInitializing = false) }
                is AuthenticationResult.Failure -> {
                    _uiState.update { it.copy(isDatabaseInitializing = false) }
                    emitEffect(MainEffect.ShowError("身份验证失败，未创建新数据库"))
                }
            }
        }
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.tryEmit(effect)
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
                AppTelemetry.w("MainViewModel", "Blind index rebuild skipped: ${error.message}")
            }
        }
    }
}
