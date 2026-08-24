package com.aozijx.passly.presentation.feature.settings.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySettingsUiMapperTest {
    @Test
    fun mapsEveryPrivacyFlagToPassiveUiModel() {
        val model = PrivacySettingsUiState(
            isSecureContentEnabled = false,
            isFlipToLockEnabled = true,
            isFlipExitAndClearStackEnabled = true,
            reauthenticateSensitiveCopies = false,
        ).toPrivacySettingsUiModel()

        assertFalse(model.isSecureContentEnabled)
        assertTrue(model.isFlipToLockEnabled)
        assertTrue(model.isFlipExitAndClearStackEnabled)
        assertFalse(model.reauthenticateSensitiveCopies)
    }
}
