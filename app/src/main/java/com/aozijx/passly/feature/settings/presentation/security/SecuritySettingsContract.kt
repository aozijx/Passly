package com.aozijx.passly.feature.settings.presentation.security

sealed interface SecuritySettingsUiState {
    data object Loading : SecuritySettingsUiState

    data class Ready(
        val lockTimeout: Long,
        val isInvalidateKeyOnBioChange: Boolean,
        val isLockOnBackground: Boolean,
        val isBiometricEnabled: Boolean,
    ) : SecuritySettingsUiState
}

sealed interface SecuritySettingsAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecuritySettingsAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecuritySettingsAction
    data class SetBiometricEnabled(val enabled: Boolean) : SecuritySettingsAction
    data class SetInvalidateKeyOnBiometricChange(val enabled: Boolean) : SecuritySettingsAction
}
