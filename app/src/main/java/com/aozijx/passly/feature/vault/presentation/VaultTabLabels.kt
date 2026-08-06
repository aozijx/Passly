package com.aozijx.passly.feature.vault.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.model.VaultTab

val VaultTab.titleRes: Int
    @StringRes get() = when (this) {
        VaultTab.ALL -> R.string.tab_all
        VaultTab.PASSWORDS -> R.string.password
        VaultTab.TOTP -> R.string.otp
    }
