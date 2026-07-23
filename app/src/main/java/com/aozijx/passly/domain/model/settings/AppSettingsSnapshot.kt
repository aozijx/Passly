package com.aozijx.passly.domain.model.settings

data class AppSettingsSnapshot(
    val appearance: AppearanceSettings,
    val interaction: InteractionSettings,
    val security: SecuritySettings,
    val notifications: NotificationSettings,
    val vault: VaultViewSettings,
    val backup: BackupSettings
)

data class AppearanceSettings(
    val isDarkMode: Boolean?,
    val isDynamicColor: Boolean,
    val themeColor: String,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean
)

data class InteractionSettings(
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val autofillUiMode: AutofillUiMode,
    val tabBarMaxTabsWithoutScroll: Int,
    val isAutoDownloadIcons: Boolean,
    val faviconDownloadWhitelist: Set<String>
)

data class SecuritySettings(
    val lockTimeout: Long,
    val isLockOnBackground: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean
)

data class NotificationSettings(
    val statusBarNotificationsEnabled: Boolean,
    val iconDownloadNotificationsEnabled: Boolean,
    val clipboardClearToastsEnabled: Boolean,
    val appCloseToastsEnabled: Boolean
)

data class VaultViewSettings(
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>,
    val visibleVaultTabs: Set<String>?,
    val vaultSortOption: VaultSortSpec
)

data class BackupSettings(
    val backupDirectoryUri: String?,
    val lastBackupExportFileName: String?
)
