package com.aozijx.passly.presentation.feature.vault.editor.wifi

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffectHandler
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.presentation.feature.vault.editor.ui.wifi.AddWifiEditorScreen

@Composable
fun AddWifiEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val viewModel = hiltViewModel<AddWifiViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_wifi_save_failed)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)

    AddWifiEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )
}
