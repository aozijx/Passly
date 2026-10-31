package com.aozijx.passly.feature.autofill.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityResultClaimTest {
    @Test
    fun `failed platform callback releases claim for cancellation while success remains terminal`() {
        val claim = ActivityResultClaim()

        try {
            claim.commit { throw IllegalStateException("platform callback failed") }
            throw AssertionError("expected platform callback failure")
        } catch (_: IllegalStateException) {
            assertFalse(claim.isClaimed)
        }

        var cancelResultCommits = 0
        assertTrue(claim.commit { cancelResultCommits++ })
        assertTrue(claim.isClaimed)
        assertFalse(claim.commit { cancelResultCommits++ })
        assertTrue("the fallback result should commit only once", cancelResultCommits == 1)
    }
}
