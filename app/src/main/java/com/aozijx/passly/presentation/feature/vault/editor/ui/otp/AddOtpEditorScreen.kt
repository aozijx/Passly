package com.aozijx.passly.presentation.feature.vault.editor.ui.otp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpAction
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpCodeState
import com.aozijx.passly.presentation.shared.components.NextFocusTextField
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.EntryEditorSection

@Composable
fun AddOtpEditorScreen(
    state: AddOtpCodeState,
    onAction: (AddOtpAction) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = state.form
    AddEntryScaffold(
        title = stringResource(R.string.vault_add_otp_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = {
            keyboardController?.hide()
            onAction(AddOtpAction.Save)
        },
        modifier = modifier,
        saveActionModifier = saveActionModifier,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = form.title,
                onValueChange = {
                    onAction(AddOtpAction.FormChanged(form.copy(title = it)))
                },
                label = stringResource(R.string.field_title),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_otp_setup)) {
            NextFocusTextField(
                value = form.uriText,
                onValueChange = { onAction(AddOtpAction.UriChanged(it)) },
                label = stringResource(R.string.twofa_uri_hint),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            onScan()
                        },
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(stringResource(R.string.vault_scan))
                    }
                },
            )

            OtpConfigForm(state = form, onAction = onAction)
        }
    }
}
