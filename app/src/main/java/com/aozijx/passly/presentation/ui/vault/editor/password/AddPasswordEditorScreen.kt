package com.aozijx.passly.presentation.ui.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.NextFocusTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.aozijx.passly.presentation.ui.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorSection
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Immutable
class PasswordEditorState(
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val tags: String,
    val isPasswordVisible: Boolean,
    val isFormValid: Boolean,
    val canSave: Boolean,
    val isSaving: Boolean,
)

data class PasswordEditorEventHandler(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onPasswordVisibilityChange: (Boolean) -> Unit,
    val onWebsiteChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onTagsChange: (String) -> Unit,
)

@Composable
fun AddPasswordEditorScreen(
    state: PasswordEditorState,
    onEvent: PasswordEditorEventHandler,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_password_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onEvent.onBack,
        onSave = onEvent.onSave,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = state.title,
                onValueChange = onEvent.onTitleChange,
                label = stringResource(R.string.field_title),
            )
            NextFocusTextField(
                value = state.username,
                onValueChange = onEvent.onUsernameChange,
                label = stringResource(R.string.field_username_hint),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            NextFocusTextField(
                value = state.password,
                onValueChange = onEvent.onPasswordChange,
                label = stringResource(R.string.password_label),
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    IconButton(onClick = { onEvent.onPasswordVisibilityChange(!state.isPasswordVisible) }) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isPasswordVisible) {
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
                value = state.tags,
                onValueChange = onEvent.onTagsChange,
                label = stringResource(R.string.field_category),
            )
            NextFocusTextField(
                value = state.website,
                onValueChange = onEvent.onWebsiteChange,
                label = stringResource(R.string.vault_add_password_website),
                keyboardType = KeyboardType.Uri,
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = onEvent.onNotesChange,
                label = { Text(stringResource(R.string.field_notes)) },
                singleLine = false,
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.isFormValid) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onEvent.onSave()
                        }
                    },
                ),
            )
        }
    }
}
