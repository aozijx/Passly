package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.feature.vault.model.AddType

enum class VaultSortOption(
    val field: EntrySortField,
) {
    TITLE(EntrySortField.TITLE),
    CREATED_AT(EntrySortField.CREATED_AT),
    UPDATED_AT(EntrySortField.UPDATED_AT),
    LAST_USED_AT(EntrySortField.LAST_USED_AT),
    USAGE_FREQUENCY(EntrySortField.USAGE_FREQUENCY),
}

sealed interface VaultUiAction {
    data class SearchQueryChanged(
        val query: String,
    ) : VaultUiAction

    data class CategorySelectionChanged(val selection: VaultCategorySelection) : VaultUiAction

    data class CategorySearchQueryChanged(
        val query: String,
    ) : VaultUiAction

    data object RetryCategories : VaultUiAction

    data class SortOptionSelected(val option: VaultSortOption) : VaultUiAction

    data class FilterToggled(
        val filter: AddType,
    ) : VaultUiAction

    data object ClearFilters : VaultUiAction

    data class SearchToggled(
        val active: Boolean,
    ) : VaultUiAction

    data object ToggleShowTotpCode : VaultUiAction

    data class QuickDelete(
        val entryId: String,
    ) : VaultUiAction

    data class CopyField(
        val entryId: String,
        val entryType: EntryType,
        val fieldKey: FieldKey,
    ) : VaultUiAction

    data class CopyOtp(
        val entryId: String,
    ) : VaultUiAction
}
