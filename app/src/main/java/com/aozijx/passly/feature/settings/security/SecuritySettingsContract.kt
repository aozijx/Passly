package com.aozijx.passly.feature.settings.security

data class SecuritySettingsUiState(
    val lockTimeout: Long = 60_000L,
    val isInvalidateKeyOnBioChange: Boolean = true,
    val isLockOnBackground: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasRecoveryEnvelope: Boolean = false,
    val recoveryCodeVerificationResult: Boolean? = null,
)

sealed interface SecuritySettingsAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecuritySettingsAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecuritySettingsAction
    data class SetBiometricEnabled(val enabled: Boolean) : SecuritySettingsAction
    data class SetInvalidateKeyOnBiometricChange(val enabled: Boolean) : SecuritySettingsAction
    data class VerifyRecoveryCode(val code: CharArray) : SecuritySettingsAction
    data object ClearVerifyResult : SecuritySettingsAction
}
