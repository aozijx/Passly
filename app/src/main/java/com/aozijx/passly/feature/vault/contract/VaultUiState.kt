package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.feature.vault.model.AddType

data class VaultUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedQuickFilter: LibraryQuickFilter = LibraryQuickFilter.ALL,
    val selectedSort: EntrySort = EntrySort.DEFAULT,
    val isSearchActive: Boolean = false,
    val availableCategories: List<String> = emptyList(),
    val visibleQuickFilters: List<LibraryQuickFilter> = LibraryQuickFilter.resolveVisible(
        LibraryQuickFilter.defaultVisibleKeys
    ),
    val showTOTPCode: Boolean = true,
    val addType: AddType? = null,
    val pendingDelete: EntryListItem? = null
)
