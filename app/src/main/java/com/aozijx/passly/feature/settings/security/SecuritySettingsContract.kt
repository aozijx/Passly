package com.aozijx.passly.feature.settings.security

data class SecuritySettingsUiState(
    val lockTimeout: Long = 60_000L,
    val isInvalidateKeyOnBioChange: Boolean = true,
    val isLockOnBackground: Boolean = false
)

sealed interface SecuritySettingsAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecuritySettingsAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecuritySettingsAction
    data class VerifyRecoveryCode(val code: String) : SecuritySettingsAction
    data object ClearVerifyResult : SecuritySettingsAction
}
