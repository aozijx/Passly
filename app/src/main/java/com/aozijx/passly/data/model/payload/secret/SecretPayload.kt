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
    val password: String? = null,
    val notes: String? = null
)

@Serializable
data class CardSecretPayload(
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null
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

@Serializable
data class VaultDataPayload(
    val customFields: List<CustomFieldPayload> = emptyList(),
    val notes: String? = null
)

// --- Sealed class root ---

@Serializable
sealed class SecretPayload {

    @Serializable
    data class Login(val data: LoginSecretPayload) : SecretPayload()

    @Serializable
    data class Note(val notes: String) : SecretPayload()

    @Serializable
    data class Card(val data: CardSecretPayload) : SecretPayload()

    @Serializable
    data class Identity(val data: IdentitySecretPayload) : SecretPayload()

    @Serializable
    data class SshKey(val data: SshSecretPayload) : SecretPayload()

    @Serializable
    data class Wifi(val data: WifiSecretPayload) : SecretPayload()

    @Serializable
    data class Passkey(val data: PasskeySecretPayload) : SecretPayload()

    @Serializable
    data class Otp(val data: OtpSecretPayload) : SecretPayload()

    @Serializable
    data class VaultData(val data: VaultDataPayload) : SecretPayload()
}
