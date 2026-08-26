package com.aozijx.passly.presentation.ui.vault.editor.otp

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorSection
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorTextField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryTitleField

@Composable
fun AddOtpEditorScreen(
    state: OtpEditorState,
    onEvent: OtpEditorEventHandler,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    AddEntryScaffold(
        title = stringResource(R.string.vault_add_otp_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onEvent.onBack,
        onSave = onEvent.onSave,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            EntryTitleField(
                value = state.title,
                onValueChange = onEvent.onTitleChange,
                label = stringResource(R.string.field_title),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_otp_setup)) {
            EntryEditorTextField(
                value = state.uriText,
                onValueChange = onEvent.onUriChange,
                label = stringResource(R.string.twofa_uri_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    TextButton(onClick = onEvent.onScan) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(stringResource(R.string.vault_scan))
                    }
                },
            )

            OtpConfigForm(state = state, onEvent = onEvent)
        }
    }
}
