package com.aozijx.passly.ui.features.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isSecureContentEnabled: Boolean = AppDefaults.SECURITY_SECURE_CONTENT_ENABLED,
    val isFlipToLockEnabled: Boolean = AppDefaults.SECURITY_FLIP_TO_LOCK_ENABLED,
    val isFlipExitAndClearStackEnabled: Boolean = AppDefaults.SECURITY_FLIP_EXIT_AND_CLEAR_STACK_ENABLED,
)

sealed interface PrivacyUiAction {
    data class SetSecureContentEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipToLockEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : PrivacyUiAction
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val securitySettingsUseCases: SecuritySettingsUseCases
) : ViewModel() {

    val config: StateFlow<PrivacyUiState> = combine(
        securitySettingsUseCases.isSecureContentEnabled,
        securitySettingsUseCases.isFlipToLockEnabled,
        securitySettingsUseCases.isFlipExitAndClearStackEnabled
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
                securitySettingsUseCases.setSecureContentEnabled(action.enabled)
            }

            is PrivacyUiAction.SetFlipToLockEnabled -> viewModelScope.launch {
                securitySettingsUseCases.setFlipToLockEnabled(action.enabled)
            }

            is PrivacyUiAction.SetFlipExitAndClearStackEnabled -> viewModelScope.launch {
                securitySettingsUseCases.setFlipExitAndClearStackEnabled(action.enabled)
            }
        }
    }
}