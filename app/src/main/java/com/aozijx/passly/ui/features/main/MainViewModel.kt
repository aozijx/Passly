package com.aozijx.passly.ui.features.main

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.VerificationGatewayImpl
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.database.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.ui.features.common.toUiMessage
import com.aozijx.passly.ui.features.main.contract.MainEffect
import com.aozijx.passly.ui.features.main.contract.MainIntent
import com.aozijx.passly.ui.features.main.contract.MainUiState
import com.aozijx.passly.ui.features.main.internal.MainDatabaseInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases,
    private val securitySettingsUseCases: SecuritySettingsUseCases,
    private val authUseCases: AuthUseCases,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases,
    private val authRequestValidator: AuthRequestValidator,
    private val databaseInitializer: MainDatabaseInitializer
) : ViewModel() {

    private val authGateway = VerificationGatewayImpl(
        scope = viewModelScope,
        authUseCases = authUseCases,
        requestValidator = authRequestValidator
    )

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
                authGateway.lock()
                databaseLifecycleUseCases.close()
            }

            MainIntent.UpdateInteraction -> authGateway.onUserInteraction()
            MainIntent.CheckAndLock -> authGateway.checkAndLock()
            MainIntent.RetryDatabaseInitialization -> initializeDatabase(isRetry = true)
            else -> Unit
        }
    }

    fun isAuthorizedNow(): Boolean = authGateway.isAuthorized.value

    fun requestAuth(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        authGateway.verifyWithBiometric(activity, title, subtitle) { result ->
            result.onSuccess { onSuccess() }
                .onFailure { error -> onError?.invoke(error.toUiMessage()) }
        }
    }

    fun requestReauth(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        authGateway.verifyWithBiometric(
            activity, title, subtitle, forceReauth = true
        ) { result ->
            result.onSuccess { onSuccess() }
                .onFailure { error -> onError?.invoke(error.toUiMessage()) }
        }
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            authGateway.isAuthorized.collect { authorized ->
                if (authorized) {
                    _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
                }
                _uiState.update { it.copy(isAuthorized = authorized) }
                if (authorized) {
                    initializeDatabase()
                    emitEffect(MainEffect.NavigateToVault)
                }
            }
        }

        viewModelScope.launch {
            authGateway.authMessage.collect { message ->
                _uiState.update { it.copy(validationMessage = message) }
                emitEffect(MainEffect.ShowError(message))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                systemSettingsUseCases.isDarkMode,
                systemSettingsUseCases.isDynamicColor,
                systemSettingsUseCases.themeColor,
                securitySettingsUseCases.lockTimeout
            ) { isDarkMode, isDynamicColor, themeColor, lockTimeout ->
                Pair(Triple(isDarkMode, isDynamicColor, themeColor), lockTimeout)
            }.collect { (triple, lockTimeout) ->
                val (isDarkMode, isDynamicColor, themeColorStr) = triple
                val themeColorLong = themeColorStr.toLongOrNull() ?: 0L
                _uiState.update {
                    it.copy(
                        isDarkMode = isDarkMode, isDynamicColor = isDynamicColor,
                        themeColor = themeColorLong
                    )
                }
                authGateway.updateLockTimeout(lockTimeout)
            }
        }
    }

    private fun initializeDatabase(isRetry: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
            val initResult =
                if (isRetry) databaseInitializer.retry() else databaseInitializer.initialize()
            _uiState.update {
                it.copy(
                    isDatabaseInitializing = false, databaseError = initResult.error
                )
            }

            initResult.recoveryNotice?.let { notice ->
                emitEffect(MainEffect.ShowToast(notice))
            }

            initResult.error?.let { error ->
                val msg = "数据库错误: ${error.toUiMessage("数据库初始化失败")}" 
                emitEffect(MainEffect.ShowError(msg))
            }
        }
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.tryEmit(effect)
    }
}