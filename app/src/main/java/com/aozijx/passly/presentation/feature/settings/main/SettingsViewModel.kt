package com.aozijx.passly.presentation.feature.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
    private val interactionSettingsRepository: InteractionSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()


    init {
        observeAuthenticationMethods()
        loadSettings()
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.SetSwipeLeftAction -> setSwipeLeftAction(action.action)
            is SettingsUiAction.SetSwipeRightAction -> setSwipeRightAction(action.action)
            is SettingsUiAction.LoadSettings -> loadSettings()
            SettingsUiAction.RequestAppPasswordEntry -> requestAppPasswordEntry()
            is SettingsUiAction.SetAppPassword -> setAppPassword(action.password)
            is SettingsUiAction.ChangeAppPassword -> changeAppPassword(
                action.currentPassword,
                action.newPassword
            )

            SettingsUiAction.DisableAppPassword -> disableAppPassword()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val interaction = interactionSettingsRepository.interaction.first()
                _uiState.update {
                    it.copy(
                        swipeLeftAction = interaction.swipeLeftAction,
                        swipeRightAction = interaction.swipeRightAction,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effects.trySend(SettingsEffect.ShowError(error.toUiMessage("加载设置失败")))
            }
        }
    }

    private fun setSwipeLeftAction(action: SwipeActionType) {
        saveSwipeAction(
            save = { interactionSettingsRepository.setSwipeLeftAction(action) },
            updateState = { it.copy(swipeLeftAction = action) },
        )
    }

    private fun setSwipeRightAction(action: SwipeActionType) {
        saveSwipeAction(
            save = { interactionSettingsRepository.setSwipeRightAction(action) },
            updateState = { it.copy(swipeRightAction = action) },
        )
    }

    private fun saveSwipeAction(
        save: suspend () -> Unit,
        updateState: (SettingsUiState) -> SettingsUiState,
    ) {
        viewModelScope.launch {
            runCatching {
                save()
                _uiState.update(updateState)
                _effects.trySend(SettingsEffect.SettingsSaved)
            }.onFailure { error ->
                _effects.trySend(SettingsEffect.ShowError(error.toUiMessage("保存失败")))
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
            val result = authenticationMethodProvisioner.authorizeAppPasswordManagement()

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
            authenticationMethodAvailability.methods.collect { methods ->
                _uiState.update {
                    it.copy(isAppPasswordEnabled = AuthenticationMethod.APP_PASSWORD in methods)
                }
            }
        }
    }


}
