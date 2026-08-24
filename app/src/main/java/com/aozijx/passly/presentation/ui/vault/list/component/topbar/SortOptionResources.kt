package com.aozijx.passly.presentation.ui.vault.list.component.topbar

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortOptionUiModel

@StringRes
fun VaultSortUiModel.labelResId(): Int = when (option) {
    VaultSortOptionUiModel.DEFAULT -> R.string.sort_last_used
    VaultSortOptionUiModel.TITLE -> R.string.sort_name
    VaultSortOptionUiModel.CREATED_AT -> R.string.sort_created
    VaultSortOptionUiModel.UPDATED_AT -> R.string.sort_updated
    VaultSortOptionUiModel.USAGE_FREQUENCY -> R.string.sort_usage
}
