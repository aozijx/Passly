package com.aozijx.passly.feature.vault.editor.password

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
import com.aozijx.passly.feature.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.editor.common.CreateEntryEffect
import com.aozijx.passly.feature.vault.editor.common.EntryEditorSection
import com.aozijx.passly.feature.vault.editor.common.EntryNotesField
import com.aozijx.passly.feature.vault.editor.common.EntryPasswordField
import com.aozijx.passly.feature.vault.editor.common.EntryTagsField
import com.aozijx.passly.feature.vault.editor.common.EntryTitleField
import com.aozijx.passly.feature.vault.editor.common.EntryUsernameField
import com.aozijx.passly.feature.vault.editor.common.EntryWebsiteField

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
                CreateEntryEffect.Saved -> latestOnSaved()
                is CreateEntryEffect.SaveFailed -> {
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
            viewModel.save()
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    ) {
        PasswordForm(
            state = uiState.form,
            onTitleChange = {
                onUserInteraction()
                viewModel.updateTitle(it)
            },
            onUsernameChange = {
                onUserInteraction()
                viewModel.updateUsername(it)
            },
            onPasswordChange = {
                onUserInteraction()
                viewModel.updatePassword(it)
            },
            onPasswordVisibilityChange = viewModel::setPasswordVisible,
            onWebsiteChange = {
                onUserInteraction()
                viewModel.updateWebsite(it)
            },
            onNotesChange = {
                onUserInteraction()
                viewModel.updateNotes(it)
            },
            onTagsChange = {
                onUserInteraction()
                viewModel.updateTags(it)
            },
            onSave = {
                keyboardController?.hide()
                viewModel.save()
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
            label = stringResource(R.string.title)
        )
        EntryUsernameField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.username_hint)
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
            label = stringResource(R.string.entry_category)
        )
        EntryWebsiteField(
            value = state.website,
            onValueChange = onWebsiteChange,
            label = stringResource(R.string.vault_add_password_website)
        )
        EntryNotesField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.remark),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (state.isValid) onSave()
                }
            )
        )
    }
}
