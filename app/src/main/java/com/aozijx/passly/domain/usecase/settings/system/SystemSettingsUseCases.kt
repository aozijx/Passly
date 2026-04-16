package com.aozijx.passly.domain.usecase.settings.system

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.domain.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * 系统级设置用例：负责全局行为、界面样式、自动填充模式等非业务逻辑设置
 */
class SystemSettingsUseCases(private val repository: SettingsRepository) {
    // 界面相关
    val isDarkMode: Flow<Boolean?> = repository.isDarkMode
    val isDynamicColor: Flow<Boolean> = repository.isDynamicColor
    val cardStyle: Flow<VaultCardStyle> = repository.cardStyle
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> = repository.cardStyleByEntryType
    val isStatusBarAutoHide: Flow<Boolean> = repository.isStatusBarAutoHide
    val isTopBarCollapsible: Flow<Boolean> = repository.isTopBarCollapsible
    val isTabBarCollapsible: Flow<Boolean> = repository.isTabBarCollapsible
    
    // 交互与自动填充
    val isSwipeEnabled: Flow<Boolean> = repository.isSwipeEnabled
    val swipeLeftAction: Flow<SwipeActionType> = repository.swipeLeftAction
    val swipeRightAction: Flow<SwipeActionType> = repository.swipeRightAction
    val autofillUiMode: Flow<AutofillUiMode> = repository.autofillUiMode

    // 保险箱 Tab 可见性
    val visibleVaultTabs: Flow<Set<String>?> = repository.visibleVaultTabs

    // 数据与下载
    val isAutoDownloadIcons: Flow<Boolean> = repository.isAutoDownloadIcons

    // 操作方法
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
    suspend fun setAutoDownloadIcons(enabled: Boolean) = repository.setAutoDownloadIcons(enabled)
}