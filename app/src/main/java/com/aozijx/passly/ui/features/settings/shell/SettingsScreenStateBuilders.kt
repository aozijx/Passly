package com.aozijx.passly.ui.features.settings.shell

import android.content.Context
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordAction
import com.aozijx.passly.ui.features.settings.internal.AppPasswordDialogEvent
import com.aozijx.passly.ui.features.settings.internal.SettingsContentActions
import com.aozijx.passly.ui.features.settings.internal.SettingsContentState
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogEvent
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsActions
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsState
import com.aozijx.passly.ui.features.settings.state.SettingsUiState

internal fun buildSettingsContentState(
    uiState: SettingsUiState,
    isAppPasswordEnabled: Boolean,
    availableCardStyles: List<VaultCardStyle>,
    passwordSelectedStyle: VaultCardStyle,
    totpSelectedStyle: VaultCardStyle,
    backupPathLabel: String,
    lastExportFileLabel: String
): SettingsContentState = SettingsContentState(
    lockTimeout = uiState.security.lockTimeout,
    isAppPasswordEnabled = isAppPasswordEnabled,
    isPasswordPreferredAuthFirst = uiState.security.isPasswordPreferredAuthFirst,
    isDeviceCredentialFallbackEnabled = uiState.security.isDeviceCredentialFallbackEnabled,
    isInvalidateKeyOnBioChange = uiState.security.isInvalidateKeyOnBioChange,
    isSecureContentEnabled = uiState.security.isSecureContentEnabled,
    isFlipToLockEnabled = uiState.security.isFlipToLockEnabled,
    isFlipExitAndClearStackEnabled = uiState.security.isFlipExitAndClearStackEnabled,
    isStatusBarAutoHide = uiState.display.isStatusBarAutoHide,
    isTopBarCollapsible = uiState.display.isTopBarCollapsible,
    isTabBarCollapsible = uiState.display.isTabBarCollapsible,
    isSwipeEnabled = uiState.vault.isSwipeEnabled,
    swipeLeftAction = uiState.vault.swipeLeftAction,
    swipeRightAction = uiState.vault.swipeRightAction,
    autofillUiMode = uiState.vault.autofillUiMode,
    visibleVaultTabs = uiState.vault.visibleVaultTabs,
    tabBarMaxTabsWithoutScroll = uiState.vault.tabBarMaxTabsWithoutScroll,
    isAutoDownloadIcons = uiState.display.isAutoDownloadIcons,
    availableCardStyles = availableCardStyles,
    passwordSelectedStyle = passwordSelectedStyle,
    totpSelectedStyle = totpSelectedStyle,
    backupPathLabel = backupPathLabel,
    lastExportFileLabel = lastExportFileLabel
)

internal fun buildSettingsDialogsState(
    uiState: SettingsUiState, localState: SettingsScreenLocalState, context: Context
): SettingsDialogsState = SettingsDialogsState(
    showRightActionDialog = localState.showRightActionDialog,
    showLeftActionDialog = localState.showLeftActionDialog,
    showLockTimeoutDialog = localState.showLockTimeoutDialog,
    showClearBackupDirConfirmDialog = localState.showClearBackupDirConfirmDialog,
    showDeviceCredentialFallbackWarningDialog =
        localState.showDeviceCredentialFallbackWarningDialog,
    activeAppPasswordDialog = localState.activeAppPasswordDialog,
    swipeLeftAction = uiState.vault.swipeLeftAction,
    swipeRightAction = uiState.vault.swipeRightAction,
    lockTimeout = uiState.security.lockTimeout,
    backupDirectoryUri = uiState.backup.directoryUri,
    context = context,
    appPasswordCurrent = localState.appPasswordCurrent,
    appPasswordNew = localState.appPasswordNew,
    appPasswordConfirm = localState.appPasswordConfirm
)

