package com.aozijx.passly.feature.vault.editor.otp

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.components.editor.OtpConfigForm
import com.aozijx.passly.feature.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.editor.common.CreateEntryEffect
import com.aozijx.passly.feature.vault.editor.common.EntryEditorSection
import com.aozijx.passly.feature.vault.editor.common.EntryEditorTextField
import com.aozijx.passly.feature.vault.editor.common.EntryTitleField

@Composable
fun AddOtpScreen(
    viewModel: AddOtpViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scannerContent: @Composable (
        onResult: (OtpConfig) -> Unit,
        onDismiss: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_otp_save_failed)
    val uriParsedMessage = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMessage = stringResource(R.string.vault_otp_uri_parse_failed)
    val latestOnSaved by rememberUpdatedState(onSaved)
    var showScanner by remember { mutableStateOf(false) }

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
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            EntryTitleField(
                value = uiState.form.title,
                onValueChange = {
                    onUserInteraction()
                    viewModel.updateForm(uiState.form.copy(title = it))
                },
                label = stringResource(R.string.title)
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_otp_setup)) {
            EntryEditorTextField(
                value = uiState.form.uriText,
                onValueChange = {
                    onUserInteraction()
                    viewModel.updateUri(it)
                },
                label = stringResource(R.string.twofa_uri_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            onUserInteraction()
                            keyboardController?.hide()
                            showScanner = true
                        }
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.vault_scan))
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

    if (showScanner) {
        scannerContent(
            { config ->
                onUserInteraction()
                viewModel.applyScannedConfig(config)
            },
            { showScanner = false }
        )
    }
}
