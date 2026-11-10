package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.feature.vault.model.AddType

data class VaultUiState(
    val search: VaultSearchInput = VaultSearchInput.Closed,
    val categorySelection: VaultCategorySelection = VaultCategorySelection.All,
    val selectedFilters: Set<AddType> = emptySet(),
    val categories: VaultCategoriesState = VaultCategoriesState.Loading,
    val categorySearchQuery: String = "",
    val showTotpCode: Boolean = true,
)

sealed interface VaultSearchInput {
    val query: String
    val isActive: Boolean

    data object Closed : VaultSearchInput {
        override val query = ""
        override val isActive = false
    }

    data class Open(override val query: String) : VaultSearchInput {
        override val isActive = true
    }
}

sealed interface VaultCategoriesState {
    data object Loading : VaultCategoriesState

    data object ReadyEmpty : VaultCategoriesState

    data class Ready(
        val values: List<String>,
    ) : VaultCategoriesState {
        init {
            require(values.isNotEmpty()) { "Ready categories must contain at least one value" }
        }
    }

    data object Failed : VaultCategoriesState
}
