package com.aozijx.passly.feature.settings.security.presentation

import com.aozijx.passly.feature.settings.security.SecuritySettingsUiState

internal sealed interface SecuritySettingsMutation {
    data class SettingsChanged(
        val lockTimeout: Long,
        val invalidateKeyOnBiometricChange: Boolean,
        val lockOnBackground: Boolean,
    ) : SecuritySettingsMutation

    data class BiometricAvailabilityChanged(
        val enabled: Boolean,
    ) : SecuritySettingsMutation

    data class RecoveryEnvelopeAvailabilityChanged(
        val available: Boolean,
    ) : SecuritySettingsMutation

    data class RecoveryCodeVerificationChanged(
        val result: Boolean?,
    ) : SecuritySettingsMutation
}

internal object SecuritySettingsReducer {
    fun reduce(
        state: SecuritySettingsUiState,
        mutation: SecuritySettingsMutation,
    ): SecuritySettingsUiState = when (mutation) {
        is SecuritySettingsMutation.SettingsChanged -> state.copy(
            lockTimeout = mutation.lockTimeout,
            isInvalidateKeyOnBioChange = mutation.invalidateKeyOnBiometricChange,
            isLockOnBackground = mutation.lockOnBackground,
        )
        is SecuritySettingsMutation.BiometricAvailabilityChanged -> state.copy(
            isBiometricEnabled = mutation.enabled,
        )
        is SecuritySettingsMutation.RecoveryEnvelopeAvailabilityChanged -> state.copy(
            hasRecoveryEnvelope = mutation.available,
        )
        is SecuritySettingsMutation.RecoveryCodeVerificationChanged -> state.copy(
            recoveryCodeVerificationResult = mutation.result,
        )
    }
}
