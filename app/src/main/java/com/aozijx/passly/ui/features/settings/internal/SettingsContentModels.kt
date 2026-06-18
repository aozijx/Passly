package com.aozijx.passly.ui.features.settings.internal

import com.aozijx.passly.domain.config.AutofillUiMode
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle

internal data class SettingsContentState(
    val lockTimeout: Long,
    val isAppPasswordEnabled: Boolean,
    val isPasswordPreferredAuthFirst: Boolean,
    val isDeviceCredentialFallbackEnabled: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val isDarkMode: Boolean,
    val isDynamicColor: Boolean,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val autofillUiMode: AutofillUiMode,
    val visibleVaultTabs: Set<String>?,
    val tabBarMaxTabsWithoutScroll: Int,
    val isAutoDownloadIcons: Boolean,
    val availableCardStyles: List<VaultCardStyle>,
    val passwordSelectedStyle: VaultCardStyle,
    val totpSelectedStyle: VaultCardStyle,
    val backupPathLabel: String,
    val lastExportFileLabel: String
)

internal data class SettingsContentActions(
    val onBack: () -> Unit,
    val onLockTimeoutChange: (Long) -> Unit,
    val onAppPasswordClick: () -> Unit,
    val onPasswordPreferredAuthFirstChange: (Boolean) -> Unit,
    val onDeviceCredentialFallbackToggleRequested: (Boolean) -> Unit,
    val onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    val onSecureContentEnabledChange: (Boolean) -> Unit,
    val onFlipToLockEnabledChange: (Boolean) -> Unit,
    val onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit,
    val onDarkModeChange: (Boolean) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onStatusBarAutoHideChange: (Boolean) -> Unit,
    val onTopBarCollapsibleChange: (Boolean) -> Unit,
    val onTabBarCollapsibleChange: (Boolean) -> Unit,
    val onSwipeEnabledChange: (Boolean) -> Unit,
    val onLeftSwipeActionClick: () -> Unit,
    val onRightSwipeActionClick: () -> Unit,
    val onToggleAutofillUiMode: () -> Unit,
    val onVisibleVaultTabsChange: (Set<String>) -> Unit,
    val onTabBarMaxTabsWithoutScrollChange: (Int) -> Unit,
    val onAutoDownloadIconsChange: (Boolean) -> Unit,
    val onPickBackupPath: () -> Unit,
    val onTestBackupWrite: () -> Unit,
    val onClearBackupPath: (() -> Unit)?,
    val onPasswordStyleSelected: (VaultCardStyle) -> Unit,
    val onTotpStyleSelected: (VaultCardStyle) -> Unit
)