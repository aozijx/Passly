package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequest
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
                code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
                requestId = request.id,
                method = method,
                attempts = com.aozijx.passly.domain.access.model.AttemptStatus(
                    used = attempts,
                    limit = MAX_ATTEMPTS,
                ),
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
        code = AuthenticationFailureCode.RATE_LIMITED,
        requestId = request.id,
        method = method,
        attempts = com.aozijx.passly.domain.access.model.AttemptStatus(
            used = state.attemptCount,
            limit = MAX_ATTEMPTS,
        ),
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
