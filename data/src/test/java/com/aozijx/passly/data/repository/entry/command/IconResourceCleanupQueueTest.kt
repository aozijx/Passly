package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IconResourceCleanupQueueTest {
    @Test
    fun graphChangesDuringAReferenceCheckAreNotLost() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val queue = cleanupQueue(backgroundScope) {
            calls++
            if (calls == 1) {
                gate.await()
                AppResult.Success(IconResourceReclaimOutcome.REFERENCED)
            } else {
                AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
            }
        }
        runCurrent()
        queue.request("candidate")
        runCurrent()
        queue.request("candidate")
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, calls)
    }

    @Test
    fun lockedRequestsAreRetainedAndDeduplicatedUntilUnlock() = runTest {
        val access = Access().apply { authenticationState.value = AuthenticationState.Locked }
        val calls = mutableListOf<String>()
        val queue = cleanupQueue(backgroundScope, access) { path ->
            calls += path
            AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
        }
        runCurrent()
        repeat(10) { queue.request("candidate") }
        runCurrent()
        assertEquals(emptyList<String>(), calls)
        access.authenticationState.value = AuthenticationState.Authenticated(2L)
        runCurrent()
        assertEquals(listOf("candidate"), calls)
    }

    @Test
    fun databaseOnlyReopenRetriesPendingCleanupWithoutAuthenticationTransition() = runTest {
        val access = Access().apply { databaseState.value = SecureSessionState.SEALED }
        val calls = mutableListOf<String>()
        val queue = cleanupQueue(backgroundScope, access) { path ->
            calls += path
            AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
        }
        runCurrent()

        queue.request("candidate")
        runCurrent()
        assertEquals(emptyList<String>(), calls)

        access.databaseState.value = SecureSessionState.UNLOCKED
        runCurrent()

        assertEquals(listOf("candidate"), calls)
    }

    @Test
    fun storageFailureIsRetriedByTheNextRequest() = runTest {
        val calls = mutableListOf<String>()
        var available = false
        val queue = cleanupQueue(backgroundScope) { path ->
            calls += path
            if (available) AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
            else AppResult.Failure(SessionModeRestricted())
        }
        runCurrent()
        queue.request("first")
        runCurrent()
        available = true
        queue.request("second")
        runCurrent()
        assertEquals(2, calls.count { it == "first" })
        assertEquals(1, calls.count { it == "second" })
    }

    @Test
    fun referencesCompleteTheRequestUntilANewGraphChangeSchedulesItAgain() = runTest {
        val access = Access()
        var calls = 0
        val queue = cleanupQueue(backgroundScope, access) {
            calls++
            AppResult.Success(IconResourceReclaimOutcome.REFERENCED)
        }
        runCurrent()
        queue.request("shared")
        runCurrent()
        access.authenticationState.value = AuthenticationState.Authenticated(2L)
        runCurrent()
        assertEquals(1, calls)
        queue.request("shared")
        runCurrent()
        assertEquals(2, calls)
    }

    @Test
    fun duplicateRequestsDuringAnAttemptCoalesceIntoOneFollowup() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val queue = cleanupQueue(backgroundScope) {
            calls++
            gate.await()
            AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
        }
        runCurrent()
        repeat(100) { queue.request("candidate") }
        runCurrent()
        repeat(100) { queue.request("candidate") }
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, calls)
    }

    @Test
    fun cancelledDatabaseLeaseDoesNotKillFutureQueueAttempts() = runTest {
        var calls = 0
        val queue = cleanupQueue(backgroundScope) {
            calls++
            if (calls == 1) {
                currentCoroutineContext().cancel()
                yield()
            }
            AppResult.Success(IconResourceReclaimOutcome.COMPLETE)
        }
        runCurrent()
        queue.request("candidate")
        runCurrent()
        queue.request("candidate")
        runCurrent()
        assertEquals(2, calls)
    }

    private class Access : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        val databaseState = MutableStateFlow(SecureSessionState.UNLOCKED)
        override fun isUnlocked() = databaseState.value == SecureSessionState.UNLOCKED
    }

    private fun cleanupQueue(
        scope: CoroutineScope,
        access: Access = Access(),
        reclaim: suspend (String) -> AppResult<IconResourceReclaimOutcome>,
    ) = IconResourceCleanupQueue(scope, access, object : SessionStateProvider {
        override val lockStateFlow = access.databaseState
        override val lockState get() = lockStateFlow.value
    }, reclaim)
}
