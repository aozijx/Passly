package com.aozijx.passly.feature.autofill.presentation.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillActivityResultClaimTest {
    @Test
    fun `fatal failure after platform commit marker keeps the claim terminal`() {
        val claim = AutofillActivityResultClaim()
        val fatal = AssertionError("post-commit diagnostics failed")
        var platformDeliveries = 0
        var thrown: AssertionError? = null

        try {
            claim.commit { markPlatformCommitted ->
                platformDeliveries++
                markPlatformCommitted()
                throw fatal
            }
        } catch (error: AssertionError) {
            thrown = error
        }

        assertSame(fatal, thrown)
        assertEquals(1, platformDeliveries)
        assertTrue("a delivered platform result cannot be reclaimed for cancellation", claim.isClaimed)
        assertFalse(claim.commit { platformDeliveries++ })
        assertEquals(1, platformDeliveries)
    }

    @Test
    fun `failed platform callback releases claim for cancellation while success remains terminal`() {
        val claim = AutofillActivityResultClaim()

        try {
            claim.commit { throw IllegalStateException("platform callback failed") }
            throw AssertionError("expected platform callback failure")
        } catch (_: IllegalStateException) {
            assertFalse(claim.isClaimed)
        }

        var cancelResultCommits = 0
        assertTrue(
            claim.commit { markPlatformCommitted ->
                cancelResultCommits++
                markPlatformCommitted()
            },
        )
        assertTrue(claim.isClaimed)
        assertFalse(claim.commit { cancelResultCommits++ })
        assertTrue("the fallback result should commit only once", cancelResultCommits == 1)
    }
}
