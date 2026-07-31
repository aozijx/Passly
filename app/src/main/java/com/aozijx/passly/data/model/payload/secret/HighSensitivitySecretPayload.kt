package com.aozijx.passly.data.model.payload.secret

import kotlinx.serialization.Serializable

@Serializable
data class HighSensitivityCardSecretPayload(
    val cardNumber: String? = null,
    val cardCvv: String? = null,
    val paymentPin: String? = null
)

@Serializable
data class HighSensitivityIdentitySecretPayload(
    val idNumber: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
)

@Serializable
data class HighSensitivitySshSecretPayload(
    val privateKey: String? = null,
    val passphrase: String? = null
)

@Serializable
data class HighSensitivityPasskeySecretPayload(
    val privateKeyReference: String? = null
)

@Serializable
data class HighSensitivityOtpSecretPayload(
    val secret: String? = null
)

@Serializable
data class HighSensitivitySecretPayload(
    val card: HighSensitivityCardSecretPayload? = null,
    val identity: HighSensitivityIdentitySecretPayload? = null,
    val ssh: HighSensitivitySshSecretPayload? = null,
    val passkey: HighSensitivityPasskeySecretPayload? = null,
    val otp: HighSensitivityOtpSecretPayload? = null,
    val schemaVersion: Int = 1
)
