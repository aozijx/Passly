package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationMethodsStateTest {
    @Test
    fun `later refresh resolves after earlier refresh and publishes last`() = runTest {
        val state = AuthenticationMethodsState()
        val firstRead = CompletableDeferred<Unit>()
        val finishFirst = CompletableDeferred<Unit>()
        val secondRead = CompletableDeferred<Unit>()
        val finishSecond = CompletableDeferred<Unit>()
        val previous = AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))
        val latest = AuthenticationMethods(setOf(AuthenticationMethod.RECOVERY_CODE))
        var resolveCount = 0
        val firstGeneration = state.beginRefresh()

        val first = launch {
            state.refresh(firstGeneration, resolve = {
                resolveCount++
                firstRead.complete(Unit)
                finishFirst.await()
                previous
            }, onFailure = {})
        }
        firstRead.await()
        val second = launch {
            val generation = state.beginRefresh()
            state.refresh(generation, resolve = {
                resolveCount++
                secondRead.complete(Unit)
                finishSecond.await()
                latest
            }, onFailure = {})
        }
        runCurrent()

        assertEquals(1, resolveCount)
        finishFirst.complete(Unit)
        first.join()
        secondRead.await()
        assertEquals(AuthenticationMethods(), state.methods.value)
        finishSecond.complete(Unit)
        second.join()

        assertEquals(latest, state.methods.value)
    }

    @Test
    fun `in-flight stale refresh cannot restore a consumed recovery method`() = runTest {
        val state = AuthenticationMethodsState()
        val readStarted = CompletableDeferred<Unit>()
        val finishRead = CompletableDeferred<Unit>()
        val withRecovery = AuthenticationMethods(
            setOf(AuthenticationMethod.APP_PASSWORD, AuthenticationMethod.RECOVERY_CODE)
        )
        state.refresh(state.beginRefresh(), resolve = { withRecovery }, onFailure = {})

        val refresh = launch {
            state.refresh(state.beginRefresh(), resolve = {
                readStarted.complete(Unit)
                finishRead.await()
                withRecovery
            }, onFailure = {})
        }
        readStarted.await()
        state.onRecoveryCodeConsumed()
        finishRead.complete(Unit)
        refresh.join()

        assertEquals(
            AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD)),
            state.methods.value,
        )
    }

    @Test
    fun `cancelled refresh releases ownership and propagates cancellation`() = runTest {
        val state = AuthenticationMethodsState()
        val entered = CompletableDeferred<Unit>()
        val pending = launch {
            state.refresh(state.beginRefresh(), resolve = {
                entered.complete(Unit)
                awaitCancellation()
            }, onFailure = {})
        }
        entered.await()
        pending.cancelAndJoin()
        val expected = AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))

        state.refresh(state.beginRefresh(), resolve = { expected }, onFailure = {})

        assertEquals(expected, state.methods.value)
    }

    @Test
    fun `cancelling refresh waiter does not cancel manager-owned refresh`() = runTest {
        val state = AuthenticationMethodsState()
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val expected = AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))
        val refresh = async {
            state.refresh(state.beginRefresh(), resolve = {
                entered.complete(Unit)
                finish.await()
                expected
            }, onFailure = {})
        }
        entered.await()
        val waiter = launch { refresh.await() }

        waiter.cancelAndJoin()
        assertTrue(refresh.isActive)
        finish.complete(Unit)
        refresh.await()

        assertEquals(expected, state.methods.value)
    }

    @Test
    fun `failed manager-owned refresh is logged after its waiter is cancelled`() = runTest {
        val state = AuthenticationMethodsState()
        val managerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val entered = CompletableDeferred<Unit>()
        val fail = CompletableDeferred<Unit>()
        val failure = IllegalStateException("refresh failed")
        val loggedFailures = mutableListOf<Throwable>()
        val refresh = managerScope.async {
            state.refresh(
                generation = state.beginRefresh(),
                resolve = {
                    entered.complete(Unit)
                    fail.await()
                    throw failure
                },
                onFailure = { loggedFailures += it },
            )
        }
        entered.await()
        val waiter = launch { refresh.await() }
        waiter.cancelAndJoin()
        fail.complete(Unit)

        try {
            refresh.await()
            throw AssertionError("failed refresh was swallowed")
        } catch (actual: IllegalStateException) {
            assertTrue(generateSequence<Throwable>(actual) { it.cause }.any { it === failure })
        }

        assertEquals(listOf(failure), loggedFailures)
        managerScope.cancel()
    }

    @Test
    fun `failure observer cannot replace resolver failure`() = runTest {
        val state = AuthenticationMethodsState()
        val resolverFailure = IllegalStateException("resolver failed")
        val observerFailure = IllegalArgumentException("logger failed")

        try {
            state.refresh(
                generation = state.beginRefresh(),
                resolve = { throw resolverFailure },
                onFailure = { throw observerFailure },
            )
            throw AssertionError("resolver failure was swallowed")
        } catch (actual: IllegalStateException) {
            assertSame(resolverFailure, actual)
            assertEquals(listOf(observerFailure), actual.suppressed.toList())
        }
    }

    @Test
    fun `failure observer cancellation remains primary`() = runTest {
        val state = AuthenticationMethodsState()
        val resolverFailure = IllegalStateException("resolver failed")
        val observerCancellation = CancellationException("observer cancelled")

        try {
            state.refresh(
                generation = state.beginRefresh(),
                resolve = { throw resolverFailure },
                onFailure = { throw observerCancellation },
            )
            throw AssertionError("observer cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(observerCancellation, actual)
            assertEquals(listOf(resolverFailure), actual.suppressed.toList())
        }
    }

    @Test
    fun `failure observer fatal error remains primary`() = runTest {
        val state = AuthenticationMethodsState()
        val resolverFailure = IllegalStateException("resolver failed")
        val observerFatal = AssertionError("observer failed fatally")

        try {
            state.refresh(
                generation = state.beginRefresh(),
                resolve = { throw resolverFailure },
                onFailure = { throw observerFatal },
            )
            throw AssertionError("observer fatal error was swallowed")
        } catch (actual: AssertionError) {
            assertSame(observerFatal, actual)
            assertEquals(listOf(resolverFailure), actual.suppressed.toList())
        }
    }
}
