package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySettingsUiMapperTest {
    @Test
    fun mapsSecurityStateAndLockConstraintsToPassiveUiModel() {
        val model = SecuritySettingsUiState(
            lockTimeout = 45_000L,
            isInvalidateKeyOnBioChange = false,
            isLockOnBackground = true,
            isBiometricEnabled = true,
        ).toSecuritySettingsUiModel(isAppPasswordEnabled = true)

        assertEquals(45_000L, model.lockTimeoutMs)
        assertEquals(LockTimeoutConstraints.SLIDER_MIN_MS / 1000f, model.sliderMinSeconds)
        assertEquals(LockTimeoutConstraints.MAX_MS / 1000f, model.sliderMaxSeconds)
        assertEquals(LockTimeoutConstraints.SLIDER_STEP_MS / 1000f, model.sliderStepSeconds)
        assertTrue(model.isAppPasswordEnabled)
        assertTrue(model.isBiometricEnabled)
        assertTrue(model.isLockOnBackground)
        assertFalse(model.isInvalidateKeyOnBioChange)
    }
}
