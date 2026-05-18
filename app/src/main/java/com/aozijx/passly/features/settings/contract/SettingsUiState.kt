package com.aozijx.passly.features.settings.contract

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle

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