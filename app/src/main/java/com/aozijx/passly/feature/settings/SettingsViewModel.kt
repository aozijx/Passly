package com.aozijx.passly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.app.database.DatabaseLifecycleUseCases
import com.aozijx.passly.data.settings.model.SettingsCommand
import com.aozijx.passly.data.settings.model.SwipeActionType
import com.aozijx.passly.data.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.presentation.SettingsMutation
import com.aozijx.passly.feature.settings.presentation.SettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: AppSettingsRepository,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeAuthenticationMethods()
        loadSettings()
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetSwipeLeftAction -> setSwipeLeftAction(intent.action)
            is SettingsIntent.SetSwipeRightAction -> setSwipeRightAction(intent.action)
            is SettingsIntent.LoadSettings -> loadSettings()
            SettingsIntent.ClearDatabase -> clearDatabase()
            SettingsIntent.RequestAppPasswordEntry -> requestAppPasswordEntry()
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
            mutate(SettingsMutation.LoadingStarted)
            runCatching {
                val interaction = settingsRepository.settings.first().interaction
                mutate(
                    SettingsMutation.SettingsLoaded(
                        swipeLeftAction = interaction.swipeLeftAction,
                        swipeRightAction = interaction.swipeRightAction,
                    )
                )
            }.onFailure { error ->
                mutate(SettingsMutation.LoadingFailed)
                _effects.trySend(SettingsEffect.ShowError(error.toUiMessage("加载设置失败")))
            }
        }
    }

    private fun setSwipeLeftAction(action: SwipeActionType) {
        saveSwipeAction(
            command = SettingsCommand.SetSwipeLeftAction(action),
            savedMutation = SettingsMutation.SwipeLeftActionSaved(action),
        )
    }

    private fun setSwipeRightAction(action: SwipeActionType) {
        saveSwipeAction(
            command = SettingsCommand.SetSwipeRightAction(action),
            savedMutation = SettingsMutation.SwipeRightActionSaved(action),
        )
    }

    private fun saveSwipeAction(
        command: SettingsCommand,
        savedMutation: SettingsMutation,
    ) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.update(command)
                mutate(savedMutation)
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
            mutate(SettingsMutation.DatabaseClearStarted)
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.CLEAR_DATABASE)
                )
            ) {
                is AuthenticationResult.Success -> {
                    val outcome = databaseLifecycleUseCases.clearAndReinitialize()
                    mutate(SettingsMutation.DatabaseClearFinished)
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
                    mutate(SettingsMutation.DatabaseClearFinished)
                is AuthenticationResult.Failure -> {
                    mutate(SettingsMutation.DatabaseClearFinished)
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

    private fun requestAppPasswordEntry() {
        viewModelScope.launch {
            val result = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.REAUTHENTICATE)
            )
            when (result) {
                is AuthenticationResult.Success -> _effects.trySend(
                    SettingsEffect.AppPasswordEntryAuthorized(
                        alreadyEnabled = _uiState.value.isAppPasswordEnabled,
                    )
                )
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> _effects.trySend(
                    SettingsEffect.AppPasswordEntryAuthenticationFailed(result.failure)
                )
            }
        }
    }

    private fun observeAuthenticationMethods() {
        viewModelScope.launch {
            authenticationManager.methods.collect { methods ->
                mutate(SettingsMutation.AppPasswordAvailabilityChanged(AuthenticationMethod.APP_PASSWORD in methods))
            }
        }
    }

    private fun isRecoveryMode(): Boolean =
        authenticationManager.state.value is AuthenticationState.RecoveryMode

    private fun mutate(mutation: SettingsMutation) {
        _uiState.value = SettingsReducer.reduce(_uiState.value, mutation)
    }
}
