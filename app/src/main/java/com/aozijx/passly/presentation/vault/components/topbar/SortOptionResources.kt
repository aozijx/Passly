package com.aozijx.passly.presentation.vault.components.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField

@StringRes
fun EntrySort.labelResId(): Int = when (field) {
    EntrySortField.TITLE -> R.string.sort_name
    EntrySortField.CREATED_AT -> R.string.sort_created
    EntrySortField.UPDATED_AT -> R.string.sort_updated
    EntrySortField.LAST_USED_AT -> R.string.sort_last_used
    EntrySortField.USAGE_FREQUENCY -> R.string.sort_usage
    EntrySortField.ENTRY_TYPE -> R.string.sort_entry_type
    EntrySortField.ID -> R.string.sort_default
}
