package com.aozijx.passly.presentation.feature.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordViewModel

import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordAction
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordFormState
import com.aozijx.passly.presentation.feature.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryEditorSection
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryNotesField
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryPasswordField
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryTagsField
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryTitleField
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryUsernameField
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryWebsiteField

@Composable
fun AddPasswordScreen(
    viewModel: AddPasswordViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_password_save_failed)
    val latestOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel, snackbarHostState, saveFailedMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddPasswordEffect.Saved -> latestOnSaved()
                is AddPasswordEffect.SaveFailed -> {
                    snackbarHostState.showSnackbar(effect.message ?: saveFailedMessage)
                }
            }
        }
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_password_title),
        canSave = uiState.canSave,
        isSaving = uiState.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = {
            keyboardController?.hide()
            viewModel.onAction(AddPasswordAction.Save)
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    ) {
        PasswordForm(
            state = uiState.form,
            onTitleChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.TitleChanged(it))
            },
            onUsernameChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.UsernameChanged(it))
            },
            onPasswordChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.PasswordChanged(it))
            },
            onPasswordVisibilityChange = {
                viewModel.onAction(AddPasswordAction.PasswordVisibilityChanged(it))
            },
            onWebsiteChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.WebsiteChanged(it))
            },
            onNotesChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.NotesChanged(it))
            },
            onTagsChange = {
                onUserInteraction()
                viewModel.onAction(AddPasswordAction.TagsChanged(it))
            },
            onSave = {
                keyboardController?.hide()
                viewModel.onAction(AddPasswordAction.Save)
            }
        )
    }
}

@Composable
private fun PasswordForm(
    state: AddPasswordFormState,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
        EntryTitleField(
            value = state.title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.field_title)
        )
        EntryUsernameField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.field_username_hint)
        )
    }

    EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
        EntryPasswordField(
            password = state.password,
            onPasswordChange = onPasswordChange,
            isVisible = state.isPasswordVisible,
            onVisibilityChange = onPasswordVisibilityChange
        )
    }

    EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
        EntryTagsField(
            value = state.tags,
            onValueChange = onTagsChange,
            label = stringResource(R.string.field_category)
        )
        EntryWebsiteField(
            value = state.website,
            onValueChange = onWebsiteChange,
            label = stringResource(R.string.vault_add_password_website)
        )
        EntryNotesField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.field_notes),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (state.isValid) onSave()
                }
            )
        )
    }
}
