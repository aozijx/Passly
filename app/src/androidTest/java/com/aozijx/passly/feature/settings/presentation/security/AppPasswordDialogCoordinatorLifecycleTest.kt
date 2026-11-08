package com.aozijx.passly.feature.settings.presentation.security

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

class AppPasswordDialogCoordinatorLifecycleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `route disposal clears old coordinator and recreated route starts empty`() {
        val showRoute = mutableStateOf(true)
        val currentCoordinator = AtomicReference<AppPasswordDialogCoordinator?>()
        compose.setContent {
            if (showRoute.value) {
                val coordinator = rememberAppPasswordDialogCoordinator()
                SideEffect { currentCoordinator.set(coordinator) }
                AppPasswordDialogCoordinatorOwner(coordinator)
            }
        }
        compose.waitForIdle()

        val oldCoordinator = requireNotNull(currentCoordinator.get())
        populate(oldCoordinator)
        compose.runOnIdle { showRoute.value = false }
        compose.waitForIdle()

        assertEmpty(oldCoordinator)

        compose.runOnIdle { showRoute.value = true }
        compose.waitForIdle()
        val recreatedCoordinator = requireNotNull(currentCoordinator.get())

        assertNotSame(oldCoordinator, recreatedCoordinator)
        assertEmpty(recreatedCoordinator)
    }

    private fun populate(coordinator: AppPasswordDialogCoordinator) {
        coordinator.apply {
            onAppPasswordEntryAuthorized(alreadyEnabled = true)
            onEvent(AppPasswordDialogEvent.ShowChange)
            onEvent(AppPasswordDialogEvent.CurrentChanged("current-password"))
            onEvent(AppPasswordDialogEvent.NewChanged("new-password-value"))
            onEvent(AppPasswordDialogEvent.ConfirmChanged("new-password-value"))
        }
    }

    private fun assertEmpty(coordinator: AppPasswordDialogCoordinator) {
        assertEquals(null, coordinator.model.activeAppPasswordDialog)
        assertEquals("", coordinator.model.appPasswordCurrent)
        assertEquals("", coordinator.model.appPasswordNew)
        assertEquals("", coordinator.model.appPasswordConfirm)
    }
}
