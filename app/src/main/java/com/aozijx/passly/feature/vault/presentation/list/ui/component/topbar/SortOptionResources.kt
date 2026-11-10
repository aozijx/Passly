package com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.list.VaultSortOption

@StringRes
fun VaultSortOption.labelResId(): Int =
    when (this) {
        VaultSortOption.TITLE -> R.string.sort_name
        VaultSortOption.CREATED_AT -> R.string.sort_created
        VaultSortOption.UPDATED_AT -> R.string.sort_updated
        VaultSortOption.LAST_USED_AT -> R.string.sort_last_used
        VaultSortOption.USAGE_FREQUENCY -> R.string.sort_usage
    }
