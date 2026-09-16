package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.feature.vault.model.AddType

sealed interface VaultUiAction {
    data class SearchQueryChanged(val query: String) : VaultUiAction
    data class CategorySelected(val category: String?) : VaultUiAction
    data object ClearCategory : VaultUiAction
    data class SortOptionSelected(val sort: EntrySort) : VaultUiAction
    data class FilterToggled(val filter: AddType?) : VaultUiAction
    data class SearchToggled(val active: Boolean) : VaultUiAction
    data object ToggleShowTotpCode : VaultUiAction
    data class AddTypeSelected(val type: AddType?) : VaultUiAction
    data class ItemToDeleteSelected(val item: EntryListItem?) : VaultUiAction
    data object ConfirmDelete : VaultUiAction
    data class QuickDelete(val entryId: String) : VaultUiAction
    data class CopyField(
        val entryId: String,
        val entryType: EntryType,
        val fieldKey: FieldKey,
    ) : VaultUiAction
    data class CopyOtp(val entryId: String) : VaultUiAction
    data class AddScannedOtp(val config: OtpConfig) : VaultUiAction
}
