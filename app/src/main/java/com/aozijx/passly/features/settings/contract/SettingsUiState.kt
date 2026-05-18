package com.aozijx.passly.features.settings.contract

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.features.settings.internal.SettingsConstants

data class SettingsUiState(
    val lockTimeout: Long = SettingsConstants.DEFAULT_LOCK_TIMEOUT_MS,
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
    val tabBarMaxTabsWithoutScroll: Int = SettingsConstants.DEFAULT_TAB_BAR_MAX_TABS,
    val isAutoDownloadIcons: Boolean = true
)