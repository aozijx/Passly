package com.aozijx.passly.domain.entry.model.credential

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

data class LoginCredential(
    val email: String? = null,
    val password: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.LOGIN
}

data class CardCredential(
    val cardType: String? = null,
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardHolder: String? = null,
    val paymentPin: String? = null,
    val paymentPlatform: String? = null,
    val billingAddress: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.CARD
}

data class IdentityCredential(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.IDENTITY
}

data class SshCredential(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.SSH
}

data class WifiCredential(
    val ssid: String,
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false
) : EntryCredential {
    init {
        require(ssid.isNotBlank()) { "Wi-Fi SSID cannot be blank" }
    }

    override val kind: EntryCredentialKind = EntryCredentialKind.WIFI
}

data class PasskeyCredential(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.PASSKEY
}

data class OtpCredential(
    val config: OtpConfig
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.OTP
}
