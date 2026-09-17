package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel

internal fun buildSettingsDialogsState(
    localState: SettingsOverlayState,
): SettingsDialogsModel = SettingsDialogsModel(
    activeAppPasswordDialog = localState.activeAppPasswordDialog,
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
    localState: SettingsOverlayState,
    submitAppPasswordAction: (AppPasswordAction) -> Unit,
): (AppPasswordDialogEvent) -> Unit = { event ->
    when (event) {
        AppPasswordDialogEvent.DismissAction -> localState.dismissAppPasswordActionDialog()
        AppPasswordDialogEvent.ShowChange -> localState.openChangeAppPasswordDialog()
        AppPasswordDialogEvent.ShowDisable -> submitAppPasswordAction(AppPasswordAction.DISABLE)
        AppPasswordDialogEvent.DismissSet -> localState.dismissSetAppPasswordDialog()
        AppPasswordDialogEvent.DismissChange -> localState.dismissChangeAppPasswordDialog()
        is AppPasswordDialogEvent.CurrentChanged -> localState.appPasswordCurrent = event.value
        is AppPasswordDialogEvent.NewChanged -> localState.appPasswordNew = event.value
        is AppPasswordDialogEvent.ConfirmChanged -> localState.appPasswordConfirm = event.value
        AppPasswordDialogEvent.ConfirmSet -> submitAppPasswordAction(AppPasswordAction.SET)
        AppPasswordDialogEvent.ConfirmChange -> submitAppPasswordAction(AppPasswordAction.CHANGE)
    }
}
