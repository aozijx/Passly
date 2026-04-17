package com.aozijx.passly.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.backup.BackupCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lockTimeout: Long = 60000L,
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val cardStyle: VaultCardStyle = VaultCardStyle.styleConfig.globalDefaultStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.styleConfig.globalDefaultStyle),
    val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val backupDirectoryUri: String? = null,
    val lastBackupExportFileName: String? = null,
    val visibleVaultTabs: Set<String>? = null,
    val isAutoDownloadIcons: Boolean = true
)

private data class CoreSettingsFlowState(
    val lockTimeout: Long,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSecureContentEnabled: Boolean
)

private data class SecurityAndStyleFlowState(
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>
)

private data class AutofillAndSwipeFlowState(
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val visibleVaultTabs: Set<String>?,
    val isAutoDownloadIcons: Boolean
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val systemSettingsUseCases = AppContainer.domain.systemSettingsUseCases
    private val securitySettingsUseCases = AppContainer.domain.securitySettingsUseCases
    private val backupSettingsUseCases = AppContainer.domain.backupSettingsUseCases
    private val backupUseCases = AppContainer.domain.backupUseCases

    val backup = BackupCoordinator(
        scope = viewModelScope,
        backupSettingsUseCases = backupSettingsUseCases,
        backupUseCases = backupUseCases,
        application = application
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            securitySettingsUseCases.lockTimeout,
            systemSettingsUseCases.isStatusBarAutoHide,
            systemSettingsUseCases.isTopBarCollapsible,
            systemSettingsUseCases.isTabBarCollapsible,
            securitySettingsUseCases.isSecureContentEnabled
        ) { lockTimeout, isStatusBarAutoHide, isTopBarCollapsible, isTabBarCollapsible, isSecureContentEnabled ->
            CoreSettingsFlowState(
                lockTimeout = lockTimeout,
                isStatusBarAutoHide = isStatusBarAutoHide,
                isTopBarCollapsible = isTopBarCollapsible,
                isTabBarCollapsible = isTabBarCollapsible,
                isSecureContentEnabled = isSecureContentEnabled
            )
        },
        combine(
            securitySettingsUseCases.isFlipToLockEnabled,
            securitySettingsUseCases.isFlipExitAndClearStackEnabled,
            systemSettingsUseCases.cardStyle,
            systemSettingsUseCases.cardStyleByEntryType
        ) { isFlipToLockEnabled, isFlipExitAndClearStackEnabled, cardStyle, cardStyleByEntryType ->
            SecurityAndStyleFlowState(
                isFlipToLockEnabled = isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
                cardStyle = cardStyle,
                cardStyleByEntryType = cardStyleByEntryType
            )
        }.combine(
            combine(
                systemSettingsUseCases.autofillUiMode,
                systemSettingsUseCases.isSwipeEnabled,
                systemSettingsUseCases.swipeLeftAction,
                systemSettingsUseCases.visibleVaultTabs,
                systemSettingsUseCases.isAutoDownloadIcons
            ) { autofillUiMode, isSwipeEnabled, swipeLeftAction, visibleVaultTabs, isAutoDownloadIcons ->
                AutofillAndSwipeFlowState(
                    autofillUiMode = autofillUiMode,
                    isSwipeEnabled = isSwipeEnabled,
                    swipeLeftAction = swipeLeftAction,
                    visibleVaultTabs = visibleVaultTabs,
                    isAutoDownloadIcons = isAutoDownloadIcons
                )
            }) { securityAndStyle, autofillAndSwipe ->
            SettingsUiState(
                isFlipToLockEnabled = securityAndStyle.isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = securityAndStyle.isFlipExitAndClearStackEnabled,
                cardStyle = securityAndStyle.cardStyle,
                cardStyleByEntryType = securityAndStyle.cardStyleByEntryType,
                autofillUiMode = autofillAndSwipe.autofillUiMode,
                isSwipeEnabled = autofillAndSwipe.isSwipeEnabled,
                swipeLeftAction = autofillAndSwipe.swipeLeftAction,
                visibleVaultTabs = autofillAndSwipe.visibleVaultTabs,
                isAutoDownloadIcons = autofillAndSwipe.isAutoDownloadIcons
            )
        },
        systemSettingsUseCases.swipeRightAction,
        backupSettingsUseCases.backupDirectoryUri,
        backupSettingsUseCases.lastBackupExportFileName
    ) { core, partialState, swipeRightAction, backupDirectoryUri, lastBackupExportFileName ->
        SettingsUiState(
            lockTimeout = core.lockTimeout,
            isStatusBarAutoHide = core.isStatusBarAutoHide,
            isTopBarCollapsible = core.isTopBarCollapsible,
            isTabBarCollapsible = core.isTabBarCollapsible,
            isSecureContentEnabled = core.isSecureContentEnabled,
            isFlipToLockEnabled = partialState.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = partialState.isFlipExitAndClearStackEnabled,
            cardStyle = partialState.cardStyle,
            cardStyleByEntryType = partialState.cardStyleByEntryType,
            autofillUiMode = partialState.autofillUiMode,
            isSwipeEnabled = partialState.isSwipeEnabled,
            swipeLeftAction = partialState.swipeLeftAction,
            swipeRightAction = swipeRightAction,
            backupDirectoryUri = backupDirectoryUri,
            lastBackupExportFileName = lastBackupExportFileName,
            visibleVaultTabs = partialState.visibleVaultTabs,
            isAutoDownloadIcons = partialState.isAutoDownloadIcons
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // --- Setter 方法 ---
    fun setStatusBarAutoHide(autoHide: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setStatusBarAutoHide(autoHide) }

    fun setTopBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setTopBarCollapsible(collapsible) }

    fun setTabBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setTabBarCollapsible(collapsible) }

    fun setSecureContentEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setSecureContentEnabled(enabled) }

    fun setFlipToLockEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setFlipToLockEnabled(enabled) }

    fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setFlipExitAndClearStackEnabled(enabled) }

    fun setLockTimeout(timeoutMs: Long) =
        viewModelScope.launch { securitySettingsUseCases.setLockTimeout(timeoutMs.coerceAtLeast(5000L)) }

    fun setCardStyle(style: VaultCardStyle) =
        viewModelScope.launch { systemSettingsUseCases.setCardStyle(style) }

    fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) =
        viewModelScope.launch {
            systemSettingsUseCases.setCardStyleForEntryType(entryTypeValue, style)
        }

    fun toggleAutofillUiMode(currentMode: AutofillUiMode) = viewModelScope.launch {
        val nextMode = when (currentMode) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
        }
        systemSettingsUseCases.setAutofillUiMode(nextMode)
    }

    fun setSwipeEnabled(enabled: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeEnabled(enabled) }

    fun setSwipeLeftAction(action: SwipeActionType) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeLeftAction(action) }

    fun setSwipeRightAction(action: SwipeActionType) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeRightAction(action) }

    fun setVisibleVaultTabs(keys: Set<String>) =
        viewModelScope.launch { systemSettingsUseCases.setVisibleVaultTabs(keys) }

    fun setAutoDownloadIcons(enabled: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setAutoDownloadIcons(enabled) }

    // --- 备份相关操作：现在非常统一 ---
    fun setBackupDirectoryUri(uri: String) = 
        backup.setBackupDirectoryUri(uri)

    fun clearBackupDirectoryUri() = 
        backup.clearBackupDirectoryUri()

    fun testBackupDirectoryWritePermission(directoryUri: String?) =
        backup.testBackupDirectoryWritePermission(directoryUri)
}