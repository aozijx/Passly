package com.aozijx.passly.domain.entry.model.credential

/** 凭据结构类型。 */
enum class EntryCredentialKind {
    NONE,
    LOGIN,
    CARD,
    IDENTITY,
    SSH,
    WIFI,
    PASSKEY,
    OTP,
}

/** 原子凭据：一条 Entry 恰好持有一种类型的 credential。 */
sealed interface EntryCredential {
    val kind: EntryCredentialKind

    data object None : EntryCredential {
        override val kind: EntryCredentialKind = EntryCredentialKind.NONE
    }
}

/** 用户自定义字段（可隐藏）。 */
data class CustomField(
    val name: String,
    val value: String,
    val kind: CustomFieldKind = CustomFieldKind.TEXT,
) {
    init {
        require(name.isNotBlank()) { "Custom field name cannot be blank" }
    }
}

enum class CustomFieldKind { TEXT, HIDDEN }
