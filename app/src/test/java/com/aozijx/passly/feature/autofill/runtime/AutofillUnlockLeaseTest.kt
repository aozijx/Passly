package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillUnlockLeaseTest {
    @Test
    fun `cancelling last release while session lock is waiting still completes the lock`() = runTest {
        val lockEntered = CompletableDeferred<Unit>()
        val allowLock = CompletableDeferred<Unit>()
        var locked = false
        val lease = AutofillUnlockLease(ControllableLockController {
            lockEntered.complete(Unit)
            allowLock.await()
            locked = true
        })
        val interactionId = AutofillInteractionId("interaction-1")
        lease.acquireOrBorrow(interactionId, acquiredUnlock = true)

        val release = launch { lease.release(interactionId) }
        runCurrent()
        lockEntered.await()
        release.cancel()
        runCurrent()

        assertFalse("release must retain its lock obligation while the controller is waiting", release.isCompleted)
        allowLock.complete(Unit)
        release.join()

        assertTrue(locked)
    }

    @Test
    fun `a transient lock failure is retried until the lease closes`() = runTest {
        var attempts = 0
        var locked = false
        val lease = AutofillUnlockLease(ControllableLockController {
            attempts++
            if (attempts == 1) error("temporary lock failure")
            locked = true
        }, this, Unit)
        val interactionId = AutofillInteractionId("interaction-2")
        lease.acquireOrBorrow(interactionId, acquiredUnlock = true)

        try {
            lease.release(interactionId)
            throw AssertionError("the first lock failure must be reported to this caller")
        } catch (expected: IllegalStateException) {
            assertEquals("temporary lock failure", expected.message)
        }
        assertEquals(1, attempts)
        val lateBorrow = async { lease.borrowIfOwned(AutofillInteractionId("interaction-late")) }
        runCurrent()
        assertTrue("backoff must release the lease mutex", lateBorrow.isCompleted)
        assertFalse("pending lock must reject new borrowers", lateBorrow.await())
        advanceUntilIdle()

        assertEquals(2, attempts)
        assertTrue(locked)
    }

    @Test
    fun `failed result finalization leaves the lease lock retryable`() = runTest {
        var lockCount = 0
        val lease = AutofillUnlockLease(ControllableLockController { lockCount++ })
        val interactionId = AutofillInteractionId("interaction-3")
        lease.acquireOrBorrow(interactionId, acquiredUnlock = true)
        lease.beginResultCompletion(interactionId)
        lease.release(interactionId)

        try {
            lease.endResultCompletion { error("finalization failed") }
            throw AssertionError("the finalization error should be propagated")
        } catch (expected: IllegalStateException) {
            assertEquals("finalization failed", expected.message)
        }

        lease.awaitReadyForInteraction()

        assertEquals(1, lockCount)
    }

    @Test
    fun `release never locks a session not acquired by this lease`() = runTest {
        var lockCount = 0
        val lease = AutofillUnlockLease(ControllableLockController { lockCount++ })
        val interactionId = AutofillInteractionId("interaction-4")
        lease.acquireOrBorrow(interactionId, acquiredUnlock = false)

        lease.release(interactionId)

        assertEquals(0, lockCount)
    }

    private class ControllableLockController(
        private val onLock: suspend (SessionLockRequest) -> Unit,
    ) : SessionLockController {
        override suspend fun lock(request: SessionLockRequest) = onLock(request)
    }
}
