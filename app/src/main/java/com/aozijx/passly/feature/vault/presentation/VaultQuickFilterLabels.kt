package com.aozijx.passly.feature.vault.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.model.VaultQuickFilter

val VaultQuickFilter.titleRes: Int
    @StringRes get() = when (this) {
        VaultQuickFilter.ALL -> R.string.tab_all
        VaultQuickFilter.PASSWORDS -> R.string.password
        VaultQuickFilter.TOTP -> R.string.otp
    }
