package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.VaultTab

data class VaultUiState(
    val searchQuery: String = "",
    val selectedEntryTypeName: String? = null,
    val selectedCategory: String? = null,
    val selectedTab: VaultTab = VaultTab.ALL,
    val selectedSort: VaultSortSpec = VaultSortSpec.DEFAULT,
    val isSearchActive: Boolean = false,
    val isVaultItemsLoading: Boolean = true,
    val availableEntryTypes: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val visibleTabs: List<VaultTab> = VaultTab.resolveVisible(VaultTab.defaultVisibleKeys),
    val vaultItemsByTab: Map<VaultTab, List<EntryListItem>> = emptyMap(),
    val showTOTPCode: Boolean = true,
    val addType: AddType? = null,
    val pendingDelete: EntryListItem? = null
)
