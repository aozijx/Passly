package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialAttemptLimiter internal constructor(
    private val clock: () -> Long
) {
    @Inject
    constructor() : this(System::currentTimeMillis)

    private val states = mutableMapOf<AuthenticationMethod, AttemptState>()

    @Synchronized
    fun beforeAttempt(
        method: AuthenticationMethod,
        request: AuthenticationRequest
    ): AuthenticationFailure? {
        val state = states[method] ?: return null
        val now = clock()
        if (state.lockedUntilMs <= 0L) return null
        if (state.lockedUntilMs <= now) {
            states.remove(method)
            return null
        }
        return rateLimitedFailure(method, request, state, now)
    }

    @Synchronized
    fun recordIncorrect(
        method: AuthenticationMethod,
        request: AuthenticationRequest
    ): AuthenticationFailure {
        val previous = states[method]
        val now = clock()
        val lockedOutExpired = previous != null &&
            previous.lockedUntilMs > 0L &&
            previous.lockedUntilMs <= now
        val attempts = if (previous == null || lockedOutExpired) {
            1
        } else {
            previous.attemptCount + 1
        }.coerceAtMost(MAX_ATTEMPTS)
        val lockedUntil = if (attempts >= MAX_ATTEMPTS) now + LOCKOUT_MS else 0L
        val state = AttemptState(attemptCount = attempts, lockedUntilMs = lockedUntil)
        states[method] = state
        return if (lockedUntil > now) {
            rateLimitedFailure(method, request, state, now)
        } else {
            AuthenticationFailure(
                authCode = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
                correlationId = request.correlationId,
                method = method,
                attemptCount = attempts,
                maxAttempts = MAX_ATTEMPTS,
                remainingAttempts = MAX_ATTEMPTS - attempts
            )
        }
    }

    @Synchronized
    fun recordSuccess(method: AuthenticationMethod) {
        states.remove(method)
    }

    private fun rateLimitedFailure(
        method: AuthenticationMethod,
        request: AuthenticationRequest,
        state: AttemptState,
        nowMs: Long
    ) = AuthenticationFailure(
        authCode = AuthenticationFailureCode.RATE_LIMITED,
        correlationId = request.correlationId,
        method = method,
        attemptCount = state.attemptCount,
        maxAttempts = MAX_ATTEMPTS,
        remainingAttempts = 0,
        retryAfterMs = (state.lockedUntilMs - nowMs).coerceAtLeast(0L)
    )

    private data class AttemptState(
        val attemptCount: Int,
        val lockedUntilMs: Long
    )

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 30_000L
    }
}
