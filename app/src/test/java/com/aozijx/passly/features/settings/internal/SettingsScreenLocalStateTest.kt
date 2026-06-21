package com.aozijx.passly.features.settings.internal

import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordAction
import com.aozijx.passly.ui.features.settings.internal.AppPasswordDialogState
import com.aozijx.passly.ui.features.settings.shell.SettingsScreenLocalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenLocalStateTest {

    @Test
    fun openLeftActionDialog_closesRightDialog() {
        val state = SettingsScreenLocalState()
        state.showRightActionDialog = true

        state.openLeftActionDialog()

        assertTrue(state.showLeftActionDialog)
        assertFalse(state.showRightActionDialog)
    }

    @Test
    fun openRightActionDialog_closesLeftDialog() {
        val state = SettingsScreenLocalState()
        state.showLeftActionDialog = true

        state.openRightActionDialog()

        assertTrue(state.showRightActionDialog)
        assertFalse(state.showLeftActionDialog)
    }

    @Test
    fun openChangeAppPasswordDialog_dismissesOtherPasswordDialogs_withoutClearingInputs() {
        val state = SettingsScreenLocalState()
        state.activeAppPasswordDialog = AppPasswordDialogState.Set
        state.appPasswordCurrent = "old"
        state.appPasswordNew = "new"
        state.appPasswordConfirm = "new"

        state.openChangeAppPasswordDialog()

        assertEquals(AppPasswordDialogState.Change, state.activeAppPasswordDialog)
        assertEquals("old", state.appPasswordCurrent)
        assertEquals("new", state.appPasswordNew)
        assertEquals("new", state.appPasswordConfirm)
    }

    @Test
    fun openAppPasswordActionDialog_overridesCurrentDialog_withoutClearingInputs() {
        val state = SettingsScreenLocalState()
        state.activeAppPasswordDialog = AppPasswordDialogState.Disable
        state.appPasswordCurrent = "old"
        state.appPasswordNew = "new"
        state.appPasswordConfirm = "new"

        state.openAppPasswordActionDialog()

        assertEquals(AppPasswordDialogState.Action, state.activeAppPasswordDialog)
        assertEquals("old", state.appPasswordCurrent)
        assertEquals("new", state.appPasswordNew)
        assertEquals("new", state.appPasswordConfirm)
    }

    @Test
    fun dismissSetAppPasswordDialog_clearsPasswordInputs() {
        val state = SettingsScreenLocalState()
        state.activeAppPasswordDialog = AppPasswordDialogState.Set
        state.appPasswordNew = "new"
        state.appPasswordConfirm = "new"

        state.dismissSetAppPasswordDialog()

        assertEquals(AppPasswordDialogState.None, state.activeAppPasswordDialog)
        assertEquals("", state.appPasswordCurrent)
        assertEquals("", state.appPasswordNew)
        assertEquals("", state.appPasswordConfirm)
    }

    @Test
    fun onAppPasswordSuccess_closesRelatedDialog_andClearsInputs() {
        val state = SettingsScreenLocalState()
        state.activeAppPasswordDialog = AppPasswordDialogState.Disable
        state.appPasswordCurrent = "old"

        state.onAppPasswordSuccess(AppPasswordAction.DISABLE)

        assertEquals(AppPasswordDialogState.None, state.activeAppPasswordDialog)
        assertEquals("", state.appPasswordCurrent)
        assertEquals("", state.appPasswordNew)
        assertEquals("", state.appPasswordConfirm)
    }
}