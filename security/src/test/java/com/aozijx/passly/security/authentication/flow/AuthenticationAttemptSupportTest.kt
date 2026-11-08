package com.aozijx.passly.security.authentication.flow
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationAttemptSupportTest {
    @Test
    fun `fatal authentication error survives trace and restore failures`() =
        runTest {
            val fatal = AssertionError("executor fatal")
            val traceFailure = IllegalStateException("trace failure")
            val restoreFailure = IllegalArgumentException("restore failure")
            val recorded = mutableListOf<Throwable>()
            var restored = false

            val actual =
                runCatching {
                    recoverAuthenticationFailure(
                        failure = fatal,
                        requestId = REQUEST_ID,
                        opensSession = true,
                        recordFailure = {
                            recorded += it
                            throw traceFailure
                        },
                        restoreState = {
                            restored = true
                            throw restoreFailure
                        },
                    )
                }.exceptionOrNull()

            assertSame(fatal, actual)
            assertEquals(listOf(fatal), recorded)
            assertTrue(restored)
            assertEquals(2, fatal.suppressed.size)
            assertSame(traceFailure, fatal.suppressed[0])
            assertSame(restoreFailure, fatal.suppressed[1])
        }

    @Test
    fun `ordinary authentication exception restores session and maps to failure`() =
        runTest {
            val failure = IllegalStateException("session transition")
            var restored = false

            val result =
                recoverAuthenticationFailure(
                    failure = failure,
                    requestId = REQUEST_ID,
                    opensSession = true,
                    recordFailure = {},
                    restoreState = { restored = true },
                )

            assertEquals(
                AuthenticationResult.Failure(
                    com.aozijx.passly.domain.access.model.AuthenticationFailure(
                        AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                        REQUEST_ID,
                    ),
                ),
                result,
            )
            assertTrue(restored)
        }

    @Test
    fun `fatal restore error supersedes ordinary authentication failure`() =
        runTest {
            val failure = IllegalStateException("executor exception")
            val restoreFatal = AssertionError("restore fatal")
            val actual =
                runCatching {
                    recoverAuthenticationFailure(
                        failure = failure,
                        requestId = REQUEST_ID,
                        opensSession = true,
                        recordFailure = {},
                        restoreState = { throw restoreFatal },
                    )
                }.exceptionOrNull()

            assertSame(restoreFatal, actual)
            assertEquals(1, restoreFatal.suppressed.size)
            assertEquals(failure.message, restoreFatal.suppressed.single().message)
        }

    @Test
    fun `cancellation remains primary while session restoration is attempted`() =
        runTest {
            val cancelled = CancellationException("caller cancelled")
            val restoreFailure = AssertionError("restore fatal")
            var restored = false
            val actual =
                runCatching {
                    recoverAuthenticationFailure(
                        failure = cancelled,
                        requestId = REQUEST_ID,
                        opensSession = true,
                        recordFailure = {},
                        restoreState = {
                            restored = true
                            throw restoreFailure
                        },
                    )
                }.exceptionOrNull()

            assertSame(cancelled, actual)
            assertTrue(restored)
            assertEquals(1, actual!!.suppressed.size)
            assertEquals(restoreFailure.message, actual.suppressed.single().message)
        }

    @Test
    fun `session restoration completes after caller cancellation`() =
        runTest {
            val restored = CompletableDeferred<Boolean>()
            val attempt =
                launch {
                    try {
                        throw CancellationException("caller cancelled")
                    } catch (cancelled: CancellationException) {
                        recoverAuthenticationFailure(
                            failure = cancelled,
                            requestId = REQUEST_ID,
                            opensSession = true,
                            recordFailure = {},
                            restoreState = {
                                delay(1)
                                restored.complete(true)
                            },
                        )
                    }
                }

            attempt.join()

            assertTrue(restored.await())
        }

    @Test
    fun `consumed credentials are cleared after fatal executor failure`() =
        runTest {
            val chars = "secret".toCharArray()
            val fatal = AssertionError("executor fatal")
            var consumedCredential: CharArray? = null

            val actual =
                runCatching {
                    withConsumedCredential(AuthInput.AppPassword.from(chars)) {
                        consumedCredential = it
                        throw fatal
                    }
                }.exceptionOrNull()

            assertSame(fatal, actual)
            assertTrue(consumedCredential!!.all { it == '\u0000' })
        }

    @Test
    fun `credential is cleared when authentication exits before executor`() =
        runTest {
            val chars = "secret".toCharArray()
            var consumedCredential: CharArray? = null

            val result =
                withConsumedCredential(AuthInput.AppPassword.from(chars)) {
                    consumedCredential = it
                    "session reused"
                }

            assertEquals("session reused", result)
            assertTrue(consumedCredential!!.all { it == '\u0000' })
        }

    @Test
    fun `authentication request slot releases after fatal error`() =
        runTest {
            val slot = AuthenticationRequestSlot()
            val fatal = AssertionError("executor fatal")
            val firstStarted = CompletableDeferred<Unit>()
            val continueFirst = CompletableDeferred<Unit>()
            val first =
                async {
                    slot.withSlot(onBusy = { "busy" }) {
                        firstStarted.complete(Unit)
                        continueFirst.await()
                        "first"
                    }
                }
            firstStarted.await()

            assertEquals("busy", slot.withSlot(onBusy = { "busy" }) { "incorrectly acquired" })
            continueFirst.complete(Unit)
            assertEquals("first", first.await())
            assertSame(
                fatal,
                runCatching {
                    slot.withSlot(onBusy = { "busy" }) { throw fatal }
                }.exceptionOrNull(),
            )
            assertEquals("available", slot.withSlot(onBusy = { "busy" }) { "available" })
            assertFalse(first.isActive)
        }

    @Test
    fun `busy authentication request clears its credential`() =
        runTest {
            val slot = AuthenticationRequestSlot()
            val chars = "secret".toCharArray()
            var consumedCredential: CharArray? = null

            val busyResult =
                slot.withSlot(
                    onBusy = { "unexpected outer busy" },
                ) {
                    withConsumedCredential(AuthInput.AppPassword.from(chars)) {
                        consumedCredential = it
                        slot.withSlot(onBusy = { "busy" }) { "unexpected acquisition" }
                    }
                }

            assertEquals("busy", busyResult)
            assertTrue(consumedCredential!!.all { it == '\u0000' })
        }

    private companion object {
        val REQUEST_ID = AuthenticationRequestId("auth-attempt")
    }
}
