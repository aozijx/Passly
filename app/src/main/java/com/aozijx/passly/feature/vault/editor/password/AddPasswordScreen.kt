package com.aozijx.passly.feature.vault.editor.password

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.AppTextField
import com.aozijx.passly.core.ui.components.PasswordInput
import com.aozijx.passly.feature.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.editor.common.CreateEntryEffect

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
    onSave: () -> Unit
) {
    AppTextField(
        value = state.title,
        onValueChange = onTitleChange,
        label = stringResource(R.string.title),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
    AppTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        label = stringResource(R.string.username_hint),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )
    PasswordInput(
        password = state.password,
        onPasswordChange = onPasswordChange,
        isVisible = state.isPasswordVisible,
        onVisibilityChange = onPasswordVisibilityChange
    )
    AppTextField(
        value = state.website,
        onValueChange = onWebsiteChange,
        label = stringResource(R.string.vault_add_password_website),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
        )
    )
    OutlinedTextField(
        value = state.notes,
        onValueChange = onNotesChange,
        label = { Text(stringResource(R.string.remark)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        maxLines = 8,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (state.isValid) onSave()
            }
        )
    )
}
