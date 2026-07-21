package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.model.core.OtpConfig
import kotlinx.serialization.Serializable

@Serializable
data class VaultCredential(
    val entryId: String,
    val email: String? = null,
    val password: String? = null,
    val otp: OtpConfig? = null,
    val twoFactorType: String = "TOTP",
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val sshPrivateKey: String? = null,
    val sshPublicKey: String? = null,
    val sshPassphrase: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList(),
    val passkeyCredentialId: String? = null,
    val passkeyRpId: String? = null,
    val passkeyUserHandle: String? = null,
    val passkeyPrivateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null,
    val customFields: List<CustomField> = emptyList(),
    val notes: String? = null,
    val idNumber: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val wifiSecurityType: String? = null,
    val wifiIsHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)