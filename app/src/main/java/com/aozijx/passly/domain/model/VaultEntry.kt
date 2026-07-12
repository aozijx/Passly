package com.aozijx.passly.domain.model

data class VaultEntry(
    val id: String = "",
    val vaultId: String = "default",
    val entryVersion: Int = 1,
    val deletedAt: Long? = null,
    val title: String,
    val username: String,
    val password: String,
    val email: String? = null,
    override val category: String,
    val notes: String? = null,

    override val iconName: String? = null,
    override val iconCustomPath: String? = null,

    val totpSecret: String? = null,
    val totpIssuer: String? = null,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val totpAlgorithm: String = "SHA1",

    val passkeyDataJson: String? = null,
    val recoveryCodes: String? = null,
    val hardwareKeyInfo: String? = null,

    val wifiSecurityType: String? = "WPA",
    val wifiIsHidden: Boolean = false,

    val cardCvv: String? = null,
    val cardExpiration: String? = null,
    val idNumber: String? = null,

    val paymentPin: String? = null,
    val paymentPlatform: String? = null,

    val securityQuestion: String? = null,
    val securityAnswer: String? = null,

    val sshPrivateKey: String? = null,
    val cryptoSeedPhrase: String? = null,

    val entryType: Int = 0,

    override val associatedAppPackage: String? = null,
    override val associatedDomain: String? = null,
    val uriList: List<String>? = null,
    val matchType: Int = 0,
    val customFieldsJson: String? = null,
    val autoSubmit: Boolean = false,

    val strengthScore: Float? = null,
    val lastUsedAt: Long? = null,
    val usageCount: Int = 0,

    val favorite: Boolean = false,
    val tags: List<String>? = null,
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val expiresAt: Long? = null
) : VaultIconable
