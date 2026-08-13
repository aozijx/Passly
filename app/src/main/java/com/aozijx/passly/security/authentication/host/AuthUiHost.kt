package com.aozijx.passly.security.authentication.host

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.security.authentication.SecretChars

data class AuthHostSnapshot(
    val resumed: Boolean,
    val finishing: Boolean,
    val destroyed: Boolean
) {
    val usable: Boolean get() = resumed && !finishing && !destroyed
}

data class BiometricPromptSpec(
    val purpose: AuthenticationPurpose,
    val allowDeviceCredential: Boolean = false
)

sealed interface BiometricHostResult {
    data class Success(val result: BiometricPrompt.AuthenticationResult) : BiometricHostResult
    data class Cancelled(val byUser: Boolean) : BiometricHostResult
    data class Failure(
        val reason: BiometricHostFailure,
        val platformCode: Int? = null
    ) : BiometricHostResult
    data object HostUnavailable : BiometricHostResult
}

enum class BiometricHostFailure {
    METHOD_UNAVAILABLE,
    RATE_LIMITED,
    CRYPTO_OBJECT_INVALID,
    AUTHENTICATION_FAILED
}

sealed interface SecretHostResult {
    data class Submitted(val secret: SecretChars) : SecretHostResult
    data class Cancelled(val byUser: Boolean) : SecretHostResult
    data object HostUnavailable : SecretHostResult
}

interface AuthUiHost {
    val ownerId: String
    fun snapshot(): AuthHostSnapshot

    suspend fun chooseMethod(
        purpose: AuthenticationPurpose,
        methods: List<AuthenticationMethod>
    ): AuthenticationMethod?

    suspend fun requestSecret(
        purpose: AuthenticationPurpose,
        method: AuthenticationMethod
    ): SecretHostResult

    suspend fun authenticateBiometric(
        spec: BiometricPromptSpec,
        cryptoObject: BiometricPrompt.CryptoObject?
    ): BiometricHostResult
}
