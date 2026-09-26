package com.aozijx.passly.presentation.feature.vault.editor.ui.password

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordAction
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordUiState
import com.aozijx.passly.presentation.shared.components.NextFocusTextField
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.EntryEditorSection

@Composable
fun AddPasswordEditorScreen(
    state: AddPasswordUiState,
    onAction: (AddPasswordAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = state.form

    fun save() {
        keyboardController?.hide()
        onAction(AddPasswordAction.Save)
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_password_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = ::save,
        modifier = modifier,
        saveActionModifier = saveActionModifier,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = form.title,
                onValueChange = { onAction(AddPasswordAction.TitleChanged(it)) },
                label = stringResource(R.string.field_title),
            )
            NextFocusTextField(
                value = form.username,
                onValueChange = { onAction(AddPasswordAction.UsernameChanged(it)) },
                label = stringResource(R.string.field_username_hint),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            NextFocusTextField(
                value = form.password,
                onValueChange = { onAction(AddPasswordAction.PasswordChanged(it)) },
                label = stringResource(R.string.password_label),
                visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onAction(AddPasswordAction.PasswordVisibilityChanged(!form.isPasswordVisible))
                        },
                    ) {
                        Icon(
                            imageVector = if (form.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (form.isPasswordVisible) {
                                stringResource(R.string.hide_password)
                            } else {
                                stringResource(R.string.show_password)
                            }
                        )
                    }
                }
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            NextFocusTextField(
                value = form.tags,
                onValueChange = { onAction(AddPasswordAction.TagsChanged(it)) },
                label = stringResource(R.string.field_category),
            )
            NextFocusTextField(
                value = form.website,
                onValueChange = { onAction(AddPasswordAction.WebsiteChanged(it)) },
                label = stringResource(R.string.vault_add_password_website),
                keyboardType = KeyboardType.Uri,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.notes,
                onValueChange = { onAction(AddPasswordAction.NotesChanged(it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                singleLine = false,
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (form.isValid) {
                            focusManager.clearFocus(force = true)
                            save()
                        }
                    },
                ),
            )
        }
    }
}
