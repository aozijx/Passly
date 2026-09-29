package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.model.SecuritySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLockHandlerTest {

    @Test
    fun `screen off seals an unlocked session independently of flip settings`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(
                isFlipToLockEnabled = false,
                isFlipExitAndClearStackEnabled = true,
            ),
        )

        val result = handler.handle(DeviceLockTrigger.SCREEN_OFF)

        assertTrue(result.locked)
        assertFalse(result.shouldClearTask)
        assertEquals(LockReason.BACKGROUND, lockController.reason)
    }

    @Test
    fun `disabled flip trigger does not lock the session`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(isFlipToLockEnabled = false),
        )

        val result = handler.handle(DeviceLockTrigger.FLIP)

        assertFalse(result.locked)
        assertNull(lockController.reason)
    }

    @Test
    fun `enabled flip locks softly and follows clear task setting`() = runTest {
        val lockController = RecordingLockController()
        val handler = handler(
            lockController = lockController,
            security = SecuritySettings(
                isFlipToLockEnabled = true,
                isFlipExitAndClearStackEnabled = true,
            ),
        )

        val result = handler.handle(DeviceLockTrigger.FLIP)

        assertTrue(result.locked)
        assertTrue(result.shouldClearTask)
        assertEquals(LockReason.USER, lockController.reason)
    }

    private fun handler(
        lockController: SessionLockController,
        security: SecuritySettings,
    ) = DeviceLockHandler(
        sessionAccessState = FakeSessionState(AuthenticationState.Authenticated(1L)),
        sessionLockController = lockController,
        securitySettings = MutableStateFlow(security),
    )

    private class FakeSessionState(initial: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)
        override fun isUnlocked(): Boolean =
            authenticationState.value is AuthenticationState.Authenticated ||
                authenticationState.value is AuthenticationState.RecoveryMode
    }

    private class RecordingLockController : SessionLockController {
        var reason: LockReason? = null

        override suspend fun lock(reason: LockReason) {
            this.reason = reason
        }
    }
}
