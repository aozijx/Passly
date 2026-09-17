package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.main.interaction.toFeatureModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel

internal fun buildSettingsDialogsState(
    localState: SettingsOverlayState,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
): SettingsDialogsModel = SettingsDialogsModel(
    showRightActionDialog = localState.showRightActionDialog,
    showLeftActionDialog = localState.showLeftActionDialog,
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
    localState: SettingsOverlayState,
    onSetSwipeRightAction: (SwipeActionType) -> Unit,
    onSetSwipeLeftAction: (SwipeActionType) -> Unit,
    submitAppPasswordAction: (AppPasswordAction) -> Unit,
): (SettingsDialogEvent) -> Unit = { event ->
    when (event) {
        is SettingsDialogEvent.SetSwipeRightAction ->
            onSetSwipeRightAction(event.action.toFeatureModel())
        is SettingsDialogEvent.SetSwipeLeftAction ->
            onSetSwipeLeftAction(event.action.toFeatureModel())
        SettingsDialogEvent.DismissRightActionDialog -> localState.dismissRightActionDialog()
        SettingsDialogEvent.DismissLeftActionDialog -> localState.dismissLeftActionDialog()
        is SettingsDialogEvent.AppPassword -> when (event.event) {
            AppPasswordDialogEvent.DismissAction -> localState.dismissAppPasswordActionDialog()
            AppPasswordDialogEvent.ShowChange -> localState.openChangeAppPasswordDialog()
            AppPasswordDialogEvent.ShowDisable ->
                submitAppPasswordAction(AppPasswordAction.DISABLE)
            AppPasswordDialogEvent.DismissSet -> localState.dismissSetAppPasswordDialog()
            AppPasswordDialogEvent.DismissChange -> localState.dismissChangeAppPasswordDialog()
            is AppPasswordDialogEvent.CurrentChanged ->
                localState.appPasswordCurrent = event.event.value
            is AppPasswordDialogEvent.NewChanged ->
                localState.appPasswordNew = event.event.value
            is AppPasswordDialogEvent.ConfirmChanged ->
                localState.appPasswordConfirm = event.event.value
            AppPasswordDialogEvent.ConfirmSet ->
                submitAppPasswordAction(AppPasswordAction.SET)
            AppPasswordDialogEvent.ConfirmChange ->
                submitAppPasswordAction(AppPasswordAction.CHANGE)
        }
    }
}
