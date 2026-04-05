package com.aozijx.passly.domain.repository

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.ui.VaultCardStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val lockTimeout: Flow<Long>
    val isBiometricEnabled: Flow<Boolean>
    val isDarkMode: Flow<Boolean?>
    val isDynamicColor: Flow<Boolean>

    val isSwipeEnabled: Flow<Boolean>
    val swipeLeftAction: Flow<SwipeActionType>
    val swipeRightAction: Flow<SwipeActionType>

    val isStatusBarAutoHide: Flow<Boolean>
    val isTopBarCollapsible: Flow<Boolean>
    val isTabBarCollapsible: Flow<Boolean>
    val isSecureContentEnabled: Flow<Boolean>
    val isFlipToLockEnabled: Flow<Boolean>
    val isFlipExitAndClearStackEnabled: Flow<Boolean>
    val cardStyle: Flow<VaultCardStyle>
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>>
    val autofillUiMode: Flow<AutofillUiMode>
    val backupDirectoryUri: Flow<String?>
    val lastBackupExportFileName: Flow<String?>

    suspend fun setLockTimeout(timeoutMs: Long)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setDarkMode(enabled: Boolean?)
    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setSwipeEnabled(enabled: Boolean)
    suspend fun setSwipeLeftAction(action: SwipeActionType)
    suspend fun setSwipeRightAction(action: SwipeActionType)

    suspend fun setStatusBarAutoHide(autoHide: Boolean)
    suspend fun setTopBarCollapsible(collapsible: Boolean)
    suspend fun setTabBarCollapsible(collapsible: Boolean)
    suspend fun setSecureContentEnabled(enabled: Boolean)
    suspend fun setFlipToLockEnabled(enabled: Boolean)
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean)
    suspend fun setCardStyle(style: VaultCardStyle)
    suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle)
    suspend fun setAutofillUiMode(mode: AutofillUiMode)
    suspend fun setBackupDirectoryUri(uri: String)
    suspend fun clearBackupDirectoryUri()
    suspend fun setLastBackupExportFileName(fileName: String)
}
