package com.aozijx.passly.features.settings.internal

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
        state.showAppPasswordActionDialog = true
        state.showSetAppPasswordDialog = true
        state.appPasswordCurrent = "old"
        state.appPasswordNew = "new"
        state.appPasswordConfirm = "new"

        state.openChangeAppPasswordDialog()

        assertTrue(state.showChangeAppPasswordDialog)
        assertFalse(state.showAppPasswordActionDialog)
        assertFalse(state.showSetAppPasswordDialog)
        assertFalse(state.showDisableAppPasswordDialog)
        assertEquals("old", state.appPasswordCurrent)
        assertEquals("new", state.appPasswordNew)
        assertEquals("new", state.appPasswordConfirm)
    }

    @Test
    fun dismissSetAppPasswordDialog_clearsPasswordInputs() {
        val state = SettingsScreenLocalState()
        state.showSetAppPasswordDialog = true
        state.appPasswordNew = "new"
        state.appPasswordConfirm = "new"

        state.dismissSetAppPasswordDialog()

        assertFalse(state.showSetAppPasswordDialog)
        assertEquals("", state.appPasswordCurrent)
        assertEquals("", state.appPasswordNew)
        assertEquals("", state.appPasswordConfirm)
    }

    @Test
    fun onAppPasswordSuccess_closesRelatedDialog_andClearsInputs() {
        val state = SettingsScreenLocalState()
        state.showDisableAppPasswordDialog = true
        state.appPasswordCurrent = "old"

        state.onAppPasswordSuccess(AppPasswordAction.DISABLE)

        assertFalse(state.showDisableAppPasswordDialog)
        assertEquals("", state.appPasswordCurrent)
        assertEquals("", state.appPasswordNew)
        assertEquals("", state.appPasswordConfirm)
    }
}