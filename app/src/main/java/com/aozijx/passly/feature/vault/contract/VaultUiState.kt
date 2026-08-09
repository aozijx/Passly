package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.VaultQuickFilter
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.feature.vault.model.AddType

data class VaultUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedQuickFilter: VaultQuickFilter = VaultQuickFilter.ALL,
    val selectedSort: VaultSortSpec = VaultSortSpec.DEFAULT,
    val isSearchActive: Boolean = false,
    val isVaultItemsLoading: Boolean = true,
    val availableCategories: List<String> = emptyList(),
    val visibleQuickFilters: List<VaultQuickFilter> = VaultQuickFilter.resolveVisible(
        VaultQuickFilter.defaultVisibleKeys
    ),
    val vaultItemsByQuickFilter: Map<VaultQuickFilter, List<EntryListItem>> = emptyMap(),
    val showTOTPCode: Boolean = true,
    val addType: AddType? = null,
    val pendingDelete: EntryListItem? = null
)
