package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialAttemptLimiterTest {
    @Test
    fun incorrectAttemptsReportRemainingRetriesAndThenRateLimit() {
        var now = 1_000L
        val limiter = CredentialAttemptLimiter(clock = { now })
        val request = request()

        val first = limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(AuthenticationFailureCode.CREDENTIAL_INCORRECT, first.code)
        assertEquals(1, first.attempts.used)
        assertEquals(4, first.attempts.remaining)
        assertNull(limiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request))

        repeat(3) {
            limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        }

        val locked = limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(AuthenticationFailureCode.RATE_LIMITED, locked.code)
        assertEquals(CredentialAttemptLimiter.MAX_ATTEMPTS, locked.attempts.used)
        assertEquals(30_000L, locked.retryAfterMs)

        now += 1_000L
        val blocked = limiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(AuthenticationFailureCode.RATE_LIMITED, blocked?.code)
        assertEquals(29_000L, blocked?.retryAfterMs)
    }

    @Test
    fun successClearsAttemptState() {
        val limiter = CredentialAttemptLimiter(clock = { 1_000L })
        val request = request()

        limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        limiter.recordSuccess(AuthenticationMethod.APP_PASSWORD)

        assertNull(limiter.beforeAttempt(AuthenticationMethod.APP_PASSWORD, request))
        val next = limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        assertEquals(1, next.attempts.used)
    }

    @Test
    fun methodsHaveSeparateAttemptState() {
        val limiter = CredentialAttemptLimiter(clock = { 1_000L })
        val request = request()

        repeat(CredentialAttemptLimiter.MAX_ATTEMPTS) {
            limiter.recordIncorrect(AuthenticationMethod.APP_PASSWORD, request)
        }

        assertNull(limiter.beforeAttempt(AuthenticationMethod.RECOVERY_CODE, request))
    }

    private fun request() = AuthenticationRequest(
        purpose = AuthenticationPurpose.UNLOCK_VAULT,
    )
}
