package com.aozijx.passly.domain.model

/**
 * 字段逻辑键定义
 */
enum class FieldKey {
    TITLE, USERNAME, PASSWORD, NOTES, URIS, TOTP_SECRET, TOTP_PERIOD, TOTP_DIGITS, TOTP_ALGORITHM, PASSKEY_DATA, RECOVERY_CODES, HARDWARE_INFO, WIFI_SECURITY, WIFI_HIDDEN, CARD_EXPIRATION, CARD_CVV, ID_NUMBER, PAYMENT_PIN, PAYMENT_PLATFORM, SECURITY_QUESTION, SECURITY_ANSWER, SSH_KEY, SEED_PHRASE
}

/**
 * 字段类型定义
 */
enum class FieldType {
    TEXT, PASSWORD, URL, TEXTAREA, SELECT, TOGGLE, PIN
}

/**
 * 字段定义数据类
 */
data class FieldDefinition(
    val key: String,
    val label: String,
    val isSensitive: Boolean = false,
    val isRequired: Boolean = false,
    val fieldType: FieldType = FieldType.TEXT
)

/**
 * 字段分组数据类
 */
data class FieldGroup(
    val title: String, val fields: List<FieldDefinition>
)
