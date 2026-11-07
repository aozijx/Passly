package com.aozijx.passly.security.authentication.method.biometric
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAliasDeletionTest {
    @Test
    fun `ordinary keystore exception is a retryable deletion failure`() {
        assertFalse(attemptBiometricAliasDeletion { throw IllegalStateException("keystore unavailable") })
    }

    @Test
    fun `fatal keystore error propagates`() {
        val fatal = AssertionError("keystore fatal")
        try {
            attemptBiometricAliasDeletion { throw fatal }
            error("fatal error was hidden")
        } catch (actual: AssertionError) {
            assertSame(fatal, actual)
        }
    }

    @Test
    fun `successful deletion is reported`() {
        assertTrue(attemptBiometricAliasDeletion {})
    }
}
