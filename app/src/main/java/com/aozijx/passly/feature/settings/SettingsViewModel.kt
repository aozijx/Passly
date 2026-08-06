package com.aozijx.passly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.diagnostics.usecase.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val authenticationManager: AuthenticationManager,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: AppSettingsRepository,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases
) : ViewModel() {

    val isAppPasswordEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.appPassword }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    init {
        loadSettings()
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetSwipeLeftAction -> setSwipeLeftAction(intent.action)
            is SettingsIntent.SetSwipeRightAction -> setSwipeRightAction(intent.action)
            is SettingsIntent.LoadSettings -> loadSettings()
            SettingsIntent.ClearDatabase -> clearDatabase()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val swipeLeft = settingsRepository.settings.first().interaction.swipeLeftAction
                val swipeRight = settingsRepository.settings.first().interaction.swipeRightAction
                _uiState.update {
                    it.copy(
                        swipeLeftAction = swipeLeft,
                        swipeRightAction = swipeRight,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effects.tryEmit(SettingsEffect.ShowError(error.message ?: "加载设置失败"))
            }
        }
    }

    private fun setSwipeLeftAction(action: SwipeActionType) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.update(SettingsCommand.SetSwipeLeftAction(action))
                _uiState.update { it.copy(swipeLeftAction = action) }
                _effects.tryEmit(SettingsEffect.SettingsSaved)
            }.onFailure { error ->
                _effects.tryEmit(SettingsEffect.ShowError(error.message ?: "保存失败"))
            }
        }
    }

    private fun setSwipeRightAction(action: SwipeActionType) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.update(SettingsCommand.SetSwipeRightAction(action))
                _uiState.update { it.copy(swipeRightAction = action) }
                _effects.tryEmit(SettingsEffect.SettingsSaved)
            }.onFailure { error ->
                _effects.tryEmit(SettingsEffect.ShowError(error.message ?: "保存失败"))
            }
        }
    }

    private fun clearDatabase() {
        if (_uiState.value.isClearingDatabase) return
        if (authenticationManager.state.value is AuthenticationState.RecoveryMode) {
            _effects.tryEmit(SettingsEffect.ShowError("恢复模式不能清除数据库"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingDatabase = true) }
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.CLEAR_DATABASE)
                )
            ) {
                is AuthenticationResult.Success -> {
                    val outcome = databaseLifecycleUseCases.clearAndReinitialize()
                    _uiState.update { it.copy(isClearingDatabase = false) }
                    if (outcome.success) {
                        _effects.emit(SettingsEffect.DatabaseCleared)
                    } else {
                        _effects.emit(
                            SettingsEffect.ShowError(
                                outcome.error?.message ?: "清除数据库失败"
                            )
                        )
                    }
                }

                is AuthenticationResult.Cancelled ->
                    _uiState.update { it.copy(isClearingDatabase = false) }
                is AuthenticationResult.Failure -> {
                    _uiState.update { it.copy(isClearingDatabase = false) }
                    _effects.emit(SettingsEffect.ShowError("身份验证失败，数据库未清除"))
                }
            }
        }
    }

    fun setAppPassword(
        password: CharArray,
        onResult: (AuthenticationResult) -> Unit
    ) {
        if (isRecoveryMode()) {
            onResult(sessionModeRestrictedResult())
            return
        }
        viewModelScope.launch {
            onResult(authenticationMethodProvisioner.setAppPassword(password))
        }
    }

    fun changeAppPassword(
        currentPassword: CharArray,
        newPassword: CharArray,
        onResult: (AuthenticationResult) -> Unit
    ) {
        if (isRecoveryMode()) {
            onResult(sessionModeRestrictedResult())
            return
        }
        viewModelScope.launch {
            onResult(
                authenticationMethodProvisioner.changeAppPassword(
                    currentPassword,
                    newPassword
                )
            )
        }
    }

    fun disableAppPassword(onResult: (AuthenticationResult) -> Unit) {
        if (isRecoveryMode()) {
            onResult(sessionModeRestrictedResult())
            return
        }
        viewModelScope.launch {
            onResult(authenticationMethodProvisioner.disableAppPassword())
        }
    }

    private fun isRecoveryMode(): Boolean =
        authenticationManager.state.value is AuthenticationState.RecoveryMode

    private fun sessionModeRestrictedResult(): AuthenticationResult =
        AuthenticationResult.Failure(
            AuthenticationFailure(
                AuthenticationFailureCode.SESSION_MODE_RESTRICTED,
                correlationId = "settings-viewmodel"
            )
        )
}
