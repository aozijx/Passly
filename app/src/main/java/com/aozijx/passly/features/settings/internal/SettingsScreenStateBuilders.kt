package com.aozijx.passly.features.settings.internal

import android.content.Context
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.features.settings.AppPasswordDialogEvent
import com.aozijx.passly.features.settings.SettingsContentActions
import com.aozijx.passly.features.settings.SettingsContentState
import com.aozijx.passly.features.settings.SettingsDialogEvent
import com.aozijx.passly.features.settings.SettingsDialogsActions
import com.aozijx.passly.features.settings.SettingsDialogsState
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.settings.contract.SettingsUiState

internal fun buildSettingsContentState(
    uiState: SettingsUiState,
    isAppPasswordEnabled: Boolean,
    availableCardStyles: List<VaultCardStyle>,
    passwordSelectedStyle: VaultCardStyle,
    totpSelectedStyle: VaultCardStyle,
    backupPathLabel: String,
    lastExportFileLabel: String
): SettingsContentState = SettingsContentState(
    lockTimeout = uiState.lockTimeout,
    isAppPasswordEnabled = isAppPasswordEnabled,
    isPasswordPreferredAuthFirst = uiState.isPasswordPreferredAuthFirst,
    isDeviceCredentialFallbackEnabled = uiState.isDeviceCredentialFallbackEnabled,
    isInvalidateKeyOnBioChange = uiState.isInvalidateKeyOnBioChange,
    isSecureContentEnabled = uiState.isSecureContentEnabled,
    isFlipToLockEnabled = uiState.isFlipToLockEnabled,
    isFlipExitAndClearStackEnabled = uiState.isFlipExitAndClearStackEnabled,
    isStatusBarAutoHide = uiState.isStatusBarAutoHide,
    isTopBarCollapsible = uiState.isTopBarCollapsible,
    isTabBarCollapsible = uiState.isTabBarCollapsible,
    isSwipeEnabled = uiState.isSwipeEnabled,
    swipeLeftAction = uiState.swipeLeftAction,
    swipeRightAction = uiState.swipeRightAction,
    autofillUiMode = uiState.autofillUiMode,
    visibleVaultTabs = uiState.visibleVaultTabs,
    tabBarMaxTabsWithoutScroll = uiState.tabBarMaxTabsWithoutScroll,
    isAutoDownloadIcons = uiState.isAutoDownloadIcons,
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
    swipeLeftAction = uiState.swipeLeftAction,
    swipeRightAction = uiState.swipeRightAction,
    lockTimeout = uiState.lockTimeout,
    backupDirectoryUri = uiState.backupDirectoryUri,
    context = context,
    appPasswordCurrent = localState.appPasswordCurrent,
    appPasswordNew = localState.appPasswordNew,
    appPasswordConfirm = localState.appPasswordConfirm
)

internal fun buildSettingsContentActions(
    uiState: SettingsUiState,
    localState: SettingsScreenLocalState,
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
    onAppPasswordClick: () -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onPickBackupPath: () -> Unit,
    onTestBackupWrite: () -> Unit
): SettingsContentActions = SettingsContentActions(
    onBack = onBack,
    onShowLockTimeoutDialog = localState::openLockTimeoutDialog,
    onAppPasswordClick = onAppPasswordClick,
    onPasswordPreferredAuthFirstChange = viewModel::setPasswordPreferredAuthFirst,
    onDeviceCredentialFallbackToggleRequested = { enabled ->
        if (enabled && !uiState.isDeviceCredentialFallbackEnabled) {
            localState.openDeviceCredentialFallbackWarningDialog()
        } else {
            viewModel.setDeviceCredentialFallbackEnabled(enabled)
        }
    },
    onInvalidateKeyOnBioChangeToggle = onInvalidateKeyOnBioChangeToggle,
    onSecureContentEnabledChange = viewModel::setSecureContentEnabled,
    onFlipToLockEnabledChange = viewModel::setFlipToLockEnabled,
    onFlipExitAndClearStackEnabledChange = viewModel::setFlipExitAndClearStackEnabled,
    onStatusBarAutoHideChange = viewModel::setStatusBarAutoHide,
    onTopBarCollapsibleChange = viewModel::setTopBarCollapsible,
    onTabBarCollapsibleChange = viewModel::setTabBarCollapsible,
    onSwipeEnabledChange = viewModel::setSwipeEnabled,
    onLeftSwipeActionClick = localState::openLeftActionDialog,
    onRightSwipeActionClick = localState::openRightActionDialog,
    onToggleAutofillUiMode = { viewModel.toggleAutofillUiMode(uiState.autofillUiMode) },
    onVisibleVaultTabsChange = viewModel::setVisibleVaultTabs,
    onTabBarMaxTabsWithoutScrollChange = viewModel::setTabBarMaxTabsWithoutScroll,
    onAutoDownloadIconsChange = viewModel::setAutoDownloadIcons,
    onPickBackupPath = onPickBackupPath,
    onTestBackupWrite = onTestBackupWrite,
    onClearBackupPath =
        if (uiState.backupDirectoryUri.isNullOrBlank()) null
        else {
            localState::openClearBackupDirConfirmDialog
        },
    onPasswordStyleSelected = { viewModel.setCardStyleForEntryType(EntryType.PASSWORD.value, it) },
    onTotpStyleSelected = { viewModel.setCardStyleForEntryType(EntryType.TOTP.value, it) }
)

internal fun buildSettingsDialogsActions(
    localState: SettingsScreenLocalState,
    viewModel: SettingsViewModel,
    submitAppPasswordAction: (AppPasswordAction) -> Unit
): SettingsDialogsActions = SettingsDialogsActions(
    onDialogEvent = { event ->
        when (event) {
            is SettingsDialogEvent.SetSwipeRightAction -> viewModel.setSwipeRightAction(event.action)
            is SettingsDialogEvent.SetSwipeLeftAction -> viewModel.setSwipeLeftAction(event.action)
            is SettingsDialogEvent.SetLockTimeout -> viewModel.setLockTimeout(event.timeoutMs)
            SettingsDialogEvent.ClearBackupDirectory -> viewModel.clearBackupDirectoryUri()
            SettingsDialogEvent.DismissRightActionDialog -> localState.dismissRightActionDialog()
            SettingsDialogEvent.DismissLeftActionDialog -> localState.dismissLeftActionDialog()
            SettingsDialogEvent.DismissLockTimeoutDialog -> localState.dismissLockTimeoutDialog()
            SettingsDialogEvent.DismissClearBackupDirConfirmDialog ->
                localState.dismissClearBackupDirConfirmDialog()
            SettingsDialogEvent.DismissDeviceCredentialFallbackWarningDialog ->
                localState.dismissDeviceCredentialFallbackWarningDialog()

            SettingsDialogEvent.ConfirmEnableDeviceCredentialFallback -> {
                viewModel.setDeviceCredentialFallbackEnabled(true)
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