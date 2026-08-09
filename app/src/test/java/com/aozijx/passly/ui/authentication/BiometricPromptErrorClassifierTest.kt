package com.aozijx.passly.ui.authentication

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.core.ui.components.auth.BiometricPromptErrorClassifier
import com.aozijx.passly.security.authentication.host.BiometricHostFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricPromptErrorClassifierTest {
    @Test
    fun unavailablePlatformErrorsAreNotReportedAsCredentialFailures() {
        val errors = listOf(
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED
        )

        errors.forEach { error ->
            assertEquals(
                BiometricHostFailure.METHOD_UNAVAILABLE,
                BiometricPromptErrorClassifier.classify(error)
            )
        }
    }

    @Test
    fun lockoutIsRateLimitedAndProcessingErrorsAreCryptoFailures() {
        assertEquals(
            BiometricHostFailure.RATE_LIMITED,
            BiometricPromptErrorClassifier.classify(BiometricPrompt.ERROR_LOCKOUT)
        )
        assertEquals(
            BiometricHostFailure.CRYPTO_OBJECT_INVALID,
            BiometricPromptErrorClassifier.classify(BiometricPrompt.ERROR_UNABLE_TO_PROCESS)
        )
    }
}
