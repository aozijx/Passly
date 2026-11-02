package com.aozijx.passly.security.authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricRotationCommitCleanupTest {
    @Test
    fun `successful cleanup reports complete`() =
        runTest {
            assertEquals(
                BiometricRotationCleanupOutcome.COMPLETE,
                finishCommittedBiometricRotation { true },
            )
        }

    @Test
    fun `unsuccessful cleanup reports pending without reverting commit`() =
        runTest {
            assertEquals(
                BiometricRotationCleanupOutcome.PENDING,
                finishCommittedBiometricRotation { false },
            )
        }

    @Test
    fun `recoverable cleanup exception reports pending`() =
        runTest {
            assertEquals(
                BiometricRotationCleanupOutcome.PENDING,
                finishCommittedBiometricRotation { throw IllegalStateException("cleanup failed") },
            )
        }

    @Test
    fun `cancellation and fatal errors propagate`() =
        runTest {
            withContext(NonCancellable) {
                val cancelled = CancellationException("cancelled")
                try {
                    finishCommittedBiometricRotation { throw cancelled }
                    error("cancellation was swallowed")
                } catch (actual: CancellationException) {
                    assertEquals(cancelled.message, actual.message)
                }

                val fatal = AssertionError("fatal")
                try {
                    finishCommittedBiometricRotation { throw fatal }
                    error("fatal error was swallowed")
                } catch (actual: AssertionError) {
                    assertEquals(fatal.message, actual.message)
                }
            }
        }
}
