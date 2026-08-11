package com.aozijx.passly.feature.settings.security.presentation

import com.aozijx.passly.feature.settings.security.SecuritySettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySettingsReducerTest {

    @Test
    fun `settings update preserves authentication state`() {
        val result = SecuritySettingsReducer.reduce(
            SecuritySettingsUiState(
                isBiometricEnabled = true,
                hasRecoveryEnvelope = true,
                recoveryCodeVerificationResult = false,
            ),
            SecuritySettingsMutation.SettingsChanged(
                lockTimeout = 30_000L,
                invalidateKeyOnBiometricChange = false,
                lockOnBackground = true,
            ),
        )

        assertEquals(30_000L, result.lockTimeout)
        assertFalse(result.isInvalidateKeyOnBioChange)
        assertTrue(result.isLockOnBackground)
        assertTrue(result.isBiometricEnabled)
        assertTrue(result.hasRecoveryEnvelope)
        assertEquals(false, result.recoveryCodeVerificationResult)
    }

    @Test
    fun `biometric update preserves recovery workflow`() {
        val result = SecuritySettingsReducer.reduce(
            SecuritySettingsUiState(
                hasRecoveryEnvelope = true,
                recoveryCodeVerificationResult = true,
            ),
            SecuritySettingsMutation.BiometricAvailabilityChanged(true),
        )

        assertTrue(result.isBiometricEnabled)
        assertTrue(result.hasRecoveryEnvelope)
        assertTrue(result.recoveryCodeVerificationResult == true)
    }

    @Test
    fun `verification result can be consumed`() {
        val verified = SecuritySettingsReducer.reduce(
            SecuritySettingsUiState(),
            SecuritySettingsMutation.RecoveryCodeVerificationChanged(true),
        )
        val consumed = SecuritySettingsReducer.reduce(
            verified,
            SecuritySettingsMutation.RecoveryCodeVerificationChanged(null),
        )

        assertTrue(verified.recoveryCodeVerificationResult == true)
        assertNull(consumed.recoveryCodeVerificationResult)
    }
}
