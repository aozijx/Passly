package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.LibrarySortSpec
import com.aozijx.passly.feature.vault.model.AddType

sealed interface VaultIntent {
    data class SearchQueryChanged(val query: String) : VaultIntent
    data class CategorySelected(val category: String?) : VaultIntent
    data object ClearCategory : VaultIntent
    data class SortOptionSelected(val sort: LibrarySortSpec) : VaultIntent
    data class QuickFilterSelected(val filter: LibraryQuickFilter) : VaultIntent
    data class SearchToggled(val active: Boolean) : VaultIntent
    data object ToggleShowTotpCode : VaultIntent
    data class AddTypeSelected(val type: AddType?) : VaultIntent
    data class ItemToDeleteSelected(val item: EntryListItem?) : VaultIntent
    data object ConfirmDelete : VaultIntent
    data class QuickDelete(val item: EntryListItem) : VaultIntent
    data class AddItem(val entry: EntryAggregate) : VaultIntent
    data class UpdateEntryAggregate(val entry: EntryAggregate) : VaultIntent
    data class AddScannedOtp(val config: OtpConfig) : VaultIntent
    data class AutoUnlockTotp(val entryId: String) : VaultIntent
}