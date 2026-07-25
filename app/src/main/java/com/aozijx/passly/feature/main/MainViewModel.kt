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
import kotlinx.coroutines.flow.combine
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
            combine(
                settingsRepository.settings.map { it.appearance.isDarkMode },
                settingsRepository.settings.map { it.appearance.isDynamicColor },
                settingsRepository.settings.map { it.appearance.themeColor }
            ) { isDarkMode, isDynamicColor, themeColorStr ->
                Triple(isDarkMode, isDynamicColor, themeColorStr)
            }.collect { (isDarkMode, isDynamicColor, themeColorStr) ->
                val themeColorLong = themeColorStr.toLongOrNull() ?: 0L
                _uiState.update {
                    it.copy(
                        isDarkMode = isDarkMode, isDynamicColor = isDynamicColor,
                        themeColor = themeColorLong
                    )
                }
            }
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
            val outcome = databaseLifecycleUseCases.retryAndReport()
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
