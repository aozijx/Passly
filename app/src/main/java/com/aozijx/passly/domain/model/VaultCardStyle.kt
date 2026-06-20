package com.aozijx.passly.domain.model

import androidx.annotation.StringRes
import com.aozijx.passly.R

enum class VaultCardStyle(
    val key: String,
    @field:StringRes val displayNameRes: Int,
    @field:StringRes val descriptionRes: Int
) {
    DEFAULT(
        "default",
        R.string.settings_card_style_default_name,
        R.string.settings_card_style_default_desc
    ),
    PASSWORD(
        "password",
        R.string.settings_card_style_password_name,
        R.string.settings_card_style_password_desc
    ),
    TOTP("totp", R.string.settings_card_style_totp_name, R.string.settings_card_style_totp_desc);

    companion object {
        fun fromKey(key: String?): VaultCardStyle {
            val normalizedKey = key?.trim()?.lowercase()
            return entries.firstOrNull { it.key == normalizedKey } ?: DEFAULT
        }

        fun normalizeGlobalStyle(style: VaultCardStyle): VaultCardStyle =
            if (style in entries) style else DEFAULT
    }
}