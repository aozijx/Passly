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
import com.aozijx.passly.domain.entry.policy.WifiQrParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadParser
import com.aozijx.passly.feature.scanner.presentation.result.ScannerPrimaryActionLabel
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultHandling
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultPlan
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.route.AnimatedScannerOverlay
import com.aozijx.passly.feature.scanner.presentation.state.ScannerOverlayState
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadFailureEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.feature.vault.presentation.editor.ui.wifi.AddWifiEditorScreen

@Composable
fun AddWifiEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    loadInitialContent: () -> AddWifiInitialContent = { AddWifiInitialContent.Blank },
    editSeedToken: String? = null,
    consumeEditSeed: ((String) -> EntryEditorSeedConsumeResult)? = null,
) {
    val viewModel = hiltViewModel<AddWifiViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_wifi_save_failed)
    val loadFailedMessage = stringResource(R.string.vault_edit_entry_load_failed)
    val seedUnavailableMessage = stringResource(R.string.wifi_scanned_seed_unavailable)
    val saveActionModifier =
        Modifier.rememberAddEntryFabTransitionModifier(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    var scannerOverlayState by remember { mutableStateOf<ScannerOverlayState>(ScannerOverlayState.Hidden) }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    EditorLoadFailureEffectHandler(uiState.loadPhase, editSeedToken != null, snackbarHostState, loadFailedMessage)
    LaunchedEffect(viewModel, editSeedToken) {
        if (editSeedToken != null) {
            viewModel.loadExistingEntry(consumeEditSeed?.invoke(editSeedToken) ?: EntryEditorSeedConsumeResult.Missing)
        } else {
            when (viewModel.beginInitialization()) {
                AddWifiInitializationGate.NeedsInitialContent -> {
                    if (viewModel.initialize(loadInitialContent()) == AddWifiInitializationResult.Unavailable) {
                        snackbarHostState.showSnackbar(seedUnavailableMessage)
                    }
                }

                AddWifiInitializationGate.Unavailable -> {
                    snackbarHostState.showSnackbar(seedUnavailableMessage)
                }

                AddWifiInitializationGate.AlreadyInitialized,
                -> {
                    Unit
                }
            }
        }
    }
    AddWifiEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onScan = { scannerOverlayState = ScannerOverlayState.Visible },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
        isEditingExisting = editSeedToken != null,
    )

    AnimatedScannerOverlay(
        state = scannerOverlayState,
        resolveResult = { rawValue ->
            val payload = (WifiQrPayloadParser.parse(rawValue) as? WifiQrParseResult.Parsed)?.payload
            val presentation =
                if (payload != null) {
                    ScannerResultPresentation(primaryActionLabel = ScannerPrimaryActionLabel.Use)
                } else {
                    ScannerResultPresentation.DEFAULT
                }
            ScannerResultPlan(
                presentation = presentation,
                confirm = {
                    if (payload != null && viewModel.tryApplyScannedWifi(payload)) {
                        ScannerResultHandling.Handled
                    } else {
                        ScannerResultHandling.Rejected
                    }
                },
            )
        },
        onDismiss = { scannerOverlayState = ScannerOverlayState.Hidden },
    )
}
