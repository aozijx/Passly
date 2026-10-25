package com.aozijx.passly.feature.vault.presentation.editor.otp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.ScannerImport
import com.aozijx.passly.feature.scanner.presentation.ScannerImportHandlingResult
import com.aozijx.passly.feature.scanner.presentation.ScannerPurpose
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffectHandler
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.feature.vault.presentation.editor.ui.otp.AddOtpEditorScreen

@Composable
fun AddOtpEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val viewModel = hiltViewModel<AddOtpViewModel>()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_otp_save_failed)
    val uriParsedMessage = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMessage = stringResource(R.string.vault_otp_uri_parse_failed)
    val saveActionModifier = Modifier.rememberAddEntryFabTransitionModifier(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    var overlay by remember { mutableStateOf<OtpEditorOverlay>(OtpEditorOverlay.None) }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    LaunchedEffect(
        viewModel,
        context,
        snackbarHostState,
        uriParsedMessage,
        uriParseFailedMessage,
    ) {
        viewModel.events.collect { event ->
            when (event) {
                AddOtpEvent.UriParsed ->
                    Toast.makeText(context, uriParsedMessage, Toast.LENGTH_SHORT).show()

                AddOtpEvent.UriParseFailed ->
                    snackbarHostState.showSnackbar(uriParseFailedMessage)
            }
        }
    }

    AddOtpEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onScan = { overlay = OtpEditorOverlay.Scanner },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )

    when (overlay) {
        OtpEditorOverlay.None -> Unit
        OtpEditorOverlay.Scanner -> ScannerRoute(
            purpose = ScannerPurpose.ApplyToOtpEditor,
            onImport = { scannerImport ->
                when (scannerImport) {
                    is ScannerImport.Otp -> {
                        viewModel.onAction(AddOtpAction.ScannedConfigApplied(scannerImport.config))
                        ScannerImportHandlingResult.Applied
                    }
                    is ScannerImport.Wifi -> ScannerImportHandlingResult.Rejected
                }
            },
            onDismiss = { overlay = OtpEditorOverlay.None },
        )
    }
}

private sealed interface OtpEditorOverlay {
    data object None : OtpEditorOverlay
    data object Scanner : OtpEditorOverlay
}
