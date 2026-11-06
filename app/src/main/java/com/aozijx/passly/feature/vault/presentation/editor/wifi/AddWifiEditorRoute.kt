package com.aozijx.passly.feature.vault.presentation.editor.wifi

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
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
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.scanner.presentation.ScannerResultHandling
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
    editEntryId: String? = null,
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
    var overlay by remember { mutableStateOf<WifiEditorOverlay>(WifiEditorOverlay.None) }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    LaunchedEffect(viewModel, editEntryId) {
        if (editEntryId != null) {
            viewModel.loadExistingEntry(editEntryId)
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
    LaunchedEffect(uiState.loadFailed, editEntryId) {
        if (editEntryId != null && uiState.loadFailed) {
            snackbarHostState.showSnackbar(loadFailedMessage)
        }
    }

    AddWifiEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onScan = { overlay = WifiEditorOverlay.Scanner },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
        isEditingExisting = editEntryId != null,
    )

    AnimatedVisibility(
        visible = overlay == WifiEditorOverlay.Scanner,
        enter =
            fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it },
                ),
        exit =
            fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { it },
                ),
    ) {
        ScannerRoute(
            onResult = { rawValue ->
                val payload = (WifiQrPayloadParser.parse(rawValue) as? WifiQrParseResult.Parsed)?.payload
                if (payload != null && viewModel.tryApplyScannedWifi(payload)) {
                    ScannerResultHandling.Handled
                } else {
                    ScannerResultHandling.Rejected
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
