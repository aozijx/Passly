package com.aozijx.passly.feature.vault.components.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.VaultSortField
import com.aozijx.passly.domain.settings.model.VaultSortSpec

@StringRes
fun VaultSortSpec.labelResId(): Int = when (field) {
    VaultSortField.TITLE -> R.string.sort_name
    VaultSortField.CREATED_AT -> R.string.sort_created
    VaultSortField.UPDATED_AT -> R.string.sort_updated
    VaultSortField.LAST_USED_AT -> R.string.sort_last_used
    VaultSortField.USAGE_FREQUENCY -> R.string.sort_usage
    VaultSortField.ENTRY_TYPE -> R.string.sort_entry_type
    VaultSortField.ID -> R.string.sort_default
}
