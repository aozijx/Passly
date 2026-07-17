package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
)

sealed interface PrivacyUiAction {
    data class SetSecureContentEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipToLockEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : PrivacyUiAction
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val deviceSettingsUseCases: DeviceSettingsUseCases
) : ViewModel() {

    val config: StateFlow<PrivacyUiState> = combine(
        deviceSettingsUseCases.isSecureContentEnabled,
        deviceSettingsUseCases.isFlipToLockEnabled,
        deviceSettingsUseCases.isFlipExitAndClearStackEnabled
    ) { sec, ftl, fec ->
        PrivacyUiState(
            isSecureContentEnabled = sec,
            isFlipToLockEnabled = ftl,
            isFlipExitAndClearStackEnabled = fec,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        PrivacyUiState()
    )

    fun onAction(action: PrivacyUiAction) {
        when (action) {
            is PrivacyUiAction.SetSecureContentEnabled -> viewModelScope.launch {
                deviceSettingsUseCases.setSecureContentEnabled(action.enabled)
            }

            is PrivacyUiAction.SetFlipToLockEnabled -> viewModelScope.launch {
                deviceSettingsUseCases.setFlipToLockEnabled(action.enabled)
            }

            is PrivacyUiAction.SetFlipExitAndClearStackEnabled -> viewModelScope.launch {
                deviceSettingsUseCases.setFlipExitAndClearStackEnabled(action.enabled)
            }
        }
    }
}