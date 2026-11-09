package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.list.VaultUiState

internal sealed interface VaultMutation {
    data class SearchQueryChanged(
        val query: String,
    ) : VaultMutation

    data class CategoryChanged(
        val category: String?,
    ) : VaultMutation

    data class CategorySearchQueryChanged(
        val query: String,
    ) : VaultMutation

    data class FilterToggled(
        val filter: AddType?,
    ) : VaultMutation

    data class SearchVisibilityChanged(
        val active: Boolean,
    ) : VaultMutation

    data object TotpVisibilityToggled : VaultMutation

    data class CategoriesStateChanged(
        val categories: VaultCategoriesState,
    ) : VaultMutation

}

internal object VaultReducer {
    fun reduce(
        state: VaultUiState,
        mutation: VaultMutation,
    ): VaultUiState =
        when (mutation) {
            is VaultMutation.SearchQueryChanged -> {
                state.copy(searchQuery = mutation.query)
            }

            is VaultMutation.CategoryChanged -> {
                state.copy(selectedCategory = mutation.category?.takeIf(String::isNotBlank))
            }

            is VaultMutation.CategorySearchQueryChanged -> {
                state.copy(categorySearchQuery = mutation.query)
            }

            is VaultMutation.FilterToggled -> {
                state.copy(
                    selectedFilters =
                        mutation.filter
                            ?.let { filter ->
                                state.selectedFilters.toMutableSet().apply {
                                    if (!add(filter)) remove(filter)
                                }
                            }.orEmpty(),
                )
            }

            is VaultMutation.SearchVisibilityChanged -> {
                state.copy(
                    isSearchActive = mutation.active,
                    searchQuery = state.searchQuery.takeIf { mutation.active }.orEmpty(),
                )
            }

            VaultMutation.TotpVisibilityToggled -> {
                state.copy(showTOTPCode = !state.showTOTPCode)
            }

            is VaultMutation.CategoriesStateChanged -> {
                state.copy(categories = mutation.categories)
            }

        }
}
