package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.VaultQuickFilter

sealed interface VaultIntent {
    data class SearchQueryChanged(val query: String) : VaultIntent
    data class CategorySelected(val category: String?) : VaultIntent
    data object ClearCategory : VaultIntent
    data class SortOptionSelected(val sort: VaultSortSpec) : VaultIntent
    data class QuickFilterSelected(val filter: VaultQuickFilter) : VaultIntent
    data class SearchToggled(val active: Boolean) : VaultIntent
    data object ToggleShowTotpCode : VaultIntent
    data class AddTypeSelected(val type: AddType?) : VaultIntent
    data class ItemToDeleteSelected(val item: EntryListItem?) : VaultIntent
    data object ConfirmDelete : VaultIntent
    data class QuickDelete(val item: EntryListItem) : VaultIntent
    data class AddItem(val entry: VaultEntry) : VaultIntent
    data class UpdateVaultEntry(val entry: VaultEntry) : VaultIntent
    data class AddScannedOtp(val config: OtpConfig) : VaultIntent
    data class AutoUnlockTotp(val entryId: String) : VaultIntent
}