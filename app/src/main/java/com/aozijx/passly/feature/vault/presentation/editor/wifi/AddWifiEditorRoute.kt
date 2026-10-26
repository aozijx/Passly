package com.aozijx.passly.feature.vault.presentation.editor.wifi

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffectHandler
import com.aozijx.passly.feature.scanner.presentation.ScannerImport
import com.aozijx.passly.feature.scanner.presentation.ScannerImportHandlingResult
import com.aozijx.passly.feature.scanner.presentation.ScannerPurpose
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.feature.vault.presentation.editor.ui.wifi.AddWifiEditorScreen

@Composable
fun AddWifiEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    loadInitialContent: () -> AddWifiInitialContent = { AddWifiInitialContent.Blank },
) {
    val viewModel = hiltViewModel<AddWifiViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_wifi_save_failed)
    val seedUnavailableMessage = stringResource(R.string.wifi_scanned_seed_unavailable)
    val saveActionModifier = Modifier.rememberAddEntryFabTransitionModifier(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    var overlay by remember { mutableStateOf<WifiEditorOverlay>(WifiEditorOverlay.None) }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    LaunchedEffect(viewModel) {
        if (viewModel.initialize(loadInitialContent) == AddWifiInitializationResult.Unavailable) {
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
                        if (viewModel.tryApplyScannedWifi(scannerImport.payload)) {
                            ScannerImportHandlingResult.Applied
                        } else ScannerImportHandlingResult.Rejected
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
