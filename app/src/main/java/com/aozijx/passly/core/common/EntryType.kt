package com.aozijx.passly.core.common

/**
 * 业务领域类型（Domain Model）
 * 完全独立于 UI 层，用于驱动业务逻辑
 *
 * 设计原则：
 * - 不依赖任何 Android 组件
 * - 不依赖 UI 逻辑
 * - 用于业务规则和验证
 */
enum class EntryType(
    val value: Int,
    val displayName: String,
    val iconDescription: String,
    val category: EntryCategory
) {
    // 账户相关
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

    // 网络/系统相关
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

    // 金融相关
    BANK_CARD(
        5,
        "银行卡",
        "信用卡/借记卡",
        EntryCategory.FINANCE
    ),

    // 加密/身份相关
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

    /**
     * 获取该类型是否支持自动填充
     */
    fun supportsAutofill(): Boolean = this in setOf(PASSWORD, WIFI, SSH_KEY)

    /**
     * 获取该类型是否需要特殊的加密处理
     */
    fun requiresStrongEncryption(): Boolean = this in setOf(SSH_KEY, SEED_PHRASE, PASSKEY)

    /**
     * 获取该类型是否包含敏感的身份信息
     */
    fun containsSensitiveIdentity(): Boolean = this in setOf(ID_CARD, BANK_CARD)
}

/**
 * 条目类别（分组维度）
 * 用于组织和过滤相关的条目类型
 */
enum class EntryCategory(val displayName: String) {
    ACCOUNT("账户"),
    AUTHENTICATION("认证"),
    NETWORK("网络"),
    FINANCE("金融"),
    CRYPTO("加密"),
    IDENTITY("身份")
}
