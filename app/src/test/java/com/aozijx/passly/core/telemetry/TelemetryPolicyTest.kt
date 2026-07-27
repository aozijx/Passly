package com.aozijx.passly.core.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryPolicyTest {
    @Test
    fun encryptedFileWindowUsesStrictExpiry() {
        val policy = TelemetryPolicy(encryptedFileEnabledUntilMs = 10_000L)

        assertTrue(policy.isEncryptedFileEnabled(nowMs = 9_999L))
        assertFalse(policy.isEncryptedFileEnabled(nowMs = 10_000L))
        assertFalse(policy.isEncryptedFileEnabled(nowMs = 10_001L))
    }
}
