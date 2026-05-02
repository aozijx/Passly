package com.aozijx.passly.features.settings.internal

import android.content.Context
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.features.settings.AppPasswordDialogEvent
import com.aozijx.passly.features.settings.SettingsContentActions
import com.aozijx.passly.features.settings.SettingsContentState
import com.aozijx.passly.features.settings.SettingsDialogsActions
import com.aozijx.passly.features.settings.SettingsDialogsState
import com.aozijx.passly.features.settings.SettingsUiState
import com.aozijx.passly.features.settings.SettingsViewModel

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
    onSetSwipeRightAction = viewModel::setSwipeRightAction,
    onSetSwipeLeftAction = viewModel::setSwipeLeftAction,
    onSetLockTimeout = viewModel::setLockTimeout,
    onClearBackupDirectory = viewModel::clearBackupDirectoryUri,
    onDismissRightActionDialog = localState::dismissRightActionDialog,
    onDismissLeftActionDialog = localState::dismissLeftActionDialog,
    onDismissLockTimeoutDialog = localState::dismissLockTimeoutDialog,
    onDismissClearBackupDirConfirmDialog = localState::dismissClearBackupDirConfirmDialog,
    onAppPasswordEvent = { event ->
        when (event) {
            AppPasswordDialogEvent.DismissAction -> localState.dismissAppPasswordActionDialog()
            AppPasswordDialogEvent.ShowChange -> localState.openChangeAppPasswordDialog()
            AppPasswordDialogEvent.ShowDisable -> localState.openDisableAppPasswordDialog()
            AppPasswordDialogEvent.DismissSet -> localState.dismissSetAppPasswordDialog()
            AppPasswordDialogEvent.DismissChange -> localState.dismissChangeAppPasswordDialog()
            AppPasswordDialogEvent.DismissDisable -> localState.dismissDisableAppPasswordDialog()
            is AppPasswordDialogEvent.CurrentChanged -> localState.appPasswordCurrent = event.value
            is AppPasswordDialogEvent.NewChanged -> localState.appPasswordNew = event.value
            is AppPasswordDialogEvent.ConfirmChanged -> localState.appPasswordConfirm = event.value
            AppPasswordDialogEvent.ConfirmSet -> submitAppPasswordAction(AppPasswordAction.SET)
            AppPasswordDialogEvent.ConfirmChange -> submitAppPasswordAction(AppPasswordAction.CHANGE)
            AppPasswordDialogEvent.ConfirmDisable -> submitAppPasswordAction(AppPasswordAction.DISABLE)
        }
    }
)