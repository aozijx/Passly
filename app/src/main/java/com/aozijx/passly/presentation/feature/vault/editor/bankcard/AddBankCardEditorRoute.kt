package com.aozijx.passly.presentation.feature.vault.editor.bankcard

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
import com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard.AddBankCardEditorScreen

@Composable
fun AddBankCardEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: AddBankCardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_bank_card_save_failed)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }
    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)

    AddBankCardEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )
}
