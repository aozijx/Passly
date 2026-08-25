package com.aozijx.passly.presentation.feature.settings.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacySettingsUiMapperTest {
    @Test
    fun mapsEveryPrivacyFlagToPassiveUiModel() {
        val model = PrivacySettingsUiState(
            isSecureContentEnabled = false,
            isFlipToLockEnabled = true,
            isFlipExitAndClearStackEnabled = true,
            reauthenticateSensitiveCopies = false,
            clipboardClearEnabled = true,
            clipboardClearDelaySeconds = 60,
        ).toPrivacySettingsUiModel()

        assertFalse(model.isSecureContentEnabled)
        assertTrue(model.isFlipToLockEnabled)
        assertTrue(model.isFlipExitAndClearStackEnabled)
        assertFalse(model.reauthenticateSensitiveCopies)
        assertTrue(model.clipboardClearEnabled)
        assertEquals(60, model.clipboardClearDelaySeconds)
        assertEquals(listOf(15, 30, 60, 120), model.clipboardClearDelayOptions)
    }
}
