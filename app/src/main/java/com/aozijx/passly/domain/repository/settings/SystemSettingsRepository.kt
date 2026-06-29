package com.aozijx.passly.domain.repository.settings

import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.ui.features.vault.model.SortOption
import kotlinx.coroutines.flow.Flow

interface SystemSettingsRepository {
    val isDarkMode: Flow<Boolean?>
    val isDynamicColor: Flow<Boolean>
    val cardStyle: Flow<VaultCardStyle>
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>>
    val isStatusBarAutoHide: Flow<Boolean>
    val isTopBarCollapsible: Flow<Boolean>
    val isTabBarCollapsible: Flow<Boolean>
    val isSwipeEnabled: Flow<Boolean>
    val swipeLeftAction: Flow<SwipeActionType>
    val swipeRightAction: Flow<SwipeActionType>
    val autofillUiMode: Flow<AutofillUiMode>
    val visibleVaultTabs: Flow<Set<String>?>
    val tabBarMaxTabsWithoutScroll: Flow<Int>
    val isAutoDownloadIcons: Flow<Boolean>
    val vaultSortOption: Flow<SortOption>
    val languageCode: Flow<String>

    suspend fun setDarkMode(enabled: Boolean?)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setCardStyle(style: VaultCardStyle)
    suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle)
    suspend fun setStatusBarAutoHide(autoHide: Boolean)
    suspend fun setTopBarCollapsible(collapsible: Boolean)
    suspend fun setTabBarCollapsible(collapsible: Boolean)
    suspend fun setSwipeEnabled(enabled: Boolean)
    suspend fun setSwipeLeftAction(action: SwipeActionType)
    suspend fun setSwipeRightAction(action: SwipeActionType)
    suspend fun setAutofillUiMode(mode: AutofillUiMode)
    suspend fun setVisibleVaultTabs(keys: Set<String>)
    suspend fun setTabBarMaxTabsWithoutScroll(maxTabs: Int)
    suspend fun setAutoDownloadIcons(enabled: Boolean)
    suspend fun setVaultSortOption(sort: SortOption)
    suspend fun setLanguageCode(code: String)
}