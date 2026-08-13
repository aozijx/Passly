package com.aozijx.passly.feature.vault.components.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.data.settings.model.LibrarySortField
import com.aozijx.passly.data.settings.model.LibrarySortSpec

@StringRes
fun LibrarySortSpec.labelResId(): Int = when (field) {
    LibrarySortField.TITLE -> R.string.sort_name
    LibrarySortField.CREATED_AT -> R.string.sort_created
    LibrarySortField.UPDATED_AT -> R.string.sort_updated
    LibrarySortField.LAST_USED_AT -> R.string.sort_last_used
    LibrarySortField.USAGE_FREQUENCY -> R.string.sort_usage
    LibrarySortField.ENTRY_TYPE -> R.string.sort_entry_type
    LibrarySortField.ID -> R.string.sort_default
}
