package com.aozijx.passly.presentation.ui.unlock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

internal enum class AuthenticationMethodUiModel {
    BIOMETRIC, APP_PASSWORD, RECOVERY_CODE,
}

internal sealed interface AuthenticationDialogUiState {
    data class ChooseMethod(val methods: List<AuthenticationMethodUiModel>) : AuthenticationDialogUiState
    data class Secret(val method: AuthenticationMethodUiModel) : AuthenticationDialogUiState
}

internal interface AuthenticationDialogEventHandler {
    fun onMethodSelected(method: AuthenticationMethodUiModel?)
    fun onSecretSubmitted(secret: CharArray?)
}

@Composable
internal fun AuthenticationDialogs(
    state: AuthenticationDialogUiState?,
    events: AuthenticationDialogEventHandler,
) {
    when (state) {
        is AuthenticationDialogUiState.ChooseMethod -> MethodDialog(
            methods = state.methods,
            onSelected = events::onMethodSelected,
            onCancel = { events.onMethodSelected(null) },
        )
        is AuthenticationDialogUiState.Secret -> SecretDialog(
            method = state.method,
            onSubmit = events::onSecretSubmitted,
            onCancel = { events.onSecretSubmitted(null) },
        )
        null -> Unit
    }
}

@Composable
private fun MethodDialog(
    methods: List<AuthenticationMethodUiModel>,
    onSelected: (AuthenticationMethodUiModel) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.auth_verify_identity)) },
        text = {
            Column {
                methods.forEach { method ->
                    TextButton(onClick = { onSelected(method) }, modifier = Modifier.fillMaxWidth()) {
                        Text(method.label())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SecretDialog(
    method: AuthenticationMethodUiModel,
    onSubmit: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var secret by remember(method) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissInput = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val submit = {
        if (secret.isNotEmpty()) {
            val chars = secret.toCharArray()
            secret = ""
            dismissInput()
            onSubmit(chars)
        }
    }
    val cancel = {
        secret = ""
        dismissInput()
        onCancel()
    }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(method.label()) },
        text = {
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = {
                    Text(stringResource(if (method == AuthenticationMethodUiModel.RECOVERY_CODE) R.string.recovery_code_label else R.string.auth_app_password_label))
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        },
        confirmButton = {
            Button(enabled = secret.isNotEmpty(), onClick = submit) {
                Text(stringResource(R.string.continue_action))
            }
        },
        dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun AuthenticationMethodUiModel.label(): String = stringResource(
    when (this) {
        AuthenticationMethodUiModel.BIOMETRIC -> R.string.auth_method_biometric
        AuthenticationMethodUiModel.APP_PASSWORD -> R.string.auth_app_password_label
        AuthenticationMethodUiModel.RECOVERY_CODE -> R.string.restore_access
    },
)
