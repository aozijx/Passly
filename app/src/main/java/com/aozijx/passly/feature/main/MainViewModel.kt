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
            MainIntent.Lock -> {
                viewModelScope.launch {
                    authenticationManager.lock(LockReason.USER)
                }
            }

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
        authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.UNLOCK_VAULT)
        ) { result ->
            if (result is AuthenticationResult.Success) onSuccess()
            else if (result is AuthenticationResult.Failure) onError?.invoke("认证失败")
        }
    }

    fun requestReauth(
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.REAUTHENTICATE)
        ) { result ->
            if (result is AuthenticationResult.Success) onSuccess()
            else if (result is AuthenticationResult.Failure) onError?.invoke("认证失败")
        }
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            authenticationManager.state.collect { state ->
                val authorized = state is AuthenticationState.Authenticated
                if (authorized) {
                    _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }

                    // 重试逻辑已下沉至 DatabaseLifecycleUseCases
                    val outcome = databaseLifecycleUseCases.preWarmAndReport()

                    _uiState.update {
                        it.copy(
                            isDatabaseInitializing = false,
                            databaseError = outcome.error
                        )
                    }

                    outcome.error?.let { error ->
                        val msg = "数据库错误: ${error.toUiMessage("数据库初始化失败")}"
                        emitEffect(MainEffect.ShowError(msg))
                    }

                    if (outcome.success) {
                        rebuildSearchIndex()
                    }

                    _uiState.update { it.copy(isAuthorized = true) }
                    emitEffect(MainEffect.NavigateToVault)
                } else {
                    _uiState.update { it.copy(isAuthorized = false) }
                }
            }
        }

    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.appearance }
                .distinctUntilChanged()
                .collect { appearance ->
                    _uiState.update {
                        it.copy(
                            themeMode = appearance.themeMode,
                            isDynamicColor = appearance.isDynamicColor,
                            customSeedArgb = appearance.customSeedArgb,
                            fontFamily = appearance.fontFamily,
                            language = appearance.language
                        )
                    }
                }
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
            val outcome = databaseLifecycleUseCases.retryAndReport()
            if (outcome.success) authenticationManager.clearDatabaseFailure()
            _uiState.update {
                it.copy(
                    isDatabaseInitializing = false, databaseError = outcome.error
                )
            }

            outcome.error?.let { error ->
                val msg = "数据库错误: ${error.toUiMessage("数据库初始化失败")}"
                emitEffect(MainEffect.ShowError(msg))
            }
        }
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
