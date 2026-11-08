package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordDialogCoordinatorTest {
    @Test
    fun `busy operation rejects dialog events`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("long-enough-password"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("long-enough-password"))

        assertTrue(coordinator.model.isSetPasswordConfirmEnabled)
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ConfirmSet, isBusy = true))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.NewChanged("rejected-password"), isBusy = true))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.DismissSet, isBusy = true))
        assertEquals(AppPasswordDialog.SET, coordinator.model.activeAppPasswordDialog)
        assertEquals("long-enough-password", coordinator.model.appPasswordNew)
    }

    @Test
    fun `coordinator owns dialog draft transitions and emits only submit actions`() {
        val coordinator = AppPasswordDialogCoordinator()

        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        assertEquals(AppPasswordDialog.SET, coordinator.model.activeAppPasswordDialog)
        assertNull(
            coordinator.onEvent(AppPasswordDialogEvent.NewChanged("long-enough-password")),
        )
        assertNull(
            coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("long-enough-password")),
        )
        assertTrue(coordinator.model.isSetPasswordConfirmEnabled)
        val setRequest =
            coordinator.onEvent(AppPasswordDialogEvent.ConfirmSet)
                as AppPasswordChangeRequest.Set
        assertArrayEquals("long-enough-password".toCharArray(), setRequest.password)
        assertArrayEquals("long-enough-password".toCharArray(), setRequest.confirmation)

        coordinator.onAppPasswordOperationSucceeded()
        assertEquals(null, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)

        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        assertEquals(AppPasswordDialog.ACTION, coordinator.model.activeAppPasswordDialog)
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ShowChange))
        assertEquals(AppPasswordDialog.CHANGE, coordinator.model.activeAppPasswordDialog)
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current")))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value")))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("different")))
        assertFalse(coordinator.model.isChangePasswordConfirmEnabled)
        coordinator.onEvent(AppPasswordDialogEvent.DismissChange)
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        assertEquals(
            AppPasswordChangeRequest.Disable,
            coordinator.onEvent(AppPasswordDialogEvent.ShowDisable),
        )
    }

    @Test
    fun `submitting copies request arrays and immediately clears coordinator drafts`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("long-enough-password"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("long-enough-password"))

        val request = coordinator.onEvent(AppPasswordDialogEvent.ConfirmSet) as AppPasswordChangeRequest.Set

        assertEquals("long-enough-password", String(request.password))
        assertEquals("long-enough-password", String(request.confirmation))
        assertNotSame(request.password, request.confirmation)
        assertEquals(AppPasswordDialog.SET, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
        request.password.fill('\u0000')
        request.confirmation.fill('\u0000')
    }

    @Test
    fun `change submit copies every field and immediately clears coordinator drafts`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        coordinator.onEvent(AppPasswordDialogEvent.ShowChange)
        coordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current-password"))
        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))

        val request = coordinator.onEvent(AppPasswordDialogEvent.ConfirmChange) as AppPasswordChangeRequest.Change

        assertEquals("current-password", String(request.currentPassword))
        assertEquals("new-password-value", String(request.newPassword))
        assertEquals("new-password-value", String(request.confirmation))
        assertNotSame(request.currentPassword, request.newPassword)
        assertNotSame(request.newPassword, request.confirmation)
        assertEquals(AppPasswordDialog.CHANGE, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
        request.currentPassword.fill('\u0000')
        request.newPassword.fill('\u0000')
        request.confirmation.fill('\u0000')
    }

    @Test
    fun `authorization and action dismissal clear stale change drafts`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        coordinator.onEvent(AppPasswordDialogEvent.ShowChange)
        coordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current-password"))
        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))

        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)

        coordinator.onEvent(AppPasswordDialogEvent.DismissAction)
        assertEquals(null, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
    }

    @Test
    fun `clear closes dialog and erases all draft snapshots idempotently`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        coordinator.onEvent(AppPasswordDialogEvent.ShowChange)
        coordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current-password"))
        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))

        coordinator.clear()
        coordinator.clear()

        assertEquals(null, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
    }

    @Test
    fun `set and change dismissal clear every draft`() {
        val setCoordinator = AppPasswordDialogCoordinator()
        setCoordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        setCoordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
        setCoordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))

        setCoordinator.onEvent(AppPasswordDialogEvent.DismissSet)
        assertEmpty(setCoordinator)

        val changeCoordinator = AppPasswordDialogCoordinator()
        changeCoordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        changeCoordinator.onEvent(AppPasswordDialogEvent.ShowChange)
        changeCoordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current-password"))
        changeCoordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
        changeCoordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))

        changeCoordinator.onEvent(AppPasswordDialogEvent.DismissChange)
        assertEmpty(changeCoordinator)
    }

    @Test
    fun `late password input after dismissal cannot repopulate cleared drafts`() {
        val coordinator = AppPasswordDialogCoordinator()
        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        coordinator.onEvent(AppPasswordDialogEvent.DismissSet)

        coordinator.onEvent(AppPasswordDialogEvent.NewChanged("late-password-value"))
        coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("late-password-value"))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ConfirmSet))

        assertEmpty(coordinator)
    }

    private fun assertEmpty(coordinator: AppPasswordDialogCoordinator) {
        assertEquals(null, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
    }
}
