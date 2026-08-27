package com.aozijx.passly.presentation.feature.settings.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.main.interaction.toFeatureModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel

internal fun buildSettingsDialogsState(
    localState: SettingsScreenLocalState,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
): SettingsDialogsModel = SettingsDialogsModel(
    showRightActionDialog = localState.showRightActionDialog,
    showLeftActionDialog = localState.showLeftActionDialog,
    showClearBackupDirConfirmDialog = localState.showClearBackupDirConfirmDialog,
    activeAppPasswordDialog = localState.activeAppPasswordDialog,
    swipeLeftAction = SwipeActionUiModel.valueOf(swipeLeftAction.name),
    swipeRightAction = SwipeActionUiModel.valueOf(swipeRightAction.name),
    appPasswordCurrent = localState.appPasswordCurrent,
    appPasswordNew = localState.appPasswordNew,
    appPasswordConfirm = localState.appPasswordConfirm,
    isSetPasswordConfirmEnabled = AppPasswordPolicy.DEFAULT.acceptsLength(
        localState.appPasswordNew.length,
    ) && localState.appPasswordNew == localState.appPasswordConfirm,
    isChangePasswordConfirmEnabled = localState.appPasswordCurrent.isNotEmpty() &&
        AppPasswordPolicy.DEFAULT.acceptsLength(localState.appPasswordNew.length) &&
        localState.appPasswordNew == localState.appPasswordConfirm,
)

internal fun buildSettingsDialogEventHandler(
    localState: SettingsScreenLocalState,
    backupDirectoryUri: String?,
    context: Context,
    onSetSwipeRightAction: (SwipeActionType) -> Unit,
    onSetSwipeLeftAction: (SwipeActionType) -> Unit,
    submitAppPasswordAction: (AppPasswordAction) -> Unit,
    onClearBackupDirectory: () -> Unit
): (SettingsDialogEvent) -> Unit = { event ->
        when (event) {
            is SettingsDialogEvent.SetSwipeRightAction -> onSetSwipeRightAction(event.action.toFeatureModel())
            is SettingsDialogEvent.SetSwipeLeftAction -> onSetSwipeLeftAction(event.action.toFeatureModel())
            SettingsDialogEvent.ClearBackupDirectory -> {
                if (!backupDirectoryUri.isNullOrBlank()) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            backupDirectoryUri.toUri(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                }
                onClearBackupDirectory()
            }
            SettingsDialogEvent.DismissRightActionDialog -> localState.dismissRightActionDialog()
            SettingsDialogEvent.DismissLeftActionDialog -> localState.dismissLeftActionDialog()
            SettingsDialogEvent.DismissClearBackupDirConfirmDialog ->
                localState.dismissClearBackupDirConfirmDialog()

            is SettingsDialogEvent.AppPassword -> {
                when (event.event) {
                    AppPasswordDialogEvent.DismissAction -> localState.dismissAppPasswordActionDialog()
                    AppPasswordDialogEvent.ShowChange -> localState.openChangeAppPasswordDialog()
                    AppPasswordDialogEvent.ShowDisable ->
                        submitAppPasswordAction(AppPasswordAction.DISABLE)
                    AppPasswordDialogEvent.DismissSet -> localState.dismissSetAppPasswordDialog()
                    AppPasswordDialogEvent.DismissChange -> localState.dismissChangeAppPasswordDialog()
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


                }
            }
        }
    }
