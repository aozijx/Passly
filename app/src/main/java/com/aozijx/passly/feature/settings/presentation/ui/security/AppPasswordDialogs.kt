package com.aozijx.passly.feature.settings.presentation.ui.security

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialog
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogEvent
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogsModel
import com.aozijx.passly.presentation.mapping.toUiState
import com.aozijx.passly.presentation.shared.components.apppassword.AppPasswordSetDialog

@Composable
internal fun AppPasswordDialogs(
    state: AppPasswordDialogsModel,
    onEvent: (AppPasswordDialogEvent) -> Unit,
) {
    when (state.activeAppPasswordDialog) {
        null -> {
            Unit
        }

        AppPasswordDialog.ACTION -> {
            AppPasswordActionDialog(
                isBusy = state.isBusy,
                onDismiss = { onEvent(AppPasswordDialogEvent.DismissAction) },
                onChangePassword = { onEvent(AppPasswordDialogEvent.ShowChange) },
                onDisablePassword = { onEvent(AppPasswordDialogEvent.ShowDisable) },
            )
        }

        AppPasswordDialog.SET -> {
            AppPasswordSetDialog(
                newPassword = state.appPasswordNew,
                confirmPassword = state.appPasswordConfirm,
                strengthUiState = state.passwordStrength.toUiState(MaterialTheme.colorScheme),
                confirmationState = state.confirmationState,
                confirmEnabled = state.isSetPasswordConfirmEnabled,
                isBusy = state.isBusy,
                onNewPasswordChange = { onEvent(AppPasswordDialogEvent.NewChanged(it)) },
                onConfirmPasswordChange = { onEvent(AppPasswordDialogEvent.ConfirmChanged(it)) },
                onConfirm = { onEvent(AppPasswordDialogEvent.ConfirmSet) },
                onDismiss = { onEvent(AppPasswordDialogEvent.DismissSet) },
            )
        }

        AppPasswordDialog.CHANGE -> {
            AppPasswordChangeDialog(state = state, onEvent = onEvent)
        }
    }
}
