package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortOptionUiModel

@StringRes
fun VaultSortUiModel.labelResId(): Int = when (option) {
    VaultSortOptionUiModel.TITLE -> R.string.sort_name
    VaultSortOptionUiModel.CREATED_AT -> R.string.sort_created
    VaultSortOptionUiModel.UPDATED_AT -> R.string.sort_updated
    VaultSortOptionUiModel.LAST_USED_AT -> R.string.sort_last_used
    VaultSortOptionUiModel.USAGE_FREQUENCY -> R.string.sort_usage
}
