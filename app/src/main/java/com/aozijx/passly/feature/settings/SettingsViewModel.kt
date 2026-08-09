package com.aozijx.passly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationManager
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetSwipeLeftAction -> setSwipeLeftAction(intent.action)
            is SettingsIntent.SetSwipeRightAction -> setSwipeRightAction(intent.action)
            is SettingsIntent.LoadSettings -> loadSettings()
            SettingsIntent.ClearDatabase -> clearDatabase()
            is SettingsIntent.SetAppPassword -> setAppPassword(intent.password)
            is SettingsIntent.ChangeAppPassword -> changeAppPassword(
                intent.currentPassword,
                intent.newPassword
            )

            SettingsIntent.DisableAppPassword -> disableAppPassword()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val interaction = settingsRepository.settings.first().interaction
                _uiState.update {
                    it.copy(
                        swipeLeftAction = interaction.swipeLeftAction,
                        swipeRightAction = interaction.swipeRightAction,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effects.trySend(SettingsEffect.ShowError(error.toUiMessage("加载设置失败")))
            }
        }
    }

    private fun setSwipeLeftAction(action: SwipeActionType) {
        saveSwipeAction(SettingsCommand.SetSwipeLeftAction(action)) {
            it.copy(swipeLeftAction = action)
        }
    }

    private fun setSwipeRightAction(action: SwipeActionType) {
        saveSwipeAction(SettingsCommand.SetSwipeRightAction(action)) {
            it.copy(swipeRightAction = action)
        }
    }

    private fun saveSwipeAction(
        command: SettingsCommand,
        updateState: (SettingsUiState) -> SettingsUiState
    ) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.update(command)
                _uiState.update(updateState)
                _effects.trySend(SettingsEffect.SettingsSaved)
            }.onFailure { error ->
                _effects.trySend(SettingsEffect.ShowError(error.toUiMessage("保存失败")))
            }
        }
    }

    private fun clearDatabase() {
        if (_uiState.value.isClearingDatabase) return
        if (authenticationManager.state.value is AuthenticationState.RecoveryMode) {
            _effects.trySend(SettingsEffect.ShowError("恢复模式不能清除数据库"))
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
                        _effects.send(SettingsEffect.DatabaseCleared)
                    } else {
                        _effects.send(
                            SettingsEffect.ShowError(
                                "清除数据库失败"
                            )
                        )
                    }
                }

                is AuthenticationResult.Cancelled ->
                    _uiState.update { it.copy(isClearingDatabase = false) }
                is AuthenticationResult.Failure -> {
                    _uiState.update { it.copy(isClearingDatabase = false) }
                    _effects.send(SettingsEffect.ShowError("身份验证失败，数据库未清除"))
                }
            }
        }
    }

    private fun setAppPassword(password: CharArray) {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordSet,
            operation = { authenticationMethodProvisioner.setAppPassword(password) }
        )
    }

    private fun changeAppPassword(
        currentPassword: CharArray,
        newPassword: CharArray
    ) {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordChanged,
            operation = {
                authenticationMethodProvisioner.changeAppPassword(
                    currentPassword,
                    newPassword
                )
            }
        )
    }

    private fun disableAppPassword() {
        runPrimaryAuthMethodChange(
            successEffect = SettingsEffect.AppPasswordDisabled,
            operation = { authenticationMethodProvisioner.disableAppPassword() }
        )
    }

    private fun runPrimaryAuthMethodChange(
        successEffect: SettingsEffect,
        operation: suspend () -> AuthenticationResult
    ) {
        if (isRecoveryMode()) {
            _effects.trySend(
                SettingsEffect.AppPasswordError(
                    "恢复模式不能修改应用密码"
                )
            )
            return
        }
        viewModelScope.launch {
            when (val result = operation()) {
                is AuthenticationResult.Success -> _effects.trySend(successEffect)
                is AuthenticationResult.Failure -> {
                    _effects.trySend(
                        SettingsEffect.AppPasswordError(
                            "操作失败"
                        )
                    )
                }

                is AuthenticationResult.Cancelled -> { /* no-op */
                }
            }
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
