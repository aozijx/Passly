package com.aozijx.passly.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.VerificationGatewayImpl
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.ui.features.settings.contract.SettingsEffect
import com.aozijx.passly.ui.features.settings.contract.SettingsIntent
import com.aozijx.passly.ui.features.settings.contract.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authUseCases: AuthUseCases,
    authRequestValidator: AuthRequestValidator,
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val authGateway = VerificationGatewayImpl(viewModelScope, authUseCases, authRequestValidator)
    val isAppPasswordEnabled: StateFlow<Boolean> = authGateway.isAppPasswordEnabled

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
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val swipeLeft = systemSettingsUseCases.swipeLeftAction.first()
                val swipeRight = systemSettingsUseCases.swipeRightAction.first()
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
                systemSettingsUseCases.setSwipeLeftAction(action)
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
                systemSettingsUseCases.setSwipeRightAction(action)
                _uiState.update { it.copy(swipeRightAction = action) }
                _effects.tryEmit(SettingsEffect.SettingsSaved)
            }.onFailure { error ->
                _effects.tryEmit(SettingsEffect.ShowError(error.message ?: "保存失败"))
            }
        }
    }
}