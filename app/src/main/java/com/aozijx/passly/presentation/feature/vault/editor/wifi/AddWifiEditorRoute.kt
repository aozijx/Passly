package com.aozijx.passly.presentation.feature.vault.editor.wifi

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffectHandler
import com.aozijx.passly.presentation.feature.scanner.ScannerImport
import com.aozijx.passly.presentation.feature.scanner.ScannerImportHandlingResult
import com.aozijx.passly.presentation.feature.scanner.ScannerPurpose
import com.aozijx.passly.presentation.feature.scanner.ScannerRoute
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.presentation.feature.vault.editor.ui.wifi.AddWifiEditorScreen

@Composable
fun AddWifiEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialContent: AddWifiInitialContent = AddWifiInitialContent.Blank,
) {
    val viewModel = hiltViewModel<AddWifiViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_wifi_save_failed)
    val seedUnavailableMessage = stringResource(R.string.wifi_scanned_seed_unavailable)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }
    var overlay by remember { mutableStateOf<WifiEditorOverlay>(WifiEditorOverlay.None) }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    LaunchedEffect(viewModel, initialContent) {
        viewModel.initialize(initialContent)
        if (initialContent == AddWifiInitialContent.SeedUnavailable) {
            snackbarHostState.showSnackbar(seedUnavailableMessage)
        }
    }

    AddWifiEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onScan = { overlay = WifiEditorOverlay.Scanner },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )

    when (overlay) {
        WifiEditorOverlay.None -> Unit
        WifiEditorOverlay.Scanner -> ScannerRoute(
            purpose = ScannerPurpose.ApplyToWifiEditor,
            onImport = { scannerImport ->
                when (scannerImport) {
                    is ScannerImport.Wifi -> {
                        viewModel.onAction(AddWifiAction.ApplyScannedWifi(scannerImport.payload))
                        ScannerImportHandlingResult.Applied
                    }
                    is ScannerImport.Otp -> ScannerImportHandlingResult.Rejected
                }
            },
            onDismiss = { overlay = WifiEditorOverlay.None },
        )
    }
}

private sealed interface WifiEditorOverlay {
    data object None : WifiEditorOverlay
    data object Scanner : WifiEditorOverlay
}
