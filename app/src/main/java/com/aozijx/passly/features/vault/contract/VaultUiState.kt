package com.aozijx.passly.features.vault.contract

import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.vault.model.SortOption
import com.aozijx.passly.features.vault.model.VaultTab

data class VaultUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedTab: VaultTab = VaultTab.ALL,
    val selectedSort: SortOption = SortOption.DEFAULT,
    val isSearchActive: Boolean = false,
    val isMoreMenuExpanded: Boolean = false,
    val isVaultItemsLoading: Boolean = true,
    val availableCategories: List<String> = emptyList(),
    val visibleTabs: List<VaultTab> = VaultTab.resolveVisible(VaultTab.defaultVisibleKeys),
    val isAutoDownloadIcons: Boolean = true,
    val vaultItems: List<VaultSummary> = emptyList(),
    val vaultItemsByTab: Map<VaultTab, List<VaultSummary>> = emptyMap(),
    val showTOTPCode: Boolean = true,
    val totpStates: Map<Int, TotpState> = emptyMap(),
    val detailCoordinatorState: VaultDetailCoordinatorState = VaultDetailCoordinatorState()
)