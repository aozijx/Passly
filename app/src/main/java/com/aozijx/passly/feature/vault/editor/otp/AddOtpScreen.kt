package com.aozijx.passly.feature.vault.editor.otp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.ui.components.AppTextField
import com.aozijx.passly.feature.vault.components.editor.OtpConfigForm
import com.aozijx.passly.feature.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.editor.common.CreateEntryEffect

@Composable
fun AddOtpScreen(
    viewModel: AddOtpViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_otp_save_failed)
    val uriParsedMessage = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMessage = stringResource(R.string.vault_otp_uri_parse_failed)
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

    LaunchedEffect(viewModel, snackbarHostState, uriParsedMessage, uriParseFailedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                AddOtpEvent.UriParsed -> {
                    Toast.makeText(context, uriParsedMessage, Toast.LENGTH_SHORT).show()
                    ClipboardUtils.clear(context)
                }

                AddOtpEvent.UriParseFailed -> {
                    snackbarHostState.showSnackbar(uriParseFailedMessage)
                }
            }
        }
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_otp_title),
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
        AppTextField(
            value = uiState.form.title,
            onValueChange = {
                onUserInteraction()
                viewModel.updateForm(uiState.form.copy(title = it))
            },
            label = stringResource(R.string.title)
        )

        AppTextField(
            value = uiState.form.uriText,
            onValueChange = {
                onUserInteraction()
                viewModel.updateUri(it)
            },
            label = stringResource(R.string.twofa_uri_hint),
            trailingIcon = {
                TextButton(
                    onClick = {
                        onUserInteraction()
                        viewModel.updateUri(
                            value = ClipboardUtils.getText(context),
                            reportFailure = true
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.paste))
                }
            }
        )

        OtpConfigForm(
            state = uiState.form,
            onFieldUpdate = {
                onUserInteraction()
                viewModel.updateForm(it)
            },
            onTypeChange = {
                onUserInteraction()
                viewModel.updateType(it)
            }
        )
    }
}
