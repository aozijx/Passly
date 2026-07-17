package com.aozijx.passly.feature.vault.components.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.settings.SortOption

@StringRes
fun SortOption.labelResId(): Int = when (this) {
    SortOption.DEFAULT -> R.string.sort_default
    SortOption.NAME_ASC, SortOption.NAME_DESC -> R.string.sort_name
    SortOption.CREATED_DESC, SortOption.CREATED_ASC -> R.string.sort_created
    SortOption.UPDATED_DESC, SortOption.UPDATED_ASC -> R.string.sort_updated
    SortOption.USAGE_DESC, SortOption.USAGE_ASC -> R.string.sort_usage
}
