package com.aozijx.passly.presentation.ui.settings.main

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogEvent
import com.aozijx.passly.presentation.ui.settings.main.model.AppPasswordDialogState
import com.aozijx.passly.presentation.ui.settings.main.model.SettingsDialogsModel
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordActionDialog
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordChangeDialog
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordChangeDialogEventHandler
import com.aozijx.passly.presentation.ui.settings.security.AppPasswordChangeDialogState
import com.aozijx.passly.presentation.ui.shared.components.apppassword.AppPasswordSetDialog

@Composable
internal fun SettingsDialogs(
    state: SettingsDialogsModel,
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
            eventHandler = object : AppPasswordChangeDialogEventHandler {
                override fun onCurrentPasswordChanged(password: String) =
                    onEvent(AppPasswordDialogEvent.CurrentChanged(password))
                override fun onNewPasswordChanged(password: String) =
                    onEvent(AppPasswordDialogEvent.NewChanged(password))
                override fun onConfirmPasswordChanged(password: String) =
                    onEvent(AppPasswordDialogEvent.ConfirmChanged(password))
                override fun onConfirm() = onEvent(AppPasswordDialogEvent.ConfirmChange)
                override fun onDismiss() = onEvent(AppPasswordDialogEvent.DismissChange)
            },
        )
    }
}
