package com.aozijx.passly.domain.usecase.settings

import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.domain.model.settings.SortOption
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.domain.repository.settings.PortableRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 可移植设置用例：UI/显示/交互偏好（可跨设备同步）。
 */
@Singleton
class PortableSettingsUseCases @Inject constructor(private val repository: PortableRepository) {
    // 外观
    val isDarkMode: Flow<Boolean?> = repository.isDarkMode
    val isDynamicColor: Flow<Boolean> = repository.isDynamicColor
    val cardStyle: Flow<VaultCardStyle> = repository.cardStyle
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> = repository.cardStyleByEntryType
    val themeColor: Flow<String> = repository.themeColor

    // 行为
    val isStatusBarAutoHide: Flow<Boolean> = repository.isStatusBarAutoHide
    val isTopBarCollapsible: Flow<Boolean> = repository.isTopBarCollapsible
    val isTabBarCollapsible: Flow<Boolean> = repository.isTabBarCollapsible
    val isSwipeEnabled: Flow<Boolean> = repository.isSwipeEnabled
    val swipeLeftAction: Flow<SwipeActionType> = repository.swipeLeftAction
    val swipeRightAction: Flow<SwipeActionType> = repository.swipeRightAction
    val autofillUiMode: Flow<AutofillUiMode> = repository.autofillUiMode
    val visibleVaultTabs: Flow<Set<String>?> = repository.visibleVaultTabs
    val tabBarMaxTabsWithoutScroll: Flow<Int> = repository.tabBarMaxTabsWithoutScroll
    val isAutoDownloadIcons: Flow<Boolean> = repository.isAutoDownloadIcons
    val faviconDownloadWhitelist: Flow<Set<String>> = repository.faviconDownloadWhitelist
    val vaultSortOption: Flow<SortOption> = repository.vaultSortOption
    val statusBarNotificationsEnabled: Flow<Boolean> = repository.statusBarNotificationsEnabled
    val iconDownloadNotificationsEnabled: Flow<Boolean> =
        repository.iconDownloadNotificationsEnabled
    val clipboardClearToastsEnabled: Flow<Boolean> = repository.clipboardClearToastsEnabled
    val appCloseToastsEnabled: Flow<Boolean> = repository.appCloseToastsEnabled

    suspend fun setDarkMode(enabled: Boolean?) = repository.setDarkMode(enabled)
    suspend fun setDynamicColor(enabled: Boolean) = repository.setDynamicColor(enabled)
    suspend fun setCardStyle(style: VaultCardStyle) = repository.setCardStyle(style)
    suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) =
        repository.setCardStyleForEntryType(entryTypeValue, style)
    suspend fun setStatusBarAutoHide(autoHide: Boolean) = repository.setStatusBarAutoHide(autoHide)
    suspend fun setTopBarCollapsible(collapsible: Boolean) =
        repository.setTopBarCollapsible(collapsible)
    suspend fun setTabBarCollapsible(collapsible: Boolean) =
        repository.setTabBarCollapsible(collapsible)
    suspend fun setSwipeEnabled(enabled: Boolean) = repository.setSwipeEnabled(enabled)
    suspend fun setSwipeLeftAction(action: SwipeActionType) = repository.setSwipeLeftAction(action)
    suspend fun setSwipeRightAction(action: SwipeActionType) = repository.setSwipeRightAction(action)
    suspend fun setAutofillUiMode(mode: AutofillUiMode) = repository.setAutofillUiMode(mode)
    suspend fun setVisibleVaultTabs(keys: Set<String>) = repository.setVisibleVaultTabs(keys)
    suspend fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) =
        repository.setTabBarMaxTabsWithoutScroll(maxTabs)
    suspend fun setAutoDownloadIcons(enabled: Boolean) = repository.setAutoDownloadIcons(enabled)
    suspend fun setFaviconDownloadWhitelist(whitelist: Set<String>) =
        repository.setFaviconDownloadWhitelist(whitelist)
    suspend fun setVaultSortOption(sort: SortOption) = repository.setVaultSortOption(sort)
    suspend fun setThemeColor(color: String) = repository.setThemeColor(color)
    suspend fun setStatusBarNotificationsEnabled(enabled: Boolean) =
        repository.setStatusBarNotificationsEnabled(enabled)

    suspend fun setIconDownloadNotificationsEnabled(enabled: Boolean) =
        repository.setIconDownloadNotificationsEnabled(enabled)

    suspend fun setClipboardClearToastsEnabled(enabled: Boolean) =
        repository.setClipboardClearToastsEnabled(enabled)

    suspend fun setAppCloseToastsEnabled(enabled: Boolean) =
        repository.setAppCloseToastsEnabled(enabled)
}
