package com.aozijx.passly.domain.entry.model

/**
 * 字段标识枚举，用于策略模式中标识条目的具体字段。
 */
enum class FieldKey {
    TITLE,
    USERNAME,
    PASSWORD,
    EMAIL,
    NOTES,
    URIS,

    TOTP_SECRET,
    TOTP_ISSUER,
    TOTP_PERIOD,
    TOTP_DIGITS,
    TOTP_ALGORITHM,

    PASSKEY_DATA,
    RECOVERY_CODES,
    HARDWARE_INFO,
    SSH_KEY,
    SEED_PHRASE,

    CARD_NUMBER,
    CARD_EXPIRATION,
    CARD_CVV,
    PAYMENT_PIN,
    PAYMENT_PLATFORM,
    SECURITY_QUESTION,
    SECURITY_ANSWER,

    ID_NUMBER,

    WIFI_SECURITY,
    WIFI_HIDDEN
}
