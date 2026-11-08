package com.aozijx.passly.security.authentication.method.credential

import com.aozijx.passly.domain.access.model.AuthenticationAttemptState
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialAttemptLimiterTest {
    @Test
    fun `attempt count survives limiter recreation and lockout counts down`() = runTest {
        var now = time()
        val store = InMemoryAttemptStore()
        val request = request()
        val firstLimiter = CredentialAttemptLimiter(store) { now }

        repeat(5) {
            assertNull(firstLimiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request))
            firstLimiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        }

        now = now.copy(epochMs = now.epochMs + 90_000L)
        val recreatedLimiter = CredentialAttemptLimiter(store) { now }
        val sameBoot = recreatedLimiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(AuthenticationFailureCode.RATE_LIMITED, sameBoot?.code)
        assertEquals(30_000L, sameBoot?.retryAfterMs)

        now = now.copy(elapsedRealtimeMs = now.elapsedRealtimeMs + 1_000L)
        assertEquals(29_000L, recreatedLimiter.status(AuthenticationMethod.APP_PASSWORD).retryAfterMs)
    }

    @Test
    fun `attempt reservation survives process death during password verification`() = runTest {
        val now = time()
        val store = InMemoryAttemptStore()
        val request = request()
        val limiter = CredentialAttemptLimiter(store) { now }

        repeat(5) {
            assertNull(limiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request))
        }

        val recreatedLimiter = CredentialAttemptLimiter(store) { now }

        assertEquals(30_000L, recreatedLimiter.status(AuthenticationMethod.APP_PASSWORD).retryAfterMs)
        assertEquals(
            AuthenticationFailureCode.RATE_LIMITED,
            recreatedLimiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request)?.code,
        )
    }

    @Test
    fun `reboot restores remaining time from wall clock and expiry resets attempts`() = runTest {
        var now = time()
        val store = InMemoryAttemptStore()
        val limiter = CredentialAttemptLimiter(store) { now }
        val request = request()

        repeat(5) {
            limiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request)
            limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        }

        now = now.copy(
            epochMs = now.epochMs + 10_000L,
            elapsedRealtimeMs = 2_000L,
            bootCount = now.bootCount + 1,
        )
        assertEquals(20_000L, limiter.status(AuthenticationMethod.APP_PASSWORD).retryAfterMs)

        now = now.copy(epochMs = now.epochMs + 20_000L)
        val expired = limiter.status(AuthenticationMethod.APP_PASSWORD)
        assertNull(expired.retryAfterMs)
        assertEquals(0, expired.attempts.used)

        assertNull(limiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request))
        val next = limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(1, next.attempts.used)
    }

    @Test
    fun `credential methods retain independent counts and success clears only its method`() = runTest {
        val store = InMemoryAttemptStore()
        val limiter = CredentialAttemptLimiter(store) { time() }
        val request = request()
        repeat(3) {
            limiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request)
            limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        }
        limiter.reserveAttempt(AuthenticationMethod.RECOVERY_CODE, request)
        limiter.recordIncorrect(AuthenticationMethod.RECOVERY_CODE, request)

        limiter.recordSuccess(AuthenticationMethod.APP_PASSWORD, request)

        assertEquals(0, limiter.status(AuthenticationMethod.APP_PASSWORD).attempts.used)
        assertEquals(1, limiter.status(AuthenticationMethod.RECOVERY_CODE).attempts.used)
    }

    @Test
    fun `storage failures fail closed and cancellation is propagated`() = runTest {
        val request = request()
        val failedStore = InMemoryAttemptStore().apply { failWith = IllegalStateException("disk unavailable") }
        val limiter = CredentialAttemptLimiter(failedStore) { time() }

        assertEquals(
            AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE,
            limiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request)?.code,
        )
        assertEquals(
            AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE,
            limiter.reserveAttempt(AuthenticationMethod.APP_PASSWORD, request)?.code,
        )
        assertEquals(
            AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE,
            limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request).code,
        )
        assertEquals(
            AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE,
            limiter.recordSuccess(AuthenticationMethod.APP_PASSWORD, request)?.code,
        )

        failedStore.failWith = CancellationException("cancelled")
        val cancellation =
            try {
                limiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request)
                null
            } catch (error: CancellationException) {
                error
            }
        assertEquals("cancelled", cancellation?.message)
    }

    private class InMemoryAttemptStore : AuthenticationAttemptStore {
        private val mutex = Mutex()
        private val states = mutableMapOf<AuthenticationMethod, AuthenticationAttemptState>()
        var failWith: Exception? = null

        override suspend fun update(
            method: AuthenticationMethod,
            transform: (AuthenticationAttemptState) -> AuthenticationAttemptState,
        ): AuthenticationAttemptState = mutex.withLock {
            failWith?.let { throw it }
            transform(states[method] ?: AuthenticationAttemptState()).also { states[method] = it }
        }
    }

    private fun request() = AuthenticationRequest(purpose = AuthenticationPurpose.UNLOCK_VAULT)

    private fun time() = CredentialAttemptTime(epochMs = 1_000_000L, elapsedRealtimeMs = 100_000L, bootCount = 10)
}
