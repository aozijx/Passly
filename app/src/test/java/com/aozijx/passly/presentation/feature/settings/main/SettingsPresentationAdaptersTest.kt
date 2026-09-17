package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPresentationAdaptersTest {

    @Test
    fun `dialog model maps password validation`() {
        val localState = SettingsOverlayState().apply {
            appPasswordCurrent = "current"
            appPasswordNew = "long-enough-password"
            appPasswordConfirm = "long-enough-password"
        }

        val result = buildSettingsDialogsState(localState)

        assertTrue(result.isSetPasswordConfirmEnabled)
        assertTrue(result.isChangePasswordConfirmEnabled)

        localState.appPasswordConfirm = "different"
        assertFalse(buildSettingsDialogsState(localState).isChangePasswordConfirmEnabled)
    }
}
