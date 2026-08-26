package com.aozijx.passly.presentation.feature.vault.editor.otp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.presentation.ui.vault.editor.otp.AddOtpEditorScreen
import com.aozijx.passly.presentation.ui.vault.editor.otp.OtpEditorAlgorithm
import com.aozijx.passly.presentation.ui.vault.editor.otp.OtpEditorEncoding
import com.aozijx.passly.presentation.ui.vault.editor.otp.OtpEditorEventHandler
import com.aozijx.passly.presentation.ui.vault.editor.otp.OtpEditorState
import com.aozijx.passly.presentation.ui.vault.editor.otp.OtpEditorType

@Composable
fun AddOtpEditorHost(
    viewModel: AddOtpViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scannerContent: @Composable (
        onResult: (OtpConfig) -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit,
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
                AddOtpEffect.Saved -> latestOnSaved()
                is AddOtpEffect.SaveFailed -> {
                    snackbarHostState.showSnackbar(effect.message ?: saveFailedMessage)
                }
            }
        }
    }
    LaunchedEffect(viewModel, snackbarHostState, uriParsedMessage, uriParseFailedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                AddOtpEvent.UriParsed ->
                    Toast.makeText(context, uriParsedMessage, Toast.LENGTH_SHORT).show()

                AddOtpEvent.UriParseFailed ->
                    snackbarHostState.showSnackbar(uriParseFailedMessage)
            }
        }
    }

    val form = uiState.form
    fun updateForm(transform: (OtpFormState) -> OtpFormState) {
        onUserInteraction()
        viewModel.onAction(AddOtpAction.FormChanged(transform(form)))
    }
    val save = {
        keyboardController?.hide()
        viewModel.onAction(AddOtpAction.Save)
    }
    AddOtpEditorScreen(
        state = form.toEditorState(uiState.canSave, uiState.isSaving),
        onEvent = OtpEditorEventHandler(
            onBack = onBack,
            onSave = save,
            onScan = {
                onUserInteraction()
                keyboardController?.hide()
                showScanner = true
            },
            onTitleChange = { updateForm { current -> current.copy(title = it) } },
            onUriChange = {
                onUserInteraction()
                viewModel.onAction(AddOtpAction.UriChanged(it))
            },
            onIssuerChange = { updateForm { current -> current.copy(issuer = it) } },
            onAccountNameChange = {
                updateForm { current -> current.copy(accountName = it) }
            },
            onSecretChange = { updateForm { current -> current.copy(secret = it) } },
            onPeriodChange = { updateForm { current -> current.copy(period = it) } },
            onDigitsChange = { updateForm { current -> current.copy(digits = it) } },
            onTypeChange = {
                onUserInteraction()
                viewModel.onAction(AddOtpAction.TypeChanged(it.toDomainType()))
            },
            onAlgorithmChange = {
                updateForm { current -> current.copy(algorithm = it.name) }
            },
            onEncodingChange = {
                updateForm { current -> current.copy(encoding = it.toDomainEncoding()) }
            },
            onCounterChange = { updateForm { current -> current.copy(counter = it) } },
        ),
        snackbarHostState = snackbarHostState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )

    if (showScanner) {
        scannerContent(
            { config ->
                onUserInteraction()
                viewModel.onAction(AddOtpAction.ScannedConfigApplied(config))
            },
            { showScanner = false },
        )
    }
}

private fun OtpFormState.toEditorState(canSave: Boolean, isSaving: Boolean) = OtpEditorState(
    title = title,
    issuer = issuer,
    accountName = accountName,
    secret = secret,
    period = period,
    digits = digits,
    type = OtpEditorType.valueOf(type.name),
    algorithm = OtpEditorAlgorithm.valueOf(
        OtpHashAlgorithm.entries.firstOrNull { it.name == algorithm }?.name
            ?: OtpHashAlgorithm.SHA1.name,
    ),
    encoding = OtpEditorEncoding.valueOf(encoding.name),
    counter = counter,
    uriText = uriText,
    canSave = canSave,
    isSaving = isSaving,
)

private fun OtpEditorType.toDomainType() = OtpType.valueOf(name)

private fun OtpEditorEncoding.toDomainEncoding() = OtpSecretEncoding.valueOf(name)
