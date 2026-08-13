package com.aozijx.passly.feature.backup.internal.archive.model

import kotlinx.serialization.Serializable

/**
 * Passly backup document v1 wire models.
 *
 * These types are intentionally independent from Room payloads and Domain
 * models. Their serialized names and meanings are frozen for document v1.
 */
@Serializable
data class BackupWebsiteRecord(
    val primaryUrl: String? = null,
    val matchDomains: Set<String> = emptySet(),
    val packageNames: Set<String> = emptySet()
)

@Serializable
data class BackupSummaryRecord(
    val title: String,
    val username: String,
    val website: BackupWebsiteRecord? = null,
    val icon: String? = null,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String? = null,
    val expiresAt: Long? = null
)

@Serializable
data class BackupLoginSecret(
    val email: String? = null,
    val password: String? = null
)

@Serializable
data class BackupCardSecret(
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null
)

@Serializable
data class BackupIdentitySecret(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
)

@Serializable
data class BackupSshSecret(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
)

@Serializable
data class BackupWifiSecret(
    val password: String? = null,
    val securityType: String? = null,
    val hidden: Boolean = false
)

@Serializable
data class BackupPasskeySecret(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
)

@Serializable
enum class BackupOtpType {
    TOTP,
    HOTP,
    STEAM
}

@Serializable
enum class BackupOtpAlgorithm {
    SHA1,
    SHA256,
    SHA512
}

@Serializable
enum class BackupOtpEncoding {
    BASE32,
    BASE64
}

@Serializable
data class BackupOtpConfig(
    val type: BackupOtpType = BackupOtpType.TOTP,
    val secret: String,
    val algorithm: BackupOtpAlgorithm = BackupOtpAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int? = 30,
    val counter: Long? = null,
    val encoding: BackupOtpEncoding = BackupOtpEncoding.BASE32,
    val issuer: String? = null,
    val accountName: String? = null
)

@Serializable
data class BackupOtpSecret(
    val config: BackupOtpConfig? = null
)

@Serializable
data class BackupCustomField(
    val name: String,
    val value: String,
    val type: Int = 0
)

@Serializable
data class BackupSecretRecord(
    val login: BackupLoginSecret? = null,
    val notes: String? = null,
    val card: BackupCardSecret? = null,
    val identity: BackupIdentitySecret? = null,
    val ssh: BackupSshSecret? = null,
    val wifi: BackupWifiSecret? = null,
    val passkey: BackupPasskeySecret? = null,
    val otp: BackupOtpSecret? = null,
    val customFields: List<BackupCustomField> = emptyList()
)
