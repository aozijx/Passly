package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.feature.vault.model.AddType

sealed interface VaultUiAction {
    data class SearchQueryChanged(val query: String) : VaultUiAction
    data class CategorySelected(val category: String?) : VaultUiAction
    data object ClearCategory : VaultUiAction
    data class SortOptionSelected(val sort: EntrySort) : VaultUiAction
    data class QuickFilterSelected(val filter: LibraryQuickFilter) : VaultUiAction
    data class SearchToggled(val active: Boolean) : VaultUiAction
    data object ToggleShowTotpCode : VaultUiAction
    data class AddTypeSelected(val type: AddType?) : VaultUiAction
    data class ItemToDeleteSelected(val item: EntryListItem?) : VaultUiAction
    data object ConfirmDelete : VaultUiAction
    data class QuickDelete(val item: EntryListItem) : VaultUiAction
    data class AddItem(val entry: Entry) : VaultUiAction
    data class UpdateEntry(val entry: Entry) : VaultUiAction
    data class AddScannedOtp(val config: OtpConfig) : VaultUiAction
    data class AutoUnlockTotp(val entryId: String) : VaultUiAction
}
