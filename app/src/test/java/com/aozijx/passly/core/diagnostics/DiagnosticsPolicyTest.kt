package com.aozijx.passly.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsPolicyTest {
    @Test
    fun fileLoggingWindowExpiresAtItsDeadline() {
        val policy = DiagnosticsPolicy(fileLoggingEnabledUntilMs = 1_000L)

        assertTrue(policy.isFileLoggingEnabled(nowMs = 999L))
        assertFalse(policy.isFileLoggingEnabled(nowMs = 1_000L))
    }
}
