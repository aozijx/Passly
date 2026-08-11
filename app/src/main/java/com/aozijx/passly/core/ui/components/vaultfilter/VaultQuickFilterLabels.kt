package com.aozijx.passly.core.ui.components.vaultfilter

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter

val LibraryQuickFilter.titleRes: Int
    @StringRes get() = when (this) {
        LibraryQuickFilter.ALL -> R.string.tab_all
        LibraryQuickFilter.PASSWORDS -> R.string.password
        LibraryQuickFilter.TOTP -> R.string.otp
    }