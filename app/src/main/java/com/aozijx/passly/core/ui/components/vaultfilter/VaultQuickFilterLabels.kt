package com.aozijx.passly.core.ui.components.vaultfilter

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.VaultQuickFilter

val VaultQuickFilter.titleRes: Int
    @StringRes get() = when (this) {
        VaultQuickFilter.ALL -> R.string.tab_all
        VaultQuickFilter.PASSWORDS -> R.string.password
        VaultQuickFilter.TOTP -> R.string.otp
    }