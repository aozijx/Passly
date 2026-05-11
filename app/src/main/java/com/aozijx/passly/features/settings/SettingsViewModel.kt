package com.aozijx.passly.features.settings

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.auth.AuthCoordinator
import com.aozijx.passly.features.auth.ui.SettingsAuthGateway
import com.aozijx.passly.features.backup.BackupCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lockTimeout: Long = 60000L,
    val isInvalidateKeyOnBioChange: Boolean = true,
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val isSecureContentEnabled: Boolean = true,
    val isPasswordPreferredAuthFirst: Boolean = true,
    val isDeviceCredentialFallbackEnabled: Boolean = true,
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
    val tabBarMaxTabsWithoutScroll: Int = 4,
    val isAutoDownloadIcons: Boolean = true
)

private data class CoreSettingsFlowState(
    val lockTimeout: Long,
    val isInvalidateKeyOnBioChange: Boolean,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSecureContentEnabled: Boolean
)

private data class SecurityAndStyleFlowState(
    val isPasswordPreferredAuthFirst: Boolean,
    val isDeviceCredentialFallbackEnabled: Boolean,
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
    val tabBarMaxTabsWithoutScroll: Int,
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

    /** 认证协调器：统一处理认证相关的 UI 流程 */
    private val authCoordinator = AuthCoordinator(
        scope = viewModelScope, authUseCases = AppContainer.domain.authUseCases
    )
    val authGateway: SettingsAuthGateway = authCoordinator

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            securitySettingsUseCases.lockTimeout,
            securitySettingsUseCases.isInvalidateKeyOnBioChange,
            systemSettingsUseCases.isStatusBarAutoHide,
            systemSettingsUseCases.isTopBarCollapsible,
            systemSettingsUseCases.isTabBarCollapsible
        ) { lockTimeout, isInvalidateKeyOnBioChange, isStatusBarAutoHide, isTopBarCollapsible, isTabBarCollapsible ->
            CoreSettingsFlowState(
                lockTimeout = lockTimeout,
                isInvalidateKeyOnBioChange = isInvalidateKeyOnBioChange,
                isStatusBarAutoHide = isStatusBarAutoHide,
                isTopBarCollapsible = isTopBarCollapsible,
                isTabBarCollapsible = isTabBarCollapsible,
                isSecureContentEnabled = true
            )
        }.combine(securitySettingsUseCases.isSecureContentEnabled) { core, isSecureContentEnabled ->
            core.copy(isSecureContentEnabled = isSecureContentEnabled)
        },
        combine(
            securitySettingsUseCases.isPasswordPreferredAuthFirst,
            securitySettingsUseCases.isDeviceCredentialFallbackEnabled
        ) { isPasswordPreferredAuthFirst, isDeviceCredentialFallbackEnabled ->
            isPasswordPreferredAuthFirst to isDeviceCredentialFallbackEnabled
        }.combine(securitySettingsUseCases.isFlipToLockEnabled) { authPrefs, isFlipToLockEnabled ->
            Triple(authPrefs.first, authPrefs.second, isFlipToLockEnabled)
        }
            .combine(securitySettingsUseCases.isFlipExitAndClearStackEnabled) { authPrefs, isFlipExitAndClearStackEnabled ->
                Pair(authPrefs, isFlipExitAndClearStackEnabled)
            }.combine(systemSettingsUseCases.cardStyle) { authPrefsAndFlipExit, cardStyle ->
            Pair(authPrefsAndFlipExit, cardStyle)
        }
            .combine(systemSettingsUseCases.cardStyleByEntryType) { authPrefsAndStyle, cardStyleByEntryType ->
                val authPrefsAndFlipExit = authPrefsAndStyle.first.first
                val cardStyle = authPrefsAndStyle.second
            SecurityAndStyleFlowState(
                isPasswordPreferredAuthFirst = authPrefsAndFlipExit.first,
                isDeviceCredentialFallbackEnabled = authPrefsAndFlipExit.second,
                isFlipToLockEnabled = authPrefsAndFlipExit.third,
                isFlipExitAndClearStackEnabled = authPrefsAndStyle.first.second,
                cardStyle = cardStyle,
                cardStyleByEntryType = cardStyleByEntryType
            )
        }.combine(
            combine(
                systemSettingsUseCases.autofillUiMode,
                systemSettingsUseCases.isSwipeEnabled,
                systemSettingsUseCases.swipeLeftAction,
                systemSettingsUseCases.visibleVaultTabs,
                systemSettingsUseCases.tabBarMaxTabsWithoutScroll
            ) { autofillUiMode, isSwipeEnabled, swipeLeftAction, visibleVaultTabs, tabBarMaxTabsWithoutScroll ->
                AutofillAndSwipeFlowState(
                    autofillUiMode = autofillUiMode,
                    isSwipeEnabled = isSwipeEnabled,
                    swipeLeftAction = swipeLeftAction,
                    visibleVaultTabs = visibleVaultTabs,
                    tabBarMaxTabsWithoutScroll = tabBarMaxTabsWithoutScroll,
                    isAutoDownloadIcons = true
                )
            }.combine(systemSettingsUseCases.isAutoDownloadIcons) { partial, isAutoDownloadIcons ->
                partial.copy(isAutoDownloadIcons = isAutoDownloadIcons)
            }) { securityAndStyle, autofillAndSwipe ->
            SettingsUiState(
                isPasswordPreferredAuthFirst = securityAndStyle.isPasswordPreferredAuthFirst,
                isDeviceCredentialFallbackEnabled = securityAndStyle.isDeviceCredentialFallbackEnabled,
                isFlipToLockEnabled = securityAndStyle.isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = securityAndStyle.isFlipExitAndClearStackEnabled,
                cardStyle = securityAndStyle.cardStyle,
                cardStyleByEntryType = securityAndStyle.cardStyleByEntryType,
                autofillUiMode = autofillAndSwipe.autofillUiMode,
                isSwipeEnabled = autofillAndSwipe.isSwipeEnabled,
                swipeLeftAction = autofillAndSwipe.swipeLeftAction,
                visibleVaultTabs = autofillAndSwipe.visibleVaultTabs,
                tabBarMaxTabsWithoutScroll = autofillAndSwipe.tabBarMaxTabsWithoutScroll,
                isAutoDownloadIcons = autofillAndSwipe.isAutoDownloadIcons
            )
        },
        systemSettingsUseCases.swipeRightAction,
        backupSettingsUseCases.backupDirectoryUri,
        backupSettingsUseCases.lastBackupExportFileName
    ) { core, partialState, swipeRightAction, backupDirectoryUri, lastBackupExportFileName ->
        SettingsUiState(
            lockTimeout = core.lockTimeout,
            isInvalidateKeyOnBioChange = core.isInvalidateKeyOnBioChange,
            isStatusBarAutoHide = core.isStatusBarAutoHide,
            isTopBarCollapsible = core.isTopBarCollapsible,
            isTabBarCollapsible = core.isTabBarCollapsible,
            isSecureContentEnabled = core.isSecureContentEnabled,
            isPasswordPreferredAuthFirst = partialState.isPasswordPreferredAuthFirst,
            isDeviceCredentialFallbackEnabled = partialState.isDeviceCredentialFallbackEnabled,
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
            tabBarMaxTabsWithoutScroll = partialState.tabBarMaxTabsWithoutScroll,
            isAutoDownloadIcons = partialState.isAutoDownloadIcons
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // --- Setter 方法 ---
    fun switchKeyInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        authCoordinator.rekeyWithInvalidationPolicy(
            activity, invalidateOnBiometricChange
        ) { result ->
            if (result.isSuccess) {
                viewModelScope.launch {
                    securitySettingsUseCases.setInvalidateKeyOnBioChange(invalidateOnBiometricChange)
                }
            }
            onResult(result)
        }
    }

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

    fun setPasswordPreferredAuthFirst(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setPasswordPreferredAuthFirst(enabled) }

    fun setDeviceCredentialFallbackEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setDeviceCredentialFallbackEnabled(enabled) }

    fun setLockTimeout(timeoutMs: Long) = viewModelScope.launch {
        securitySettingsUseCases.setLockTimeout(
            timeoutMs.coerceAtLeast(
                5000L
            )
        )
    }

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

    fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) =
        viewModelScope.launch { systemSettingsUseCases.setTabBarMaxTabsWithoutScroll(maxTabs) }

    fun setAutoDownloadIcons(enabled: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setAutoDownloadIcons(enabled) }


    // --- 备份相关操作 ---
    fun setBackupDirectoryUri(uri: String) = backup.setBackupDirectoryUri(uri)

    fun clearBackupDirectoryUri() = backup.clearBackupDirectoryUri()

    fun testBackupDirectoryWritePermission(directoryUri: String?) =
        backup.testBackupDirectoryWritePermission(directoryUri)
}