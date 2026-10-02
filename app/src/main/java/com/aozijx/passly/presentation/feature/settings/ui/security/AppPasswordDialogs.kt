package com.aozijx.passly.presentation.feature.settings.ui.security

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordDialogEvent
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordDialogState
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordDialogsModel
import com.aozijx.passly.presentation.shared.components.apppassword.AppPasswordSetDialog

@Composable
internal fun AppPasswordDialogs(
    state: AppPasswordDialogsModel,
    onEvent: (AppPasswordDialogEvent) -> Unit,
) {
    when (state.activeAppPasswordDialog) {
        AppPasswordDialogState.None -> Unit
        AppPasswordDialogState.Action -> AppPasswordActionDialog(
            onDismiss = { onEvent(AppPasswordDialogEvent.DismissAction) },
            onChangePassword = { onEvent(AppPasswordDialogEvent.ShowChange) },
            onDisablePassword = { onEvent(AppPasswordDialogEvent.ShowDisable) },
        )
        AppPasswordDialogState.Set -> AppPasswordSetDialog(
            newPassword = state.appPasswordNew,
            confirmPassword = state.appPasswordConfirm,
            confirmEnabled = state.isSetPasswordConfirmEnabled,
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
