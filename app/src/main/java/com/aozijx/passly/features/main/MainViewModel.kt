package com.aozijx.passly.features.main

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.features.auth.AuthCoordinator
import com.aozijx.passly.features.auth.ui.AuthScreenAuthGateway
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.main.contract.MainUiState
import com.aozijx.passly.features.main.internal.MainDatabaseInitializer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val systemSettingsUseCases = AppContainer.domain.systemSettingsUseCases
    private val securitySettingsUseCases = AppContainer.domain.securitySettingsUseCases
    private val authUseCases = AppContainer.domain.authUseCases
    private val databaseLifecycleUseCases = AppContainer.domain.databaseLifecycleUseCases

    private val authValidationSupport = AuthValidationSupport()
    private val databaseInitializer = MainDatabaseInitializer(databaseLifecycleUseCases)

    private val authCoordinator = AuthCoordinator(
        scope = viewModelScope,
        authUseCases = authUseCases,
        validationSupport = authValidationSupport
    )
    val authScreenGateway: AuthScreenAuthGateway = authCoordinator

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<MainEffect> = _effects.asSharedFlow()

    init {
        observeSettings()
        // 注意：此处不再调用 initializeDatabase()，由 observeAuthStates 驱动
        observeAuthStates()
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.Lock -> {
                authCoordinator.lock()
                databaseLifecycleUseCases.close()
            }

            MainIntent.UpdateInteraction -> authCoordinator.onUserInteraction()
            MainIntent.CheckAndLock -> authCoordinator.checkAndLock()
            MainIntent.RetryDatabaseInitialization -> initializeDatabase(isRetry = true)
            else -> Unit
        }
    }

    fun isAuthorizedNow(): Boolean = authCoordinator.isAuthorized.value

    fun requestAuth(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        authCoordinator.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            authCoordinator.isAuthorized.collect { authorized ->
                _uiState.update { it.copy(isAuthorized = authorized) }
                if (authorized) {
                    // 硬件口令已就绪，现在可以安全初始化数据库
                    initializeDatabase()
                    emitEffect(MainEffect.NavigateToVault)
                }
            }
        }

        viewModelScope.launch {
            authCoordinator.authMessage.collect { message ->
                _uiState.update { it.copy(validationMessage = message) }
                emitEffect(MainEffect.ShowError(message))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            systemSettingsUseCases.isDarkMode.collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
            }
        }

        viewModelScope.launch {
            systemSettingsUseCases.isDynamicColor.collect { isDynamicColor ->
                _uiState.update { it.copy(isDynamicColor = isDynamicColor) }
            }
        }

        viewModelScope.launch {
            securitySettingsUseCases.lockTimeout.collect { lockTimeout ->
                authCoordinator.updateLockTimeout(lockTimeout)
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
                val msg = "数据库错误: ${authValidationSupport.sanitizeMessage(error.message)}"
                emitEffect(MainEffect.ShowError(msg))
            }
        }
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.tryEmit(effect)
    }
}