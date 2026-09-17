package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.presentation.ui.settings.main.AppPasswordDialogStateHolder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordPresentationAdaptersTest {

    @Test
    fun `dialog model maps password validation`() {
        val stateHolder = AppPasswordDialogStateHolder().apply {
            appPasswordCurrent = "current"
            appPasswordNew = "long-enough-password"
            appPasswordConfirm = "long-enough-password"
        }

        val result = buildAppPasswordDialogsModel(stateHolder)

        assertTrue(result.isSetPasswordConfirmEnabled)
        assertTrue(result.isChangePasswordConfirmEnabled)

        stateHolder.appPasswordConfirm = "different"
        assertFalse(buildAppPasswordDialogsModel(stateHolder).isChangePasswordConfirmEnabled)
    }
}
