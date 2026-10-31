package com.aozijx.passly.feature.settings.presentation.ui.security

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogEvent
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogState
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogsModel
import com.aozijx.passly.presentation.shared.components.apppassword.AppPasswordSetDialog

@Composable
internal fun AppPasswordDialogs(
    state: AppPasswordDialogsModel,
    onEvent: (AppPasswordDialogEvent) -> Unit,
) {
    when (state.activeAppPasswordDialog) {
        AppPasswordDialogState.None -> Unit
        AppPasswordDialogState.Action -> AppPasswordActionDialog(
            isBusy = state.isBusy,
            onDismiss = { onEvent(AppPasswordDialogEvent.DismissAction) },
            onChangePassword = { onEvent(AppPasswordDialogEvent.ShowChange) },
            onDisablePassword = { onEvent(AppPasswordDialogEvent.ShowDisable) },
        )
        AppPasswordDialogState.Set -> AppPasswordSetDialog(
            newPassword = state.appPasswordNew,
            confirmPassword = state.appPasswordConfirm,
            confirmEnabled = state.isSetPasswordConfirmEnabled,
            isBusy = state.isBusy,
            onNewPasswordChange = { onEvent(AppPasswordDialogEvent.NewChanged(it)) },
            onConfirmPasswordChange = { onEvent(AppPasswordDialogEvent.ConfirmChanged(it)) },
            onConfirm = { onEvent(AppPasswordDialogEvent.ConfirmSet) },
            onDismiss = { onEvent(AppPasswordDialogEvent.DismissSet) },
        )
        AppPasswordDialogState.Change -> AppPasswordChangeDialog(
            state = AppPasswordChangeDialogState(
                currentPassword = state.appPasswordCurrent,
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                confirmEnabled = state.isChangePasswordConfirmEnabled,
                isBusy = state.isBusy,
            ),
            onEvent = { event ->
                when (event) {
                    is AppPasswordChangeDialogEvent.CurrentPasswordChanged ->
                        onEvent(AppPasswordDialogEvent.CurrentChanged(event.password))
                    is AppPasswordChangeDialogEvent.NewPasswordChanged ->
                        onEvent(AppPasswordDialogEvent.NewChanged(event.password))
                    is AppPasswordChangeDialogEvent.ConfirmPasswordChanged ->
                        onEvent(AppPasswordDialogEvent.ConfirmChanged(event.password))
                    AppPasswordChangeDialogEvent.Confirmed ->
                        onEvent(AppPasswordDialogEvent.ConfirmChange)
                    AppPasswordChangeDialogEvent.Dismissed ->
                        onEvent(AppPasswordDialogEvent.DismissChange)
                }
            },
        )
    }
}
