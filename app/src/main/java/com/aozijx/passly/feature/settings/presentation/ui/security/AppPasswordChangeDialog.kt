package com.aozijx.passly.feature.settings.presentation.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogEvent
import com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogsModel
import com.aozijx.passly.presentation.shared.components.apppassword.PasswordFields
import com.aozijx.passly.presentation.mapping.toUiState

@Composable
internal fun AppPasswordChangeDialog(
    state: AppPasswordDialogsModel,
    onEvent: (AppPasswordDialogEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissInput = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val submit = {
        if (!state.isBusy && state.isChangePasswordConfirmEnabled) {
            dismissInput()
            onEvent(AppPasswordDialogEvent.ConfirmChange)
        }
    }
    val dismiss = {
        dismissInput()
        if (!state.isBusy) onEvent(AppPasswordDialogEvent.DismissChange)
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.settings_auth_change_app_password)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.appPasswordCurrent,
                    onValueChange = {
                        onEvent(AppPasswordDialogEvent.CurrentChanged(it))
                    },
                    enabled = !state.isBusy,
                    label = { Text(stringResource(R.string.settings_auth_current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        ),
                    modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordFields(
                    newPassword = state.appPasswordNew,
                    confirmPassword = state.appPasswordConfirm,
                    onNewPasswordChange = {
                        onEvent(AppPasswordDialogEvent.NewChanged(it))
                    },
                    onConfirmPasswordChange = {
                        onEvent(AppPasswordDialogEvent.ConfirmChanged(it))
                    },
                    onDone = submit,
                    strengthUiState = state.passwordStrength.toUiState(MaterialTheme.colorScheme),
                    confirmationState = state.confirmationState,
                    enabled = !state.isBusy,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = submit,
                enabled = !state.isBusy && state.isChangePasswordConfirmEnabled,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = dismiss,
                enabled = !state.isBusy,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
