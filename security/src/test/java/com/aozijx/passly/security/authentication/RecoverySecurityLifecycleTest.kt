package com.aozijx.passly.security.authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoverySecurityLifecycleTest {

    @Test
    fun `successfully consumed recovery code cannot be consumed again`() = runBlocking {
        var recoveryEnvelopePresent = true
        var rollbackCount = 0
        val consume = suspend {
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
    fun `failed recovery code consumption seals staged session`() = runBlocking {
        var sealed = false

        val consumed = consumeRecoveryCodeOrRollback(
            consume = { error("persistent envelope deletion failed") },
            rollback = { sealed = true }
        )

        assertFalse(consumed)
        assertTrue(sealed)
    }

    @Test
    fun `cancelled recovery code consumption still seals staged session`() = runBlocking {
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
    fun `recovery password provisioning seals before availability refresh`() = runBlocking {
        val events = mutableListOf<String>()

        finishRecoveryPasswordProvisioning(
            seal = { events += "sealed" },
            refreshAvailability = { events += "refreshed" }
        )

        assertEquals(listOf("sealed", "refreshed"), events)
    }

    @Test
    fun `availability refresh failure cannot reopen completed recovery session`() = runBlocking {
        var sealed = false

        finishRecoveryPasswordProvisioning(
            seal = { sealed = true },
            refreshAvailability = { error("derived availability refresh failed") }
        )

        assertTrue(sealed)
    }
}
