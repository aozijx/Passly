package com.aozijx.passly.feature.settings.appearance

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.VaultCardStyle

@StringRes
fun VaultCardStyle.displayNameRes(): Int = when (this) {
    VaultCardStyle.DEFAULT -> R.string.settings_card_style_default_name
    VaultCardStyle.PASSWORD -> R.string.settings_card_style_password_name
    VaultCardStyle.TOTP -> R.string.settings_card_style_totp_name
}

@StringRes
fun VaultCardStyle.descriptionRes(): Int = when (this) {
    VaultCardStyle.DEFAULT -> R.string.settings_card_style_default_desc
    VaultCardStyle.PASSWORD -> R.string.settings_card_style_password_desc
    VaultCardStyle.TOTP -> R.string.settings_card_style_totp_desc
}
