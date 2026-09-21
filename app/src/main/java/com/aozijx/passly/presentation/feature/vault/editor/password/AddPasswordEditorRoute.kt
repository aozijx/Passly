package com.aozijx.passly.presentation.feature.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffectHandler
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.presentation.feature.vault.editor.ui.password.AddPasswordEditorScreen
import com.aozijx.passly.presentation.feature.vault.editor.ui.password.PasswordEditorEventHandler
import com.aozijx.passly.presentation.feature.vault.editor.ui.password.PasswordEditorState

@Composable
fun AddPasswordEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: AddPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_password_save_failed)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }
    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)

    fun submit(action: AddPasswordAction) {
        viewModel.onAction(action)
    }

    val save = {
        keyboardController?.hide()
        submit(AddPasswordAction.Save)
    }
    val form = uiState.form
    AddPasswordEditorScreen(
        state = PasswordEditorState(
            title = form.title,
            username = form.username,
            password = form.password,
            website = form.website,
            notes = form.notes,
            tags = form.tags,
            isPasswordVisible = form.isPasswordVisible,
            isFormValid = form.isValid,
            canSave = uiState.canSave,
            isSaving = uiState.isSaving,
        ),
        onEvent = PasswordEditorEventHandler(
            onBack = onBack,
            onSave = save,
            onTitleChange = { submit(AddPasswordAction.TitleChanged(it)) },
            onUsernameChange = { submit(AddPasswordAction.UsernameChanged(it)) },
            onPasswordChange = { submit(AddPasswordAction.PasswordChanged(it)) },
            onPasswordVisibilityChange = {
                submit(AddPasswordAction.PasswordVisibilityChanged(it))
            },
            onWebsiteChange = { submit(AddPasswordAction.WebsiteChanged(it)) },
            onNotesChange = { submit(AddPasswordAction.NotesChanged(it)) },
            onTagsChange = { submit(AddPasswordAction.TagsChanged(it)) },
        ),
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )
}
