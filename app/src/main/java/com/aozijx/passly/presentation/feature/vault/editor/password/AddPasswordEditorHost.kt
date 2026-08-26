package com.aozijx.passly.presentation.feature.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
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
import com.aozijx.passly.presentation.ui.vault.editor.password.AddPasswordEditorScreen
import com.aozijx.passly.presentation.ui.vault.editor.password.PasswordEditorEventHandler
import com.aozijx.passly.presentation.ui.vault.editor.password.PasswordEditorState

@Composable
fun AddPasswordEditorHost(
    viewModel: AddPasswordViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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

    fun submit(action: AddPasswordAction, userInitiated: Boolean = false) {
        if (userInitiated) onUserInteraction()
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
            onTitleChange = { submit(AddPasswordAction.TitleChanged(it), true) },
            onUsernameChange = { submit(AddPasswordAction.UsernameChanged(it), true) },
            onPasswordChange = { submit(AddPasswordAction.PasswordChanged(it), true) },
            onPasswordVisibilityChange = {
                submit(AddPasswordAction.PasswordVisibilityChanged(it))
            },
            onWebsiteChange = { submit(AddPasswordAction.WebsiteChanged(it), true) },
            onNotesChange = { submit(AddPasswordAction.NotesChanged(it), true) },
            onTagsChange = { submit(AddPasswordAction.TagsChanged(it), true) },
        ),
        snackbarHostState = snackbarHostState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}
