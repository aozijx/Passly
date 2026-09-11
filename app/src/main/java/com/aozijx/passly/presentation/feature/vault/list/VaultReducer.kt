package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.presentation.feature.vault.list.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType

internal sealed interface VaultMutation {
    data class SearchQueryChanged(val query: String) : VaultMutation
    data class CategoryChanged(val category: String?) : VaultMutation
    data class FilterToggled(val filter: AddType?) : VaultMutation
    data class SortChanged(val sort: EntrySort) : VaultMutation
    data class SearchVisibilityChanged(val active: Boolean) : VaultMutation
    data object TotpVisibilityToggled : VaultMutation
    data class AddTypeChanged(val type: AddType?) : VaultMutation
    data class PendingDeleteChanged(val item: EntryListItem?) : VaultMutation
    data class DeletedEntryHandled(val entryId: String) : VaultMutation
    data class CategoriesChanged(val categories: List<String>) : VaultMutation

    data object DialogsCleared : VaultMutation
}

internal object VaultReducer {
    fun reduce(state: VaultUiState, mutation: VaultMutation): VaultUiState =
        when (mutation) {
            is VaultMutation.SearchQueryChanged -> state.copy(searchQuery = mutation.query)
            is VaultMutation.CategoryChanged -> state.copy(selectedCategory = mutation.category)
            is VaultMutation.FilterToggled -> state.copy(
                selectedFilters = mutation.filter?.let { filter ->
                    state.selectedFilters.toMutableSet().apply {
                        if (!add(filter)) remove(filter)
                    }
                }.orEmpty(),
            )
            is VaultMutation.SortChanged -> state.copy(selectedSort = mutation.sort)
            is VaultMutation.SearchVisibilityChanged -> state.copy(
                isSearchActive = mutation.active,
                searchQuery = state.searchQuery.takeIf { mutation.active }.orEmpty(),
            )
            VaultMutation.TotpVisibilityToggled ->
                state.copy(showTOTPCode = !state.showTOTPCode)
            is VaultMutation.AddTypeChanged -> state.copy(addType = mutation.type)
            is VaultMutation.PendingDeleteChanged -> state.copy(pendingDelete = mutation.item)
            is VaultMutation.DeletedEntryHandled -> state.copy(
                pendingDelete = state.pendingDelete?.takeUnless { it.id.value == mutation.entryId },
            )
            is VaultMutation.CategoriesChanged ->
                state.copy(availableCategories = mutation.categories)
            VaultMutation.DialogsCleared -> state.copy(
                addType = null,
                pendingDelete = null,
            )
        }
}
