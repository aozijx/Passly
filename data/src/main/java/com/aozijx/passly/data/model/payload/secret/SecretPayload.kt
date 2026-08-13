package com.aozijx.passly.data.model.payload.secret

import kotlinx.serialization.Serializable

// --- Enum mirrors for domain OtpType, OtpHashAlgorithm, OtpSecretEncoding ---

@Serializable
enum class OtpTypePayload {
    TOTP,
    HOTP,
    STEAM
}

@Serializable
enum class OtpHashAlgorithmPayload {
    SHA1,
    SHA256,
    SHA512
}

@Serializable
enum class OtpSecretEncodingPayload {
    BASE32,
    BASE64
}

// --- OtpConfig DTO ---

@Serializable
data class OtpConfigPayload(
    val type: OtpTypePayload = OtpTypePayload.TOTP,
    val secret: String,
    val algorithm: OtpHashAlgorithmPayload = OtpHashAlgorithmPayload.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: OtpSecretEncodingPayload = OtpSecretEncodingPayload.BASE32,
    val issuer: String? = null,
    val accountName: String? = null
)

// --- Secret data payloads ---

@Serializable
data class LoginSecretPayload(
    val email: String? = null,
    val password: String? = null
)

@Serializable
data class CardSecretPayload(
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val hasCardNumber: Boolean = false,
    val hasCardCvv: Boolean = false,
    val hasPaymentPin: Boolean = false
)

@Serializable
data class IdentitySecretPayload(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
)

@Serializable
data class SshSecretPayload(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
)

@Serializable
data class WifiSecretPayload(
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false
)

@Serializable
data class PasskeySecretPayload(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
)

@Serializable
data class OtpSecretPayload(
    val config: OtpConfigPayload? = null
)

@Serializable
data class CustomFieldPayload(
    val name: String,
    val value: String,
    val type: Int = 0
)

// --- Flat root payload ---

/**
 * 原子凭据载荷。
 *
 * [EntryEntity.entryType] is the discriminator. Repository policy permits at
 * most one typed payload slot; notes and custom fields are common extensions.
 * The reset development schema starts again at payload version 1.
 */
@Serializable
data class SecretPayload(
    val login: LoginSecretPayload? = null,
    val notes: String? = null,
    val card: CardSecretPayload? = null,
    val identity: IdentitySecretPayload? = null,
    val ssh: SshSecretPayload? = null,
    val wifi: WifiSecretPayload? = null,
    val passkey: PasskeySecretPayload? = null,
    val otp: OtpSecretPayload? = null,
    val customFields: List<CustomFieldPayload> = emptyList(),
    val schemaVersion: Int = 1
)
