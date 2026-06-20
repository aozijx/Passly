package com.aozijx.passly.domain.model

enum class EntryType(
    val value: Int,
    val displayName: String,
    val iconDescription: String,
    val category: EntryCategory
) {
    PASSWORD(
        0,
        "密码",
        "账号密码",
        EntryCategory.ACCOUNT
    ),
    TOTP(
        1,
        "两步验证",
        "动态验证码",
        EntryCategory.AUTHENTICATION
    ),
    PASSKEY(
        2,
        "Passkey",
        "FIDO2/WebAuthn",
        EntryCategory.AUTHENTICATION
    ),
    RECOVERY_CODE(
        4,
        "恢复码",
        "备用验证码",
        EntryCategory.AUTHENTICATION
    ),

    WIFI(
        3,
        "WiFi",
        "无线网络",
        EntryCategory.NETWORK
    ),
    SSH_KEY(
        8,
        "SSH密钥",
        "安全Shell密钥",
        EntryCategory.NETWORK
    ),

    BANK_CARD(
        5,
        "银行卡",
        "信用卡/借记卡",
        EntryCategory.FINANCE
    ),

    SEED_PHRASE(
        6,
        "助记词",
        "加密货币种子短语",
        EntryCategory.CRYPTO
    ),
    ID_CARD(
        7,
        "证件",
        "身份证/护照",
        EntryCategory.IDENTITY
    );

    companion object {
        fun fromValue(value: Int): EntryType = entries.find { it.value == value } ?: PASSWORD

        fun allByCategory(category: EntryCategory): List<EntryType> =
            entries.filter { it.category == category }
    }

    fun supportsAutofill(): Boolean = this in setOf(PASSWORD, WIFI, SSH_KEY)

    fun requiresStrongEncryption(): Boolean = this in setOf(SSH_KEY, SEED_PHRASE, PASSKEY)

    fun containsSensitiveIdentity(): Boolean = this in setOf(ID_CARD, BANK_CARD)
}

enum class EntryCategory(val displayName: String) {
    ACCOUNT("账户"),
    AUTHENTICATION("认证"),
    NETWORK("网络"),
    FINANCE("金融"),
    CRYPTO("加密"),
    IDENTITY("身份")
}