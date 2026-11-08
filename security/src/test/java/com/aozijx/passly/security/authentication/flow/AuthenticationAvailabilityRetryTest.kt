package com.aozijx.passly.security.authentication.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationAvailabilityRetryTest {
    @Test
    fun `retries transient refresh failures with bounded backoff until snapshot resolves`() =
        runTest {
            var attempts = 0
            val retry =
                AuthenticationAvailabilityRetry(
                    scope = backgroundScope,
                    initialDelayMs = 10L,
                    maxDelayMs = 20L,
                ) {
                    attempts++
                    if (attempts < 3) throw IllegalStateException("temporary store failure")
                }

            retry.schedule()
            retry.schedule()
            runCurrent()
            assertEquals(0, attempts)

            advanceTimeBy(10L)
            runCurrent()
            assertEquals(1, attempts)

            advanceTimeBy(20L)
            runCurrent()
            assertEquals(2, attempts)

            advanceTimeBy(20L)
            runCurrent()
            assertEquals(3, attempts)
        }
}
