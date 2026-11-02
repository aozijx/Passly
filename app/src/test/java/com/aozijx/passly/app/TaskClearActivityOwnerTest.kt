package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskClearActivityOwnerTest {
    @Test
    fun `pause releases claim for replacement owner and cancels prior delay`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val firstScheduler = RecordingScheduler()
        val secondScheduler = RecordingScheduler()
        var firstCloseCount = 0
        var secondCloseCount = 0
        val first = TaskClearActivityOwner(requests, firstScheduler, {}, { firstCloseCount++ })
        val second = TaskClearActivityOwner(requests, secondScheduler, {}, { secondCloseCount++ })

        first.onResumed()
        requests.requestClear()
        firstScheduler.runPosted()
        first.onPaused()
        second.onResumed()
        secondScheduler.runPosted()
        secondScheduler.runDelayed()

        assertEquals(0, firstCloseCount)
        assertEquals(1, secondCloseCount)
    }

    @Test
    fun `stale delayed close cannot finish after successful authentication`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val scheduler = RecordingScheduler()
        var closeCount = 0
        val owner = TaskClearActivityOwner(requests, scheduler, {}, { closeCount++ })

        owner.onResumed()
        requests.requestClear()
        scheduler.runPosted()
        val staleCallback = scheduler.delayed.single()
        val authenticated = AuthenticationState.AutofillAuthenticated(2L)
        session.authenticationState.value = authenticated
        requests.onAuthenticationStateChanged(authenticated)
        staleCallback.run()

        assertEquals(0, closeCount)
    }

    @Test
    fun `failed authentication returning to locked resumes delayed close`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val scheduler = RecordingScheduler()
        var closeCount = 0
        val owner = TaskClearActivityOwner(requests, scheduler, {}, { closeCount++ })

        owner.onResumed()
        requests.requestClear()
        scheduler.runPosted()
        val firstDelay = scheduler.delayed.single()
        session.authenticationState.value = AuthenticationState.AwaitingHost(AuthenticationRequestId.new())
        firstDelay.run()

        assertEquals(0, closeCount)
        session.authenticationState.value = AuthenticationState.Locked
        requests.onAuthenticationStateChanged(AuthenticationState.Locked)
        scheduler.runPosted()
        scheduler.runDelayed()

        assertEquals(1, closeCount)
    }

    @Test
    fun `request delivered during authentication is retried when locked returns`() {
        val session = TestSessionAccessState(AuthenticationState.Locked)
        val requests = ActiveTaskClearRequests(session)
        val scheduler = RecordingScheduler()
        var closeCount = 0
        val owner = TaskClearActivityOwner(requests, scheduler, {}, { closeCount++ })

        owner.onResumed()
        requests.requestClear()
        session.authenticationState.value = AuthenticationState.AwaitingHost(AuthenticationRequestId.new())
        scheduler.runPosted()

        assertEquals(0, closeCount)
        session.authenticationState.value = AuthenticationState.Locked
        requests.onAuthenticationStateChanged(AuthenticationState.Locked)
        scheduler.runPosted()
        scheduler.runDelayed()

        assertEquals(1, closeCount)
    }

    private class RecordingScheduler : TaskClearCallbackScheduler {
        private val posted = mutableListOf<Runnable>()
        val delayed = mutableListOf<Runnable>()
        private val removed = mutableSetOf<Runnable>()

        override fun post(callback: Runnable) {
            posted += callback
        }

        override fun postDelayed(
            callback: Runnable,
            delayMs: Long,
        ) {
            assertEquals(1_000L, delayMs)
            delayed += callback
        }

        override fun remove(callback: Runnable) {
            removed += callback
        }

        fun runPosted() {
            val pending = posted.toList()
            posted.clear()
            pending.filterNot(removed::contains).forEach(Runnable::run)
        }

        fun runDelayed() {
            val pending = delayed.toList()
            delayed.clear()
            pending.filterNot(removed::contains).forEach(Runnable::run)
        }
    }

    private class TestSessionAccessState(
        initial: AuthenticationState,
    ) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(initial)

        override fun isUnlocked(): Boolean = authenticationState.value is AuthenticationState.Authenticated
    }
}