internal fun buildSettingsContentActions(
    uiState: SettingsUiState,
    localState: SettingsScreenLocalState,
    onBack: () -> Unit,
    configProvider: UserConfigProvider,
    onAppPasswordClick: () -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onPickBackupPath: () -> Unit,
    onTestBackupWrite: () -> Unit
): SettingsContentActions = SettingsContentActions(
    onBack = onBack,
    onShowLockTimeoutDialog = localState::openLockTimeoutDialog,
    onAppPasswordClick = onAppPasswordClick,
    onPasswordPreferredAuthFirstChange = configProvider::setPasswordPreferredAuthFirst,
    onDeviceCredentialFallbackToggleRequested = { enabled ->
        if (enabled && !uiState.security.isDeviceCredentialFallbackEnabled) {
            localState.openDeviceCredentialFallbackWarningDialog()
        } else {
            configProvider.setDeviceCredentialFallbackEnabled(enabled)
        }
    },
    onInvalidateKeyOnBioChangeToggle = onInvalidateKeyOnBioChangeToggle,
    onSecureContentEnabledChange = configProvider::setSecureContentEnabled,
    onFlipToLockEnabledChange = configProvider::setFlipToLockEnabled,
    onFlipExitAndClearStackEnabledChange = configProvider::setFlipExitAndClearStackEnabled,
    onStatusBarAutoHideChange = configProvider::setStatusBarAutoHide,
    onTopBarCollapsibleChange = configProvider::setTopBarCollapsible,
    onTabBarCollapsibleChange = configProvider::setTabBarCollapsible,
    onSwipeEnabledChange = configProvider::setSwipeEnabled,
    onLeftSwipeActionClick = localState::openLeftActionDialog,
    onRightSwipeActionClick = localState::openRightActionDialog,
    onToggleAutofillUiMode = { configProvider.toggleAutofillUiMode(uiState.vault.autofillUiMode) },
    onVisibleVaultTabsChange = configProvider::setVisibleVaultTabs,
    onTabBarMaxTabsWithoutScrollChange = configProvider::setTabBarMaxTabsWithoutScroll,
    onAutoDownloadIconsChange = configProvider::setAutoDownloadIcons,
    onPickBackupPath = onPickBackupPath,
    onTestBackupWrite = onTestBackupWrite,
    onClearBackupPath =
        if (uiState.backup.directoryUri.isNullOrBlank()) null
        else {
            localState::openClearBackupDirConfirmDialog
        },
    onPasswordStyleSelected = {
        configProvider.setCardStyleForEntryType(
            EntryType.PASSWORD.value,
            it
        )
    },
    onTotpStyleSelected = { configProvider.setCardStyleForEntryType(EntryType.TOTP.value, it) }
)

internal fun buildSettingsDialogsActions(
    localState: SettingsScreenLocalState,
    configProvider: UserConfigProvider,
    submitAppPasswordAction: (AppPasswordAction) -> Unit
): SettingsDialogsActions = SettingsDialogsActions(
    onDialogEvent = { event ->
        when (event) {
            is SettingsDialogEvent.SetSwipeRightAction -> configProvider.setSwipeRightAction(event.action)
            is SettingsDialogEvent.SetSwipeLeftAction -> configProvider.setSwipeLeftAction(event.action)
            is SettingsDialogEvent.SetLockTimeout -> configProvider.setLockTimeout(event.timeoutMs)
            SettingsDialogEvent.ClearBackupDirectory -> configProvider.clearBackupDirectoryUri()
            SettingsDialogEvent.DismissRightActionDialog -> localState.dismissRightActionDialog()
            SettingsDialogEvent.DismissLeftActionDialog -> localState.dismissLeftActionDialog()
            SettingsDialogEvent.DismissLockTimeoutDialog -> localState.dismissLockTimeoutDialog()
            SettingsDialogEvent.DismissClearBackupDirConfirmDialog ->
                localState.dismissClearBackupDirConfirmDialog()

            SettingsDialogEvent.DismissDeviceCredentialFallbackWarningDialog ->
                localState.dismissDeviceCredentialFallbackWarningDialog()

            SettingsDialogEvent.ConfirmEnableDeviceCredentialFallback -> {
                configProvider.setDeviceCredentialFallbackEnabled(true)
                localState.dismissDeviceCredentialFallbackWarningDialog()
            }

            is SettingsDialogEvent.AppPassword -> {
                when (event.event) {
                    AppPasswordDialogEvent.DismissAction -> localState.dismissAppPasswordActionDialog()
                    AppPasswordDialogEvent.ShowChange -> localState.openChangeAppPasswordDialog()
                    AppPasswordDialogEvent.ShowDisable -> localState.openDisableAppPasswordDialog()
                    AppPasswordDialogEvent.DismissSet -> localState.dismissSetAppPasswordDialog()
                    AppPasswordDialogEvent.DismissChange -> localState.dismissChangeAppPasswordDialog()
                    AppPasswordDialogEvent.DismissDisable -> localState.dismissDisableAppPasswordDialog()
                    is AppPasswordDialogEvent.CurrentChanged ->
                        localState.appPasswordCurrent = event.event.value

                    is AppPasswordDialogEvent.NewChanged -> localState.appPasswordNew =
                        event.event.value

                    is AppPasswordDialogEvent.ConfirmChanged ->
                        localState.appPasswordConfirm = event.event.value

                    AppPasswordDialogEvent.ConfirmSet ->
                        submitAppPasswordAction(AppPasswordAction.SET)

                    AppPasswordDialogEvent.ConfirmChange ->
                        submitAppPasswordAction(AppPasswordAction.CHANGE)

                    AppPasswordDialogEvent.ConfirmDisable ->
                        submitAppPasswordAction(AppPasswordAction.DISABLE)
                }
            }
        }
    }
)