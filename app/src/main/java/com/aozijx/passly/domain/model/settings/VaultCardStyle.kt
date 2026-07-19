package com.aozijx.passly.domain.model.settings

enum class AutofillUiMode { SYSTEM_INLINE, BOTTOM_SHEET }

enum class SwipeActionType { DELETE, DETAIL, COPY_PASSWORD, COPY_USERNAME }

enum class VaultCardStyle(val key: String) {
    DEFAULT("default"),
    PASSWORD("password"),
    TOTP("totp");

    companion object {
        fun fromKey(key: String?): VaultCardStyle {
            val normalizedKey = key?.trim()?.lowercase()
            return entries.firstOrNull { it.key == normalizedKey } ?: DEFAULT
        }

        fun normalizeGlobalStyle(style: VaultCardStyle): VaultCardStyle =
            if (style in entries) style else DEFAULT
    }
}
