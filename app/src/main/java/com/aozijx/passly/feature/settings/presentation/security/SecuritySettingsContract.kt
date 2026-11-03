package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface SecuritySettingsUiState {
    data object Loading : SecuritySettingsUiState

    data class Failed(
        val failure: ReadFailure,
    ) : SecuritySettingsUiState

    data class Ready(
        val lockTimeout: Long,
        val isInvalidateKeyOnBioChange: Boolean,
        val isLockOnBackground: Boolean,
        val isBiometricEnabled: Boolean,
        val isAuthenticationChangeInProgress: Boolean,
    ) : SecuritySettingsUiState
}

sealed interface SecuritySettingsAction {
    data object RetryRead : SecuritySettingsAction

    data class SetLockTimeout(
        val timeoutMs: Long,
    ) : SecuritySettingsAction

    data class ToggleLockOnBackground(
        val enabled: Boolean,
    ) : SecuritySettingsAction

    data class SetBiometricEnabled(
        val enabled: Boolean,
    ) : SecuritySettingsAction

    data class SetInvalidateKeyOnBiometricChange(
        val enabled: Boolean,
    ) : SecuritySettingsAction
}

sealed interface SecuritySettingsNotice {
    data object BiometricRotationCleanupPending : SecuritySettingsNotice
}

sealed interface SecuritySettingsEffect {
    data class Error(
        val message: UiErrorMessage,
    ) : SecuritySettingsEffect

    data class Notice(
        val notice: SecuritySettingsNotice,
    ) : SecuritySettingsEffect
}
