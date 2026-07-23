package com.aozijx.passly.feature.vault.contract

import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.domain.model.settings.VaultSortSpec
import com.aozijx.passly.feature.vault.model.VaultTab

data class VaultUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedTab: VaultTab = VaultTab.ALL,
    val selectedSort: VaultSortSpec = VaultSortSpec.DEFAULT,
    val isSearchActive: Boolean = false,
    val isMoreMenuExpanded: Boolean = false,
    val isVaultItemsLoading: Boolean = true,
    val availableCategories: List<String> = emptyList(),
    val visibleTabs: List<VaultTab> = VaultTab.resolveVisible(VaultTab.defaultVisibleKeys),
    val isAutoDownloadIcons: Boolean = true,
    val vaultItems: List<VaultListItem> = emptyList(),
    val vaultItemsByTab: Map<VaultTab, List<VaultListItem>> = emptyMap(),
    val showTOTPCode: Boolean = true
)
