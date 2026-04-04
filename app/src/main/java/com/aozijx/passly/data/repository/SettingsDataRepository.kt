package com.aozijx.passly.data.repository

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.data.local.AppPrefs
import com.aozijx.passly.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsDataRepository(private val prefs: AppPrefs) : SettingsRepository {
    override val lockTimeout: Flow<Long> = prefs.lockTimeout
    override val isBiometricEnabled: Flow<Boolean> = prefs.isBiometricEnabled
    override val isDarkMode: Flow<Boolean?> = prefs.isDarkMode
    override val isDynamicColor: Flow<Boolean> = prefs.isDynamicColor

    override val isSwipeEnabled: Flow<Boolean> = prefs.isSwipeEnabled
    override val swipeLeftAction: Flow<SwipeActionType> = prefs.swipeLeftAction
    override val swipeRightAction: Flow<SwipeActionType> = prefs.swipeRightAction

    override val isStatusBarAutoHide: Flow<Boolean> = prefs.isStatusBarAutoHide
    override val isTopBarCollapsible: Flow<Boolean> = prefs.isTopBarCollapsible
    override val isTabBarCollapsible: Flow<Boolean> = prefs.isTabBarCollapsible
    override val isSecureContentEnabled: Flow<Boolean> = prefs.isSecureContentEnabled
    override val isFlipToLockEnabled: Flow<Boolean> = prefs.isFlipToLockEnabled
    override val isFlipExitAndClearStackEnabled: Flow<Boolean> = prefs.isFlipExitAndClearStackEnabled
    override val cardStyle: Flow<VaultCardStyle> = prefs.cardStyle
    override val autofillUiMode: Flow<AutofillUiMode> = prefs.autofillUiMode

    override suspend fun setLockTimeout(timeoutMs: Long) {
        prefs.setLockTimeout(timeoutMs)
    }
    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.setBiometricEnabled(enabled)
    }
    override suspend fun setDarkMode(enabled: Boolean?) {
        prefs.setDarkMode(enabled)
    }
    override suspend fun setDynamicColor(enabled: Boolean) {
        prefs.setDynamicColor(enabled)
    }

    override suspend fun setSwipeEnabled(enabled: Boolean) {
        prefs.setSwipeEnabled(enabled)
    }
    override suspend fun setSwipeLeftAction(action: SwipeActionType) {
        prefs.setSwipeLeftAction(action)
    }
    override suspend fun setSwipeRightAction(action: SwipeActionType) {
        prefs.setSwipeRightAction(action)
    }

    override suspend fun setStatusBarAutoHide(autoHide: Boolean) {
        prefs.setStatusBarAutoHide(autoHide)
    }
    override suspend fun setTopBarCollapsible(collapsible: Boolean) {
        prefs.setTopBarCollapsible(collapsible)
    }
    override suspend fun setTabBarCollapsible(collapsible: Boolean) {
        prefs.setTabBarCollapsible(collapsible)
    }
    override suspend fun setSecureContentEnabled(enabled: Boolean) {
        prefs.setSecureContentEnabled(enabled)
    }
    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        prefs.setFlipToLockEnabled(enabled)
    }
    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) {
        prefs.setFlipExitAndClearStackEnabled(enabled)
    }
    override suspend fun setCardStyle(style: VaultCardStyle) {
        prefs.setCardStyle(style)
    }
    override suspend fun setAutofillUiMode(mode: AutofillUiMode) {
        prefs.setAutofillUiMode(mode)
    }
}
