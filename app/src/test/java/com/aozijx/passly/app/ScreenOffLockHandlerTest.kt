package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOffLockHandlerTest {

    @Test
    fun `screen off always seals an unlocked session`() = runTest {
        val session = FakeSessionState(AuthenticationState.Authenticated(1L))
        val lockController = RecordingLockController()
        val result = handler(session, lockController).handle()

        assertTrue(result.locked)
        assertEquals(LockReason.BACKGROUND, lockController.reason)
        assertFalse(result.shouldClearTask)
    }

    @Test
    fun `screen off follows active nested clear task setting`() = runTest {
        val session = FakeSessionState(AuthenticationState.Authenticated(1L))
        val lockController = RecordingLockController()
        val result = handler(
            session = session,
            lockController = lockController,
            security = SecuritySettings(
                isFlipToLockEnabled = true,
                isFlipExitAndClearStackEnabled = true,
            ),
        ).handle()

        assertTrue(result.locked)
        assertTrue(result.shouldClearTask)
    }

    @Test
    fun `screen off ignores an already locked session`() = runTest {
        val session = FakeSessionState(AuthenticationState.Locked)
        val lockController = RecordingLockController()
        val result = handler(session, lockController).handle()

        assertFalse(result.locked)
        assertEquals(null, lockController.reason)
    }

    private fun handler(
        session: SecureSessionAccessState,
        lockController: SessionLockController,
        security: SecuritySettings = SecuritySettings(),
    ) = ScreenOffLockHandler(
        sessionAccessState = session,
        sessionLockController = lockController,
        securitySettingsSource = object : SecuritySettingsSource {
            override val security: Flow<SecuritySettings> = flowOf(security)
        },
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
