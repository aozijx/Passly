package com.aozijx.passly.features.settings.internal

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.features.settings.contract.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class SettingsStateCoordinator(
    scope: CoroutineScope,
    systemSettingsUseCases: SystemSettingsUseCases,
    securitySettingsUseCases: SecuritySettingsUseCases,
    backupSettingsUseCases: BackupSettingsUseCases
) {

    private val coreSettings = combine(
        securitySettingsUseCases.lockTimeout,
        securitySettingsUseCases.isInvalidateKeyOnBioChange,
        systemSettingsUseCases.isStatusBarAutoHide,
        systemSettingsUseCases.isTopBarCollapsible,
        systemSettingsUseCases.isTabBarCollapsible
    ) { lockTimeout, isInvalidateKeyOnBioChange, isStatusBarAutoHide, isTopBarCollapsible, isTabBarCollapsible ->
        CoreSettings(
            lockTimeout = lockTimeout,
            isInvalidateKeyOnBioChange = isInvalidateKeyOnBioChange,
            isStatusBarAutoHide = isStatusBarAutoHide,
            isTopBarCollapsible = isTopBarCollapsible,
            isTabBarCollapsible = isTabBarCollapsible,
            isSecureContentEnabled = true
        )
    }.combine(securitySettingsUseCases.isSecureContentEnabled) { core, isSecureContentEnabled ->
        core.copy(isSecureContentEnabled = isSecureContentEnabled)
    }

    private val securityAndStyle = combine(
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
            SecurityAndStyle(
                isPasswordPreferredAuthFirst = authPrefsAndFlipExit.first,
                isDeviceCredentialFallbackEnabled = authPrefsAndFlipExit.second,
                isFlipToLockEnabled = authPrefsAndFlipExit.third,
                isFlipExitAndClearStackEnabled = authPrefsAndStyle.first.second,
                cardStyle = cardStyle,
                cardStyleByEntryType = cardStyleByEntryType
            )
        }

    private val autofillAndSwipe = combine(
        systemSettingsUseCases.autofillUiMode,
        systemSettingsUseCases.isSwipeEnabled,
        systemSettingsUseCases.swipeLeftAction,
        systemSettingsUseCases.visibleVaultTabs,
        systemSettingsUseCases.tabBarMaxTabsWithoutScroll
    ) { autofillUiMode, isSwipeEnabled, swipeLeftAction, visibleVaultTabs, tabBarMaxTabsWithoutScroll ->
        AutofillAndSwipePrefs(
            autofillUiMode = autofillUiMode,
            isSwipeEnabled = isSwipeEnabled,
            swipeLeftAction = swipeLeftAction,
            visibleVaultTabs = visibleVaultTabs,
            tabBarMaxTabsWithoutScroll = tabBarMaxTabsWithoutScroll,
            isAutoDownloadIcons = true
        )
    }.combine(systemSettingsUseCases.isAutoDownloadIcons) { partial, isAutoDownloadIcons ->
        partial.copy(isAutoDownloadIcons = isAutoDownloadIcons)
    }

    private val partialState = securityAndStyle.combine(
        autofillAndSwipe
    ) { secAndStyle, autofill ->
        SettingsUiState(
            isPasswordPreferredAuthFirst = secAndStyle.isPasswordPreferredAuthFirst,
            isDeviceCredentialFallbackEnabled = secAndStyle.isDeviceCredentialFallbackEnabled,
            isFlipToLockEnabled = secAndStyle.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = secAndStyle.isFlipExitAndClearStackEnabled,
            cardStyle = secAndStyle.cardStyle,
            cardStyleByEntryType = secAndStyle.cardStyleByEntryType,
            autofillUiMode = autofill.autofillUiMode,
            isSwipeEnabled = autofill.isSwipeEnabled,
            swipeLeftAction = autofill.swipeLeftAction,
            visibleVaultTabs = autofill.visibleVaultTabs,
            tabBarMaxTabsWithoutScroll = autofill.tabBarMaxTabsWithoutScroll,
            isAutoDownloadIcons = autofill.isAutoDownloadIcons
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        coreSettings,
        partialState,
        systemSettingsUseCases.swipeRightAction,
        backupSettingsUseCases.backupDirectoryUri,
        backupSettingsUseCases.lastBackupExportFileName
    ) { core, partial, swipeRightAction, backupDirectoryUri, lastBackupExportFileName ->
        partial.copy(
            lockTimeout = core.lockTimeout,
            isInvalidateKeyOnBioChange = core.isInvalidateKeyOnBioChange,
            isStatusBarAutoHide = core.isStatusBarAutoHide,
            isTopBarCollapsible = core.isTopBarCollapsible,
            isTabBarCollapsible = core.isTabBarCollapsible,
            isSecureContentEnabled = core.isSecureContentEnabled,
            swipeRightAction = swipeRightAction,
            backupDirectoryUri = backupDirectoryUri,
            lastBackupExportFileName = lastBackupExportFileName
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), SettingsUiState())
}

private data class CoreSettings(
    val lockTimeout: Long,
    val isInvalidateKeyOnBioChange: Boolean,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSecureContentEnabled: Boolean
)

private data class SecurityAndStyle(
    val isPasswordPreferredAuthFirst: Boolean,
    val isDeviceCredentialFallbackEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>
)

private data class AutofillAndSwipePrefs(
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val visibleVaultTabs: Set<String>?,
    val tabBarMaxTabsWithoutScroll: Int,
    val isAutoDownloadIcons: Boolean
)