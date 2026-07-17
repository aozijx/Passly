package com.aozijx.passly.domain.repository.settings

import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.domain.model.settings.SortOption
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import kotlinx.coroutines.flow.Flow

/**
 * 可移植设置（跨设备同步）：UI/显示/交互偏好。
 */
data class PortableSettings(
    val isDarkMode: Boolean?,
    val isDynamicColor: Boolean,
    val themeColor: String,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val visibleVaultTabs: Set<String>?,
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val autofillUiMode: AutofillUiMode,
    val tabBarMaxTabsWithoutScroll: Int,
    val isAutoDownloadIcons: Boolean,
    val faviconDownloadWhitelist: Set<String>,
    val vaultSortOption: SortOption
)

interface PortableRepository {
    fun getSettingsFlow(): Flow<PortableSettings>

    // 外观
    val isDarkMode: Flow<Boolean?>
    val isDynamicColor: Flow<Boolean>
    val cardStyle: Flow<VaultCardStyle>
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>>
    val themeColor: Flow<String>

    // 行为
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
    val faviconDownloadWhitelist: Flow<Set<String>>
    val vaultSortOption: Flow<SortOption>

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
    suspend fun setFaviconDownloadWhitelist(whitelist: Set<String>)
    suspend fun setVaultSortOption(sort: SortOption)
    suspend fun setThemeColor(color: String)
}
