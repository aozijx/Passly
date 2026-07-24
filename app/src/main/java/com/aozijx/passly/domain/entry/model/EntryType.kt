package com.aozijx.passly.domain.entry.model

enum class EntryType(val displayName: String) {
    LOGIN("登录"),
    NOTE("笔记"),
    CARD("卡片"),
    IDENTITY("身份"),
    SSH_KEY("SSH密钥"),
    WIFI("Wi-Fi"),
    PASSKEY("Passkey"),
    TOTP("TOTP"),
    PASSPORT("护照"),
    LICENSE("证件"),
    DATABASE("数据库"),
    SERVER("服务器"),
    API_KEY("API密钥"),
    CRYPTO_WALLET("加密钱包"),
    BANK_CARD("银行卡"),
    ID_CARD("身份证"),
    SEED_PHRASE("助记词"),
    RECOVERY_CODE("恢复码");

    companion object {
        fun fromName(name: String): EntryType =
            entries.find { it.name == name } ?: LOGIN
    }
}
