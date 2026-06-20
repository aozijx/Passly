package com.aozijx.passly.ui.features.vault.contract

import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.model.TotpState
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.ui.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.ui.features.vault.model.SortOption
import com.aozijx.passly.ui.features.vault.model.VaultTab

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
    val isAutoDownloadIcons: Boolean = AppDefaults.DISPLAY_AUTO_DOWNLOAD_ICONS,
    val vaultItems: List<VaultSummary> = emptyList(),
    val vaultItemsByTab: Map<VaultTab, List<VaultSummary>> = emptyMap(),
    val showTOTPCode: Boolean = true,
    val totpStates: Map<Int, TotpState> = emptyMap(),
    val detailCoordinatorState: VaultDetailCoordinatorState = VaultDetailCoordinatorState()
)