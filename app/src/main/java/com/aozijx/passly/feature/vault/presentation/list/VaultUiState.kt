package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.feature.vault.model.AddType

data class VaultUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedFilters: Set<AddType> = emptySet(),
    val selectedSort: EntrySort? = null,
    val isSearchActive: Boolean = false,
    val availableCategories: List<String> = emptyList(),
    val showTOTPCode: Boolean = true,
    val pendingDelete: EntryListItem? = null
)
