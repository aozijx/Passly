package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest
import com.aozijx.passly.presentation.feature.settings.ui.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.feature.settings.ui.main.model.AppPasswordDialogState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordDialogCoordinatorTest {

    @Test
    fun `coordinator owns dialog draft transitions and emits only submit actions`() {
        val coordinator = AppPasswordDialogCoordinator()

        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = false)
        assertEquals(AppPasswordDialogState.Set, coordinator.model.activeAppPasswordDialog)
        assertNull(
            coordinator.onEvent(AppPasswordDialogEvent.NewChanged("long-enough-password")),
        )
        assertNull(
            coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("long-enough-password")),
        )
        assertTrue(coordinator.model.isSetPasswordConfirmEnabled)
        val setRequest = coordinator.onEvent(AppPasswordDialogEvent.ConfirmSet)
            as AppPasswordChangeRequest.Set
        assertArrayEquals("long-enough-password".toCharArray(), setRequest.password)
        assertArrayEquals("long-enough-password".toCharArray(), setRequest.confirmation)

        coordinator.onAppPasswordOperationSucceeded()
        assertEquals(AppPasswordDialogState.None, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)

        coordinator.onAppPasswordEntryAuthorized(alreadyEnabled = true)
        assertEquals(AppPasswordDialogState.Action, coordinator.model.activeAppPasswordDialog)
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ShowChange))
        assertEquals(AppPasswordDialogState.Change, coordinator.model.activeAppPasswordDialog)
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.CurrentChanged("current")))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.NewChanged("new-password-value")))
        assertNull(coordinator.onEvent(AppPasswordDialogEvent.ConfirmChanged("different")))
        assertFalse(coordinator.model.isChangePasswordConfirmEnabled)
        assertEquals(
            AppPasswordChangeRequest.Disable,
            coordinator.onEvent(AppPasswordDialogEvent.ShowDisable),
        )
    }
}
