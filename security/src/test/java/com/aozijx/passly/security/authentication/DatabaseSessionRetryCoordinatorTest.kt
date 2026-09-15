package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.lock.LockStateManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSessionRetryCoordinatorTest {
    @Test
    fun successPublishesAuthenticatedOnlyAfterSessionIsOpen() = runTest {
        val lifecycle = RecordingLifecycle()
        val lockState = LockStateManager()
        var failure: Throwable? = IllegalStateException("first failure")
        var authenticated = false
        val coordinator = DatabaseSessionRetryCoordinator(
            sessionManager = lifecycle,
            lockStateManager = lockState,
            currentFailure = { failure },
            updateFailure = { failure = it },
            publishAuthenticated = {
                assertEquals(SecureSessionState.UNLOCKED, lockState.state)
                authenticated = true
            },
        )

        val result = coordinator.retry()

        assertEquals(DatabaseSessionRetryResult.Ready, result)
        assertEquals(1, lifecycle.unlockCalls)
        assertEquals(null, failure)
        assertTrue(authenticated)
    }

    @Test
    fun failureRemainsLockedAndReturnsTheLatestCause() = runTest {
        val latest = IllegalStateException("still broken")
        val lifecycle = RecordingLifecycle(unlockError = latest)
        val lockState = LockStateManager()
        var failure: Throwable? = IllegalStateException("first failure")
        var authenticated = false
        val coordinator = DatabaseSessionRetryCoordinator(
            sessionManager = lifecycle,
            lockStateManager = lockState,
            currentFailure = { failure },
            updateFailure = { failure = it },
            publishAuthenticated = { authenticated = true },
        )

        val result = coordinator.retry()

        assertEquals(DatabaseSessionRetryResult.Failed(latest), result)
        assertSame(latest, failure)
        assertEquals(SecureSessionState.SEALED, lockState.state)
        assertFalse(authenticated)
    }

    @Test
    fun retryIsUnavailableWithoutARecordedOpenFailure() = runTest {
        val lifecycle = RecordingLifecycle()
        val coordinator = DatabaseSessionRetryCoordinator(
            sessionManager = lifecycle,
            lockStateManager = LockStateManager(),
            currentFailure = { null },
            updateFailure = {},
            publishAuthenticated = {},
        )

        assertEquals(DatabaseSessionRetryResult.Unavailable, coordinator.retry())
        assertEquals(0, lifecycle.unlockCalls)
    }

    private class RecordingLifecycle(
        private val unlockError: Throwable? = null,
    ) : DatabaseSessionLifecycle {
        override val lockState: SecureSessionState = SecureSessionState.SEALED
        var unlockCalls = 0

        override suspend fun unlock(): Throwable? = unlockError.also { unlockCalls++ }
        override suspend fun softLock() = Unit
        override suspend fun seal() = Unit
    }
}