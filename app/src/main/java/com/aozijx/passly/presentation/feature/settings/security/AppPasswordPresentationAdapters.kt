package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.AppPasswordDialogStateHolder
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogsModel

internal fun buildAppPasswordDialogsModel(
    stateHolder: AppPasswordDialogStateHolder,
): AppPasswordDialogsModel = AppPasswordDialogsModel(
    activeAppPasswordDialog = stateHolder.activeAppPasswordDialog,
    appPasswordCurrent = stateHolder.appPasswordCurrent,
    appPasswordNew = stateHolder.appPasswordNew,
    appPasswordConfirm = stateHolder.appPasswordConfirm,
    isSetPasswordConfirmEnabled = AppPasswordPolicy.DEFAULT.acceptsLength(
        stateHolder.appPasswordNew.length,
    ) && stateHolder.appPasswordNew == stateHolder.appPasswordConfirm,
    isChangePasswordConfirmEnabled = stateHolder.appPasswordCurrent.isNotEmpty() &&
        AppPasswordPolicy.DEFAULT.acceptsLength(stateHolder.appPasswordNew.length) &&
        stateHolder.appPasswordNew == stateHolder.appPasswordConfirm,
)

internal fun buildAppPasswordDialogEventHandler(
    stateHolder: AppPasswordDialogStateHolder,
    submitAppPasswordAction: (AppPasswordAction) -> Unit,
): (AppPasswordDialogEvent) -> Unit = { event ->
    when (event) {
        AppPasswordDialogEvent.DismissAction -> stateHolder.dismissAppPasswordActionDialog()
        AppPasswordDialogEvent.ShowChange -> stateHolder.openChangeAppPasswordDialog()
        AppPasswordDialogEvent.ShowDisable -> submitAppPasswordAction(AppPasswordAction.DISABLE)
        AppPasswordDialogEvent.DismissSet -> stateHolder.dismissSetAppPasswordDialog()
        AppPasswordDialogEvent.DismissChange -> stateHolder.dismissChangeAppPasswordDialog()
        is AppPasswordDialogEvent.CurrentChanged -> stateHolder.appPasswordCurrent = event.value
        is AppPasswordDialogEvent.NewChanged -> stateHolder.appPasswordNew = event.value
        is AppPasswordDialogEvent.ConfirmChanged -> stateHolder.appPasswordConfirm = event.value
        AppPasswordDialogEvent.ConfirmSet -> submitAppPasswordAction(AppPasswordAction.SET)
        AppPasswordDialogEvent.ConfirmChange -> submitAppPasswordAction(AppPasswordAction.CHANGE)
    }
}
