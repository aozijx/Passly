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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffectHandler
import com.aozijx.passly.presentation.feature.scanner.VaultScanner
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.presentation.feature.vault.editor.ui.otp.AddOtpEditorScreen

@Composable
fun AddOtpEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: AddOtpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_otp_save_failed)
    val uriParsedMessage = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMessage = stringResource(R.string.vault_otp_uri_parse_failed)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }
    var showScanner by remember { mutableStateOf(false) }

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
        onScan = { showScanner = true },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )

    if (showScanner) {
        VaultScanner(
            onSaveOtp = { config ->
                viewModel.onAction(AddOtpAction.ScannedConfigApplied(config))
            },
            onDismiss = { showScanner = false },
        )
    }
}
