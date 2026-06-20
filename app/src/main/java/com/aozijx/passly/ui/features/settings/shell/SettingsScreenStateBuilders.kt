package com.aozijx.passly.ui.features.settings.shell

import android.content.Context
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordAction
import com.aozijx.passly.ui.features.settings.internal.AppPasswordDialogEvent
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogEvent
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsActions
import com.aozijx.passly.ui.features.settings.internal.SettingsDialogsState

internal fun buildSettingsDialogsState(
    localState: SettingsScreenLocalState,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    backupDirectoryUri: String?,
    context: Context
): SettingsDialogsState = SettingsDialogsState(
    showRightActionDialog = localState.showRightActionDialog,
    showLeftActionDialog = localState.showLeftActionDialog,
    showClearBackupDirConfirmDialog = localState.showClearBackupDirConfirmDialog,
    showDeviceCredentialFallbackWarningDialog =
        localState.showDeviceCredentialFallbackWarningDialog,
    activeAppPasswordDialog = localState.activeAppPasswordDialog,
    swipeLeftAction = swipeLeftAction,
    swipeRightAction = swipeRightAction,
    backupDirectoryUri = backupDirectoryUri,
    context = context,
    appPasswordCurrent = localState.appPasswordCurrent,
    appPasswordNew = localState.appPasswordNew,
    appPasswordConfirm = localState.appPasswordConfirm
)

internal fun buildSettingsDialogsActions(
    localState: SettingsScreenLocalState,
    onSetSwipeRightAction: (SwipeActionType) -> Unit,
    onSetSwipeLeftAction: (SwipeActionType) -> Unit,
    onSetDeviceCredentialFallback: (Boolean) -> Unit,
    submitAppPasswordAction: (AppPasswordAction) -> Unit,
    onClearBackupDirectory: () -> Unit
): SettingsDialogsActions = SettingsDialogsActions(
    onDialogEvent = { event ->
        when (event) {
            is SettingsDialogEvent.SetSwipeRightAction -> onSetSwipeRightAction(event.action)
            is SettingsDialogEvent.SetSwipeLeftAction -> onSetSwipeLeftAction(event.action)
            SettingsDialogEvent.ClearBackupDirectory -> onClearBackupDirectory()
            SettingsDialogEvent.DismissRightActionDialog -> localState.dismissRightActionDialog()
            SettingsDialogEvent.DismissLeftActionDialog -> localState.dismissLeftActionDialog()
            SettingsDialogEvent.DismissClearBackupDirConfirmDialog ->
                localState.dismissClearBackupDirConfirmDialog()

            SettingsDialogEvent.DismissDeviceCredentialFallbackWarningDialog ->
                localState.dismissDeviceCredentialFallbackWarningDialog()

            SettingsDialogEvent.ConfirmEnableDeviceCredentialFallback -> {
                onSetDeviceCredentialFallback(true)
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