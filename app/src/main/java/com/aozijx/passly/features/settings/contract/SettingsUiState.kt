package com.aozijx.passly.features.settings.contract

import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.domain.model.VaultCardStyle

data class SettingsUiState(
    val lockTimeout: Long = AppDefaults.Security.DEFAULT_LOCK_TIMEOUT_MS,
    val isInvalidateKeyOnBioChange: Boolean = AppDefaults.Security.DEFAULT_INVALIDATE_KEY_ON_BIO_CHANGE,
    val isStatusBarAutoHide: Boolean = AppDefaults.Display.DEFAULT_STATUS_BAR_AUTO_HIDE,
    val isTopBarCollapsible: Boolean = AppDefaults.Display.DEFAULT_TOP_BAR_COLLAPSIBLE,
    val isTabBarCollapsible: Boolean = AppDefaults.Display.DEFAULT_TAB_BAR_COLLAPSIBLE,
    val isSecureContentEnabled: Boolean = AppDefaults.Security.DEFAULT_SECURE_CONTENT_ENABLED,
    val isPasswordPreferredAuthFirst: Boolean = AppDefaults.Security.DEFAULT_PASSWORD_PREFERRED_AUTH_FIRST,
    val isDeviceCredentialFallbackEnabled: Boolean = AppDefaults.Security.DEFAULT_DEVICE_CREDENTIAL_FALLBACK,
    val isFlipToLockEnabled: Boolean = AppDefaults.Security.DEFAULT_FLIP_TO_LOCK,
    val isFlipExitAndClearStackEnabled: Boolean = AppDefaults.Security.DEFAULT_FLIP_EXIT_AND_CLEAR_STACK,
    val cardStyle: VaultCardStyle = AppDefaults.CardStyle.GLOBAL_DEFAULT_STYLE,
    val cardStyleByEntryType: Map<Int, VaultCardStyle> = mapOf(AppDefaults.CardStyle.GLOBAL_DEFAULT_STYLE_ENTRY),
    val autofillUiMode: AutofillUiMode = AppDefaults.Vault.DEFAULT_AUTOFILL_UI_MODE,
    val isSwipeEnabled: Boolean = AppDefaults.Vault.DEFAULT_SWIPE_ENABLED,
    val swipeLeftAction: SwipeActionType = AppDefaults.Vault.DEFAULT_SWIPE_LEFT_ACTION,
    val swipeRightAction: SwipeActionType = AppDefaults.Vault.DEFAULT_SWIPE_RIGHT_ACTION,
    val backupDirectoryUri: String? = null,
    val lastBackupExportFileName: String? = null,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = AppDefaults.Vault.DEFAULT_TAB_BAR_MAX_TABS,
    val isAutoDownloadIcons: Boolean = AppDefaults.Display.DEFAULT_AUTO_DOWNLOAD_ICONS
)