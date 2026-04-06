package com.aozijx.passly.domain.model

/**
 * 字段键：定义保险库条目中支持的所有逻辑字段
 */
enum class FieldKey {
    USERNAME, PASSWORD, NOTES, URIS, TOTP_SECRET, TOTP_PERIOD, TOTP_DIGITS, TOTP_ALGORITHM, PASSKEY_DATA, RECOVERY_CODES, HARDWARE_INFO, WIFI_ENCRYPTION, WIFI_HIDDEN, CARD_EXPIRATION, CARD_CVV, PAYMENT_PIN, PAYMENT_PLATFORM, SECURITY_QUESTION, SECURITY_ANSWER, SEED_PHRASE, ID_NUMBER, SSH_KEY
}

/**
 * 字段标签：将逻辑键与 UI 显示文本关联
 */
data class FieldLabel(
    val label: String, val key: FieldKey
)
