package com.aozijx.passly.feature.vault.presentation

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.LibrarySortSpec
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType

internal sealed interface VaultMutation {
    data class SearchQueryChanged(val query: String) : VaultMutation
    data class CategoryChanged(val category: String?) : VaultMutation
    data class QuickFilterChanged(val filter: LibraryQuickFilter) : VaultMutation
    data class SortChanged(val sort: LibrarySortSpec) : VaultMutation
    data class SearchVisibilityChanged(val active: Boolean) : VaultMutation
    data object TotpVisibilityToggled : VaultMutation
    data class AddTypeChanged(val type: AddType?) : VaultMutation
    data class PendingDeleteChanged(val item: EntryListItem?) : VaultMutation
    data class DeletedEntryHandled(val entryId: String) : VaultMutation
    data class VisibleQuickFiltersChanged(
        val filters: List<LibraryQuickFilter>,
    ) : VaultMutation

    data class ListChanged(
        val isLoading: Boolean,
        val categories: List<String>,
        val itemsByQuickFilter: Map<LibraryQuickFilter, List<EntryListItem>>,
    ) : VaultMutation

    data object DialogsCleared : VaultMutation
}

internal object VaultReducer {
    fun reduce(state: VaultUiState, mutation: VaultMutation): VaultUiState =
        when (mutation) {
            is VaultMutation.SearchQueryChanged -> state.copy(searchQuery = mutation.query)
            is VaultMutation.CategoryChanged -> state.copy(selectedCategory = mutation.category)
            is VaultMutation.QuickFilterChanged ->
                state.copy(selectedQuickFilter = mutation.filter)
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
                pendingDelete = state.pendingDelete?.takeUnless { it.id == mutation.entryId },
            )
            is VaultMutation.VisibleQuickFiltersChanged ->
                state.copy(visibleQuickFilters = mutation.filters)
            is VaultMutation.ListChanged -> state.copy(
                isVaultItemsLoading = mutation.isLoading,
                availableCategories = mutation.categories,
                vaultItemsByQuickFilter = mutation.itemsByQuickFilter,
            )
            VaultMutation.DialogsCleared -> state.copy(
                addType = null,
                pendingDelete = null,
            )
        }
}
