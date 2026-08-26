package com.aozijx.passly.presentation.ui.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorSection
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryNotesField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryPasswordField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryTagsField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryTitleField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryUsernameField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryWebsiteField

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
            EntryTitleField(
                value = state.title,
                onValueChange = onEvent.onTitleChange,
                label = stringResource(R.string.field_title),
            )
            EntryUsernameField(
                value = state.username,
                onValueChange = onEvent.onUsernameChange,
                label = stringResource(R.string.field_username_hint),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            EntryPasswordField(
                password = state.password,
                onPasswordChange = onEvent.onPasswordChange,
                isVisible = state.isPasswordVisible,
                onVisibilityChange = onEvent.onPasswordVisibilityChange,
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            EntryTagsField(
                value = state.tags,
                onValueChange = onEvent.onTagsChange,
                label = stringResource(R.string.field_category),
            )
            EntryWebsiteField(
                value = state.website,
                onValueChange = onEvent.onWebsiteChange,
                label = stringResource(R.string.vault_add_password_website),
            )
            EntryNotesField(
                value = state.notes,
                onValueChange = onEvent.onNotesChange,
                label = stringResource(R.string.field_notes),
                keyboardActions = KeyboardActions(
                    onDone = { if (state.isFormValid) onEvent.onSave() },
                ),
            )
        }
    }
}
