package com.aozijx.passly.security.authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoverySecurityLifecycleTest {
    @Test
    fun `successfully consumed recovery code cannot be consumed again`() =
        runBlocking {
            var recoveryEnvelopePresent = true
            var rollbackCount = 0
            val consume =
                suspend {
                    check(recoveryEnvelopePresent) { "recovery envelope is already consumed" }
                    recoveryEnvelopePresent = false
                }

            val firstUse = consumeRecoveryCodeOrRollback(consume) { rollbackCount++ }
            val secondUse = consumeRecoveryCodeOrRollback(consume) { rollbackCount++ }

            assertTrue(firstUse)
            assertFalse(secondUse)
            assertFalse(recoveryEnvelopePresent)
            assertEquals(1, rollbackCount)
        }

    @Test
    fun `failed recovery code consumption seals staged session`() =
        runBlocking {
            var sealed = false

            val consumed =
                consumeRecoveryCodeOrRollback(
                    consume = { error("persistent envelope deletion failed") },
                    rollback = { sealed = true },
                )

            assertFalse(consumed)
            assertTrue(sealed)
        }

    @Test
    fun `cancelled recovery code consumption still seals staged session`() =
        runBlocking {
            var sealed = false
            var cancelled = false

            try {
                consumeRecoveryCodeOrRollback(
                    consume = { throw CancellationException("cancelled") },
                    rollback = { sealed = true },
                )
            } catch (_: CancellationException) {
                cancelled = true
            }

            assertTrue(cancelled)
            assertTrue(sealed)
        }

    @Test
    fun `fatal recovery consumption survives rollback failure`() =
        runBlocking {
            val fatal = AssertionError("recovery envelope deletion fatal")
            val rollbackFailure = IllegalStateException("rollback failed")
            var sealed = false

            val actual =
                runCatching {
                    consumeRecoveryCodeOrRollback(
                        consume = { throw fatal },
                        rollback = {
                            sealed = true
                            throw rollbackFailure
                        },
                    )
                }.exceptionOrNull()

            assertTrue(actual is AssertionError)
            assertEquals(fatal.message, actual?.message)
            assertTrue(sealed)
            assertEquals(1, fatal.suppressed.size)
            assertEquals(rollbackFailure.message, fatal.suppressed.single().message)
        }

    @Test
    fun `rollback fatal supersedes ordinary recovery consumption failure`() =
        runBlocking {
            val consumeFailure = IllegalStateException("recovery envelope deletion failed")
            val rollbackFatal = AssertionError("rollback fatal")

            val actual =
                runCatching {
                    consumeRecoveryCodeOrRollback(
                        consume = { throw consumeFailure },
                        rollback = { throw rollbackFatal },
                    )
                }.exceptionOrNull()

            assertTrue(actual is AssertionError)
            assertEquals(rollbackFatal.message, actual?.message)
            assertEquals(1, actual!!.suppressed.size)
            assertEquals(consumeFailure.message, actual.suppressed.single().message)
        }

    @Test
    fun `recovery password provisioning seals before availability refresh`() =
        runBlocking {
            val events = mutableListOf<String>()

            finishRecoveryPasswordProvisioning(
                seal = { events += "sealed" },
                refreshAvailability = { events += "refreshed" },
            )

            assertEquals(listOf("sealed", "refreshed"), events)
        }

    @Test
    fun `availability refresh failure cannot reopen completed recovery session`() =
        runBlocking {
            var sealed = false

            finishRecoveryPasswordProvisioning(
                seal = { sealed = true },
                refreshAvailability = { error("derived availability refresh failed") },
            )

            assertTrue(sealed)
        }

    @Test
    fun `availability refresh fatal error propagates only after recovery session is sealed`() =
        runBlocking {
            val fatal = AssertionError("fatal refresh failure")
            var sealed = false
            val actual =
                runCatching {
                    finishRecoveryPasswordProvisioning(
                        seal = { sealed = true },
                        refreshAvailability = { throw fatal },
                    )
                }.exceptionOrNull()

            assertSame(fatal, actual)
            assertTrue(sealed)
        }
}
