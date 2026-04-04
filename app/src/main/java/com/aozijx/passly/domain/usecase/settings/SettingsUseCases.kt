package com.aozijx.passly.domain.usecase.settings

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsUseCases(private val repository: SettingsRepository) {
    val lockTimeout: Flow<Long> = repository.lockTimeout
    val isBiometricEnabled: Flow<Boolean> = repository.isBiometricEnabled
    val isDarkMode: Flow<Boolean?> = repository.isDarkMode
    val isDynamicColor: Flow<Boolean> = repository.isDynamicColor

    val isSwipeEnabled: Flow<Boolean> = repository.isSwipeEnabled
    val swipeLeftAction: Flow<SwipeActionType> = repository.swipeLeftAction
    val swipeRightAction: Flow<SwipeActionType> = repository.swipeRightAction

    val isStatusBarAutoHide: Flow<Boolean> = repository.isStatusBarAutoHide
    val isTopBarCollapsible: Flow<Boolean> = repository.isTopBarCollapsible
    val isTabBarCollapsible: Flow<Boolean> = repository.isTabBarCollapsible
    val isSecureContentEnabled: Flow<Boolean> = repository.isSecureContentEnabled
    val isFlipToLockEnabled: Flow<Boolean> = repository.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled: Flow<Boolean> = repository.isFlipExitAndClearStackEnabled
    val cardStyle: Flow<VaultCardStyle> = repository.cardStyle
    val autofillUiMode: Flow<AutofillUiMode> = repository.autofillUiMode

    suspend fun setLockTimeout(timeoutMs: Long) = repository.setLockTimeout(timeoutMs)
    suspend fun setBiometricEnabled(enabled: Boolean) = repository.setBiometricEnabled(enabled)
    suspend fun setDarkMode(enabled: Boolean?) = repository.setDarkMode(enabled)
    suspend fun setDynamicColor(enabled: Boolean) = repository.setDynamicColor(enabled)

    suspend fun setSwipeEnabled(enabled: Boolean) = repository.setSwipeEnabled(enabled)
    suspend fun setSwipeLeftAction(action: SwipeActionType) = repository.setSwipeLeftAction(action)
    suspend fun setSwipeRightAction(action: SwipeActionType) = repository.setSwipeRightAction(action)

    suspend fun setStatusBarAutoHide(autoHide: Boolean) = repository.setStatusBarAutoHide(autoHide)
    suspend fun setTopBarCollapsible(collapsible: Boolean) = repository.setTopBarCollapsible(collapsible)
    suspend fun setTabBarCollapsible(collapsible: Boolean) = repository.setTabBarCollapsible(collapsible)
    suspend fun setSecureContentEnabled(enabled: Boolean) = repository.setSecureContentEnabled(enabled)
    suspend fun setFlipToLockEnabled(enabled: Boolean) = repository.setFlipToLockEnabled(enabled)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = repository.setFlipExitAndClearStackEnabled(enabled)
    suspend fun setCardStyle(style: VaultCardStyle) = repository.setCardStyle(style)
    suspend fun setAutofillUiMode(mode: AutofillUiMode) = repository.setAutofillUiMode(mode)
}
